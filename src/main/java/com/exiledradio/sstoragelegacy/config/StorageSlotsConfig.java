/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.config;

import net.minecraftforge.common.config.Config;

/** Slots of one storage type and tier (the original's StorageConfig). */
public class StorageSlotsConfig {
    @Config.Comment("Number of inventory slots in the storage")
    @Config.RangeInt(min = 1, max = 180)
    public int inventorySlotCount;

    @Config.Comment("Number of upgrade slots in the storage")
    @Config.RangeInt(min = 0, max = 10)
    public int upgradeSlotCount;

    public StorageSlotsConfig(int inventorySlotCount, int upgradeSlotCount) {
        this.inventorySlotCount = inventorySlotCount;
        this.upgradeSlotCount = upgradeSlotCount;
    }
}
