/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.block.StorageTier;
import com.exiledradio.sstoragelegacy.block.WoodType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Any storage block in item form. Wood, colours and the like live in NBT (see
 * {@link StorageItemData}); a packed storage or a shulker box also carries its contents.
 */
public class StorageBlockItem extends ItemBlock {
    private final StorageTier tier;

    public StorageBlockItem(StorageBlockBase block) {
        super(block);
        this.tier = block.getTier();
        setRegistryName(block.getRegistryName());
    }

    public StorageTier getTier() {
        return tier;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        // Server-side I18n so names are right in server messages too; the lang file is on both sides.
        WoodType woodType = StorageItemData.getWoodType(stack);
        String key = getTranslationKey(stack) + ".name";
        // With materials no wood shows, so the name leaves it out, as in the original.
        if (woodType == null || !BarrelMaterials.get(stack).isEmpty()) {
            return I18n.translateToLocalFormatted(key, "", "").trim();
        }
        return I18n.translateToLocalFormatted(key, I18n.translateToLocal(woodType.getTranslationKey()), " ").trim();
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull net.minecraft.entity.player.EntityPlayer player, @Nonnull World world, @Nonnull net.minecraft.util.math.BlockPos pos,
                                net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull net.minecraft.block.state.IBlockState newState) {
        if (StorageItemData.isDoubleChest(stack) && getBlock() instanceof com.exiledradio.sstoragelegacy.block.ChestBlock
                && !com.exiledradio.sstoragelegacy.block.ChestBlock.canPlaceDoubleChest(world, pos, player)) {
            return false;
        }
        return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }

    // Item.addInformation is itself client-only, so FML strips this override on a dedicated
    // server along with the client-only ITooltipFlag in its signature.
    @SideOnly(Side.CLIENT)
    @Override
    @SuppressWarnings("deprecation")
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        if (stack.getTagCompound() != null && stack.getTagCompound().getBoolean(StorageItemData.PACKED_TAG)) {
            tooltip.add(TextFormatting.GOLD + I18n.translateToLocal("tile.sstoragelegacy.barrel.tooltip.packed").trim());
        }
        if (com.exiledradio.sstoragelegacy.client.StorageTooltipRenderer.showsContents(stack) && !net.minecraft.client.gui.GuiScreen.isShiftKeyDown()) {
            tooltip.add(com.exiledradio.sstoragelegacy.client.StorageTooltipRenderer.getShiftHint());
        }
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab) || !tier.isAvailable()) {
            return;
        }
        // The original's list for wooden storages: every wood for every tier, then the dyed ones
        // (hidden for iron and up when the client config says so).
        for (WoodType woodType : WoodType.values()) {
            if (woodType.isAvailable()) {
                items.add(StorageItemData.setWoodType(new ItemStack(this), woodType));
            }
        }
        addExtraSubItems(items);
        if (com.exiledradio.sstoragelegacy.ModConfig.client.showHigherTierTintedVariants
                || !(getBlock() instanceof com.exiledradio.sstoragelegacy.block.StorageBlockBase)
                || ((com.exiledradio.sstoragelegacy.block.StorageBlockBase) getBlock()).getTier() == com.exiledradio.sstoragelegacy.block.StorageTier.WOOD) {
            addTintedSubItems(items);
        }
    }

    /** Between the woods and the dyed ones (barrels: the flat-top example). */
    protected void addExtraSubItems(NonNullList<ItemStack> items) {
    }

    /** All 16 dyes on both parts, then the original's yellow/lime example. */
    protected void addTintedSubItems(NonNullList<ItemStack> items) {
        for (EnumDyeColor color : EnumDyeColor.values()) {
            int rgb = colorOf(color);
            items.add(StorageItemData.setAccentColor(StorageItemData.setMainColor(new ItemStack(this), rgb), rgb));
        }
        items.add(StorageItemData.setAccentColor(StorageItemData.setMainColor(new ItemStack(this), colorOf(EnumDyeColor.YELLOW)), colorOf(EnumDyeColor.LIME)));
    }

    /** A dye's colour as the dye recipe applies it to an undyed part. */
    public static int colorOf(EnumDyeColor color) {
        float[] rgb = color.getColorComponentValues();
        return ((int) (rgb[0] * 255.0F) << 16) | ((int) (rgb[1] * 255.0F) << 8) | (int) (rgb[2] * 255.0F);
    }
}
