package dev.clipboardhud;

import com.mojang.logging.LogUtils;
import dev.clipboardhud.client.ClipboardPreviewController;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/** Client-only bootstrap: registers the HUD overlay, the page key binding and the event listeners.
 * Forge 1.20.1 has no client-only @Mod dist, so a dedicated server constructs this class and does nothing;
 * client classes are only loaded through {@link Client}. Uses Forge 47.1 APIs only (no constructor injection),
 * so the same JAR also runs on NeoForge 1.20.1.
 */
@Mod(ClipboardHudClient.MOD_ID)
public final class ClipboardHudClient {
    public static final String MOD_ID = "create_clipboard_hud";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ClipboardHudClient() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            LOGGER.info("[{}] Client-only mod; nothing to do on a dedicated server.", MOD_ID);
            return;
        }
        Client.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    private static final class Client {
        private final ClipboardPreviewController preview = new ClipboardPreviewController();

        static void register(IEventBus modBus) {
            var client = new Client();
            modBus.addListener(client::onClientSetup);
            modBus.addListener(client::registerGuiOverlays);
            modBus.addListener(client::registerReloadListeners);
            modBus.addListener(client.preview::registerKeyMappings);
            MinecraftForge.EVENT_BUS.addListener(client.preview::tick);
            MinecraftForge.EVENT_BUS.addListener(client.preview::scroll);
            MinecraftForge.EVENT_BUS.addListener(client.preview::screenOpening);
            MinecraftForge.EVENT_BUS.addListener(client.preview::screenClosing);
        }

        private void onClientSetup(FMLClientSetupEvent event) {
            var createVersion = ModList.get().getModContainerById("create")
                    .orElseThrow(() -> new IllegalStateException("Required Create dependency was not loaded"))
                    .getModInfo().getVersion();
            LOGGER.info("[{}] Read-only clipboard preview ready; Create {}. Personal session pages and configurable modifier-wheel navigation.",
                    MOD_ID, createVersion);
        }

        private void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            // Registered for this mod's namespace: create_clipboard_hud:clipboard_preview.
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "clipboard_preview", preview::render);
        }

        private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) resources -> preview.invalidateLayout());
        }
    }
}
