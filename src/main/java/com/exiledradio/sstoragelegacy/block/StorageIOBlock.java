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
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Storage I/O, Input and Output (the original 1.20's StorageIOBlock); see {@link StorageIOTileEntity}. */
public class StorageIOBlock extends Block {
    public enum Mode {
        IO("storage_io"),
        INPUT("storage_input"),
        OUTPUT("storage_output");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Mode mode;

    public StorageIOBlock(Mode mode) {
        super(Material.ROCK);
        this.mode = mode;
        setHardness(3.0F);
        setResistance(6.0F);
        setSoundType(SoundType.STONE);
        setHarvestLevel("pickaxe", 0);
        setTranslationKey(SophisticatedStorageLegacy.MODID + "." + mode.getName());
        setRegistryName(SophisticatedStorageLegacy.MODID, mode.getName());
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new StorageIOTileEntity();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (!world.isRemote && te instanceof StorageIOTileEntity) {
            ((StorageIOTileEntity) te).tryToAddToController();
        }
    }

    @Override
    public void breakBlock(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (!world.isRemote && te instanceof StorageIOTileEntity) {
            StorageIOTileEntity io = (StorageIOTileEntity) te;
            if (io.isLinked()) {
                io.unlinkFromController();
            } else {
                io.removeFromController();
            }
        }
        super.breakBlock(world, pos, state);
    }
}
