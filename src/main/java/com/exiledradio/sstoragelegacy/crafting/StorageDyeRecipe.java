/*
 * Modified from Sophisticated Core / Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.crafting;

import com.exiledradio.sstoragelegacy.item.StorageBlockItem;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage block plus dyes. Dyes in columns left of (or level with) the storage colour the
 * main body, dyes in columns right of (or level with) it colour the accent trim.
 */
public class StorageDyeRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    private static final String[] DYE_ORE_NAMES = {
            "dyeWhite", "dyeOrange", "dyeMagenta", "dyeLightBlue", "dyeYellow", "dyeLime", "dyePink", "dyeGray",
            "dyeLightGray", "dyeCyan", "dyePurple", "dyeBlue", "dyeBrown", "dyeGreen", "dyeRed", "dyeBlack"
    };

    /** A barrel with materials can't be dyed (the original's isTintable). */
    private static boolean isDyeableStorageItem(ItemStack stack) {
        return stack.getItem() instanceof StorageBlockItem && com.exiledradio.sstoragelegacy.item.BarrelMaterials.get(stack).isEmpty();
    }

    @Override
    public boolean matches(@Nonnull InventoryCrafting inv, @Nonnull World worldIn) {
        boolean storagePresent = false;
        boolean dyePresent = false;
        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack slotStack = inv.getStackInSlot(slot);
            if (slotStack.isEmpty()) {
                continue;
            }
            if (isDyeableStorageItem(slotStack)) {
                if (storagePresent) {
                    return false;
                }
                storagePresent = true;
            } else if (getDyeColor(slotStack) != null) {
                dyePresent = true;
            } else {
                return false;
            }
        }
        return storagePresent && dyePresent;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {
        Map<Integer, List<EnumDyeColor>> columnDyes = new HashMap<>();
        ItemStack storage = ItemStack.EMPTY;
        int storageColumn = 0;

        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack slotStack = inv.getStackInSlot(slot);
            if (slotStack.isEmpty()) {
                continue;
            }
            int column = slot % inv.getWidth();
            if (isDyeableStorageItem(slotStack)) {
                if (!storage.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                storage = slotStack;
                storageColumn = column;
            } else {
                EnumDyeColor dyeColor = getDyeColor(slotStack);
                if (dyeColor == null) {
                    return ItemStack.EMPTY;
                }
                columnDyes.computeIfAbsent(column, c -> new ArrayList<>()).add(dyeColor);
            }
        }
        if (storage.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack colored = storage.copy();
        colored.setCount(1);
        List<EnumDyeColor> mainDyes = new ArrayList<>();
        List<EnumDyeColor> trimDyes = new ArrayList<>();
        for (Map.Entry<Integer, List<EnumDyeColor>> entry : columnDyes.entrySet()) {
            if (entry.getKey() <= storageColumn) {
                mainDyes.addAll(entry.getValue());
            }
            if (entry.getKey() >= storageColumn) {
                trimDyes.addAll(entry.getValue());
            }
        }
        if (!mainDyes.isEmpty()) {
            StorageItemData.setMainColor(colored, calculateColor(startColor(StorageItemData.getMainColor(colored)), 0, mainDyes));
        }
        if (!trimDyes.isEmpty()) {
            StorageItemData.setAccentColor(colored, calculateColor(startColor(StorageItemData.getAccentColor(colored)), 0, trimDyes));
        }
        return colored;
    }

    /** The original starts undyed parts from 0, which calculateColor treats as "no base colour". */
    private static int startColor(int color) {
        return color == StorageItemData.NO_COLOR ? 0 : color;
    }

    @Nullable
    private static EnumDyeColor getDyeColor(ItemStack stack) {
        for (int id : OreDictionary.getOreIDs(stack)) {
            String oreName = OreDictionary.getOreName(id);
            for (int i = 0; i < DYE_ORE_NAMES.length; i++) {
                if (DYE_ORE_NAMES[i].equals(oreName)) {
                    return EnumDyeColor.byMetadata(i);
                }
            }
        }
        return null;
    }

    /** Same mixing as leather armour dyeing, starting from the current colour if it was dyed. */
    public static int calculateColor(int baseColor, int defaultColor, List<EnumDyeColor> dyes) {
        if (dyes.isEmpty()) {
            return baseColor;
        }

        int[] rgb = new int[3];
        int sumMaxComponent = 0;
        int numberOfColors = 0;
        if (baseColor != defaultColor) {
            float baseRed = (baseColor >> 16 & 255);
            float baseGreen = (baseColor >> 8 & 255);
            float baseBlue = (baseColor & 255);
            sumMaxComponent = (int) ((float) sumMaxComponent + Math.max(baseRed, Math.max(baseGreen, baseBlue)));
            rgb[0] = (int) ((float) rgb[0] + baseRed);
            rgb[1] = (int) ((float) rgb[1] + baseGreen);
            rgb[2] = (int) ((float) rgb[2] + baseBlue);
            ++numberOfColors;
        }

        for (EnumDyeColor dye : dyes) {
            float[] dyeRgb = dye.getColorComponentValues();
            int dyeRed = (int) (dyeRgb[0] * 255.0F);
            int dyeGreen = (int) (dyeRgb[1] * 255.0F);
            int dyeBlue = (int) (dyeRgb[2] * 255.0F);
            sumMaxComponent += Math.max(dyeRed, Math.max(dyeGreen, dyeBlue));
            rgb[0] += dyeRed;
            rgb[1] += dyeGreen;
            rgb[2] += dyeBlue;
            ++numberOfColors;
        }

        int avgRed = rgb[0] / numberOfColors;
        int avgGreen = rgb[1] / numberOfColors;
        int avgBlue = rgb[2] / numberOfColors;
        float avgMaxComponent = (float) sumMaxComponent / (float) numberOfColors;
        float maxAvgComponent = (float) Math.max(avgRed, Math.max(avgGreen, avgBlue));
        avgRed = (int) ((float) avgRed * avgMaxComponent / maxAvgComponent);
        avgGreen = (int) ((float) avgGreen * avgMaxComponent / maxAvgComponent);
        avgBlue = (int) ((float) avgBlue * avgMaxComponent / maxAvgComponent);
        int finalColor = (avgRed << 8) + avgGreen;
        finalColor = (finalColor << 8) + avgBlue;
        return finalColor;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width >= 2 && height >= 1;
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
}
