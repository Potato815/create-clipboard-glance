package dev.clipboardhud.testing;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/** Vanilla registries for tests that create ItemStacks (Create's ClipboardEntry holds one).
 * Plain JUnit: Forge and Create are not loaded, so Create blocks and items are unavailable.
 */
public final class VanillaBootstrap {
    private VanillaBootstrap() {}

    public static synchronized void ensure() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
}
