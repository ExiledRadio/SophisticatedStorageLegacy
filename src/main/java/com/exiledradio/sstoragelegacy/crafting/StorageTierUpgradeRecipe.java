/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.crafting;

import com.exiledradio.sstoragelegacy.item.StorageBlockItem;
import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nonnull;

/**
 * A shaped recipe that turns a barrel into the next tier. The result keeps the old barrel's NBT,
 * so wood, colours, flat top and name carry over, as in the original.
 */
public class StorageTierUpgradeRecipe extends ShapedOreRecipe {

    public StorageTierUpgradeRecipe(ResourceLocation group, ItemStack result, CraftingHelper.ShapedPrimer primer) {
        super(group, result, primer);
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {
        return keepStorageData(super.getCraftingResult(inv), inv);
    }

    /** The result with the crafted storage's NBT, its carried contents grown to the new tier (also the shapeless recipe's). */
    static ItemStack keepStorageData(ItemStack upgraded, InventoryCrafting inv) {
        ItemStack storage = findStorage(inv);
        NBTTagCompound tag = storage.getTagCompound();
        if (tag != null) {
            upgraded.setTagCompound(tag.copy());
            growCarriedContents(upgraded, net.minecraft.block.Block.getBlockFromItem(storage.getItem()));
        }
        return upgraded;
    }

    /**
     * Contents carried in an upgraded item (shulker box, packed storage) grow like a placed
     * storage's, by the difference between the old storage's tier and the new one's.
     */
    public static void growCarriedContents(ItemStack upgraded, net.minecraft.block.Block from) {
        if (!upgraded.hasTagCompound()) {
            return;
        }
        NBTTagCompound contents = upgraded.getTagCompound().getCompoundTag(com.exiledradio.sstoragelegacy.item.StorageItemData.CONTENTS_TAG);
        net.minecraft.block.Block to = net.minecraft.block.Block.getBlockFromItem(upgraded.getItem());
        if (!contents.isEmpty() && from instanceof com.exiledradio.sstoragelegacy.block.StorageBlockBase && to instanceof com.exiledradio.sstoragelegacy.block.StorageBlockBase) {
            com.exiledradio.sstoragelegacy.block.StorageBlockBase fromStorage = (com.exiledradio.sstoragelegacy.block.StorageBlockBase) from;
            com.exiledradio.sstoragelegacy.block.StorageBlockBase toStorage = (com.exiledradio.sstoragelegacy.block.StorageBlockBase) to;
            int halves = com.exiledradio.sstoragelegacy.item.StorageItemData.isDoubleChest(upgraded) ? 2 : 1;
            com.exiledradio.sstoragelegacy.block.StorageBlockWrapper.increaseSize(contents,
                    halves * (toStorage.getDefaultInventorySlots() - fromStorage.getDefaultInventorySlots()),
                    toStorage.getDefaultUpgradeSlots() - fromStorage.getDefaultUpgradeSlots());
        }
    }

    private static ItemStack findStorage(InventoryCrafting inv) {
        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack slotStack = inv.getStackInSlot(slot);
            if (slotStack.getItem() instanceof StorageBlockItem) {
                return slotStack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    /** Registered in recipes/_factories.json as "sstoragelegacy:storage_tier_upgrade". */
    public static class Factory implements IRecipeFactory {
        @Override
        public StorageTierUpgradeRecipe parse(JsonContext context, JsonObject json) {
            ShapedOreRecipe recipe = ShapedOreRecipe.factory(context, json);
            CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
            primer.width = recipe.getRecipeWidth();
            primer.height = recipe.getRecipeHeight();
            primer.mirrored = true;
            primer.input = recipe.getIngredients();
            String group = recipe.getGroup();
            return new StorageTierUpgradeRecipe(group.isEmpty() ? null : new ResourceLocation(group), recipe.getRecipeOutput(), primer);
        }
    }
}
