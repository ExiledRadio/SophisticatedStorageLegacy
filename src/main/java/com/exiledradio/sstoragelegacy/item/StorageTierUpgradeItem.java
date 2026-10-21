/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.block.StorageTier;
import com.exiledradio.sstoragelegacy.block.StorageTileEntity;
import com.exiledradio.sstoragelegacy.init.ModContent;
import com.exiledradio.scorelegacy.inventory.InventoryHandler;
import com.exiledradio.sstoragelegacy.block.ChestBlock;
import com.exiledradio.sstoragelegacy.block.ChestTileEntity;
import com.exiledradio.sstoragelegacy.block.ControllerTileEntity;
import com.exiledradio.sstoragelegacy.block.ShulkerBoxBlock;
import com.exiledradio.sstoragelegacy.block.WoodType;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.properties.IProperty;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.NonNullList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Right-click a placed storage (barrel, chest, shulker box) of the "from" tier to turn it into the
 * "to" tier of the same kind in place, keeping its contents, upgrades, settings, wood, colours and
 * name. As in the original, it doesn't work while someone has the storage open.
 */
public class StorageTierUpgradeItem extends Item {
    public enum TierUpgrade {
        /** Only turns vanilla chests and shulker boxes into the basic sophisticated ones. */
        BASIC("basic", null, StorageTier.WOOD),
        BASIC_TO_COPPER("basic_to_copper", StorageTier.WOOD, StorageTier.COPPER),
        BASIC_TO_IRON("basic_to_iron", StorageTier.WOOD, StorageTier.IRON),
        BASIC_TO_GOLD("basic_to_gold", StorageTier.WOOD, StorageTier.GOLD),
        BASIC_TO_DIAMOND("basic_to_diamond", StorageTier.WOOD, StorageTier.DIAMOND),
        BASIC_TO_NETHERITE("basic_to_netherite", StorageTier.WOOD, StorageTier.NETHERITE),
        BASIC_TO_NETHER_STAR("basic_to_nether_star", StorageTier.WOOD, StorageTier.NETHER_STAR),
        COPPER_TO_IRON("copper_to_iron", StorageTier.COPPER, StorageTier.IRON),
        COPPER_TO_GOLD("copper_to_gold", StorageTier.COPPER, StorageTier.GOLD),
        COPPER_TO_DIAMOND("copper_to_diamond", StorageTier.COPPER, StorageTier.DIAMOND),
        COPPER_TO_NETHERITE("copper_to_netherite", StorageTier.COPPER, StorageTier.NETHERITE),
        COPPER_TO_NETHER_STAR("copper_to_nether_star", StorageTier.COPPER, StorageTier.NETHER_STAR),
        IRON_TO_GOLD("iron_to_gold", StorageTier.IRON, StorageTier.GOLD),
        IRON_TO_DIAMOND("iron_to_diamond", StorageTier.IRON, StorageTier.DIAMOND),
        IRON_TO_NETHERITE("iron_to_netherite", StorageTier.IRON, StorageTier.NETHERITE),
        IRON_TO_NETHER_STAR("iron_to_nether_star", StorageTier.IRON, StorageTier.NETHER_STAR),
        GOLD_TO_DIAMOND("gold_to_diamond", StorageTier.GOLD, StorageTier.DIAMOND),
        GOLD_TO_NETHERITE("gold_to_netherite", StorageTier.GOLD, StorageTier.NETHERITE),
        GOLD_TO_NETHER_STAR("gold_to_nether_star", StorageTier.GOLD, StorageTier.NETHER_STAR),
        DIAMOND_TO_NETHERITE("diamond_to_netherite", StorageTier.DIAMOND, StorageTier.NETHERITE),
        DIAMOND_TO_NETHER_STAR("diamond_to_nether_star", StorageTier.DIAMOND, StorageTier.NETHER_STAR);

        private final String name;
        @Nullable
        private final StorageTier from;
        private final StorageTier to;

        TierUpgrade(String name, @Nullable StorageTier from, StorageTier to) {
            this.name = name;
            this.from = from;
            this.to = to;
        }

        public String getRegistryName() {
            return name + "_tier_upgrade";
        }

        /** Listed only when both tiers are (copper and netherite need a mod adding them). */
        public boolean isAvailable() {
            return (from == null || from.isAvailable()) && to.isAvailable();
        }

        /** The basic upgrade and every basic-to-X one also turn vanilla storages into ours. */
        boolean upgradesVanilla() {
            return from == null || from == StorageTier.WOOD;
        }
    }

