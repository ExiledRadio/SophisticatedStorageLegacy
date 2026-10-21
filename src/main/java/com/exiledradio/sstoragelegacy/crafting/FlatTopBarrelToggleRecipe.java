/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.crafting;

import com.exiledradio.sstoragelegacy.item.BarrelItem;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;

/** A barrel on its own in the grid toggles between the normal and the flat-top look. */
public class FlatTopBarrelToggleRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    @Override
    public boolean matches(@Nonnull InventoryCrafting inv, @Nonnull World worldIn) {
        return !findBarrel(inv).isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {
        ItemStack barrel = findBarrel(inv);
        if (barrel.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = barrel.copy();
        result.setCount(1);
        return StorageItemData.setFlatTop(result, !StorageItemData.isFlatTop(result));
    }

    /** The single barrel in the grid, or empty if there is anything else or more than one. */
    private static ItemStack findBarrel(InventoryCrafting inv) {
        ItemStack found = ItemStack.EMPTY;
        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!found.isEmpty() || !(stack.getItem() instanceof BarrelItem)) {
                return ItemStack.EMPTY;
            }
            found = stack;
        }
        return found;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 1;
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
