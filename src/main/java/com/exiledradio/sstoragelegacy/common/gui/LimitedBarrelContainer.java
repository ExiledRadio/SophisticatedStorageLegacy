/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.common.gui;

import com.exiledradio.sstoragelegacy.block.LimitedBarrelTileEntity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Collections;
import java.util.List;

/** A limited barrel's GUI: its few big slots laid out like the front (the original's LimitedBarrelContainerMenu). */
public class LimitedBarrelContainer extends StorageBlockContainer {
    public LimitedBarrelContainer(EntityPlayer player, LimitedBarrelTileEntity tile) {
        super(player, tile);
    }

    @Override
    protected int[] getStorageSlotPosition(int slotIndex) {
        return LimitedBarrelLayout.getSlotPosition(slotIndex, getStorageWrapper().getNumberOfInventorySlots());
    }

    @Override
    public int getStorageInventoryHeight() {
        return LimitedBarrelLayout.STORAGE_SLOTS_HEIGHT;
    }

    /** Every slot is shown on the front anyway; no colour stripes, as in the original. */
    @Override
    public List<Integer> getSlotOverlayColors(int slot) {
        return Collections.emptyList();
    }
}
