package dev.clipboardhud.create;

import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import dev.clipboardhud.testing.VanillaBootstrap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Create 6.0.8 stores the document as NBT on ClipboardBlockEntity.dataContainer. Create blocks cannot be
 * registered in plain JUnit, so a vanilla stack carries the same tag, written by Create's own ClipboardEntry.saveAll.
 */
class CreateClipboardReaderTest {
    @BeforeAll
    static void bootstrap() { VanillaBootstrap.ensure(); }

    @Test
    void missingDataIsNotDeclaredEmptyAndDoesNotKeepOldContent() {
        var reader = new CreateClipboardReader();
        var ready = reader.read(clipboard(List.of(List.of(entry()))));
        assertEquals(CreateClipboardReader.Status.READY, ready.status());
        assertTrue(ready.hasContent());
        var unavailable = reader.read(new ItemStack(Items.PAPER));
        assertEquals(CreateClipboardReader.Status.DATA_UNAVAILABLE, unavailable.status());
        assertFalse(unavailable.hasContent());
        assertFalse(CreateClipboardReader.Result.error().hasContent());
        assertTrue(unavailable.document().isEmpty());
    }

    @Test
    void confirmedEmptyContentPreservesBlankPages() {
        var result = new CreateClipboardReader().read(clipboard(List.of(List.of(), List.of())));
        assertEquals(CreateClipboardReader.Status.EMPTY, result.status());
        assertFalse(result.hasContent());
        assertEquals(2, result.document().orElseThrow().pages().size());
    }

    @Test
    void unchangedContentReusesSnapshotButEntryEditInvalidatesIt() {
        var reader = new CreateClipboardReader();
        var original = entry().displayItem(new ItemStack(Items.IRON_INGOT), 128);
        var pages = List.of(List.of(original));
        var stack = clipboard(pages);
        var before = reader.read(stack);
        assertSame(before, reader.read(stack));
        // Create's screen saves its reading page into the same stack; the document itself is unchanged.
        stack.getOrCreateTag().putInt("PreviouslyOpenedPage", 3);
        assertSame(before, reader.read(stack));
        // Create's screen writes edits into the same stack and tag; only the content tells the change.
        original.checked = true;
        ClipboardEntry.saveAll(pages, stack);
        var after = reader.read(stack);
        assertNotSame(before, after);
        assertFalse(before.document().orElseThrow().pages().get(0).get(0).checked());
        assertTrue(after.document().orElseThrow().pages().get(0).get(0).checked());
    }

    @Test
    void snapshotDetachesPagesNestedTextAndIconsInBothDirections() {
        var suffix = Component.literal(" nested");
        var icon = new ItemStack(Items.IRON_INGOT, 2);
        icon.setHoverName(Component.literal("named item"));
        var original = new ClipboardEntry(false, Component.literal("원문").append(suffix)).displayItem(icon, 128);
        var entries = new ArrayList<>(List.of(original));
        var pages = new ArrayList<List<ClipboardEntry>>(List.of(entries));
        var snapshot = new ClipboardDocumentSnapshot(pages);
        var saved = snapshot.pages().get(0).get(0);
        suffix.append(" changed");
        icon.setHoverName(Component.literal("renamed"));
        icon.setCount(9);
        entries.clear();
        pages.clear();
        assertEquals("원문 nested", saved.text().getString());
        assertEquals(2, saved.icon().getCount());
        assertEquals("named item", saved.icon().getHoverName().getString());
        var exposedText = saved.text();
        ((MutableComponent) exposedText.getSiblings().get(0)).append(" caller");
        var exposedIcon = saved.icon();
        exposedIcon.setHoverName(Component.literal("caller"));
        exposedIcon.setCount(5);
        assertEquals("원문 nested", saved.text().getString());
        assertEquals("named item", saved.icon().getHoverName().getString());
        assertEquals(2, saved.icon().getCount());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.pages().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.pages().get(0).clear());
    }

    @Test
    void readDoesNotChangeTheClipboardStackTextChecksIconsOrSharedPage() {
        var original = entry().displayItem(new ItemStack(Items.IRON_INGOT), 128);
        var stack = clipboard(List.of(List.of(original)));
        stack.getOrCreateTag().putInt("PreviouslyOpenedPage", 3);
        stack.getOrCreateTag().putBoolean("Readonly", true);
        CompoundTag tag = stack.getTag();
        CompoundTag before = tag.copy();
        new CreateClipboardReader().read(stack);
        assertSame(tag, stack.getTag());
        assertEquals(before, stack.getTag());
        assertEquals(3, stack.getTag().getInt("PreviouslyOpenedPage"));
        assertTrue(stack.getTag().getBoolean("Readonly"));
        assertEquals(1, stack.getCount());
    }

    @Test
    void orderCheckStyleAndItemTagsArePartOfChangeDetection() {
        var reader = new CreateClipboardReader();
        var first = entry();
        var second = new ClipboardEntry(true, Component.literal("second"));
        var stack = clipboard(List.of(List.of(first, second)));
        var initial = reader.read(stack);
        assertSame(initial, reader.read(stack));
        ClipboardEntry.saveAll(List.of(List.of(second, first)), stack);
        var reordered = reader.read(stack);
        assertNotSame(initial, reordered);
        assertEquals("second", reordered.document().orElseThrow().pages().get(0).get(0).text().getString());
        first.checked = true;
        ClipboardEntry.saveAll(List.of(List.of(second, first)), stack);
        var checked = reader.read(stack);
        assertNotSame(reordered, checked);
        first.text.withStyle(ChatFormatting.RED);
        ClipboardEntry.saveAll(List.of(List.of(second, first)), stack);
        var styled = reader.read(stack);
        assertNotSame(checked, styled);
        first.displayItem(new ItemStack(Items.IRON_INGOT), 1);
        ClipboardEntry.saveAll(List.of(List.of(second, first)), stack);
        var withIcon = reader.read(stack);
        assertNotSame(styled, withIcon);
        first.icon.setHoverName(Component.literal("custom"));
        ClipboardEntry.saveAll(List.of(List.of(second, first)), stack);
        assertNotSame(withIcon, reader.read(stack));
    }

    @Test
    void addressTextAndStoredPageOrderAreKeptExactly() {
        var snapshot = new CreateClipboardReader().read(clipboard(List.of(List.of(entry()), List.of())))
                .document().orElseThrow();
        assertEquals("# 배송 주소", snapshot.pages().get(0).get(0).text().getString());
        assertTrue(snapshot.pages().get(1).isEmpty());
    }

    private ClipboardEntry entry() { return new ClipboardEntry(false, Component.literal("# 배송 주소")); }

    /** A stack carrying Create's clipboard tag (Pages → Entries), as on ClipboardBlockEntity.dataContainer. */
    private static ItemStack clipboard(List<List<ClipboardEntry>> pages) {
        var stack = new ItemStack(Items.PAPER);
        ClipboardEntry.saveAll(pages, stack);
        return stack;
    }
}
