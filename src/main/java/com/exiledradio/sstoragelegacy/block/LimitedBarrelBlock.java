/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.settings.itemdisplay.ItemDisplaySettingsCategory;
import com.exiledradio.scorelegacy.settings.nosort.NoSortSettingsCategory;
import com.exiledradio.sstoragelegacy.item.PackingTapeItem;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

/**
 * A barrel with 1 to 4 big slots on a divided front (the original's LimitedBarrelBlock). Right-
 * clicking the front puts the held item in the slot clicked (twice quickly: all matching items
 * from the inventory), left-clicking takes one out (sneaking: a stack). The GUI opens from the
 * sides or when sneaking. FACING (horizontal) and VERTICAL_FACING are stored in metadata; a
 * barrel facing up or down keeps a horizontal facing so the front reads upright.
 */
public class LimitedBarrelBlock extends StorageBlockBase {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyEnum<VerticalFacing> VERTICAL_FACING = PropertyEnum.create("vertical_facing", VerticalFacing.class);
    private final int slotCount;

    public LimitedBarrelBlock(StorageTier tier, int slotCount) {
        super(Material.WOOD, tier, "limited_barrel_" + slotCount, "limited_" + tier.getModelPrefix() + "barrel_" + slotCount);
        this.slotCount = slotCount;
        setHardness(2.5F);
        setSoundType(SoundType.WOOD);
        setDefaultState(blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(VERTICAL_FACING, VerticalFacing.NO)
                .withProperty(BarrelBlock.FLAT_TOP, false)
                .withProperty(BarrelBlock.WOOD, WoodType.DEFAULT)
                .withProperty(BarrelBlock.TINT, TintMode.NONE)
                .withProperty(BarrelBlock.PACKED, false)
                .withProperty(BarrelBlock.LOCKED, false)
                .withProperty(BarrelBlock.SHOW_TIER, true));
    }

    public int getSlotCount() {
        return slotCount;
    }

    /** Normal stacks per slot (config, read live like the original's). */
    public int getBaseStackSizeMultiplier() {
        return com.exiledradio.sstoragelegacy.ModConfig.storages.getLimited(getTier(), slotCount).baseSlotLimitMultiplier;
    }

    @Override
    public int getDefaultInventorySlots() {
        return slotCount;
    }

    @Override
    public int getDefaultUpgradeSlots() {
        return com.exiledradio.sstoragelegacy.ModConfig.storages.getLimited(getTier(), slotCount).upgradeSlotCount;
    }

