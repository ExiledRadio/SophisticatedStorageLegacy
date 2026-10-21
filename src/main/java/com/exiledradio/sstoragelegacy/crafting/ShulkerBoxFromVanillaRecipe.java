/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.crafting;

import com.exiledradio.sstoragelegacy.item.StorageBlockItem;
import com.exiledradio.sstoragelegacy.item.StorageItemContents;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nonnull;

/**
 * A vanilla shulker box and a redstone torch make our shulker box in the same colour, with the
 * vanilla box's items inside (the original's ShulkerBoxFromVanillaShapelessRecipe). Purple, the
 * 1.12 default, stays undyed, as with the basic tier upgrade.
 */
public class ShulkerBoxFromVanillaRecipe extends ShapelessOreRecipe {

    public ShulkerBoxFromVanillaRecipe(ResourceLocation group, ItemStack result, net.minecraft.util.NonNullList<net.minecraft.item.crafting.Ingredient> input) {
        super(group, input, result);
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {
        ItemStack result = super.getCraftingResult(inv);
        ItemStack vanilla = findVanillaShulkerBox(inv);
        if (vanilla.isEmpty()) {
            return result;
        }
        EnumDyeColor color = getColor(Block.getBlockFromItem(vanilla.getItem()));
        if (color != EnumDyeColor.PURPLE) {
            int rgb = StorageBlockItem.colorOf(color);
            StorageItemData.setAccentColor(StorageItemData.setMainColor(result, rgb), rgb);
        }
        NBTTagCompound blockEntity = vanilla.getTagCompound() == null ? null : vanilla.getTagCompound().getCompoundTag("BlockEntityTag");
        if (blockEntity != null) {
            NBTTagList items = blockEntity.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < items.tagCount(); i++) {
                StorageItemContents.insert(result, new ItemStack(items.getCompoundTagAt(i)), false);
            }
        }
        if (vanilla.hasDisplayName()) {
            result.setStackDisplayName(vanilla.getDisplayName());
        }
        return result;
    }

    private static ItemStack findVanillaShulkerBox(InventoryCrafting inv) {
        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (Block.getBlockFromItem(stack.getItem()) instanceof BlockShulkerBox) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Every colour getter on BlockShulkerBox is client-only in 1.12; getBlockByColor isn't. */
    private static EnumDyeColor getColor(Block block) {
        for (EnumDyeColor color : EnumDyeColor.values()) {
            if (BlockShulkerBox.getBlockByColor(color) == block) {
                return color;
            }
        }
        return EnumDyeColor.PURPLE;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    /** Registered in recipes/_factories.json as "sstoragelegacy:shulker_box_from_vanilla". */
    public static class Factory implements IRecipeFactory {
        @Override
        public ShulkerBoxFromVanillaRecipe parse(JsonContext context, JsonObject json) {
            ShapelessOreRecipe recipe = ShapelessOreRecipe.factory(context, json);
            String group = recipe.getGroup();
            return new ShulkerBoxFromVanillaRecipe(group.isEmpty() ? null : new ResourceLocation(group), recipe.getRecipeOutput(), recipe.getIngredients());
        }
    }
}
