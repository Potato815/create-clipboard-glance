package dev.clipboardhud.client;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.equipment.clipboard.ClipboardBlockEntity;
import com.simibubi.create.content.equipment.clipboard.ClipboardScreen;
import dev.clipboardhud.client.input.HudPageInputPolicy;
import dev.clipboardhud.client.input.HudPageKeyBinding;
import dev.clipboardhud.client.reading.ClipboardScreenStartPage;
import dev.clipboardhud.client.reading.PersonalPagePositions;
import dev.clipboardhud.client.render.CreateGoggleClipboardOverlay;
import dev.clipboardhud.create.CreateClipboardReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

/** Connects the game to the HUD: finds the aimed clipboard, reads it, draws it, turns personal pages on the wheel,
 * and links Create's own clipboard screen to the personal page. One aimed clipboard plus bounded personal
 * positions for the current client level. Never writes the document.
 */
public final class ClipboardPreviewController {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** A failed read or draw is retried after this delay instead of every frame. */
    private static final long RETRY_NANOS = 1_000_000_000L;
    private static final long WARNING_INTERVAL_NANOS = 5_000_000_000L;
    /** The HUD counts as visible to the wheel and to the opened screen only if it was drawn this recently. */
    private static final long RECENTLY_SHOWN_NANOS = 500_000_000L;
    /** Create 6.0.8 clipboard item tags. */
    private static final String PAGES = "Pages";
    private static final String PREVIOUSLY_OPENED_PAGE = "PreviouslyOpenedPage";

    private final CreateClipboardReader reader = new CreateClipboardReader();
    private final PersonalPagePositions<ClipboardBlockEntity> positions = new PersonalPagePositions<>();
    private final HudPageKeyBinding pageKey = new HudPageKeyBinding();
    /** Presentation state of the shown target; null until drawn, replaced after a target change or failure. */
    private CreateGoggleClipboardOverlay overlay;
    private ClientLevel readingLevel;
    private ClientLevel targetLevel;
    private ClipboardBlockEntity target;
    private CreateClipboardReader.Result result = CreateClipboardReader.Result.unavailable();
    private long nextReadNanos;
    private long nextRenderNanos;
    private long nextWarningNanos;
    private ClipboardBlockEntity lastRenderedTarget;
    private long lastRenderedNanos;

