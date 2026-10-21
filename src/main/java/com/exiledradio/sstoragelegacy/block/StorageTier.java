/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.compat.MaterialCompat;

import javax.annotation.Nullable;

/**
 * Storage tiers, with the original's slot counts (its Config defaults; barrels, chests and
 * shulker boxes share them). The top tier is the Nether Star, matching Backpacks Legacy. The
 * original's copper and netherite tiers are there too, listed and craftable only when another mod
 * adds copper or netherite (see {@link MaterialCompat}); netherite is a second top tier beside the
 * Nether Star, with the same slots.
 */
public enum StorageTier {
    WOOD("", 27, 1, null),
    COPPER("copper_", 45, 1, MaterialCompat.Material.COPPER),
    IRON("iron_", 54, 2, null),
    GOLD("gold_", 81, 3, null),
    DIAMOND("diamond_", 108, 4, null),
    NETHERITE("netherite_", 132, 5, MaterialCompat.Material.NETHERITE),
    NETHER_STAR("nether_star_", 132, 5, null);

    private final String prefix;
    private final int inventorySlots;
    private final int upgradeSlots;
    @Nullable
    private final MaterialCompat.Material material;

    StorageTier(String prefix, int inventorySlots, int upgradeSlots, @Nullable MaterialCompat.Material material) {
        this.prefix = prefix;
        this.inventorySlots = inventorySlots;
        this.upgradeSlots = upgradeSlots;
        this.material = material;
    }

    /** Whether this tier is listed and craftable: always, except copper and netherite without a mod adding them. */
    public boolean isAvailable() {
        return material == null || material.isAvailable();
    }

    /** The registry name of this tier's storage of a kind: "barrel" -> "iron_barrel". */
    public String getRegistryName(String storageName) {
        return prefix + storageName;
    }

    public int getInventorySlots() {
        return inventorySlots;
    }

    public int getUpgradeSlots() {
        return upgradeSlots;
    }

    /** Prefix of this tier's names and item models (see tools/gen_barrel_models.py): "" or e.g. "iron_". */
    public String getModelPrefix() {
        return prefix;
    }
}
