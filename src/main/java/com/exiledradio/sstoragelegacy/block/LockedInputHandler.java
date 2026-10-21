/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.util.IItemHandlerSimpleInserter;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;

/**
 * What hoppers and pipes see of a locked storage (the original's ContentsFilteredItemHandler):
 * only items the storage already holds go in. Taking items out is unchanged.
 */
class LockedInputHandler implements IItemHandlerSimpleInserter {
    private final IItemHandlerSimpleInserter delegate;
    private final IItemHandler contents;

    LockedInputHandler(IItemHandlerSimpleInserter delegate, IItemHandler contents) {
        this.delegate = delegate;
        this.contents = contents;
    }

    IItemHandlerSimpleInserter getDelegate() {
        return delegate;
    }

    private boolean isAlreadyStored(ItemStack stack) {
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            if (ItemHandlerHelper.canItemStacksStack(contents.getStackInSlot(slot), stack)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(@Nonnull ItemStack stack, boolean simulate) {
        return isAlreadyStored(stack) ? delegate.insertItem(stack, simulate) : stack;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return isAlreadyStored(stack) ? delegate.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        delegate.setStackInSlot(slot, stack);
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return isAlreadyStored(stack) && delegate.isItemValid(slot, stack);
    }
}
