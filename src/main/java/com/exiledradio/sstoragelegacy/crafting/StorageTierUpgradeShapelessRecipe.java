/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.crafting;

import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nonnull;

/**
 * The shapeless tier recipe (the original 1.20's StorageTierUpgradeShapelessRecipe): a diamond
 * storage and a netherite ingot make the netherite storage, keeping the storage's NBT and growing
 * carried contents like {@link StorageTierUpgradeRecipe}. 1.12 has no smithing table, which the
 * 1.18 original used for this step.
 */
public class StorageTierUpgradeShapelessRecipe extends ShapelessOreRecipe {

    public StorageTierUpgradeShapelessRecipe(ResourceLocation group, ItemStack result, net.minecraft.util.NonNullList<net.minecraft.item.crafting.Ingredient> input) {
        super(group, input, result);
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {
        return StorageTierUpgradeRecipe.keepStorageData(super.getCraftingResult(inv), inv);
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    /** Registered in recipes/_factories.json as "sstoragelegacy:storage_tier_upgrade_shapeless". */
    public static class Factory implements IRecipeFactory {
        @Override
        public StorageTierUpgradeShapelessRecipe parse(JsonContext context, JsonObject json) {
            ShapelessOreRecipe recipe = ShapelessOreRecipe.factory(context, json);
            String group = recipe.getGroup();
            return new StorageTierUpgradeShapelessRecipe(group.isEmpty() ? null : new ResourceLocation(group), recipe.getRecipeOutput(), recipe.getIngredients());
        }
    }
}
