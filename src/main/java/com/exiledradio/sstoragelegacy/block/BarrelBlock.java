/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.common.gui.GuiHandler;
import com.exiledradio.sstoragelegacy.item.PackingTapeItem;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/**
 * A barrel of one tier (wood, iron, gold, diamond, nether star). FACING and OPEN are stored in metadata; FLAT_TOP, WOOD and TINT come
 * from the tile entity as actual-state properties, which the multipart blockstate uses to
 * stack the original's model parts.
 */
public class BarrelBlock extends StorageBlockBase {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyBool OPEN = PropertyBool.create("open");
    public static final PropertyBool FLAT_TOP = PropertyBool.create("flat_top");
    public static final PropertyEnum<WoodType> WOOD = PropertyEnum.create("wood", WoodType.class);
    public static final PropertyEnum<TintMode> TINT = PropertyEnum.create("tint", TintMode.class);
    public static final PropertyBool PACKED = PropertyBool.create("packed");
    /** Locked and showing its lock (the storage tool can hide it). */
    public static final PropertyBool LOCKED = PropertyBool.create("locked");
    /** The tier trim is shown (the storage tool can hide it). */
    public static final PropertyBool SHOW_TIER = PropertyBool.create("show_tier");

    public BarrelBlock(StorageTier tier) {
        super(Material.WOOD, tier, "barrel");
        setHardness(2.5F);
        setSoundType(SoundType.WOOD);
        setDefaultState(blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(OPEN, false)
                .withProperty(FLAT_TOP, false)
                .withProperty(WOOD, WoodType.DEFAULT)
                .withProperty(TINT, TintMode.NONE)
                .withProperty(PACKED, false)
                .withProperty(LOCKED, false)
                .withProperty(SHOW_TIER, true));
    }

    // ---- State ---------------------------------------------------------------------------

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new net.minecraftforge.common.property.ExtendedBlockState(this, new net.minecraft.block.properties.IProperty[] {FACING, OPEN, FLAT_TOP, WOOD, TINT, PACKED, LOCKED, SHOW_TIER},
                new net.minecraftforge.common.property.IUnlistedProperty[] {MATERIALS});
    }

    /** The barrel's materials for its model (see BarrelMaterialModel); empty for the wood look. */
    public static final net.minecraftforge.common.property.IUnlistedProperty<java.util.Map<BarrelMaterial, String>> MATERIALS =
            new net.minecraftforge.common.property.IUnlistedProperty<java.util.Map<BarrelMaterial, String>>() {
                @Override
                public String getName() {
                    return "materials";
                }

                @Override
                public boolean isValid(java.util.Map<BarrelMaterial, String> value) {
                    return true;
                }

                @Override
                @SuppressWarnings({"unchecked", "rawtypes"})
                public Class<java.util.Map<BarrelMaterial, String>> getType() {
                    return (Class) java.util.Map.class;
                }

                @Override
                public String valueToString(java.util.Map<BarrelMaterial, String> value) {
                    return String.valueOf(value);
                }
            };

    /** Barrels and limited barrels: the tile's materials for the model. */
    static IBlockState withMaterials(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (state instanceof net.minecraftforge.common.property.IExtendedBlockState && te instanceof BarrelTileEntity) {
            return ((net.minecraftforge.common.property.IExtendedBlockState) state).withProperty(MATERIALS, ((BarrelTileEntity) te).getMaterials());
        }
        return state;
    }

    @Nonnull
    @Override
    public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
        return withMaterials(state, world, pos);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7)).withProperty(OPEN, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(OPEN) ? 8 : 0);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof BarrelTileEntity) {
            BarrelTileEntity barrel = (BarrelTileEntity) te;
            return state.withProperty(FLAT_TOP, barrel.isFlatTop())
                    .withProperty(WOOD, barrel.getRenderWoodType())
                    .withProperty(TINT, barrel.getTintMode())
                    .withProperty(PACKED, barrel.isPacked())
                    .withProperty(LOCKED, barrel.isLocked() && barrel.shouldShowLock())
                    .withProperty(SHOW_TIER, barrel.shouldShowTier());
        }
        return state;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        // Lid towards the player, like the original (nearest looking direction, opposite).
        return getDefaultState().withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer));
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
    public BlockRenderLayer getRenderLayer() {
        // The tint overlays are drawn over the wood with transparent pixels, like grass.
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        // The lid is recessed a pixel, so neighbours must not cull the faces touching it.
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return true;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.SOLID;
    }

    @Override
    public boolean isSideSolid(@Nonnull IBlockState base, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        return true;
    }

    @Override
    public EnumFacing getFacing(IBlockState state) {
        return state.getValue(FACING);
    }

    @Override
    protected StorageTileEntity createStorageTile() {
        return new BarrelTileEntity();
    }
}
