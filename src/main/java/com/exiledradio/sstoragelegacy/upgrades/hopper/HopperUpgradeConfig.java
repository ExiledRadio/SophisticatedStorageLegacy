/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.upgrades.hopper;

import net.minecraftforge.common.config.Config;

/** One hopper upgrade's settings (the original's HopperUpgradeConfig). */
public class HopperUpgradeConfig {
    @Config.Comment("Number of input filter slots")
    @Config.RangeInt(min = 1, max = 8)
    public int inputFilterSlots;
    @Config.Comment("Number of input filter slots displayed in a row")
    @Config.RangeInt(min = 1, max = 4)
    public int inputFilterSlotsInRow;
    @Config.Comment("Number of output filter slots")
    @Config.RangeInt(min = 1, max = 8)
    public int outputFilterSlots;
    @Config.Comment("Number of output filter slots displayed in a row")
    @Config.RangeInt(min = 1, max = 4)
    public int outputFilterSlotsInRow;
    @Config.Comment("Number of ticks between each transfer")
    @Config.RangeInt(min = 1, max = 100)
    public int transferSpeedTicks;
    @Config.Comment("Maximum stack size that can be transferred in one transfer")
    @Config.RangeInt(min = 1, max = 64)
    public int maxTransferStackSize;

    public HopperUpgradeConfig(int inputFilterSlots, int inputFilterSlotsInRow, int outputFilterSlots, int outputFilterSlotsInRow,
                               int transferSpeedTicks, int maxTransferStackSize) {
        this.inputFilterSlots = inputFilterSlots;
        this.inputFilterSlotsInRow = inputFilterSlotsInRow;
        this.outputFilterSlots = outputFilterSlots;
        this.outputFilterSlotsInRow = outputFilterSlotsInRow;
        this.transferSpeedTicks = transferSpeedTicks;
        this.maxTransferStackSize = maxTransferStackSize;
    }
}
