/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.crafting;

import com.exiledradio.sstoragelegacy.block.BarrelBlock;
import com.exiledradio.sstoragelegacy.block.BarrelMaterial;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelBlock;
import com.exiledradio.sstoragelegacy.item.BarrelMaterials;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A barrel (or limited barrel) with full blocks around it takes their looks (the original's
 * BarrelMaterialRecipe). Around the barrel in the grid:
 * <pre>
 *   top            inner trim, top, top trim   top trim
 *   side           (barrel)                    side trim
 *   bottom         bottom, bottom trim         bottom trim
 * </pre>
 * Parts without a block keep the barrel's own material or take one from related parts.
 */
public class BarrelMaterialRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private static boolean isBarrel(ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        return block instanceof BarrelBlock || block instanceof LimitedBarrelBlock;
    }

    @Override
    public boolean matches(@Nonnull InventoryCrafting inv, @Nonnull World world) {
        int barrelRow = -1;
        int barrelCol = -1;
        int minRow = Integer.MAX_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int maxCol = Integer.MIN_VALUE;
        Map<Integer, Integer> rowCounts = new HashMap<>();
        for (int row = 0; row < inv.getHeight(); row++) {
            for (int col = 0; col < inv.getWidth(); col++) {
                ItemStack stack = inv.getStackInRowAndColumn(col, row);
                if (stack.isEmpty()) {
                    continue;
                }
                if (isBarrel(stack)) {
                    if (barrelRow >= 0) {
                        return false;
                    }
                    barrelRow = row;
                    barrelCol = col;
                } else if (BarrelMaterials.getMaterialName(stack) != null) {
                    // The middle of the bottom row can't also have a block left of it in that row.
                    if (barrelCol == col && barrelRow < row && rowCounts.getOrDefault(row, 0) > 0) {
                        return false;
                    }
                    rowCounts.merge(row, 1, Integer::sum);
                    minRow = Math.min(minRow, row);
                    minCol = Math.min(minCol, col);
                    maxRow = Math.max(maxRow, row);
                    maxCol = Math.max(maxCol, col);
                } else {
                    return false;
                }
            }
        }
        if (barrelRow < 0 || rowCounts.isEmpty() || minRow < barrelRow - 1 || maxRow > barrelRow + 1 || minCol < barrelCol - 1 || maxCol > barrelCol + 1) {
            return false;
        }
        return rowCounts.getOrDefault(barrelRow - 1, 0) <= 3 && rowCounts.getOrDefault(barrelRow, 0) <= 2 && rowCounts.getOrDefault(barrelRow + 1, 0) <= 2;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {
        int barrelRow = -1;
        int barrelCol = -1;
        ItemStack barrel = ItemStack.EMPTY;
        for (int row = 0; row < inv.getHeight() && barrel.isEmpty(); row++) {
            for (int col = 0; col < inv.getWidth(); col++) {
                ItemStack stack = inv.getStackInRowAndColumn(col, row);
                if (isBarrel(stack)) {
                    barrelRow = row;
                    barrelCol = col;
                    barrel = stack.copy();
                    barrel.setCount(1);
                    break;
                }
            }
        }
        if (barrel.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Map<BarrelMaterial, String> materials = new EnumMap<>(BarrelMaterial.class);
        materials.putAll(BarrelMaterials.uncompact(BarrelMaterials.get(barrel)));
        for (int row = 0; row < inv.getHeight(); row++) {
            for (int col = 0; col < inv.getWidth(); col++) {
                ItemStack stack = inv.getStackInRowAndColumn(col, row);
                String name = (row == barrelRow && col == barrelCol) || stack.isEmpty() ? null : BarrelMaterials.getMaterialName(stack);
                if (name == null) {
                    continue;
                }
                boolean first = true;
                for (BarrelMaterial material : getMaterialsAt(row, col, barrelRow, barrelCol)) {
                    if (first || !materials.containsKey(material)) {
                        materials.put(material, name);
                    }
                    first = false;
                }
            }
        }
        BarrelMaterials.fillEmptyFromDefaults(materials);
        Map<BarrelMaterial, String> compacted = BarrelMaterials.compact(materials);
        BarrelMaterials.set(barrel, compacted);
        BarrelMaterials.removeCoveredTints(barrel, compacted);
        return barrel;
    }

    private static List<BarrelMaterial> getMaterialsAt(int row, int col, int barrelRow, int barrelCol) {
        if (row < barrelRow) {
            if (col < barrelCol) {
                return Collections.singletonList(BarrelMaterial.TOP);
            }
            return col == barrelCol ? Arrays.asList(BarrelMaterial.TOP_INNER_TRIM, BarrelMaterial.TOP, BarrelMaterial.TOP_TRIM)
                    : Collections.singletonList(BarrelMaterial.TOP_TRIM);
        }
        if (row == barrelRow) {
            return Collections.singletonList(col < barrelCol ? BarrelMaterial.SIDE : BarrelMaterial.SIDE_TRIM);
        }
        if (col < barrelCol) {
            return Collections.singletonList(BarrelMaterial.BOTTOM);
        }
        return col == barrelCol ? Arrays.asList(BarrelMaterial.BOTTOM, BarrelMaterial.BOTTOM_TRIM) : Collections.singletonList(BarrelMaterial.BOTTOM_TRIM);
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height > 1;
    }

    @Nonnull
    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    /** Registered in recipes/_factories.json as "sstoragelegacy:barrel_material". */
    public static class Factory implements IRecipeFactory {
        @Override
        public IRecipe parse(JsonContext context, JsonObject json) {
            return new BarrelMaterialRecipe();
        }
    }
}
