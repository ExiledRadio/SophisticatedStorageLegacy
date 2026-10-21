/*
 * Sophisticated Storage Legacy - an unofficial 1.12.2 port of Sophisticated Storage.
 * Original mod (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio, also GPLv3.
 */
package com.exiledradio.sstoragelegacy;

import com.exiledradio.scorelegacy.SophisticatedCoreLegacy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = SophisticatedStorageLegacy.MODID,
        name = SophisticatedStorageLegacy.NAME,
        version = SophisticatedStorageLegacy.VERSION,
        dependencies = "required-after:" + SophisticatedCoreLegacy.MODID + "@[1.1.0,)"
)
public class SophisticatedStorageLegacy {

    public static final String MODID = "sstoragelegacy";
    public static final String NAME = "Sophisticated Storage Legacy";
    // Replaced at build time by ForgeGradle from mod_version in gradle.properties.
    // Shows literally as "@VERSION@" in IDE dev runs; that is expected.
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static SophisticatedStorageLegacy instance;

    // Client-only code lives behind ClientProxy so that no client class is reachable from
    // common code: @SideOnly on methods doesn't stop a dedicated server from verifying them.
    @SidedProxy(
            clientSide = "com.exiledradio.sstoragelegacy.client.ClientProxy",
            serverSide = "com.exiledradio.sstoragelegacy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        com.exiledradio.scorelegacy.util.RecipeHelper.addAlwaysCompactedSource(() -> ModConfig.upgrades.alwaysCompactedItems);
        com.exiledradio.scorelegacy.api.UpgradeLimits.register(com.exiledradio.sstoragelegacy.config.UpgradeLimitsConfig.INSTANCE);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(net.minecraftforge.fml.common.event.FMLPostInitializationEvent event) {
        com.exiledradio.scorelegacy.compat.MaterialCompat.loadCompatRecipes(MODID);
    }

    @Mod.EventHandler
    public void serverStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
        if (Boolean.TRUE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment"))) {
            event.registerServerCommand(new com.exiledradio.sstoragelegacy.common.DevCommand());
        }
    }
}
