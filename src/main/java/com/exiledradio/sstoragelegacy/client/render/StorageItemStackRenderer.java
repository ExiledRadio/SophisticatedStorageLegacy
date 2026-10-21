/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.render;

import com.exiledradio.sstoragelegacy.block.ChestBlock;
import com.exiledradio.sstoragelegacy.block.ShulkerBoxBlock;
import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

/**
 * Chest and shulker box items: drawn with the same layered models as the placed blocks, from the
 * look in the stack's NBT (the original's ChestItemRenderer / ShulkerBoxItemRenderer). The item
 * models are "builtin/entity" with vanilla's chest display transforms.
 */
public class StorageItemStackRenderer extends TileEntityItemStackRenderer {
    @Override
    public void renderByItem(@Nonnull ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (!(block instanceof StorageBlockBase)) {
            return;
        }
        StorageBlockBase storage = (StorageBlockBase) block;
        int main = StorageItemData.getMainColor(stack);
        int accent = StorageItemData.getAccentColor(stack);
        if (block instanceof ChestBlock) {
            NBTTagCompound tag = stack.getTagCompound();
            boolean packed = tag != null && tag.getBoolean(StorageItemData.PACKED_TAG);
            if (StorageItemData.isDoubleChest(stack)) {
                // Both halves side by side, scaled down to fit (the original's ChestItemRenderer).
                GlStateManager.pushMatrix();
                GlStateManager.scale(0.8F, 0.8F, 0.8F);
                GlStateManager.translate(0.72, 0.0, 0.0);
                ChestRenderer.renderChest(null, 0, 0, 0, EnumFacing.SOUTH, ChestBlock.Type.LEFT, 0.0F, StorageItemData.getWoodType(stack), main, accent,
                        storage.getTier(), packed, false, 1.0F, -1);
                ChestRenderer.renderChest(null, -1, 0, 0, EnumFacing.SOUTH, ChestBlock.Type.RIGHT, 0.0F, StorageItemData.getWoodType(stack), main, accent,
                        storage.getTier(), packed, false, 1.0F, -1);
                GlStateManager.popMatrix();
            } else {
                ChestRenderer.renderChest(null, 0, 0, 0, EnumFacing.SOUTH, ChestBlock.Type.SINGLE, 0.0F, StorageItemData.getWoodType(stack), main, accent,
                        storage.getTier(), packed, false, 1.0F, -1);
            }
        } else if (block instanceof ShulkerBoxBlock) {
            ShulkerBoxRenderer.renderShulkerBox(null, 0, 0, 0, EnumFacing.UP, 0.0F, main, accent, storage.getTier(), -1);
        }
    }
}
