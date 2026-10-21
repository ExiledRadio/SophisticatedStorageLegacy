/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.common.gui;

import com.exiledradio.sstoragelegacy.block.DecorationTableTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/**
 * The decoration table's slots (the original 1.20's DecorationTableMenu): seven part slots in two
 * columns, the three dyes, the storage to decorate, its result, and the player's inventory.
 */
public class DecorationTableContainer extends Container {
    public static final int DECORATION_SLOT_PADDING = 12;
    public static final int FIRST_DYE_SLOT = 7;
    public static final int STORAGE_SLOT = 10;
    public static final int RESULT_SLOT = 11;
    public static final int FIRST_PLAYER_SLOT = 12;

    private final DecorationTableTileEntity tile;
    private final Slot resultSlot;
    private final Slot storageSlot;

    public DecorationTableContainer(EntityPlayer player, DecorationTableTileEntity tile) {
        this.tile = tile;
        ItemStackHandler blocks = tile.getDecorativeBlocks();
        int yOffset = 17;
        int y = yOffset;
        for (int slot = 0; slot < 4; slot++) {
            addSlotToContainer(new SlotItemHandler(blocks, slot, 8, y));
            y += 18 + DECORATION_SLOT_PADDING;
        }
        y = yOffset + 18 + DECORATION_SLOT_PADDING;
        for (int slot = 4; slot < 7; slot++) {
            addSlotToContainer(new SlotItemHandler(blocks, slot, 56, y));
            y += 18 + (slot < 6 ? DECORATION_SLOT_PADDING : 0);
        }
        int dyeX = 100;
        for (int slot = 0; slot < 3; slot++) {
            addSlotToContainer(new SlotItemHandler(tile.getDyes(), slot, dyeX + slot * 18, yOffset));
        }
        storageSlot = addSlotToContainer(new SlotItemHandler(tile.getStorageBlock(), 0, dyeX, getSlot(DecorationTableTileEntity.BOTTOM_TRIM_SLOT).yPos));
        resultSlot = addSlotToContainer(new ResultSlot(dyeX + 36, storageSlot.yPos));
        int playerY = y + 14;
        InventoryPlayer inventory = player.inventory;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 45 + col * 18, playerY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inventory, col, 45 + col * 18, playerY + 3 * 18 + 4));
        }
    }

    public DecorationTableTileEntity getTile() {
        return tile;
    }

    public Slot getResultSlot() {
        return resultSlot;
    }

    public Slot getStorageSlot() {
        return storageSlot;
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        return !tile.isInvalid() && player.getDistanceSq(tile.getPos().getX() + 0.5D, tile.getPos().getY() + 0.5D, tile.getPos().getZ() + 0.5D) <= 64.0D;
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = slot.getStack();
        ItemStack copy = slotStack.copy();
        if (index >= FIRST_PLAYER_SLOT) {
            boolean moved = false;
            if (tile.getDecorativeBlocks().isItemValid(0, slotStack)) {
                moved = mergeItemStack(slotStack, 0, FIRST_DYE_SLOT, false);
            } else if (isDye(slotStack)) {
                moved = mergeItemStack(slotStack, FIRST_DYE_SLOT, STORAGE_SLOT, false);
            } else if (tile.getStorageBlock().isItemValid(0, slotStack)) {
                moved = mergeItemStack(slotStack, STORAGE_SLOT, STORAGE_SLOT + 1, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else if (index == RESULT_SLOT) {
            if (!resultSlot.canTakeStack(player) || !mergeItemStack(slotStack, FIRST_PLAYER_SLOT, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
            resultSlot.onTake(player, copy);
            return copy;
        } else if (!mergeItemStack(slotStack, FIRST_PLAYER_SLOT, inventorySlots.size(), true)) {
            return ItemStack.EMPTY;
        }
        if (slotStack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (copy.getCount() == slotStack.getCount()) {
            return ItemStack.EMPTY;
        }
        tile.updateResultAndMarkDirty();
        return copy;
    }

    private boolean isDye(ItemStack stack) {
        for (int slot = 0; slot < 3; slot++) {
            if (tile.getDyes().isItemValid(slot, stack)) {
                return true;
            }
        }
        return false;
    }

    /** The decorated result: taking it uses up what it needs and the storage it was made from. */
    private class ResultSlot extends Slot {
        ResultSlot(int x, int y) {
            super(new InventoryBasic("result", false, 1) {
                @Nonnull
                @Override
                public ItemStack getStackInSlot(int index) {
                    return tile.getResult();
                }

                @Nonnull
                @Override
                public ItemStack decrStackSize(int index, int count) {
                    ItemStack result = tile.getResult();
                    if (result.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    ItemStack taken = result.copy();
                    taken.setCount(Math.min(count, result.getCount()));
                    return taken;
                }
            }, 0, x, y);
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return !tile.getResult().isEmpty() && tile.getMissingDyes().isEmpty();
        }

        @Nonnull
        @Override
        public ItemStack onTake(EntityPlayer player, @Nonnull ItemStack stack) {
            if (!player.world.isRemote) {
                tile.consumeIngredientsOnCraft();
                tile.getStorageBlock().extractItem(0, 1, false);
            }
            return super.onTake(player, stack);
        }
    }
}
