/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.render;

import com.exiledradio.scorelegacy.upgrades.UpgradeItemBase;
import com.exiledradio.scorelegacy.upgrades.stack.StackUpgradeItem;
import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.item.StorageTierUpgradeItem;
import com.exiledradio.sstoragelegacy.item.StorageToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * What the player holds, as far as storage renderers care (the original's StorageRenderer
 * cache): the storage tool and its mode, an upgrade, a tier upgrade. Worked out once a tick.
 */
public final class StorageRenderState {
    private static long lastCacheTime = -1;
    private static boolean holdsItemThatShowsUpgrades;
    private static boolean holdsToolSetToToggleUpgrades;
    private static boolean holdsItemThatShowsHiddenTiers;
    private static boolean holdsItemThatShowsFillLevels;
    private static boolean holdsToolInToggleLockOrLockDisplay;
    private static boolean holdsToolInToggleFillLevelDisplay;

    private StorageRenderState() {}

    private static void refresh() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.world.getTotalWorldTime() == lastCacheTime) {
            return;
        }
        lastCacheTime = mc.world.getTotalWorldTime();
        EntityPlayerSP player = mc.player;
        if (player == null) {
            holdsItemThatShowsUpgrades = holdsToolSetToToggleUpgrades = holdsItemThatShowsHiddenTiers = false;
            holdsItemThatShowsFillLevels = holdsToolInToggleLockOrLockDisplay = holdsToolInToggleFillLevelDisplay = false;
            return;
        }
        ItemStack tool = getTool(player);
        StorageToolItem.Mode mode = tool == null ? null : StorageToolItem.getMode(tool);
        boolean holdsTierUpgrade = holds(player, StorageTierUpgradeItem.class::isInstance);
        holdsToolSetToToggleUpgrades = mode == StorageToolItem.Mode.UPGRADES_DISPLAY;
        holdsItemThatShowsUpgrades = tool != null || holds(player, item -> item instanceof UpgradeItemBase && isStorageItem(item));
        holdsItemThatShowsFillLevels = tool != null || holdsTierUpgrade || holds(player, item -> item instanceof StackUpgradeItem && isStorageItem(item));
        holdsItemThatShowsHiddenTiers = mode == StorageToolItem.Mode.TIER_DISPLAY || holdsTierUpgrade;
        holdsToolInToggleLockOrLockDisplay = mode == StorageToolItem.Mode.LOCK || mode == StorageToolItem.Mode.LOCK_DISPLAY;
        holdsToolInToggleFillLevelDisplay = mode == StorageToolItem.Mode.FILL_LEVEL_DISPLAY;
    }

    @Nullable
    private static ItemStack getTool(EntityPlayerSP player) {
        if (player.getHeldItemMainhand().getItem() instanceof StorageToolItem) {
            return player.getHeldItemMainhand();
        }
        return player.getHeldItemOffhand().getItem() instanceof StorageToolItem ? player.getHeldItemOffhand() : null;
    }

    private static boolean holds(EntityPlayerSP player, Predicate<Item> matcher) {
        return matcher.test(player.getHeldItemMainhand().getItem()) || matcher.test(player.getHeldItemOffhand().getItem());
    }

    private static boolean isStorageItem(Item item) {
        ResourceLocation name = item.getRegistryName();
        return name != null && name.getNamespace().equals(SophisticatedStorageLegacy.MODID);
    }

    public static boolean holdsItemThatShowsUpgrades() {
        refresh();
        return holdsItemThatShowsUpgrades;
    }

    /** The tool in upgrades mode, pointed at a storage that hides them: drawn faded. */
    public static boolean holdsToolSetToToggleUpgrades() {
        refresh();
        return holdsToolSetToToggleUpgrades;
    }

    public static boolean holdsItemThatShowsHiddenTiers() {
        refresh();
        return holdsItemThatShowsHiddenTiers;
    }

    public static boolean holdsItemThatShowsFillLevels() {
        refresh();
        return holdsItemThatShowsFillLevels;
    }

    public static boolean holdsToolInToggleLockOrLockDisplay() {
        refresh();
        return holdsToolInToggleLockOrLockDisplay;
    }

    public static boolean holdsToolInToggleFillLevelDisplay() {
        refresh();
        return holdsToolInToggleFillLevelDisplay;
    }
}
