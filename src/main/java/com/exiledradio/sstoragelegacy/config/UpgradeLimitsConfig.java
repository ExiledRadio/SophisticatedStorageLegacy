/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.config;

import com.exiledradio.scorelegacy.api.UpgradeGroup;
import com.exiledradio.scorelegacy.api.UpgradeLimits;
import com.exiledradio.sstoragelegacy.ModConfig;
import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Reads `upgrades.maxUpgradesPerStorage` (the original's MaxUgradesPerStorageConfig) for Core's limit check. */
public final class UpgradeLimitsConfig implements UpgradeLimits.IUpgradeCountLimitConfig {
    public static final UpgradeLimitsConfig INSTANCE = new UpgradeLimitsConfig();
    @Nullable
    private static Map<String, Map<String, Integer>> limits = null;

    private UpgradeLimitsConfig() {}

    public static void clearCache() {
        limits = null;
    }

    @Override
    public int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeName) {
        // Only this mod's upgrades: another mod's item could share a path.
        if (upgradeName == null || !SophisticatedStorageLegacy.MODID.equals(upgradeName.getNamespace())) {
            return Integer.MAX_VALUE;
        }
        return getLimits().getOrDefault(storageType, new HashMap<>()).getOrDefault(upgradeName.getPath(), Integer.MAX_VALUE);
    }

    @Override
    public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup group) {
        return getLimits().getOrDefault(storageType, new HashMap<>()).getOrDefault(group.getName(), Integer.MAX_VALUE);
    }

    private static Map<String, Map<String, Integer>> getLimits() {
        if (limits == null) {
            Map<String, Map<String, Integer>> parsed = new HashMap<>();
            for (String entry : ModConfig.upgrades.maxUpgradesPerStorage) {
                String[] parts = entry.trim().split("\\|");
                if (parts.length != 3) {
                    continue;
                }
                try {
                    parsed.computeIfAbsent(parts[0], k -> new HashMap<>()).put(parts[1], Integer.parseInt(parts[2]));
                } catch (NumberFormatException ignored) {
                    // a typo in the config: the entry is skipped
                }
            }
            limits = parsed;
        }
        return limits;
    }
}
