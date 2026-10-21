/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.common.gui;

import com.exiledradio.scorelegacy.common.gui.SettingsContainerBase;
import com.exiledradio.sstoragelegacy.block.StorageTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nonnull;

/** The settings screen of a barrel (memory, no-sort, item display...). */
public class StorageBlockSettingsContainer extends SettingsContainerBase {
    private final StorageTileEntity tile;

    public StorageBlockSettingsContainer(EntityPlayer player, StorageTileEntity tile) {
        super(player, tile.getStorageWrapper());
        this.tile = tile;
    }

    public StorageTileEntity getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        return !tile.isInvalid() && tile.getWorld().getTileEntity(tile.getPos()) == tile
                && player.getDistanceSq(tile.getPos().getX() + 0.5D, tile.getPos().getY() + 0.5D, tile.getPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public String getStorageTitle() {
        return tile.getDisplayName().getFormattedText().trim();
    }

    @Override
    protected void openStorageGuiAgain(EntityPlayerMP playerMP) {
        GuiHandler.openStorage(playerMP, tile.getPos());
    }
}
