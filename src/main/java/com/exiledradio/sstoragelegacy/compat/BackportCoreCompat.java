package com.exiledradio.sstoragelegacy.compat;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import net.minecraftforge.fml.common.Loader;

/**
 * Puts this mod's settings on Backport Core's shared settings screen when Core is installed. Soft: only
 * reflection, so the mod runs without Core, and an older Core without the settings package is ignored.
 */
public final class BackportCoreCompat {

    private BackportCoreCompat() {}

    /** Call in pre-init. */
    public static void registerSettings() {
        if (!Loader.isModLoaded("backportcore")) {
            return;
        }
        try {
            Class.forName("com.exiledradio.backportcore.settings.BackportSettings")
                    .getMethod("register", String.class, Class[].class)
                    .invoke(null, SophisticatedStorageLegacy.MODID, new Class<?>[0]);
        } catch (Throwable t) {
            SophisticatedStorageLegacy.LOGGER.debug("Backport Core has no settings screen; using Forge's config screen", t);
        }
    }
}
