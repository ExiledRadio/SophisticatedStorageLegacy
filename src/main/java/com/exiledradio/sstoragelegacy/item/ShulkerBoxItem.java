/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.block.ShulkerBoxBlock;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;

/**
 * A shulker box in item form: no wood, just colours, and it carries its contents (the original
 * keeps them in world data by UUID; here they are in the stack, like a packed barrel).
 */
public class ShulkerBoxItem extends StorageBlockItem {
    public ShulkerBoxItem(ShulkerBoxBlock block) {
        super(block);
        setMaxStackSize(1);
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab) || !getTier().isAvailable()) {
            return;
        }
        // The original's list: the plain box, then the dyed ones.
        items.add(new ItemStack(this));
        addTintedSubItems(items);
    }
}
