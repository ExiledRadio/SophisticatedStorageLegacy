/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.block.StorageTier;
import com.exiledradio.sstoragelegacy.block.TintMode;
import com.exiledradio.sstoragelegacy.block.WoodType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** The barrel in item form: also a flat top, and one merged item model per look. */
public class BarrelItem extends StorageBlockItem {

    public BarrelItem(StorageBlockBase block) {
        super(block);
    }

    // Item.addInformation is itself client-only, so FML strips this override on a dedicated
    // server along with the client-only ITooltipFlag in its signature.
    @SideOnly(Side.CLIENT)
    @Override
    @SuppressWarnings("deprecation")
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        if (StorageItemData.isFlatTop(stack)) {
            tooltip.add(I18n.translateToLocal("tile.sstoragelegacy.barrel.tooltip.flat_top").trim());
        }
        super.addInformation(stack, world, tooltip, flag);
    }

    @Override
    protected void addExtraSubItems(NonNullList<ItemStack> items) {
        // The original lists a flat-top example for the wood tier only.
        if (getTier() == StorageTier.WOOD) {
            items.add(StorageItemData.setFlatTop(StorageItemData.setWoodType(new ItemStack(this), WoodType.ACACIA), true));
        }
    }

    /**
     * The item model for a stack, matching tools/gen_barrel_models.py: the tier prefix, then
     * item_key: wood plus which tints, or "tinted" when both are dyed (no wood shows then), plus "_flat".
     */
    public String getModelKey(ItemStack stack) {
        return getModelKeyPrefix() + getLookKey(stack);
    }

    /** What every model name of this item starts with: the tier prefix ("" or e.g. "iron_"). */
    public String getModelKeyPrefix() {
        return getTier().getModelPrefix();
    }

    /** The look part of the model name, the same for every tier. */
    public static String getLookKey(ItemStack stack) {
        int main = StorageItemData.getMainColor(stack);
        int accent = StorageItemData.getAccentColor(stack);
        TintMode tint = TintMode.of(main != StorageItemData.NO_COLOR, accent != StorageItemData.NO_COLOR);
        WoodType woodType = StorageItemData.getWoodType(stack);
        String key;
        if (tint == TintMode.BOTH) {
            key = "tinted";
        } else {
            key = (woodType != null ? woodType : WoodType.DEFAULT).getName() + (tint == TintMode.NONE ? "" : "_" + tint.getName());
        }
        return key + (StorageItemData.isFlatTop(stack) ? "_flat" : "");
    }
}
