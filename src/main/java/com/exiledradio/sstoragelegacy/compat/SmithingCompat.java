/*
 * Written for Sophisticated Storage Legacy by ExiledRadio, GPLv3.
 */
package com.exiledradio.sstoragelegacy.compat;

import com.exiledradio.scorelegacy.compat.MaterialCompat;
import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.item.StorageBlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * Diamond storages become netherite ones on a smithing table, as in the original, where a mod
 * adds one through Backport Core's smithing upgrades (read by reflection, so nothing here needs
 * it). The crafting-grid recipes stay. The result carries {@link StorageBlockItem#SMITHED_FROM_TAG}
 * so taking it grows carried contents to the netherite tier.
 */
public final class SmithingCompat {
    private static final String[] KINDS = {"barrel", "chest", "shulker_box", "limited_barrel_1", "limited_barrel_2", "limited_barrel_3", "limited_barrel_4"};

    private SmithingCompat() {}

    /** postInit, after Core has decided whether netherite exists. */
    public static void postInit() {
        if (!Loader.isModLoaded("backportcore") || !MaterialCompat.Material.NETHERITE.isAvailable()) {
            return;
        }
        try {
            Method add = Class.forName("com.exiledradio.backportcore.smithing.SmithingUpgrades")
                    .getMethod("add", Item.class, Ingredient.class, ItemStack.class, String.class);
            int added = 0;
            for (String kind : KINDS) {
                String fromName = SophisticatedStorageLegacy.MODID + ":" + (kind.startsWith("limited_") ? "limited_diamond_" + kind.substring(8) : "diamond_" + kind);
                String toName = SophisticatedStorageLegacy.MODID + ":" + (kind.startsWith("limited_") ? "limited_netherite_" + kind.substring(8) : "netherite_" + kind);
                Item from = Item.REGISTRY.getObject(new ResourceLocation(fromName));
                Item to = Item.REGISTRY.getObject(new ResourceLocation(toName));
                if (from == null || to == null) {
                    continue;
                }
                ItemStack result = new ItemStack(to);
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString(StorageBlockItem.SMITHED_FROM_TAG, fromName);
                result.setTagCompound(tag);
                add.invoke(null, from, null, result, SophisticatedStorageLegacy.MODID);
                added++;
            }
            SophisticatedStorageLegacy.LOGGER.info("{} netherite smithing upgrades added", added);
        } catch (ReflectiveOperationException | RuntimeException e) {
            SophisticatedStorageLegacy.LOGGER.warn("Couldn't add the netherite smithing upgrades", e);
        }
    }
}
