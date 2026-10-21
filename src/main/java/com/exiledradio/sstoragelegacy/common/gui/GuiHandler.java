package com.exiledradio.sstoragelegacy.common.gui;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelTileEntity;
import com.exiledradio.sstoragelegacy.block.StorageTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/**
 * GUI ids. Screens are returned as Object and built by the client proxy, so this class
 * names no client type and is safe to load on a dedicated server.
 */
public class GuiHandler implements IGuiHandler {
    public static final int STORAGE = 0;
    public static final int STORAGE_SETTINGS = 1;
    public static final int DECORATION_TABLE = 2;

    public static void openStorage(EntityPlayer player, BlockPos pos) {
        player.openGui(SophisticatedStorageLegacy.instance, STORAGE, player.world, pos.getX(), pos.getY(), pos.getZ());
    }

    public static void openStorageSettings(EntityPlayer player, BlockPos pos) {
        player.openGui(SophisticatedStorageLegacy.instance, STORAGE_SETTINGS, player.world, pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == DECORATION_TABLE && te instanceof com.exiledradio.sstoragelegacy.block.DecorationTableTileEntity) {
            return new DecorationTableContainer(player, (com.exiledradio.sstoragelegacy.block.DecorationTableTileEntity) te);
        }
        if (!(te instanceof StorageTileEntity)) {
            return null;
        }
        if (id == STORAGE) {
            return te instanceof LimitedBarrelTileEntity ? new LimitedBarrelContainer(player, (LimitedBarrelTileEntity) te)
                    : new StorageBlockContainer(player, (StorageTileEntity) te);
        }
        if (id == STORAGE_SETTINGS) {
            return te instanceof LimitedBarrelTileEntity ? new LimitedBarrelSettingsContainer(player, (LimitedBarrelTileEntity) te)
                    : new StorageBlockSettingsContainer(player, (StorageTileEntity) te);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        Object container = getServerGuiElement(id, player, world, x, y, z);
        return container == null ? null : SophisticatedStorageLegacy.proxy.createGui(container);
    }
}
