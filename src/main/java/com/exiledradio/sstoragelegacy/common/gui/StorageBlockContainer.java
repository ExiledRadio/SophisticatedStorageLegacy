/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.common.gui;

import com.exiledradio.scorelegacy.common.gui.StorageContainerBase;
import com.exiledradio.sstoragelegacy.block.StorageTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

/** The barrel GUI: Core's storage container (upgrade slots, tabs, sorting) for a placed barrel. */
public class StorageBlockContainer extends StorageContainerBase {
    private final StorageTileEntity tile;

    public StorageBlockContainer(EntityPlayer player, StorageTileEntity tile) {
        super(player, tile.getStorageWrapper(), -1);
        this.tile = tile;
        tile.startOpen(player);
    }

    public StorageTileEntity getTile() {
        return tile;
    }

    /**
     * Closes the GUI as soon as the barrel is broken or replaced, so nothing can be moved into
     * an inventory that no longer exists.
     */
    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        return !tile.isInvalid() && tile.getWorld().getTileEntity(tile.getPos()) == tile
                && player.getDistanceSq(tile.getPos().getX() + 0.5D, tile.getPos().getY() + 0.5D, tile.getPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public BlockPos getStorageBlockPos() {
        return tile.getPos();
    }

    @Override
    public String getStorageTitle() {
        return tile.getDisplayName().getFormattedText().trim();
    }

    @Override
    protected void openGuiAgain(EntityPlayerMP playerMP) {
        GuiHandler.openStorage(playerMP, tile.getPos());
    }

    @Override
    protected void openSettingsGui(EntityPlayerMP playerMP) {
        GuiHandler.openStorageSettings(playerMP, tile.getPos());
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        tile.stopOpen(player);
    }
}
