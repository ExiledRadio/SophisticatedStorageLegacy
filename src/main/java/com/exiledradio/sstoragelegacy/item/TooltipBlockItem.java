/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** A plain block item with the block's ".tooltip" lines (controller, storage link). */
public class TooltipBlockItem extends ItemBlock {
    public TooltipBlockItem(Block block) {
        super(block);
        //noinspection ConstantConditions - registered blocks have a registry name
        setRegistryName(block.getRegistryName());
    }

    @SideOnly(Side.CLIENT)
    @Override
    @SuppressWarnings("deprecation")
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        for (String line : I18n.translateToLocal(block.getTranslationKey() + ".tooltip").split("\\\\n")) {
            tooltip.add(TextFormatting.DARK_GRAY + line);
        }
    }
}
