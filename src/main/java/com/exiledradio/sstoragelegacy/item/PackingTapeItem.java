/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Right-click a barrel to pack it: it can't be opened then, and breaking it gives a barrel item
 * that keeps everything inside, unpacked again when placed. 8 uses, as in the original.
 */
public class PackingTapeItem extends Item {
    public PackingTapeItem(CreativeTabs tab) {
        setRegistryName(SophisticatedStorageLegacy.MODID, "packing_tape");
        setTranslationKey(SophisticatedStorageLegacy.MODID + ".packing_tape");
        setMaxStackSize(1);
        setMaxDamage(8);
        setCreativeTab(tab);
    }

    // Item.addInformation is itself client-only, so FML strips this override on a dedicated server.
    @SideOnly(Side.CLIENT)
    @Override
    @SuppressWarnings("deprecation")
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        tooltip.add(TextFormatting.DARK_GRAY + I18n.translateToLocalFormatted("item.sstoragelegacy.packing_tape.tooltip",
                TextFormatting.GREEN + String.valueOf(getMaxDamage(stack) - getDamage(stack)) + TextFormatting.DARK_GRAY));
    }
}
