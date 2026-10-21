/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.config;

import net.minecraftforge.common.config.Config;

/** One limited barrel tier and slot count (the original's LimitedBarrelConfig). */
public class LimitedBarrelConfig {
    @Config.Comment("Multiplier that's used to calculate base slot limit")
    @Config.RangeInt(min = 1, max = 8192)
    public int baseSlotLimitMultiplier;

    @Config.Comment("Number of upgrade slots in the storage")
    @Config.RangeInt(min = 0, max = 10)
    public int upgradeSlotCount;

    public LimitedBarrelConfig(int baseSlotLimitMultiplier, int upgradeSlotCount) {
        this.baseSlotLimitMultiplier = baseSlotLimitMultiplier;
        this.upgradeSlotCount = upgradeSlotCount;
    }
}
