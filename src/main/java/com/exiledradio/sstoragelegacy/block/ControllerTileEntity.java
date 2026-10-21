/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.controller.ControllerTileEntityBase;
import com.exiledradio.scorelegacy.controller.IControllableStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Storage's controller (the original's ControllerBlockEntity): deposits what the player holds
 * (a quick second click: everything in the inventory the network already has), and passes the
 * storage tool's toggles to every connected storage.
 */
public class ControllerTileEntity extends ControllerTileEntityBase {
    private static final int DOUBLE_CLICK_TICKS = 10;
    private long lastDepositTime = -100;

    public void depositPlayerItems(EntityPlayer player, EnumHand hand) {
        long time = world.getTotalWorldTime();
        boolean doubleClick = time - lastDepositTime < DOUBLE_CLICK_TICKS;
        lastDepositTime = time;
        if (doubleClick) {
            boolean changed = false;
            for (int slot = 0; slot < player.inventory.mainInventory.size(); slot++) {
                ItemStack stack = player.inventory.mainInventory.get(slot);
                if (!stack.isEmpty() && canDepositStack(stack)) {
                    ItemStack remaining = insertItem(stack, false, false);
                    if (remaining.getCount() != stack.getCount()) {
                        player.inventory.mainInventory.set(slot, remaining);
                        changed = true;
                    }
                }
            }
            if (changed) {
                player.inventory.markDirty();
            }
            return;
        }
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && canDepositStack(held)) {
            player.setHeldItem(hand, insertItem(held, false, false));
        }
    }

    /** Only what the network already holds or remembers goes in by hand, as in the original. */
    private boolean canDepositStack(ItemStack stack) {
        return hasItem(stack.getItem()) || isMemorizedItem(stack);
    }

    /** The storages with contents of their own (one per double chest), for upgrades put in through the controller. */
    public List<StorageTileEntity> getConnectedStorageTiles() {
        return getConnectedStorages();
    }

    private List<StorageTileEntity> getConnectedStorages() {
        List<StorageTileEntity> storages = new ArrayList<>();
        for (BlockPos storagePos : getStoragePositions()) {
            IControllableStorage storage = getStorage(storagePos);
            if (storage instanceof StorageTileEntity) {
                storages.add((StorageTileEntity) storage);
            }
        }
        return storages;
    }

    /**
     * The original's controller toggles: if any connected storage is "off" for this setting,
     * those are toggled on; only when all are on are they all toggled off.
     */
    private <T extends StorageTileEntity> void toggleAll(Class<T> type, Predicate<T> isOn, Consumer<T> toggle) {
        List<T> on = new ArrayList<>();
        List<T> off = new ArrayList<>();
        for (StorageTileEntity storage : getConnectedStorages()) {
            if (type.isInstance(storage)) {
                T typed = type.cast(storage);
                (isOn.test(typed) ? on : off).add(typed);
            }
        }
        (off.isEmpty() ? on : off).forEach(toggle);
    }

    public void toggleLock() {
        toggleAll(StorageTileEntity.class, StorageTileEntity::isLocked, StorageTileEntity::toggleLock);
    }

    public void toggleLockVisibility() {
        List<StorageTileEntity> visible = new ArrayList<>();
        List<StorageTileEntity> hidden = new ArrayList<>();
        for (StorageTileEntity storage : getConnectedStorages()) {
            if (storage.isLocked()) {
                (storage.shouldShowLock() ? visible : hidden).add(storage);
            }
        }
        (hidden.isEmpty() ? visible : hidden).forEach(StorageTileEntity::toggleLockVisibility);
    }

    public void toggleCountVisibility() {
        toggleAll(LimitedBarrelTileEntity.class, LimitedBarrelTileEntity::shouldShowCounts, LimitedBarrelTileEntity::toggleCountVisibility);
    }

    public void toggleTierVisibility() {
        toggleAll(StorageTileEntity.class, StorageTileEntity::shouldShowTier, StorageTileEntity::toggleTierVisibility);
    }

    public void toggleUpgradesVisibility() {
        toggleAll(StorageTileEntity.class, StorageTileEntity::shouldShowUpgrades, StorageTileEntity::toggleUpgradesVisibility);
    }

    public void toggleFillLevelVisibility() {
        toggleAll(LimitedBarrelTileEntity.class, LimitedBarrelTileEntity::shouldShowFillLevels, LimitedBarrelTileEntity::toggleFillLevelVisibility);
    }

    /** The link outlines can reach anywhere in range, so the renderer must not be culled by the block's own box. */
    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos).grow(SEARCH_RANGE);
    }
}