    private final TierUpgrade tierUpgrade;

    public StorageTierUpgradeItem(TierUpgrade tierUpgrade, CreativeTabs tab) {
        this.tierUpgrade = tierUpgrade;
        setRegistryName(SophisticatedStorageLegacy.MODID, tierUpgrade.getRegistryName());
        setTranslationKey(SophisticatedStorageLegacy.MODID + "." + tierUpgrade.getRegistryName());
        setCreativeTab(tab);
    }

    @Override
    public void getSubItems(@javax.annotation.Nonnull CreativeTabs tab, @javax.annotation.Nonnull net.minecraft.util.NonNullList<ItemStack> items) {
        if (tierUpgrade.isAvailable()) {
            super.getSubItems(tab, items);
        }
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        IBlockState state = world.getBlockState(pos);
        if (tierUpgrade.upgradesVanilla() && (state.getBlock() == Blocks.CHEST || state.getBlock() instanceof BlockShulkerBox)) {
            return upgradeVanilla(player, world, pos, state, hand);
        }
        TileEntity clicked = world.getTileEntity(pos);
        if (clicked instanceof ControllerTileEntity && tierUpgrade.from != null) {
            // On a controller: every connected storage of the right tier, one item each, as in the original.
            if (!world.isRemote) {
                ItemStack held = player.getHeldItem(hand);
                for (BlockPos storagePos : new java.util.ArrayList<>(((ControllerTileEntity) clicked).getStoragePositions())) {
                    if (held.isEmpty()) {
                        break;
                    }
                    if (canUpgrade(world, storagePos, world.getBlockState(storagePos))) {
                        upgradeStorage(player, world, storagePos, world.getBlockState(storagePos), held);
                    }
                }
            }
            return EnumActionResult.SUCCESS;
        }
        if (!canUpgrade(world, pos, state)) {
            return EnumActionResult.PASS;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        upgradeStorage(player, world, pos, state, player.getHeldItem(hand));
        return EnumActionResult.SUCCESS;
    }

    public boolean canUpgrade(World world, BlockPos pos, IBlockState state) {
        if (tierUpgrade.from == null || !(state.getBlock() instanceof StorageBlockBase)) {
            return false;
        }
        StorageBlockBase block = (StorageBlockBase) state.getBlock();
        TileEntity te = world.getTileEntity(pos);
        if (block.getTier() != tierUpgrade.from || ModContent.getStorage(block.getStorageName(), tierUpgrade.to) == null
                || !(te instanceof StorageTileEntity) || ((StorageTileEntity) te).getMainTile().isOpen()) {
            return false;
        }
        if (isDoubleChest(state)) {
            BlockPos otherPos = pos.offset(ChestBlock.getConnectedDirection(state));
            return world.getBlockState(otherPos).getBlock() == block && world.getTileEntity(otherPos) instanceof ChestTileEntity;
        }
        return true;
    }

    private static boolean isDoubleChest(IBlockState state) {
        return state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestBlock.Type.SINGLE;
    }

    /** Upgrade items it takes: two for a double chest, which upgrades both halves (as in the original). */
    private static int getCountRequired(IBlockState state) {
        return isDoubleChest(state) ? 2 : 1;
    }

    /**
     * Replaces the storage with the next tier's, keeping everything; uses one upgrade (two for a
     * double chest) outside creative. False when the player holds too few of them.
     */
    public boolean upgradeStorage(EntityPlayer player, World world, BlockPos pos, IBlockState state, ItemStack held) {
        int countRequired = getCountRequired(state);
        if (!player.capabilities.isCreativeMode && held.getCount() < countRequired) {
            return false;
        }
        StorageBlockBase oldBlock = (StorageBlockBase) state.getBlock();
        StorageBlockBase target = ModContent.getStorage(oldBlock.getStorageName(), tierUpgrade.to);
        int addedInventorySlots = target.getDefaultInventorySlots() - oldBlock.getDefaultInventorySlots();
        int addedUpgradeSlots = target.getDefaultUpgradeSlots() - oldBlock.getDefaultUpgradeSlots();
        if (isDoubleChest(state)) {
            // Both halves, with the main growing by both halves' new slots.
            BlockPos otherPos = pos.offset(ChestBlock.getConnectedDirection(state));
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(pos);
            ChestTileEntity other = (ChestTileEntity) world.getTileEntity(otherPos);
            chest.setUpgrading(true);
            other.setUpgrading(true);
            replaceStorage(world, pos, state, target, chest.isMainChest() ? 2 * addedInventorySlots : addedInventorySlots, addedUpgradeSlots);
            replaceStorage(world, otherPos, world.getBlockState(otherPos), target, other.isMainChest() ? 2 * addedInventorySlots : addedInventorySlots, addedUpgradeSlots);
        } else {
            replaceStorage(world, pos, state, target, addedInventorySlots, addedUpgradeSlots);
        }
        if (!player.capabilities.isCreativeMode) {
            held.shrink(countRequired);
        }
        return true;
    }

    private static void replaceStorage(World world, BlockPos pos, IBlockState state, StorageBlockBase target, int addedInventorySlots, int addedUpgradeSlots) {
        StorageTileEntity storage = (StorageTileEntity) world.getTileEntity(pos);
        NBTTagCompound saved = storage.writeToNBT(new NBTTagCompound());
        // Replacing the block runs its breakBlock, which would otherwise spill the contents.
        storage.setUpgrading(true);
        IBlockState newState = copyProperties(state, target.getDefaultState());
        world.setBlockState(pos, newState, 3);
        TileEntity newTe = world.getTileEntity(pos);
        if (newTe instanceof StorageTileEntity) {
            newTe.readFromNBT(saved);
            ((StorageTileEntity) newTe).getStorageWrapper().increaseSize(addedInventorySlots, addedUpgradeSlots);
            newTe.markDirty();
            world.notifyBlockUpdate(pos, newState, newState, 3);
        }
    }

    /**
     * A vanilla chest becomes an oak chest and a vanilla shulker box a shulker box in its colour
     * (purple, the 1.12 default, stays undyed), keeping the items and name, as in the original.
     * A vanilla double chest becomes a double chest (see upgradeVanillaDoubleChest).
     */
    private EnumActionResult upgradeVanilla(EntityPlayer player, World world, BlockPos pos, IBlockState state, EnumHand hand) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityLockableLoot)) {
            return EnumActionResult.PASS;
        }
        boolean isChest = state.getBlock() == Blocks.CHEST;
        StorageBlockBase target = ModContent.getStorage(isChest ? "chest" : "shulker_box", tierUpgrade.to);
        if (target == null) {
            return EnumActionResult.PASS;
        }
        if (isChest) {
            BlockPos partnerPos = null;
            for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
                if (world.getBlockState(pos.offset(side)).getBlock() == Blocks.CHEST) {
                    if (partnerPos != null) {
                        return EnumActionResult.PASS;
                    }
                    partnerPos = pos.offset(side);
                }
            }
            if (partnerPos != null) {
                return upgradeVanillaDoubleChest(player, world, pos, partnerPos, state, hand, target);
            }
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        TileEntityLockableLoot vanilla = (TileEntityLockableLoot) te;
        // Everything is worked out before the vanilla block is touched, so nothing can be lost.
        IBlockState newState;
        ItemStack look = new ItemStack(target);
        if (isChest) {
            newState = target.getDefaultState().withProperty(ChestBlock.FACING, state.getValue(BlockChest.FACING));
            StorageItemData.setWoodType(look, WoodType.OAK);
        } else {
            newState = target.getDefaultState().withProperty(ShulkerBoxBlock.FACING, state.getValue(BlockShulkerBox.FACING));
            EnumDyeColor color = getShulkerColor(state.getBlock());
            if (color != EnumDyeColor.PURPLE) {
                int rgb = StorageBlockItem.colorOf(color);
                StorageItemData.setAccentColor(StorageItemData.setMainColor(look, rgb), rgb);
            }
        }
        if (vanilla.hasCustomName()) {
            look.setStackDisplayName(vanilla.getName());
        }
        replaceVanilla(world, pos, vanilla, newState, look);
        if (!player.capabilities.isCreativeMode) {
            player.getHeldItem(hand).shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    /**
     * A vanilla double chest becomes a double chest of ours, for two upgrades (as in the original):
     * both halves are converted, and the left one's items join the right one's.
     */
    private EnumActionResult upgradeVanillaDoubleChest(EntityPlayer player, World world, BlockPos pos, BlockPos partnerPos, IBlockState state,
                                                       EnumHand hand, StorageBlockBase target) {
        EnumFacing facing = state.getValue(BlockChest.FACING);
        EnumFacing toPartner = EnumFacing.getFacingFromVector(partnerPos.getX() - pos.getX(), 0, partnerPos.getZ() - pos.getZ());
        ChestBlock.Type type;
        if (toPartner == facing.rotateY()) {
            type = ChestBlock.Type.LEFT;
        } else if (toPartner == facing.rotateYCCW()) {
            type = ChestBlock.Type.RIGHT;
        } else {
            return EnumActionResult.PASS;
        }
        TileEntity clickedTe = world.getTileEntity(pos);
        TileEntity partnerTe = world.getTileEntity(partnerPos);
        if (!(clickedTe instanceof TileEntityLockableLoot) || !(partnerTe instanceof TileEntityLockableLoot)
                || (!player.capabilities.isCreativeMode && player.getHeldItem(hand).getCount() < 2)) {
            return EnumActionResult.PASS;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        IBlockState newState = target.getDefaultState().withProperty(ChestBlock.FACING, facing);
        TileEntityLockableLoot clickedChest = (TileEntityLockableLoot) clickedTe;
        TileEntityLockableLoot partnerChest = (TileEntityLockableLoot) partnerTe;
        StorageTileEntity clicked = replaceVanilla(world, pos, clickedChest, newState.withProperty(ChestBlock.TYPE, type), vanillaChestLook(target, clickedChest));
        StorageTileEntity partner = replaceVanilla(world, partnerPos, partnerChest, newState.withProperty(ChestBlock.TYPE, type.getOpposite()),
                vanillaChestLook(target, partnerChest));
        if (clicked instanceof ChestTileEntity && partner instanceof ChestTileEntity) {
            if (type == ChestBlock.Type.LEFT) {
                ((ChestTileEntity) clicked).joinWith((ChestTileEntity) partner);
            } else {
                ((ChestTileEntity) partner).joinWith((ChestTileEntity) clicked);
            }
        }
        if (!player.capabilities.isCreativeMode) {
            player.getHeldItem(hand).shrink(2);
        }
        return EnumActionResult.SUCCESS;
    }

    private static ItemStack vanillaChestLook(StorageBlockBase target, TileEntityLockableLoot vanilla) {
        ItemStack look = StorageItemData.setWoodType(new ItemStack(target), WoodType.OAK);
        if (vanilla.hasCustomName()) {
            look.setStackDisplayName(vanilla.getName());
        }
        return look;
    }

    /** Takes the vanilla storage's items, puts ours in its place with that look, and the items in it. */
    @Nullable
    private static StorageTileEntity replaceVanilla(World world, BlockPos pos, TileEntityLockableLoot vanilla, IBlockState newState, ItemStack look) {
        NonNullList<ItemStack> items = NonNullList.withSize(vanilla.getSizeInventory(), ItemStack.EMPTY);
        for (int slot = 0; slot < items.size(); slot++) {
            // removeStackFromSlot fills an unopened loot chest first, like opening it would.
            items.set(slot, vanilla.removeStackFromSlot(slot));
        }
        // The vanilla block is emptied above, so replacing it drops nothing.
        world.removeTileEntity(pos);
        world.setBlockState(pos, newState, 3);
        TileEntity newTe = world.getTileEntity(pos);
        if (!(newTe instanceof StorageTileEntity)) {
            return null;
        }
        StorageTileEntity storage = (StorageTileEntity) newTe;
        storage.readFromStack(look);
        InventoryHandler inventory = storage.getStorageWrapper().getInventoryHandler();
        for (int slot = 0; slot < items.size() && slot < inventory.getSlots(); slot++) {
            inventory.setStackInSlot(slot, items.get(slot));
        }
        storage.markDirty();
        world.notifyBlockUpdate(pos, newState, newState, 3);
        return storage;
    }

    /**
     * A vanilla shulker box's colour. Every colour getter on BlockShulkerBox is client-only in
     * 1.12 (calling one on a dedicated server is a NoSuchMethodError); getBlockByColor isn't.
     */
    private static EnumDyeColor getShulkerColor(net.minecraft.block.Block block) {
        for (EnumDyeColor color : EnumDyeColor.values()) {
            if (BlockShulkerBox.getBlockByColor(color) == block) {
                return color;
            }
        }
        return EnumDyeColor.PURPLE;
    }

    /** Every tier of a kind has the same properties (facing...), so they carry over one to one. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IBlockState copyProperties(IBlockState from, IBlockState to) {
        for (IProperty property : from.getPropertyKeys()) {
            if (to.getPropertyKeys().contains(property)) {
                to = to.withProperty(property, from.getValue(property));
            }
        }
        return to;
    }
}
