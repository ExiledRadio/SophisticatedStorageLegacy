/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.common;

import com.exiledradio.sstoragelegacy.block.LimitedBarrelBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** World interaction the blocks can't see themselves (the original's CommonEventHandler). */
public class CommonEventHandler {
    private static final int AVERAGE_MAX_ITEM_ENTITY_DROP_COUNT = 20;

    /**
     * Breaking a storage that would spill more item entities than the config allows is refused
     * with a hint to use packing tape, unless the player sneaks (the original's onBlockBreak).
     */
    @SubscribeEvent
    public void onBlockBreak(net.minecraftforge.event.world.BlockEvent.BreakEvent event) {
        net.minecraft.entity.player.EntityPlayer player = event.getPlayer();
        net.minecraft.tileentity.TileEntity storageTe = event.getWorld().getTileEntity(event.getPos());
        if (player != null && storageTe instanceof com.exiledradio.sstoragelegacy.block.StorageTileEntity
                && com.exiledradio.scorelegacy.upgrades.infinity.InfinityUpgradeItem.isLockedFor(
                ((com.exiledradio.sstoragelegacy.block.StorageTileEntity) storageTe).getMainTile().getStorageWrapper().getUpgradeHandler(), player)) {
            event.setCanceled(true);
            net.minecraft.util.text.ITextComponent message = new net.minecraft.util.text.TextComponentTranslation("gui.sstoragelegacy.status.infinity_upgrade_only_admin_break");
            message.getStyle().setColor(net.minecraft.util.text.TextFormatting.RED);
            player.sendStatusMessage(message, true);
            return;
        }
        if (player == null || player.isSneaking() || !(event.getState().getBlock() instanceof com.exiledradio.sstoragelegacy.block.StorageBlockBase)
                || ((com.exiledradio.sstoragelegacy.block.StorageBlockBase) event.getState().getBlock()).keepsContentsWhenBroken()) {
            return;
        }
        net.minecraft.tileentity.TileEntity te = event.getWorld().getTileEntity(event.getPos());
        if (!(te instanceof com.exiledradio.sstoragelegacy.block.StorageTileEntity) || ((com.exiledradio.sstoragelegacy.block.StorageTileEntity) te).isPacked()) {
            return;
        }
        com.exiledradio.sstoragelegacy.block.StorageTileEntity storage = (com.exiledradio.sstoragelegacy.block.StorageTileEntity) te;
        net.minecraftforge.items.IItemHandler inventory = storage.getStorageWrapper().getInventoryHandler();
        int firstSlot = 0;
        if (storage.getMainTile() != storage) {
            // The half of a double chest that keeps nothing spills its share of the main's slots, the last ones.
            int ownSlots = inventory.getSlots();
            inventory = storage.getMainTile().getStorageWrapper().getInventoryHandler();
            firstSlot = Math.max(0, inventory.getSlots() - ownSlots);
        }
        int itemEntities = 0;
        for (int slot = firstSlot; slot < inventory.getSlots(); slot++) {
            net.minecraft.item.ItemStack stack = inventory instanceof com.exiledradio.scorelegacy.inventory.InventoryHandler
                    ? ((com.exiledradio.scorelegacy.inventory.InventoryHandler) inventory).getRawStack(slot) : inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                itemEntities += (int) Math.ceil(stack.getCount() / (double) Math.min(stack.getMaxStackSize(), AVERAGE_MAX_ITEM_ENTITY_DROP_COUNT));
            }
        }
        if (itemEntities > com.exiledradio.sstoragelegacy.ModConfig.tooManyItemEntityDrops) {
            event.setCanceled(true);
            net.minecraft.util.text.ITextComponent name = storage.getDisplayName().createCopy();
            name.getStyle().setColor(net.minecraft.util.text.TextFormatting.GREEN);
            net.minecraft.util.text.ITextComponent count = new net.minecraft.util.text.TextComponentString(String.valueOf(itemEntities));
            count.getStyle().setColor(net.minecraft.util.text.TextFormatting.RED);
            net.minecraft.util.text.ITextComponent tape = new net.minecraft.util.text.TextComponentTranslation("item.sstoragelegacy.packing_tape.name");
            tape.getStyle().setColor(net.minecraft.util.text.TextFormatting.GREEN);
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("gui.sstoragelegacy.status.too_many_item_entity_drops", name, count, tape));
        }
    }

    /**
     * Creative left-click on a limited barrel's front takes items instead of breaking it
     * (survival goes through Block.onBlockClicked, which creative never calls).
     */
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isRemote || !event.getEntityPlayer().capabilities.isCreativeMode) {
            return;
        }
        IBlockState state = event.getWorld().getBlockState(event.getPos());
        if (state.getBlock() instanceof LimitedBarrelBlock
                && ((LimitedBarrelBlock) state.getBlock()).tryToTakeItem(event.getWorld(), event.getPos(), event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    /** Sneak + right-click with a dye on a limited barrel's front dyes all its counts (1.12 skips the block when sneaking with an item). */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote || !event.getEntityPlayer().isSneaking() || event.getFace() == null) {
            return;
        }
        IBlockState state = event.getWorld().getBlockState(event.getPos());
        if (state.getBlock() instanceof LimitedBarrelBlock
                && ((LimitedBarrelBlock) state.getBlock()).tryToDyeAll(event.getWorld(), event.getPos(), state, event.getFace(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            return;
        }
        // Sneak + right-click with upgrades: fill a storage's upgrade slots from its front, or every storage through the controller.
        boolean filled = false;
        if (state.getBlock() instanceof com.exiledradio.sstoragelegacy.block.StorageBlockBase
                && event.getFace() == ((com.exiledradio.sstoragelegacy.block.StorageBlockBase) state.getBlock()).getFacing(state)) {
            net.minecraft.tileentity.TileEntity te = event.getWorld().getTileEntity(event.getPos());
            filled = te instanceof com.exiledradio.sstoragelegacy.block.StorageTileEntity
                    && com.exiledradio.sstoragelegacy.block.StorageBlockBase.tryFillUpgrades(event.getEntityPlayer(), event.getHand(),
                    (com.exiledradio.sstoragelegacy.block.StorageTileEntity) te);
        } else if (state.getBlock() instanceof com.exiledradio.sstoragelegacy.block.ControllerBlock) {
            filled = com.exiledradio.sstoragelegacy.block.ControllerBlock.tryFillUpgrades(event.getWorld(), event.getPos(), event.getEntityPlayer(), event.getHand());
        }
        if (filled) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }
}