    public void registerKeyMappings(RegisterKeyMappingsEvent event) { pageKey.register(event); }

    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        updateReadingSession(minecraft.level);
        var aimed = aimedClipboard(minecraft);
        if (aimed == null) {
            clear();
            return;
        }
        if (targetLevel != minecraft.level || target != aimed) {
            clear();
            targetLevel = minecraft.level;
            target = aimed;
        }
        long now = System.nanoTime();
        if (now < nextReadNanos) return;
        try {
            result = reader.read(aimed.dataContainer);
        } catch (RuntimeException failure) {
            reader.clear();
            result = CreateClipboardReader.Result.error();
            nextReadNanos = now + RETRY_NANOS;
            warn(failure);
        }
    }

    /** Forge IGuiOverlay; partialTick drives Create's goggle entrance like Create's own overlay. */
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        var minecraft = Minecraft.getInstance();
        // hitResult can change between ticks. Never draw the old clipboard over a new target.
        if (target == null || targetLevel != minecraft.level || aimedClipboard(minecraft) != target) return;
        if (!result.hasContent()) {
            // Nothing is drawn; once entries appear the HUD enters fresh, and the wheel stays with the hotbar.
            overlay = null;
            lastRenderedTarget = null;
            return;
        }
        if (System.nanoTime() < nextRenderNanos) return;
        try {
            if (overlay == null) overlay = new CreateGoggleClipboardOverlay();
            if (overlay.render(graphics, minecraft, partialTick, result, positions.current(target, pageCount()))) {
                lastRenderedTarget = target;
                lastRenderedNanos = System.nanoTime();
            } else {
                lastRenderedTarget = null;
            }
        } catch (RuntimeException failure) {
            lastRenderedTarget = null;
            overlay = null;
            nextRenderNanos = System.nanoTime() + RETRY_NANOS;
            warn(failure);
        }
    }

    public void scroll(InputEvent.MouseScrollingEvent event) {
        var minecraft = Minecraft.getInstance();
        int pages = pageCount();
        boolean visible = target != null && targetLevel == minecraft.level && minecraft.isWindowActive()
                && shownRecently(target) && aimedClipboard(minecraft) == target && System.nanoTime() >= nextRenderNanos;
        if (!HudPageInputPolicy.handles(visible, pageKey.held(minecraft), pages, event.getScrollDelta())) return;
        int step = HudPageInputPolicy.step(event.getScrollDelta());
        int before = positions.current(target, pages);
        positions.move(target, step, pages);
        // At the first/last page the wheel is still consumed; a small nudge shows it was received.
        if (positions.current(target, pages) == before && overlay != null) overlay.nudge(step);
        // Consume at the first/last page too; navigation never changes the hotbar unexpectedly.
        event.setCanceled(true);
    }

    public void screenOpening(ScreenEvent.Opening event) {
        var minecraft = Minecraft.getInstance();
        if (!(event.getNewScreen() instanceof ClipboardScreen screen) || minecraft.level == null
                || screen.targetedBlock == null || screen.item == null) return;
        try {
            var opened = loadedClipboard(minecraft.level, screen.targetedBlock);
            if (opened == null) return;
            updateReadingSession(minecraft.level);
            // The HUD page (shown just before the click, or remembered) continues in Create's screen.
            boolean personal = positions.has(opened) || shownRecently(opened);
            // In Create 6.0.8 the screen holds the block entity's own stack and writes into it on close.
            ItemStack shared = screen.item;
            var tag = shared.getTag();
            int pageCount = tag == null ? 0 : tag.getList(PAGES, Tag.TAG_COMPOUND).size();
            int createPage = tag == null ? 0 : tag.getInt(PREVIOUSLY_OPENED_PAGE);
            int start = ClipboardScreenStartPage.choose(personal, positions.current(opened, pageCount), createPage, pageCount);
            if (start != createPage) {
                // Before init(), Create's public reopenWith adopts this copy's page. The block entity's stack is
                // untouched; Create's own close/edit packet still saves the edits and the page the player ends on,
                // and the server sync then updates the block entity.
                ItemStack copy = shared.copy();
                copy.getOrCreateTag().putInt(PREVIOUSLY_OPENED_PAGE, start);
                screen.reopenWith(copy);
            }
        } catch (RuntimeException failure) { warn(failure); }
    }

    public void screenClosing(ScreenEvent.Closing event) {
        var minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof ClipboardScreen screen) || minecraft.level == null || screen.targetedBlock == null) return;
        ClientLevel closingLevel = minecraft.level;
        BlockPos closedPos = screen.targetedBlock.immutable();
        ClipboardBlockEntity closedTarget = loadedClipboard(closingLevel, closedPos);
        if (closedTarget == null) return;
        // Closing fires before Create.removed(). tell() queues rather than running immediately on this thread.
        minecraft.tell(() -> {
            try {
                if (minecraft.level != closingLevel || minecraft.screen == screen || closedTarget.isRemoved()
                        || loadedClipboard(closingLevel, closedPos) != closedTarget) return;
                updateReadingSession(closingLevel);
                // Create.removed() has written the page the player ends on into the screen's stack.
                var tag = screen.item == null ? null : screen.item.getTag();
                if (tag != null) {
                    positions.remember(closedTarget, tag.getInt(PREVIOUSLY_OPENED_PAGE), tag.getList(PAGES, Tag.TAG_COMPOUND).size());
                }
            } catch (RuntimeException failure) { warn(failure); }
        });
    }

    /** Resources (fonts, language, textures) changed: rebuild the layout on the next frame. */
    public void invalidateLayout() {
        overlay = null;
        nextRenderNanos = 0;
    }

    private int pageCount() { return result.document().map(document -> document.pages().size()).orElse(0); }

    private boolean shownRecently(ClipboardBlockEntity clipboard) {
        return lastRenderedTarget == clipboard && System.nanoTime() - lastRenderedNanos < RECENTLY_SHOWN_NANOS;
    }

    /** Personal positions only last for one client level. */
    private void updateReadingSession(ClientLevel current) {
        if (readingLevel == current) return;
        positions.clear();
        readingLevel = current;
    }

    private ClipboardBlockEntity aimedClipboard(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null || minecraft.getOverlay() != null
                || minecraft.options.hideGui || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return null;
        double reach = minecraft.player.getBlockReach();
        if (minecraft.player.getEyePosition().distanceToSqr(hit.getLocation()) > reach * reach) return null;
        return loadedClipboard(minecraft.level, hit.getBlockPos());
    }

    /** Only already loaded chunks are inspected; the HUD never loads chunks or searches for blocks. */
    private static ClipboardBlockEntity loadedClipboard(ClientLevel world, BlockPos pos) {
        if (!world.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return null;
        var entity = world.getBlockEntity(pos);
        return entity instanceof ClipboardBlockEntity clipboard && !clipboard.isRemoved() ? clipboard : null;
    }

    private void clear() {
        if (target == null && targetLevel == null) return;
        targetLevel = null;
        target = null;
        lastRenderedTarget = null;
        lastRenderedNanos = 0;
        reader.clear();
        result = CreateClipboardReader.Result.unavailable();
        nextReadNanos = 0;
        nextRenderNanos = 0;
        overlay = null;
    }

    private void warn(RuntimeException failure) {
        long now = System.nanoTime();
        if (now >= nextWarningNanos) {
            nextWarningNanos = now + WARNING_INTERVAL_NANOS;
            LOGGER.warn("[create_clipboard_hud] Clipboard preview failed; original clipboard is untouched", failure);
        }
    }
}
