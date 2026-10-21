/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * A shulker box of one tier, dyeable. Breaking it always keeps the contents in the item; it needs
 * room in front of its lid to open, and pistons break it, like vanilla shulker boxes. Drawn by a TESR.
 */
public class ShulkerBoxBlock extends StorageBlockBase {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public ShulkerBoxBlock(StorageTier tier) {
        super(Material.ROCK, tier, "shulker_box");
        setHardness(2.0F);
        setSoundType(SoundType.STONE);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    @Override
    public EnumFacing getFacing(IBlockState state) {
        return state.getValue(FACING);
    }

    @Override
    protected StorageTileEntity createStorageTile() {
        return new ShulkerBoxTileEntity();
    }

    @Override
    public boolean keepsContentsWhenBroken() {
        return true;
    }

    /** A closed box opens only if its lid has room to rise (vanilla's check). */
    @Override
    protected boolean canOpen(World world, BlockPos pos, IBlockState state, StorageTileEntity tile) {
        if (!(tile instanceof ShulkerBoxTileEntity) || !((ShulkerBoxTileEntity) tile).isClosed()) {
            return true;
        }
        EnumFacing facing = state.getValue(FACING);
        AxisAlignedBB lidSpace = FULL_BLOCK_AABB.expand(0.5F * facing.getXOffset(), 0.5F * facing.getYOffset(), 0.5F * facing.getZOffset())
                .contract(facing.getXOffset(), facing.getYOffset(), facing.getZOffset());
        return !world.collidesWithAnyBlock(lidSpace.offset(pos.offset(facing)));
    }

    // ---- State ---------------------------------------------------------------------------

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        // Lid away from the face it was placed against, like vanilla shulker boxes.
        return getDefaultState().withProperty(FACING, facing);
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
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
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
        TileEntity te = source.getTileEntity(pos);
        if (te instanceof ShulkerBoxTileEntity) {
            // The lifted shell makes the box taller in its facing direction, as in vanilla.
            float lift = 0.5F * ((ShulkerBoxTileEntity) te).getProgress(1.0F);
            EnumFacing facing = state.getValue(FACING);
            return FULL_BLOCK_AABB.expand(lift * facing.getXOffset(), lift * facing.getYOffset(), lift * facing.getZOffset());
        }
        return FULL_BLOCK_AABB;
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
        return face == state.getValue(FACING).getOpposite() ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        TileEntity te = world.getTileEntity(pos);
        return te != null && te.receiveClientEvent(id, param);
    }
}
