/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A storage link (the original's StorageLinkBlock): a thin plate on the side of a block. Linked
 * to a controller with the storage tool, it adds the storages around that block to the network.
 */
public class StorageLinkBlock extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    private static final AxisAlignedBB[] BOXES = {
            new AxisAlignedBB(1 / 16D, 14 / 16D, 1 / 16D, 15 / 16D, 1, 15 / 16D), // down
            new AxisAlignedBB(1 / 16D, 0, 1 / 16D, 15 / 16D, 2 / 16D, 15 / 16D), // up
            new AxisAlignedBB(1 / 16D, 1 / 16D, 14 / 16D, 15 / 16D, 15 / 16D, 1), // north
            new AxisAlignedBB(1 / 16D, 1 / 16D, 0, 15 / 16D, 15 / 16D, 2 / 16D), // south
            new AxisAlignedBB(14 / 16D, 1 / 16D, 1 / 16D, 1, 15 / 16D, 15 / 16D), // west
            new AxisAlignedBB(0, 1 / 16D, 1 / 16D, 2 / 16D, 15 / 16D, 15 / 16D) // east
    };

    public StorageLinkBlock() {
        super(Material.ROCK);
        setRegistryName(SophisticatedStorageLegacy.MODID, "storage_link");
        setTranslationKey(SophisticatedStorageLegacy.MODID + ".storage_link");
        setHardness(2.5F);
        setResistance(5.0F);
        setHarvestLevel("pickaxe", 0);
        setSoundType(SoundType.STONE);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

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

    /** Faces away from the clicked block; on another link facing this way it turns back, as in the original. */
    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        IBlockState against = world.getBlockState(pos.offset(facing.getOpposite()));
        if (against.getBlock() == this && against.getValue(FACING) == facing) {
            return getDefaultState().withProperty(FACING, facing.getOpposite());
        }
        return getDefaultState().withProperty(FACING, facing);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOXES[state.getValue(FACING).getIndex()];
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
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new StorageLinkTileEntity();
    }

    @Override
    public void breakBlock(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (!world.isRemote && te instanceof StorageLinkTileEntity) {
            ((StorageLinkTileEntity) te).unlinkFromController();
        }
        super.breakBlock(world, pos, state);
    }
}
