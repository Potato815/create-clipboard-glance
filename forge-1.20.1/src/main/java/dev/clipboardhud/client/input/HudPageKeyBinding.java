package dev.clipboardhud.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Normal Controls-menu binding for a held modifier. Its labels are lang entries. */
public final class HudPageKeyBinding {
    private KeyMapping modifier;
    public void register(RegisterKeyMappingsEvent event) {
        modifier = new KeyMapping("key.create_clipboard_hud.page_modifier", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories.create_clipboard_hud");
        event.register(modifier);
    }
    public boolean held(Minecraft minecraft) {
        if (modifier == null || !minecraft.isWindowActive() || !modifier.isConflictContextAndModifierActive()) return false;
        if (modifier.getKey().equals(InputConstants.UNKNOWN)) return false;
        if (modifier.isDown()) return true;
        // Also accept Right Ctrl while the binding remains at default Left Ctrl.
        return modifier.getKey().equals(modifier.getDefaultKey())
                && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}
