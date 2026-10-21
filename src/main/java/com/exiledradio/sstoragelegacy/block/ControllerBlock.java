/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The storage controller (the original's ControllerBlock): placed next to storages it takes in
 * the whole connected group; right-click deposits, and pipes see every storage through it.
 */
public class ControllerBlock extends Block {
    public ControllerBlock() {
        super(Material.ROCK);
        setRegistryName(SophisticatedStorageLegacy.MODID, "controller");
        setTranslationKey(SophisticatedStorageLegacy.MODID + ".controller");
        setHardness(3.0F);
        setResistance(6.0F);
        setHarvestLevel("pickaxe", 0);
        setSoundType(SoundType.STONE);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new ControllerTileEntity();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (!world.isRemote && te instanceof ControllerTileEntity) {
            ((ControllerTileEntity) te).searchAndAddStorages();
        }
    }

    @Override
    public void breakBlock(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (!world.isRemote && te instanceof ControllerTileEntity) {
            ((ControllerTileEntity) te).detachFromStoragesAndUnlinkBlocks();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof ControllerTileEntity && hand == EnumHand.MAIN_HAND) {
            boolean appliedUpgrade = false;
            for (StorageTileEntity storage : ((ControllerTileEntity) te).getConnectedStorageTiles()) {
                if (StorageBlockBase.tryAddSingleUpgrade(player, hand, storage)) {
                    appliedUpgrade = true;
                }
            }
            if (!appliedUpgrade) {
                ((ControllerTileEntity) te).depositPlayerItems(player, hand);
            }
        }
        return true;
    }

    /** Sneak + right-click with upgrades: each connected storage takes as many as fit (from the right-click event). */
    public static boolean tryFillUpgrades(World world, BlockPos pos, EntityPlayer player, EnumHand hand) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof ControllerTileEntity)) {
            return false;
        }
        boolean added = false;
        for (StorageTileEntity storage : ((ControllerTileEntity) te).getConnectedStorageTiles()) {
            if (StorageBlockBase.tryFillUpgrades(player, hand, storage)) {
                added = true;
            }
        }
        return added;
    }
}
