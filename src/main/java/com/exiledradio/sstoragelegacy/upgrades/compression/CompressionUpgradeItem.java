/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.upgrades.compression;

import com.exiledradio.scorelegacy.api.IInventoryPartUpgrade;
import com.exiledradio.scorelegacy.api.IStorageWrapper;
import com.exiledradio.scorelegacy.api.UpgradeSlotChangeResult;
import com.exiledradio.scorelegacy.api.UpgradeType;
import com.exiledradio.scorelegacy.inventory.IInventoryPart;
import com.exiledradio.scorelegacy.inventory.InventoryHandler;
import com.exiledradio.scorelegacy.settings.memory.MemorySettingsCategory;
import com.exiledradio.scorelegacy.upgrades.UpgradeItemBase;
import com.exiledradio.scorelegacy.upgrades.UpgradeWrapperBase;
import com.exiledradio.sstoragelegacy.ModConfig;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Compression upgrade (the original's CompressionUpgradeItem): takes over the first free run of
 * up to 5 slots and keeps an item and its compressed forms there as one amount. It has no
 * settings tab and can't be switched off. The original remembers its first slot in the upgrade;
 * here each compression upgrade simply takes the first slots no earlier one has.
 */
public class CompressionUpgradeItem extends UpgradeItemBase<CompressionUpgradeItem.Wrapper> {
    public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);

    public CompressionUpgradeItem(String name, CreativeTabs tab) {
        super(name, tab);
    }

    @Override
    public UpgradeType<Wrapper> getType() {
        return TYPE;
    }

    @Override
    public UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelBackpack) {
        UpgradeSlotChangeResult limits = com.exiledradio.scorelegacy.api.UpgradeLimits.check(this, storageWrapper);
        if (!limits.isSuccessful()) {
            return limits;
        }
        InventoryHandler inventory = storageWrapper.getInventoryHandler();
        CompressionInventoryPart part = createPart(inventory, inventory.getParts(),
                () -> storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class));
        if (part == null) {
            return UpgradeSlotChangeResult.fail("add.compression_no_space", Collections.emptySet(), Collections.emptySet());
        }
        if (!part.isCompatible()) {
            Set<Integer> errorSlots = new HashSet<>();
            for (int slot = part.getFirstSlot(); slot < part.getFirstSlot() + part.getSlots(); slot++) {
                if (!inventory.getRawStack(slot).isEmpty()) {
                    errorSlots.add(slot);
                }
            }
            return UpgradeSlotChangeResult.fail("add.compression_incompatible_items", Collections.emptySet(), errorSlots);
        }
        return UpgradeSlotChangeResult.SUCCESS;
    }

    /** A part on the first run of slots no other part has, or null if there is none. */
    @Nullable
    static CompressionInventoryPart createPart(InventoryHandler inventory, List<IInventoryPart> takenParts,
                                               java.util.function.Supplier<MemorySettingsCategory> memory) {
        int usable = inventory.getUsableSlots();
        int first = 0;
        boolean moved = true;
        while (moved) {
            moved = false;
            for (IInventoryPart taken : takenParts) {
                if (taken.coversSlot(first)) {
                    first = taken.getFirstSlot() + taken.getSlots();
                    moved = true;
                }
            }
        }
        int end = Math.min(usable, first + ModConfig.upgrades.compression.maxNumberOfSlots);
        for (IInventoryPart taken : takenParts) {
            if (taken.getFirstSlot() >= first) {
                end = Math.min(end, taken.getFirstSlot());
            }
        }
        return end > first ? new CompressionInventoryPart(inventory, first, end - first, memory) : null;
    }

    public static class Wrapper extends UpgradeWrapperBase<Wrapper, CompressionUpgradeItem> implements IInventoryPartUpgrade {
        protected Wrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
            super(storageWrapper, upgrade, upgradeSaveHandler);
        }

        @Nullable
        @Override
        public IInventoryPart getInventoryPart(InventoryHandler inventoryHandler, List<IInventoryPart> takenParts) {
            return createPart(inventoryHandler, takenParts, () -> storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class));
        }

        @Override
        public boolean canBeDisabled() {
            return false;
        }

        @Override
        public boolean hideSettingsTab() {
            return true;
        }
    }
}
