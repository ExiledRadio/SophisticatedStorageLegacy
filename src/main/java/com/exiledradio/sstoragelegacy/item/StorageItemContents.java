/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.block.StorageTileEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.inventory.SlotFurnaceOutput;
import net.minecraft.inventory.SlotMerchantResult;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * The contents a shulker box item carries, used without placing it (the original's
 * StackStorageWrapper): a tile outside the world reads the stack, takes items like the placed box
 * would (its slots, stack upgrades and refusals), and writes them back.
 *
 * <p>Stashing (the original's IStashStorageItem): right-click a slot while carrying a shulker box to
 * put that slot's items in the box, or right-click a shulker box in a slot while carrying items to
 * put those in. 1.12 has no item hook for clicks in inventories, so the client sends the click as a
 * StashMessage and the server calls {@link #tryStash}.
 */
public final class StorageItemContents {
    private StorageItemContents() {}

    /** A shulker box item on its own: one that can carry contents. */
    public static boolean isStashStorage(ItemStack stack) {
        return stack.getItem() instanceof ShulkerBoxItem && stack.getCount() == 1;
    }

    /** Puts a stack into a storage item's contents; returns what didn't fit. Simulating leaves the item as it was. */
    public static ItemStack insert(ItemStack storageStack, ItemStack stack, boolean simulate) {
        Block block = Block.getBlockFromItem(storageStack.getItem());
        if (stack.isEmpty() || !(block instanceof StorageBlockBase)) {
            return stack;
        }
        StorageTileEntity tile = ((StorageBlockBase) block).createDetachedTile(storageStack);
        ItemStack remaining = ItemHandlerHelper.insertItem(tile.getStorageWrapper().getInventoryForInputOutput(), stack.copy(), simulate);
        if (!simulate && remaining.getCount() != stack.getCount()) {
            tile.writeToStack(storageStack, true);
        }
        return remaining;
    }

    /** Whether a right-click on this slot would stash anything; with simulate false, it does. */
    public static boolean tryStash(Slot slot, EntityPlayer player, boolean simulate) {
        ItemStack carried = player.inventory.getItemStack();
        ItemStack inSlot = slot.getStack();
        if (isStashStorage(carried) && !inSlot.isEmpty() && slot.canTakeStack(player) && !isOutputSlot(slot)) {
            // The slot's items into the carried box.
            int fits = inSlot.getCount() - insert(carried, inSlot, true).getCount();
            if (fits <= 0) {
                return false;
            }
            if (!simulate) {
                ItemStack taken = slot.decrStackSize(fits);
                insert(carried, taken, false);
                slot.onSlotChanged();
            }
            return true;
        }
        if (isStashStorage(inSlot) && !carried.isEmpty() && !isOutputSlot(slot)) {
            // The carried items into the box in the slot.
            ItemStack remaining = insert(inSlot, carried, simulate);
            if (remaining.getCount() == carried.getCount()) {
                return false;
            }
            if (!simulate) {
                player.inventory.setItemStack(remaining);
                slot.putStack(inSlot);
            }
            return true;
        }
        return false;
    }

    /** Taking from a result slot crafts or smelts, so stashing never takes from one. */
    private static boolean isOutputSlot(Slot slot) {
        return slot instanceof SlotCrafting || slot instanceof SlotFurnaceOutput || slot instanceof SlotMerchantResult;
    }
}
