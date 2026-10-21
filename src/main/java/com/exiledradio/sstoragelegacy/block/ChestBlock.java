/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A chest of one tier, in any wood, dyeable. Drawn by a TESR (the original's chest model and
 * textures), like vanilla chests.
 *
 * <p>Like vanilla chests, two of them side by side become a double chest (the original 1.20.x's,
 * GPL source a41c8065): same tier, wood, colours and facing, and at most one with upgrades. A
 * chest placed next to one joins it; sneaking joins the chest whose side was clicked, or none.
 * TYPE is which half a chest is, seen from its front's point of view, as vanilla names them.
 */
public class ChestBlock extends StorageBlockBase {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyEnum<Type> TYPE = PropertyEnum.create("type", Type.class);
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 14 / 16.0, 15 / 16.0);
    private static final AxisAlignedBB NORTH_SHAPE = new AxisAlignedBB(1 / 16.0, 0, 0, 15 / 16.0, 14 / 16.0, 15 / 16.0);
    private static final AxisAlignedBB SOUTH_SHAPE = new AxisAlignedBB(1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 14 / 16.0, 1);
    private static final AxisAlignedBB WEST_SHAPE = new AxisAlignedBB(0, 0, 1 / 16.0, 15 / 16.0, 14 / 16.0, 15 / 16.0);
    private static final AxisAlignedBB EAST_SHAPE = new AxisAlignedBB(1 / 16.0, 0, 1 / 16.0, 1, 14 / 16.0, 15 / 16.0);

    /** Vanilla 1.14+'s ChestType. */
    public enum Type implements IStringSerializable {
        SINGLE("single"),
        LEFT("left"),
        RIGHT("right");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public Type getOpposite() {
            return this == SINGLE ? SINGLE : this == LEFT ? RIGHT : LEFT;
        }

        @Nonnull
        @Override
        public String getName() {
            return name;
        }
    }

    public ChestBlock(StorageTier tier) {
        super(Material.WOOD, tier, "chest");
        setHardness(2.5F);
        setSoundType(SoundType.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(TYPE, Type.SINGLE));
    }

    /** Which way the other half is (vanilla's getConnectedDirection; rotateY is clockwise). */
    public static EnumFacing getConnectedDirection(IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        return state.getValue(TYPE) == Type.LEFT ? facing.rotateY() : facing.rotateYCCW();
    }

    @Override
    public EnumFacing getFacing(IBlockState state) {
        return state.getValue(FACING);
    }

    @Override
    protected StorageTileEntity createStorageTile() {
        return new ChestTileEntity();
    }

    // ---- State ---------------------------------------------------------------------------

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, TYPE);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        Type type = Type.values()[Math.min(Type.values().length - 1, (meta >> 2) & 3)];
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(TYPE, type);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(TYPE).ordinal() << 2;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(@Nonnull IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(@Nonnull IBlockState state, Mirror mirror) {
        // Mirroring swaps which side the other half is on.
        IBlockState rotated = state.withRotation(mirror.toRotation(state.getValue(FACING)));
        return mirror == Mirror.NONE ? rotated : rotated.withProperty(TYPE, state.getValue(TYPE).getOpposite());
    }

    // ---- Placing and joining -------------------------------------------------------------

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing clickedFace, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        // Front towards the player, like vanilla chests.
        EnumFacing direction = placer.getHorizontalFacing().getOpposite();
        ItemStack stack = placer.getHeldItem(hand);
        if (StorageItemData.isDoubleChest(stack)) {
            // The other half goes on its left (see onBlockPlacedBy); this one keeps the contents.
            return getDefaultState().withProperty(FACING, direction).withProperty(TYPE, Type.RIGHT);
        }
        Type type = Type.SINGLE;
        boolean sneaking = placer.isSneaking();
        if (clickedFace.getAxis().isHorizontal() && sneaking) {
            EnumFacing partnerFacing = getPartnerFacing(world, pos, clickedFace.getOpposite(), stack);
            if (partnerFacing != null && partnerFacing.getAxis() != clickedFace.getAxis()) {
                direction = partnerFacing;
                type = partnerFacing.rotateYCCW() == clickedFace.getOpposite() ? Type.RIGHT : Type.LEFT;
            }
        }
        if (type == Type.SINGLE && !sneaking) {
            if (direction == getPartnerFacing(world, pos, direction.rotateY(), stack)) {
                type = Type.LEFT;
            } else if (direction == getPartnerFacing(world, pos, direction.rotateYCCW(), stack)) {
                type = Type.RIGHT;
            }
        }
        return getDefaultState().withProperty(FACING, direction).withProperty(TYPE, type);
    }

    /**
     * The facing of the chest next to pos on the given side if a chest placed from this stack could
     * join it: a single chest of this block, unpacked, in the same wood and colours, and not both
     * with upgrades. Null otherwise.
     */
    @Nullable
    private EnumFacing getPartnerFacing(World world, BlockPos pos, EnumFacing side, ItemStack stack) {
        BlockPos neighbourPos = pos.offset(side);
        IBlockState neighbour = world.getBlockState(neighbourPos);
        if (neighbour.getBlock() != this || neighbour.getValue(TYPE) != Type.SINGLE) {
            return null;
        }
        TileEntity te = world.getTileEntity(neighbourPos);
        if (!(te instanceof ChestTileEntity)) {
            return null;
        }
        ChestTileEntity chest = (ChestTileEntity) te;
        WoodType wood = StorageItemData.getWoodType(stack);
        boolean matches = !chest.isPacked()
                && StorageItemData.getMainColor(stack) == chest.getMainColor() && StorageItemData.getAccentColor(stack) == chest.getAccentColor()
                && (wood != null ? wood : WoodType.DEFAULT) == chest.getRenderWoodType()
                && (!StorageItemData.hasUpgrades(stack) || chest.getStorageWrapper().hasNoUpgrades());
        return matches ? neighbour.getValue(FACING) : null;
    }

    /** Whether a packed double chest can go here: the spot for its other half must be free too. */
    public static boolean canPlaceDoubleChest(World world, BlockPos pos, EntityPlayer player) {
        BlockPos otherPos = pos.offset(player.getHorizontalFacing().rotateY());
        return world.getBlockState(otherPos).getBlock().isReplaceable(world, otherPos) && world.getWorldBorder().contains(otherPos)
                && player.canPlayerEdit(otherPos, EnumFacing.UP, ItemStack.EMPTY);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (world.isRemote) {
            return;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof ChestTileEntity)) {
            return;
        }
        ChestTileEntity current = (ChestTileEntity) te;
        if (StorageItemData.isDoubleChest(stack)) {
            // The other half: the same look, no contents, pointing at this one.
            BlockPos otherPos = pos.offset(state.getValue(FACING).rotateYCCW());
            world.setBlockState(otherPos, state.withProperty(TYPE, Type.LEFT), 3);
            TileEntity otherTe = world.getTileEntity(otherPos);
            if (otherTe instanceof ChestTileEntity) {
                ItemStack look = stack.copy();
                look.setCount(1);
                look.getTagCompound().removeTag(StorageItemData.CONTENTS_TAG);
                ((ChestTileEntity) otherTe).readFromStack(look);
                ((ChestTileEntity) otherTe).setMainPos(pos);
                ((ChestTileEntity) otherTe).tryToAddToController();
                ((ChestTileEntity) otherTe).syncTogglesFrom(current);
            }
            return;
        }
        Type type = state.getValue(TYPE);
        if (type == Type.SINGLE) {
            return;
        }
        BlockPos otherPos = pos.offset(getConnectedDirection(state));
        IBlockState otherState = world.getBlockState(otherPos);
        TileEntity otherTe = world.getTileEntity(otherPos);
        if (otherState.getBlock() != this || !(otherTe instanceof ChestTileEntity)) {
            world.setBlockState(pos, state.withProperty(TYPE, Type.SINGLE), 3);
            return;
        }
        world.setBlockState(otherPos, otherState.withProperty(TYPE, type.getOpposite()), 3);
        ChestTileEntity other = (ChestTileEntity) otherTe;
        // The half with upgrades keeps the contents; otherwise the right half does.
        if (current.getStorageWrapper().hasNoUpgrades() && (type == Type.LEFT || !other.getStorageWrapper().hasNoUpgrades())) {
            current.joinWith(other);
            current.syncTogglesFrom(other);
        } else {
            other.joinWith(current);
            other.syncTogglesFrom(current);
        }
    }

    /**
     * Catches a double chest whose other half went without breaking (replaced by a command, or
     * the pair set apart in some other way): this half becomes a single chest.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block neighbourBlock, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, neighbourBlock, fromPos);
        if (world.isRemote || state.getValue(TYPE) == Type.SINGLE || !fromPos.equals(pos.offset(getConnectedDirection(state)))) {
            return;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof ChestTileEntity) || ((ChestTileEntity) te).isUpgrading()) {
            return;
        }
        IBlockState other = world.getBlockState(fromPos);
        if (other.getBlock() == this && other.getValue(FACING) == state.getValue(FACING) && other.getValue(TYPE) == state.getValue(TYPE).getOpposite()) {
            return;
        }
        ((ChestTileEntity) te).becomeSingle();
        world.setBlockState(pos, state.withProperty(TYPE, Type.SINGLE), 3);
    }

    // ---- Opening, packing, breaking ------------------------------------------------------

    @Override
    protected void pack(World world, BlockPos pos, IBlockState state, StorageTileEntity tile) {
        super.pack(world, pos, state, tile);
        ChestTileEntity other = tile instanceof ChestTileEntity ? ((ChestTileEntity) tile).getOtherPart() : null;
        if (other != null) {
            super.pack(world, other.getPos(), world.getBlockState(other.getPos()), other);
        }
    }

    @Override
    public void breakBlock(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (!world.isRemote && te instanceof ChestTileEntity && state.getValue(TYPE) != Type.SINGLE && !((ChestTileEntity) te).isUpgrading()) {
            ChestTileEntity chest = (ChestTileEntity) te;
            BlockPos otherPos = pos.offset(getConnectedDirection(state));
            IBlockState otherState = world.getBlockState(otherPos);
            boolean otherIsPartner = otherState.getBlock() == this && otherState.getValue(TYPE) == state.getValue(TYPE).getOpposite();
            if (chest.isPacked()) {
                // A packed double chest dropped as one item (see createStack); the other half goes with it.
                if (otherIsPartner) {
                    world.setBlockToAir(otherPos);
                }
            } else if (otherIsPartner) {
                TileEntity otherTe = world.getTileEntity(otherPos);
                if (chest.isMainChest() && otherTe instanceof ChestTileEntity) {
                    chest.handOverTo((ChestTileEntity) otherTe);
                } else if (!chest.isMainChest()) {
                    chest.takeShareFromMain();
                }
                world.setBlockState(otherPos, otherState.withProperty(TYPE, Type.SINGLE), 3);
            }
        }
        super.breakBlock(world, pos, state);
    }

    /** A packed double chest's item carries both halves: the main's contents, flagged as a double chest. */
    @Override
    protected ItemStack createStack(IBlockAccess world, BlockPos pos, boolean withContents) {
        TileEntity te = world.getTileEntity(pos);
        IBlockState state = world.getBlockState(pos);
        if (withContents && te instanceof ChestTileEntity && ((ChestTileEntity) te).isPacked() && state.getBlock() == this
                && state.getValue(TYPE) != Type.SINGLE) {
            ItemStack stack = new ItemStack(this);
            ((ChestTileEntity) te).getMainTile().writeToStack(stack, true);
            return StorageItemData.setDoubleChest(stack, true);
        }
        return super.createStack(world, pos, withContents);
    }

    /** A hopper upgrade on a double chest reaches along both halves (the original's getNeighborPos). */
    @Override
    public List<BlockPos> getNeighborPositions(IBlockState state, BlockPos origin, EnumFacing side) {
        if (state.getValue(TYPE) == Type.SINGLE) {
            return Collections.singletonList(origin.offset(side));
        }
        EnumFacing connected = getConnectedDirection(state);
        if (connected == side) {
            return Collections.singletonList(origin.offset(side, 2));
        } else if (connected.getOpposite() == side) {
            return Collections.singletonList(origin.offset(side));
        }
        return Arrays.asList(origin.offset(side), origin.offset(connected).offset(side));
    }

    // ---- Rendering and shape -------------------------------------------------------------

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        if (state.getValue(TYPE) == Type.SINGLE) {
            return SHAPE;
        }
        switch (getConnectedDirection(state)) {
            case SOUTH:
                return SOUTH_SHAPE;
            case WEST:
                return WEST_SHAPE;
            case EAST:
                return EAST_SHAPE;
            default:
                return NORTH_SHAPE;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        // Lid open/close goes to the tile on clients (see ChestTileEntity).
        TileEntity te = world.getTileEntity(pos);
        return te != null && te.receiveClientEvent(id, param);
    }
}
