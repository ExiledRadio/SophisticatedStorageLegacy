package com.exiledradio.sstoragelegacy;

import com.exiledradio.sstoragelegacy.common.CommonEventHandler;
import com.exiledradio.sstoragelegacy.common.gui.GuiHandler;
import com.exiledradio.sstoragelegacy.network.StoragePacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;

/** Setup shared by both sides. Also the proxy on a dedicated server. */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        StoragePacketHandler.init();
        MinecraftForge.EVENT_BUS.register(new CommonEventHandler());
    }

    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(SophisticatedStorageLegacy.instance, new GuiHandler());
        // JEI's "+" button fills an open Crafting Upgrade tab in the barrel GUI too.
        com.exiledradio.scorelegacy.compat.jei.StorageContainerClasses.register(com.exiledradio.sstoragelegacy.common.gui.StorageBlockContainer.class);
    }

    /** The client screen for a container. Never called on a dedicated server. */
    @Nullable
    public Object createGui(Object container) {
        return null;
    }
}
