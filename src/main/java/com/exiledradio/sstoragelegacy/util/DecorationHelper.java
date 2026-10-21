/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.util;

import com.exiledradio.sstoragelegacy.block.BarrelMaterial;
import com.exiledradio.sstoragelegacy.item.BarrelMaterials;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * What decorating costs (the original's DecorationHelper), shared by the decoration table and the
 * paintbrush. A block or dye is 24 parts; leftover parts are kept for the next use. Each part of a
 * barrel costs its share of a block, and a colour costs its RGB share of red, green and blue dye
 * (main colour 18 parts, accent 6). Keys of the parts maps are material names (see
 * BarrelMaterials) or the dye ore names.
 */
public final class DecorationHelper {
    public static final int BLOCK_TOTAL_PARTS = 24;
    public static final String RED_DYE = "dyeRed";
    public static final String GREEN_DYE = "dyeGreen";
    public static final String BLUE_DYE = "dyeBlue";
    private static final int MAIN_COLOR_PARTS = 18;
    private static final int ACCENT_COLOR_PARTS = 6;
    private static final Map<BarrelMaterial, Integer> PARTS_NEEDED = new EnumMap<>(BarrelMaterial.class);

    static {
        PARTS_NEEDED.put(BarrelMaterial.TOP_INNER_TRIM, 1);
        PARTS_NEEDED.put(BarrelMaterial.TOP_TRIM, 1);
        PARTS_NEEDED.put(BarrelMaterial.SIDE_TRIM, 4);
        PARTS_NEEDED.put(BarrelMaterial.BOTTOM_TRIM, 1);
        PARTS_NEEDED.put(BarrelMaterial.TOP, 3);
        PARTS_NEEDED.put(BarrelMaterial.SIDE, 12);
        PARTS_NEEDED.put(BarrelMaterial.BOTTOM, 3);
    }

    private DecorationHelper() {}

    /** Whether a stack is in the ore dictionary under this name. */
    public static boolean hasOreName(ItemStack stack, String oreName) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (OreDictionary.getOreName(id).equals(oreName)) {
                return true;
            }
        }
        return false;
    }

    // ---- Dyes ------------------------------------------------------------------------------

    /** Dye parts for the colours being set that differ from the storage's (-1 = not set / none). */
    public static Map<String, Integer> getDyePartsNeeded(int mainColorBeingSet, int accentColorBeingSet, int storageMainColor, int storageAccentColor) {
        return getDyePartsNeeded(mainColorBeingSet, accentColorBeingSet, storageMainColor, storageAccentColor, MAIN_COLOR_PARTS, ACCENT_COLOR_PARTS);
    }

    public static Map<String, Integer> getDyePartsNeeded(int mainColorBeingSet, int accentColorBeingSet, int storageMainColor, int storageAccentColor,
                                                         int mainColorParts, int accentColorParts) {
        Map<String, Integer> partsNeeded = new HashMap<>();
        if (mainColorBeingSet != -1 && mainColorBeingSet != storageMainColor) {
            addRgbParts(calculateRgbPartsNeeded(mainColorBeingSet, mainColorParts), partsNeeded);
        }
        if (accentColorBeingSet != -1 && accentColorBeingSet != storageAccentColor) {
            addRgbParts(calculateRgbPartsNeeded(accentColorBeingSet, accentColorParts), partsNeeded);
        }
        return partsNeeded;
    }

    private static void addRgbParts(int[] rgb, Map<String, Integer> partsNeeded) {
        String[] dyes = {RED_DYE, GREEN_DYE, BLUE_DYE};
        for (int i = 0; i < 3; i++) {
            if (rgb[i] != 0) {
                partsNeeded.merge(dyes[i], rgb[i], Integer::sum);
            }
        }
    }

    /** The parts split by the colour's RGB proportions, rounded by largest remainder. */
    private static int[] calculateRgbPartsNeeded(int color, int totalParts) {
        float[] ratios = {(color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F};
        float total = ratios[0] + ratios[1] + ratios[2];
        if (total == 0) {
            // Black: the original divides by zero here; an even split is what that amounts to.
            ratios = new float[] {1 / 3F, 1 / 3F, 1 / 3F};
            total = 1;
        }
        int[] result = new int[3];
        double[] remainders = new double[3];
        for (int i = 0; i < 3; i++) {
            double scaled = ratios[i] / total * totalParts;
            result[i] = (int) scaled;
            remainders[i] = scaled - result[i];
        }
        int remaining = totalParts - (result[0] + result[1] + result[2]);
        Integer[] indices = {0, 1, 2};
        Arrays.sort(indices, Comparator.comparingDouble(i -> -remainders[i]));
        for (int i = 0; i < remaining; i++) {
            result[indices[i % 3]]++;
        }
        return result;
    }

    public static boolean consumeDyes(int mainColorBeingSet, int accentColorBeingSet, Map<String, Integer> remainingParts, List<IItemHandler> dyes,
                                      int storageMainColor, int storageAccentColor, boolean simulate) {
        Map<String, Integer> partsNeeded = getDyePartsNeeded(mainColorBeingSet, accentColorBeingSet, storageMainColor, storageAccentColor);
        return partsNeeded.isEmpty() || consumeDyePartsNeeded(partsNeeded, dyes, remainingParts, simulate).hasEnough();
    }

    public static ConsumptionResult consumeDyePartsNeeded(Map<String, Integer> partsNeeded, List<IItemHandler> handlers, Map<String, Integer> remainingParts, boolean simulate) {
        return consumePartsNeeded(partsNeeded, handlers, (dye, stack) -> hasOreName(stack, dye), remainingParts, simulate);
    }

    // ---- Materials -------------------------------------------------------------------------

    public static boolean consumeMaterials(Map<String, Integer> remainingParts, List<IItemHandler> blocks, Map<BarrelMaterial, String> originalMaterials,
                                           Map<BarrelMaterial, String> materials, boolean simulate) {
        return consumeMaterialPartsNeeded(getMaterialPartsNeeded(originalMaterials, materials), remainingParts, blocks, simulate).hasEnough();
    }

    public static ConsumptionResult consumeMaterialPartsNeeded(Map<String, Integer> partsNeeded, Map<String, Integer> remainingParts, List<IItemHandler> blocks, boolean simulate) {
        return consumePartsNeeded(partsNeeded, blocks, (name, stack) -> name.equals(BarrelMaterials.getMaterialName(stack)), remainingParts, simulate);
    }

    /**
     * Parts of each block needed to go from one set of (leaf) materials to another. Unchanged parts
     * are free, and so is the top trim when it reuses the inner trim's block.
     */
    public static Map<String, Integer> getMaterialPartsNeeded(Map<BarrelMaterial, String> originalMaterials, Map<BarrelMaterial, String> materialsToApply) {
        Map<String, Integer> partsNeeded = new HashMap<>();
        Map<BarrelMaterial, String> original = BarrelMaterials.uncompact(originalMaterials);
        Map<BarrelMaterial, String> toApply = BarrelMaterials.uncompact(materialsToApply);
        String topInnerTrim = addCost(toApply, BarrelMaterial.TOP_INNER_TRIM, null, partsNeeded, original);
        String topTrim = addCost(toApply, BarrelMaterial.TOP_TRIM, topInnerTrim, partsNeeded, original);
        String sideTrim = addCost(toApply, BarrelMaterial.SIDE_TRIM, topTrim, partsNeeded, original);
        addCost(toApply, BarrelMaterial.BOTTOM_TRIM, sideTrim, partsNeeded, original);
        String top = addCost(toApply, BarrelMaterial.TOP, topTrim, partsNeeded, original);
        String side = addCost(toApply, BarrelMaterial.SIDE, top, partsNeeded, original);
        addCost(toApply, BarrelMaterial.BOTTOM, side, partsNeeded, original);
        return partsNeeded;
    }

    @Nullable
    private static String addCost(Map<BarrelMaterial, String> materials, BarrelMaterial material, @Nullable String defaultMaterial,
                                  Map<String, Integer> partsNeeded, Map<BarrelMaterial, String> originalMaterials) {
        boolean same = Objects.equals(originalMaterials.get(material), materials.get(material));
        boolean noCost = (material == BarrelMaterial.TOP_TRIM && defaultMaterial != null) || same || !materials.containsKey(material);
        String name = materials.getOrDefault(material, defaultMaterial);
        if (!noCost && name != null) {
            partsNeeded.merge(name, PARTS_NEEDED.get(material), Integer::sum);
        }
        return name;
    }

    // ---- Taking parts ----------------------------------------------------------------------

    private static ConsumptionResult consumePartsNeeded(Map<String, Integer> partsNeeded, List<IItemHandler> handlers, BiPredicate<String, ItemStack> matcher,
                                                        Map<String, Integer> remainingParts, boolean simulate) {
        Map<String, Integer> missingParts = new HashMap<>();
        for (Map.Entry<String, Integer> entry : partsNeeded.entrySet()) {
            String key = entry.getKey();
            int parts = entry.getValue();
            int remaining = remainingParts.getOrDefault(key, 0);
            if (remaining > parts) {
                if (!simulate) {
                    remainingParts.put(key, remaining - parts);
                }
                continue;
            }
            if (!simulate) {
                remainingParts.remove(key);
            }
            if (remaining == parts) {
                continue;
            }
            parts -= remaining;
            int missing = consumeFromHandlers(handlers, matcher, remainingParts, simulate, key, parts);
            if (missing > 0) {
                missingParts.put(key, missing);
            }
        }
        return new ConsumptionResult(missingParts.isEmpty(), missingParts);
    }

    /** Takes whole items for the parts; returns the parts still missing. Leftover parts are kept. */
    private static int consumeFromHandlers(List<IItemHandler> handlers, BiPredicate<String, ItemStack> matcher, Map<String, Integer> remainingParts,
                                           boolean simulate, String key, int parts) {
        for (IItemHandler handler : handlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (!matcher.test(key, handler.getStackInSlot(slot))) {
                    continue;
                }
                int toRemove = (int) Math.ceil((double) parts / BLOCK_TOTAL_PARTS);
                ItemStack removed = handler.extractItem(slot, toRemove, simulate);
                int partsRemoved = removed.getCount() * BLOCK_TOTAL_PARTS;
                if (partsRemoved >= parts) {
                    if (partsRemoved > parts && !simulate) {
                        remainingParts.put(key, partsRemoved - parts);
                    }
                    return 0;
                }
                parts -= partsRemoved;
            }
        }
        return parts;
    }

    /** Whether there was enough, and how many parts of each key were missing. */
    public static final class ConsumptionResult {
        private final boolean hasEnough;
        private final Map<String, Integer> missingParts;

        ConsumptionResult(boolean hasEnough, Map<String, Integer> missingParts) {
            this.hasEnough = hasEnough;
            this.missingParts = missingParts;
        }

        public boolean hasEnough() {
            return hasEnough;
        }

        public Map<String, Integer> getMissingParts() {
            return missingParts;
        }
    }
}
