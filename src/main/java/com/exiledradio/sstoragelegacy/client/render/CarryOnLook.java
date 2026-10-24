/*
 * Written for Sophisticated Storage Legacy by ExiledRadio, GPLv3.
 */
package com.exiledradio.sstoragelegacy.client.render;

import com.exiledradio.sstoragelegacy.item.BarrelMaterials;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

/**
 * Carry On draws a carried block as a plain {@code new ItemStack(block, 1, meta)}, without the
 * tile data, so a barrel showed the default (acacia) look. It hands the carrier to the item
 * model, though, and keeps the block's tile data in the held {@code carryon:tile_item}
 * ({@code stateid} and {@code tileData}); a barrel tile saves its look under the same keys as
 * the barrel item. Read by name, so nothing here needs Carry On.
 */
final class CarryOnLook {
    private static final String CARRY_ON = "carryon";
    private static final String TILE_DATA = "tileData";
    private static final String STATE_ID = "stateid";
    private static final String[] LOOK_TAGS = {StorageItemData.WOOD_TYPE_TAG, StorageItemData.MAIN_COLOR_TAG, StorageItemData.ACCENT_COLOR_TAG,
            StorageItemData.FLAT_TOP_TAG, BarrelMaterials.MATERIALS_TAG};

    private CarryOnLook() {}

    /**
     * For a plain barrel stack (no NBT) drawn for an entity carrying that very block with Carry On:
     * a stack with the carried barrel's look. Otherwise null.
     */
    @Nullable
    static ItemStack getCarriedLook(ItemStack stack, @Nullable EntityLivingBase entity) {
        if (stack.hasTagCompound() || entity == null) {
            return null;
        }
        ItemStack held = entity.getHeldItemMainhand();
        if (held.isEmpty() || held.getItem().getRegistryName() == null || !CARRY_ON.equals(held.getItem().getRegistryName().getNamespace())) {
            return null;
        }
        NBTTagCompound tag = held.getTagCompound();
        if (tag == null || !tag.hasKey(TILE_DATA) || !tag.hasKey(STATE_ID)) {
            return null;
        }
        Block block = Block.getStateById(tag.getInteger(STATE_ID)).getBlock();
        if (Item.getItemFromBlock(block) != stack.getItem()) {
            return null;
        }
        NBTTagCompound tileData = tag.getCompoundTag(TILE_DATA);
        NBTTagCompound look = new NBTTagCompound();
        for (String key : LOOK_TAGS) {
            if (tileData.hasKey(key)) {
                look.setTag(key, tileData.getTag(key).copy());
            }
        }
        ItemStack lookStack = new ItemStack(stack.getItem(), 1, stack.getMetadata());
        lookStack.setTagCompound(look);
        return lookStack;
    }
}
