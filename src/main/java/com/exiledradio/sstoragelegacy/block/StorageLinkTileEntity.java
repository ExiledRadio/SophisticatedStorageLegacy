/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.controller.ILinkable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/** A storage link (the original's StorageLinkBlockEntity): linked to a controller, it brings in the storages around the block it sits on. */
public class StorageLinkTileEntity extends TileEntity implements ILinkable {
    @Nullable
    private BlockPos controllerPos = null;

    @Override
    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        markDirty();
    }

    @Nullable
    @Override
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    @Override
    public void removeControllerPos() {
        controllerPos = null;
        markDirty();
    }

    @Override
    public BlockPos getStorageBlockPos() {
        return pos;
    }

    @Override
    public World getStorageBlockWorld() {
        return world;
    }

    @Override
    public boolean canConnectStorages() {
        return true;
    }

    /** The block this link is attached to. */
    @Override
    public Set<BlockPos> getConnectablePositions() {
        IBlockState state = world.getBlockState(pos);
        EnumFacing facing = state.getBlock() instanceof StorageLinkBlock ? state.getValue(StorageLinkBlock.FACING) : EnumFacing.UP;
        return Collections.singleton(pos.offset(facing.getOpposite()));
    }

    @Override
    public boolean connectLinkedSelf() {
        return false;
    }

    @Override
    public boolean isLinked() {
        return controllerPos != null;
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        saveControllerPos(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        controllerPos = null;
        loadControllerPos(compound);
    }
}
