/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.upgrades.hopper;

import com.exiledradio.scorelegacy.api.UpgradeType;
import com.exiledradio.scorelegacy.upgrades.UpgradeItemBase;
import net.minecraft.creativetab.CreativeTabs;

import java.util.function.Supplier;

/** Hopper upgrade, basic or advanced: the advanced one is faster, moves more and lets each side be set. */
public class HopperUpgradeItem extends UpgradeItemBase<HopperUpgradeWrapper> {
    public static final UpgradeType<HopperUpgradeWrapper> TYPE = new UpgradeType<>(HopperUpgradeWrapper::new);

    private final Supplier<HopperUpgradeConfig> config;
    private final boolean advanced;

    public HopperUpgradeItem(String name, CreativeTabs tab, Supplier<HopperUpgradeConfig> config, boolean advanced) {
        super(name, tab);
        this.config = config;
        this.advanced = advanced;
    }

    @Override
    public UpgradeType<HopperUpgradeWrapper> getType() {
        return TYPE;
    }

    /** Only the advanced hopper's sides can be changed; the basic one always pulls from the top and pushes down. */
    public boolean isAdvanced() {
        return advanced;
    }

    public int getInputFilterSlotCount() {
        return config.get().inputFilterSlots;
    }

    public int getOutputFilterSlotCount() {
        return config.get().outputFilterSlots;
    }

    public int getTransferSpeedTicks() {
        return config.get().transferSpeedTicks;
    }

    public int getMaxTransferStackSize() {
        return config.get().maxTransferStackSize;
    }
}
