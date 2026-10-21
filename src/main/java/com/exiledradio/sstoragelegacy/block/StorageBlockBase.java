/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.common.gui.GuiHandler;
import com.exiledradio.sstoragelegacy.item.PackingTapeItem;
import com.exiledradio.sstoragelegacy.item.StorageToolItem;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/**
 * What every storage block shares (the original's StorageBlockBase): a tier, a tile with a Core
 * storage, the GUI on right-click, packing tape, drops that carry the look (and the contents when
 * packed, or always for shulker boxes), and comparator output.
 */
public abstract class StorageBlockBase extends Block {
    private final StorageTier tier;
    private final String storageName;

    protected StorageBlockBase(Material material, StorageTier tier, String storageName) {
        this(material, tier, storageName, tier.getRegistryName(storageName));
    }

    /** With a registry name that isn't tier prefix + kind (limited barrels: "limited_iron_barrel_1"). */
    protected StorageBlockBase(Material material, StorageTier tier, String storageName, String name) {
        super(material);
        this.tier = tier;
        if (tier == StorageTier.NETHERITE) {
            // The original's netherite storages resist explosions like obsidian (1200 in 1.16 terms).
            setResistance(2000.0F);
        }
        this.storageName = storageName;
        setTranslationKey(SophisticatedStorageLegacy.MODID + "." + name);
        setRegistryName(SophisticatedStorageLegacy.MODID, name);
    }

    public StorageTier getTier() {
        return tier;
    }

    /** "barrel", "chest", "shulker_box": the kind, shared by every tier. */
    public String getStorageName() {
        return storageName;
    }

    /** Inventory slots a new storage of this block gets (config). */
    public int getDefaultInventorySlots() {
        return com.exiledradio.sstoragelegacy.ModConfig.storages.get(storageName, getTier()).inventorySlotCount;
    }

    /** Upgrade slots a new storage of this block gets (config). */
    public int getDefaultUpgradeSlots() {
        return com.exiledradio.sstoragelegacy.ModConfig.storages.get(storageName, getTier()).upgradeSlotCount;
    }

    /** The direction the storage's front faces. */
    public abstract EnumFacing getFacing(IBlockState state);

    /** Whether the front faces up or down (the original's getVerticalFacing). */
    public VerticalFacing getVerticalFacing(IBlockState state) {
        return VerticalFacing.fromDirection(getFacing(state));
    }

    /** The front's horizontal direction; a barrel facing up counts as north, down as south, as in the original. */
    public EnumFacing getHorizontalDirection(IBlockState state) {
        EnumFacing facing = getFacing(state);
        if (facing == EnumFacing.UP) {
            return EnumFacing.NORTH;
        }
        return facing == EnumFacing.DOWN ? EnumFacing.SOUTH : facing;
    }

    protected abstract StorageTileEntity createStorageTile();

    /** A tile outside any world holding a stack's contents (shulker box items, packed storages). */
    public StorageTileEntity createDetachedTile(ItemStack stack) {
        StorageTileEntity tile = createStorageTile();
        tile.setDetachedBlock(this);
        tile.readFromStack(stack);
        return tile;
    }

    /** Whether breaking it always keeps the contents in the item (shulker boxes). */
    public boolean keepsContentsWhenBroken() {
        return false;
    }

    /** Whether it can be opened right now (a shulker box needs room to open). */
    protected boolean canOpen(World world, BlockPos pos, IBlockState state, StorageTileEntity tile) {
        return true;
    }

