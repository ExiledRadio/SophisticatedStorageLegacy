/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.block.LimitedBarrelBlock;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** A limited barrel in item form: its slot count and base stack multiplier in the tooltip. */
public class LimitedBarrelItem extends BarrelItem {
    private final LimitedBarrelBlock limitedBarrel;

    public LimitedBarrelItem(LimitedBarrelBlock block) {
        super(block);
        this.limitedBarrel = block;
    }

    @SideOnly(Side.CLIENT)
    @Override
    @SuppressWarnings("deprecation")
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        int slots = limitedBarrel.getSlotCount();
        String key = "tile.sstoragelegacy." + (slots == 1 ? "limited_barrel_singular" : "limited_barrel_plural") + ".tooltip";
        tooltip.add(TextFormatting.DARK_GRAY + I18n.translateToLocalFormatted(key, slots, limitedBarrel.getBaseStackSizeMultiplier()));
        super.addInformation(stack, world, tooltip, flag);
    }

    /** limited_<n>_ then the barrel's key: see tools/gen_limited_barrel_models.py. */
    @Override
    public String getModelKeyPrefix() {
        return "limited_" + limitedBarrel.getSlotCount() + "_" + super.getModelKeyPrefix();
    }
}
