/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.network;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** Storage's own network channel (Core has its own for the GUI). */
public final class StoragePacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(SophisticatedStorageLegacy.MODID);

    private StoragePacketHandler() {}

    public static void init() {
        int id = 0;
        INSTANCE.registerMessage(ScrolledToolMessage.Handler.class, ScrolledToolMessage.class, id++, Side.SERVER);
        INSTANCE.registerMessage(StashMessage.Handler.class, StashMessage.class, id++, Side.SERVER);
        INSTANCE.registerMessage(DecorationTableMessage.Handler.class, DecorationTableMessage.class, id++, Side.SERVER);
    }
}