    // ---- State ---------------------------------------------------------------------------

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new net.minecraftforge.common.property.ExtendedBlockState(this, new net.minecraft.block.properties.IProperty[] {FACING, VERTICAL_FACING,
                BarrelBlock.FLAT_TOP, BarrelBlock.WOOD, BarrelBlock.TINT, BarrelBlock.PACKED, BarrelBlock.LOCKED, BarrelBlock.SHOW_TIER},
                new net.minecraftforge.common.property.IUnlistedProperty[] {BarrelBlock.MATERIALS});
    }

    @Nonnull
    @Override
    public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
        return BarrelBlock.withMaterials(state, world, pos);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        int vertical = (meta >> 2) & 3;
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(VERTICAL_FACING, VerticalFacing.values()[vertical < VerticalFacing.values().length ? vertical : 0]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(VERTICAL_FACING).ordinal() << 2);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof LimitedBarrelTileEntity) {
            LimitedBarrelTileEntity barrel = (LimitedBarrelTileEntity) te;
            return state.withProperty(BarrelBlock.FLAT_TOP, barrel.isFlatTop())
                    .withProperty(BarrelBlock.WOOD, barrel.getRenderWoodType())
                    .withProperty(BarrelBlock.TINT, barrel.getTintMode())
                    .withProperty(BarrelBlock.PACKED, barrel.isPacked())
                    .withProperty(BarrelBlock.LOCKED, barrel.isLocked() && barrel.shouldShowLock())
                    .withProperty(BarrelBlock.SHOW_TIER, barrel.shouldShowTier());
        }
        return state;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        // Front towards the player; facing up or down, the top of the front is away from them.
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite())
                .withProperty(VERTICAL_FACING, VerticalFacing.fromDirection(EnumFacing.getDirectionFromEntityLiving(pos, placer)));
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

    @Override
    public VerticalFacing getVerticalFacing(IBlockState state) {
        return state.getValue(VERTICAL_FACING);
    }

    @Override
    public EnumFacing getHorizontalDirection(IBlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public EnumFacing getFacing(IBlockState state) {
        VerticalFacing vertical = state.getValue(VERTICAL_FACING);
        return vertical == VerticalFacing.NO ? state.getValue(FACING) : vertical.getDirection();
    }

    // ---- Rendering and shape -------------------------------------------------------------

    @Nonnull
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
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
    protected StorageTileEntity createStorageTile() {
        return new LimitedBarrelTileEntity();
    }

    // ---- Interaction ---------------------------------------------------------------------

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        ItemStack held = player.getHeldItem(hand);
        if (!(te instanceof LimitedBarrelTileEntity) || ((LimitedBarrelTileEntity) te).isPacked() || side != getFacing(state)
                || player.isSneaking() || held.getItem() instanceof PackingTapeItem) {
            return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
        }
        // The front: deposit or dye, never the GUI.
        if (world.isRemote || hand == EnumHand.OFF_HAND || player.isSpectator()) {
            return true;
        }
        LimitedBarrelTileEntity barrel = (LimitedBarrelTileEntity) te;
        // An upgrade that fits goes into the upgrade slots; otherwise it is deposited like any item.
        if (tryAddSingleUpgrade(player, hand, barrel)) {
            return true;
        }
        int slot = getInteractionSlot(state, hitX, hitY, hitZ);
        if (!barrel.depositItem(player, hand, held, slot) && held.getItem() == Items.DYE) {
            barrel.applyDye(slot, held, EnumDyeColor.byDyeDamage(held.getMetadata()), false);
        }
        return true;
    }

    /** Sneak + dye on the front dyes every slot's count (called from the right-click event: 1.12 skips the block when sneaking with an item). */
    public boolean tryToDyeAll(World world, BlockPos pos, IBlockState state, EnumFacing side, ItemStack held) {
        if (side != getFacing(state) || held.getItem() != Items.DYE) {
            return false;
        }
        TileEntity te = world.getTileEntity(pos);
        return te instanceof LimitedBarrelTileEntity && ((LimitedBarrelTileEntity) te).applyDye(0, held, EnumDyeColor.byDyeDamage(held.getMetadata()), true);
    }

    /** Survival left-click (server side): take from the slot looked at. Creative is handled in the left-click event. */
    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        if (!world.isRemote) {
            tryToTakeItem(world, pos, player);
        }
    }

    public boolean tryToTakeItem(World world, BlockPos pos, EntityPlayer player) {
        IBlockState state = world.getBlockState(pos);
        TileEntity te = world.getTileEntity(pos);
        RayTraceResult hit = getHitResult(player);
        if (state.getBlock() != this || !(te instanceof LimitedBarrelTileEntity) || hit == null || !pos.equals(hit.getBlockPos())
                || hit.sideHit != getFacing(state)) {
            return false;
        }
        LimitedBarrelTileEntity barrel = (LimitedBarrelTileEntity) te;
        Vec3d relative = hit.hitVec.subtract(pos.getX(), pos.getY(), pos.getZ());
        return !barrel.isPacked() && barrel.tryToTakeItem(player, getInteractionSlot(state, (float) relative.x, (float) relative.y, (float) relative.z));
    }

    public boolean isLookingAtFront(EntityPlayer player, BlockPos pos, IBlockState state) {
        RayTraceResult hit = getHitResult(player);
        return hit != null && pos.equals(hit.getBlockPos()) && hit.sideHit == getFacing(state);
    }

    /** The block the player looks at. Entity.rayTrace is client-only in 1.12, so this is the same trace by hand. */
    @Nullable
    private static RayTraceResult getHitResult(EntityPlayer player) {
        double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        Vec3d eyes = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d look = player.getLookVec();
        RayTraceResult hit = player.world.rayTraceBlocks(eyes, eyes.add(look.x * reach, look.y * reach, look.z * reach), false, false, true);
        return hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK ? hit : null;
    }

    /**
     * Which slot a point on the front is (hit coordinates within the block): the original's
     * rotations, turning the front to face north, where up is the top row and +x the right.
     */
    int getInteractionSlot(IBlockState state, float hitX, float hitY, float hitZ) {
        if (slotCount == 1) {
            return 0;
        }
        Vector3f coords = new Vector3f(hitX - 0.5F, hitY - 0.5F, hitZ - 0.5F);
        VerticalFacing vertical = state.getValue(VERTICAL_FACING);
        if (vertical != VerticalFacing.NO) {
            getNorthBasedRotation(state.getValue(FACING)).transform(coords);
            getNorthBasedRotation(vertical.getDirection().getOpposite()).transform(coords);
        } else {
            getNorthBasedRotation(state.getValue(FACING).getOpposite()).transform(coords);
        }
        boolean top = coords.y > 0;
        boolean right = coords.x > 0;
        if (slotCount == 2) {
            return top ? 0 : 1;
        }
        if (slotCount == 3) {
            return top ? 0 : right ? 2 : 1;
        }
        return top ? (right ? 1 : 0) : (right ? 3 : 2);
    }

    private static Matrix3f getNorthBasedRotation(EnumFacing direction) {
        Matrix3f rotation = new Matrix3f();
        Matrix3f flip = new Matrix3f();
        flip.rotY((float) Math.PI);
        switch (direction) {
            case DOWN:
                rotation.rotX((float) Math.toRadians(90));
                rotation.mul(flip);
                break;
            case UP:
                rotation.rotX((float) Math.toRadians(-90));
                rotation.mul(flip);
                break;
            case SOUTH:
                rotation.rotY((float) Math.PI);
                break;
            case WEST:
                rotation.rotY((float) Math.toRadians(-90));
                break;
            case EAST:
                rotation.rotY((float) Math.toRadians(90));
                break;
            default:
                rotation.setIdentity();
        }
        return rotation;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntity te = world.getTileEntity(pos);
        if (!world.isRemote && te instanceof LimitedBarrelTileEntity) {
            // Every slot shown on the front and kept in place by sorting, as in the original.
            StorageBlockWrapper wrapper = ((LimitedBarrelTileEntity) te).getStorageWrapper();
            ItemDisplaySettingsCategory display = wrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
            NoSortSettingsCategory noSort = wrapper.getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class);
            int slots = wrapper.getInventoryHandler().getSlots();
            for (int slot = 0; slot < slots; slot++) {
                if (display != null) {
                    display.selectSlot(slot);
                }
            }
            if (noSort != null) {
                noSort.selectSlots(0, slots);
            }
        }
    }
}
