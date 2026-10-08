package dev.clipboardhud;

import com.mojang.logging.LogUtils;
import dev.clipboardhud.client.ClipboardPreviewController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/** Client-only bootstrap: registers the HUD layer, the page key binding and the event listeners. */
@Mod(value = ClipboardHudClient.MOD_ID, dist = Dist.CLIENT)
public final class ClipboardHudClient {
    public static final String MOD_ID = "create_clipboard_hud";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ClipboardPreviewController preview = new ClipboardPreviewController();

    public ClipboardHudClient(IEventBus modBus) {
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::registerGuiLayers);
        modBus.addListener(this::registerReloadListeners);
        modBus.addListener(preview::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(preview::tick);
        NeoForge.EVENT_BUS.addListener(preview::scroll);
        NeoForge.EVENT_BUS.addListener(preview::screenOpening);
        NeoForge.EVENT_BUS.addListener(preview::screenClosing);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        var createVersion = ModList.get().getModContainerById("create")
                .orElseThrow(() -> new IllegalStateException("Required Create dependency was not loaded"))
                .getModInfo().getVersion();
        LOGGER.info("[{}] Read-only clipboard preview ready; Create {}. Personal session pages and configurable modifier-wheel navigation.",
                MOD_ID, createVersion);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "clipboard_preview"), preview::render);
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resources -> preview.invalidateLayout());
    }
}
