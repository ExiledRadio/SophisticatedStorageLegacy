/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.common.gui;

import com.exiledradio.scorelegacy.settings.itemdisplay.ItemDisplaySettingsCategory;
import com.exiledradio.scorelegacy.settings.nosort.NoSortSettingsCategory;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelTileEntity;
import net.minecraft.entity.player.EntityPlayer;

/** A limited barrel's settings: every slot is always displayed and never sorted, so those tabs are gone. */
public class LimitedBarrelSettingsContainer extends StorageBlockSettingsContainer {
    public LimitedBarrelSettingsContainer(EntityPlayer player, LimitedBarrelTileEntity tile) {
        super(player, tile);
    }

    @Override
    protected int[] getStorageSlotPosition(int slot) {
        return LimitedBarrelLayout.getSlotPosition(slot, getStorageWrapper().getNumberOfInventorySlots());
    }

    @Override
    public int getStorageInventoryHeight() {
        return LimitedBarrelLayout.STORAGE_SLOTS_HEIGHT;
    }

    @Override
    public boolean isSettingsCategoryDisabled(String categoryName) {
        return categoryName.equals(ItemDisplaySettingsCategory.NAME) || categoryName.equals(NoSortSettingsCategory.NAME);
    }
}
