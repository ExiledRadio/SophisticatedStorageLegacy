/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/**
 * A shulker box: dyeable (no wood), its shell lifts and turns while open, and it never takes other
 * shulker boxes (ours or vanilla), as in the original. Breaking it keeps the contents in the item.
 */
public class ShulkerBoxTileEntity extends StorageTileEntity {
    private static final int OPEN_EVENT = 1;

    private boolean opened = false;
    private float progress = 0.0F;
    private float prevProgress = 0.0F;

    @Override
    public boolean isAllowedInStorage(ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        return !(block instanceof ShulkerBoxBlock) && !(block instanceof BlockShulkerBox)
                && !com.exiledradio.sstoragelegacy.ModConfig.shulkerBoxDisallowedItems.isItemDisallowed(stack);
    }

    @Override
    protected void onOpenStateChanged(boolean open) {
        world.addBlockEvent(pos, getBlockType(), OPEN_EVENT, open ? 1 : 0);
        EnumFacing facing = getBlockType() instanceof ShulkerBoxBlock ? world.getBlockState(pos).getValue(ShulkerBoxBlock.FACING) : EnumFacing.UP;
        playSound(open ? SoundEvents.BLOCK_SHULKER_BOX_OPEN : SoundEvents.BLOCK_SHULKER_BOX_CLOSE, facing, 0.5F);
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        if (id == OPEN_EVENT) {
            opened = type > 0;
            return true;
        }
        return super.receiveClientEvent(id, type);
    }

    @Override
    public void update() {
        super.update();
        prevProgress = progress;
        if (opened && progress < 1.0F) {
            progress = Math.min(1.0F, progress + 0.1F);
        } else if (!opened && progress > 0.0F) {
            progress = Math.max(0.0F, progress - 0.1F);
        }
    }

    /** How far the shell is lifted, 0 to 1, between ticks. */
    public float getProgress(float partialTicks) {
        return prevProgress + (progress - prevProgress) * partialTicks;
    }

    public boolean isClosed() {
        return progress <= 0.0F && !opened;
    }
}
