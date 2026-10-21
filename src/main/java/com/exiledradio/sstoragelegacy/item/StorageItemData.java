/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.block.WoodType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

/**
 * The look of a storage block in item form, in the stack's NBT. Same tag names as the
 * original's StorageBlockItem / WoodStorageBlockItem / BarrelBlockItem.
 */
public final class StorageItemData {
    public static final String WOOD_TYPE_TAG = "woodType";
    public static final String MAIN_COLOR_TAG = "mainColor";
    public static final String ACCENT_COLOR_TAG = "accentColor";
    public static final String FLAT_TOP_TAG = "flatTop";
    /** A barrel packed with packing tape: it carries its contents (CONTENTS_TAG) as an item. */
    public static final String PACKED_TAG = "packed";
    public static final String CONTENTS_TAG = "contents";
    /** A packed double chest: placing it puts down both halves (the original's ChestBlockItem tag). */
    public static final String DOUBLE_CHEST_TAG = "doubleChest";

    /** "No colour" for main / accent, as in the original. */
    public static final int NO_COLOR = -1;

    private StorageItemData() {}

    @Nullable
    public static WoodType getWoodType(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(WOOD_TYPE_TAG) ? WoodType.fromName(tag.getString(WOOD_TYPE_TAG)) : null;
    }

    public static ItemStack setWoodType(ItemStack stack, @Nullable WoodType woodType) {
        if (woodType == null) {
            removeTag(stack, WOOD_TYPE_TAG);
        } else {
            getOrCreateTag(stack).setString(WOOD_TYPE_TAG, woodType.getName());
        }
        return stack;
    }

    public static int getMainColor(ItemStack stack) {
        return getColor(stack, MAIN_COLOR_TAG);
    }

    public static int getAccentColor(ItemStack stack) {
        return getColor(stack, ACCENT_COLOR_TAG);
    }

    public static ItemStack setMainColor(ItemStack stack, int color) {
        setColor(stack, MAIN_COLOR_TAG, color);
        return stack;
    }

    public static ItemStack setAccentColor(ItemStack stack, int color) {
        setColor(stack, ACCENT_COLOR_TAG, color);
        return stack;
    }

    public static boolean isFlatTop(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean(FLAT_TOP_TAG);
    }

    public static ItemStack setFlatTop(ItemStack stack, boolean flatTop) {
        if (flatTop) {
            getOrCreateTag(stack).setBoolean(FLAT_TOP_TAG, true);
        } else {
            removeTag(stack, FLAT_TOP_TAG);
        }
        return stack;
    }

    public static boolean isDoubleChest(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean(DOUBLE_CHEST_TAG);
    }

    public static ItemStack setDoubleChest(ItemStack stack, boolean doubleChest) {
        if (doubleChest) {
            getOrCreateTag(stack).setBoolean(DOUBLE_CHEST_TAG, true);
        } else {
            removeTag(stack, DOUBLE_CHEST_TAG);
        }
        return stack;
    }

    /** Whether contents carried in the stack (packed storage, shulker box) include any upgrade. */
    public static boolean hasUpgrades(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(CONTENTS_TAG)) {
            return false;
        }
        NBTTagCompound upgrades = tag.getCompoundTag(CONTENTS_TAG).getCompoundTag(com.exiledradio.scorelegacy.inventory.UpgradeHandler.UPGRADE_INVENTORY_TAG);
        return upgrades.getTagList("Items", net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND).tagCount() > 0;
    }

    private static int getColor(ItemStack stack, String key) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(key) ? tag.getInteger(key) : NO_COLOR;
    }

    private static void setColor(ItemStack stack, String key, int color) {
        if (color == NO_COLOR) {
            removeTag(stack, key);
        } else {
            getOrCreateTag(stack).setInteger(key, color);
        }
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    private static void removeTag(ItemStack stack, String key) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return;
        }
        tag.removeTag(key);
        if (tag.isEmpty()) {
            // An empty compound would stop this stack stacking with a plain one.
            stack.setTagCompound(null);
        }
    }
}