    // ---- Tile entity ---------------------------------------------------------------------

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return createStorageTile();
    }

    // ---- Interaction ---------------------------------------------------------------------

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof StorageTileEntity) || ((StorageTileEntity) te).isPacked()) {
            // A packed storage can't be opened; break it to carry it away.
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        StorageTileEntity tile = (StorageTileEntity) te;
        if (hand == EnumHand.OFF_HAND || player.isSpectator()) {
            return true;
        }
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() instanceof PackingTapeItem && !keepsContentsWhenBroken()) {
            if (!player.capabilities.isCreativeMode) {
                held.damageItem(1, player);
            }
            pack(world, pos, state, tile);
            return true;
        }
        if (facing == getFacing(state) && tryAddSingleUpgrade(player, hand, tile)) {
            return true;
        }
        if (!canOpen(world, pos, state, tile)) {
            return true;
        }
        // The half of a double chest that keeps nothing opens the other half's screen.
        GuiHandler.openStorage(player, tile.getMainTile().getPos());
        return true;
    }

    /**
     * One of Storage's upgrades in hand goes into the storage's upgrade slots, if it may (the
     * original's tryAddSingleUpgrade: right-click the front, or the controller for every storage).
     */
    public static boolean tryAddSingleUpgrade(EntityPlayer player, EnumHand hand, StorageTileEntity tile) {
        ItemStack held = player.getHeldItem(hand);
        if (!(held.getItem() instanceof com.exiledradio.scorelegacy.api.IUpgradeItem) || held.getItem().getRegistryName() == null
                || !SophisticatedStorageLegacy.MODID.equals(held.getItem().getRegistryName().getNamespace()) || tile.isPacked()) {
            return false;
        }
        StorageBlockWrapper wrapper = tile.getMainTile().getStorageWrapper();
        if (!((com.exiledradio.scorelegacy.api.IUpgradeItem<?>) held.getItem()).canAddUpgradeTo(wrapper, held, true).isSuccessful()) {
            return false;
        }
        IItemHandlerModifiable upgrades = wrapper.getUpgradeHandler();
        ItemStack one = ItemHandlerHelper.copyStackWithSize(held, 1);
        if (!ItemHandlerHelper.insertItem(upgrades, one, true).isEmpty()) {
            return false;
        }
        ItemHandlerHelper.insertItem(upgrades, one, false);
        held.shrink(1);
        return true;
    }

    /** Sneak + right-click the front: as many of the held upgrades as fit (the original's tryFillUpgrades). */
    public static boolean tryFillUpgrades(EntityPlayer player, EnumHand hand, StorageTileEntity tile) {
        boolean added = false;
        while (!player.getHeldItem(hand).isEmpty() && tryAddSingleUpgrade(player, hand, tile)) {
            added = true;
        }
        return added;
    }

    /** Packing tape: the storage can't be opened, and breaks into an item that keeps the contents. */
    protected void pack(World world, BlockPos pos, IBlockState state, StorageTileEntity tile) {
        tile.setPacked(true);
        // A packed storage leaves the network, as in the original.
        tile.removeFromController();
    }

    /** The blocks next to this storage on a side, which a hopper upgrade moves items to and from. */
    public java.util.List<BlockPos> getNeighborPositions(IBlockState state, BlockPos origin, EnumFacing side) {
        return java.util.Collections.singletonList(origin.offset(side));
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof StorageTileEntity) {
            ((StorageTileEntity) te).readFromStack(stack);
            if (!world.isRemote) {
                ((StorageTileEntity) te).tryToAddToController();
            }
            if (placer != null && placer.getHeldItemOffhand().getItem() instanceof StorageToolItem) {
                StorageToolItem.useOffHandOnPlaced(placer.getHeldItemOffhand(), (StorageTileEntity) te);
            }
            if (!world.isRemote) {
                world.notifyBlockUpdate(pos, state, state, 3);
            }
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof StorageTileEntity) {
            ((StorageTileEntity) te).recheckOpen();
        }
    }

    // ---- Drops ---------------------------------------------------------------------------

    @Override
    public void breakBlock(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof StorageTileEntity) {
            StorageTileEntity tile = (StorageTileEntity) te;
            if (!tile.isUpgrading()) {
                tile.removeFromController();
            }
            if (!tile.isUpgrading() && !tile.isPacked() && !keepsContentsWhenBroken()) {
                // Like vanilla chests (and the original without packing tape), contents and upgrades spill.
                StorageBlockWrapper wrapper = tile.getStorageWrapper();
                spill(world, pos, wrapper.getInventoryHandler());
                spill(world, pos, wrapper.getUpgradeHandler());
            }
            world.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(world, pos, state);
    }

    private static void spill(World world, BlockPos pos, IItemHandlerModifiable handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            // What is really stored: an infinity upgrade shows endless stacks that must not spill.
            ItemStack stack = handler instanceof com.exiledradio.scorelegacy.inventory.InventoryHandler
                    ? ((com.exiledradio.scorelegacy.inventory.InventoryHandler) handler).getRawStack(slot) : handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                // spawnItemStack splits it into normal-sized stacks, so stack-upgraded counts drop whole.
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                if (handler instanceof com.exiledradio.scorelegacy.inventory.InventoryHandler) {
                    ((com.exiledradio.scorelegacy.inventory.InventoryHandler) handler).setRawStack(slot, ItemStack.EMPTY);
                } else {
                    handler.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    /**
     * Keeps the tile entity until the drops are made, so they can carry the look, name and
     * contents. harvestBlock removes the block afterwards.
     */
    @Override
    public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest) {
        if (willHarvest) {
            return true;
        }
        if (!world.isRemote && player.capabilities.isCreativeMode && keepsContentsWhenBroken()) {
            // Creative mode drops nothing, but a shulker box must not lose what is inside (the original does the same).
            if (world.getTileEntity(pos) instanceof StorageTileEntity) {
                spawnAsEntity(world, pos, createStack(world, pos, true));
            }
        }
        return super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    public void harvestBlock(@Nonnull World world, EntityPlayer player, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable TileEntity te, ItemStack tool) {
        super.harvestBlock(world, player, pos, state, te, tool);
        world.setBlockToAir(pos);
    }

    @Override
    public void getDrops(@Nonnull NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, @Nonnull IBlockState state, int fortune) {
        TileEntity te = world.getTileEntity(pos);
        boolean withContents = keepsContentsWhenBroken() || (te instanceof StorageTileEntity && ((StorageTileEntity) te).isPacked());
        drops.add(createStack(world, pos, withContents));
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
        return createStack(world, pos, false);
    }

    /** The storage's own item without contents (wood, colours, tier): pick block, settings tab icon. */
    public ItemStack getStorageStack(IBlockAccess world, BlockPos pos) {
        return createStack(world, pos, false);
    }

    protected ItemStack createStack(IBlockAccess world, BlockPos pos, boolean withContents) {
        ItemStack stack = new ItemStack(this);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof StorageTileEntity) {
            ((StorageTileEntity) te).writeToStack(stack, withContents);
        }
        return stack;
    }

    // ---- Comparator ----------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof StorageTileEntity ? ItemHandlerHelper.calcRedstoneFromInventory(((StorageTileEntity) te).getMainTile().getStorageWrapper().getInventoryHandler()) : 0;
    }

    /**
     * Same list as the item's. The default would add a bare stack, which JEI shows as an extra
     * entry the original doesn't have.
     */
    @Override
    public void getSubBlocks(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        Item.getItemFromBlock(this).getSubItems(tab, items);
    }
}
