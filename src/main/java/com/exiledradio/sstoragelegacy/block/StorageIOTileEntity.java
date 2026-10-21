/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.api.IStorageWrapper;
import com.exiledradio.scorelegacy.controller.ControllerTileEntityBase;
import com.exiledradio.scorelegacy.controller.IControllableStorage;
import com.exiledradio.scorelegacy.controller.IControllerBoundable;
import com.exiledradio.scorelegacy.controller.ILinkable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * Storage I/O, Input and Output (the original 1.20's StorageIOBlockEntity and its two
 * subclasses): placed against a controller's network, or linked to it with the storage tool, it
 * gives pipes and hoppers the whole network's inventory: both ways, insert only (one slot that
 * routes everything, the cheap one), or extract only. It joins the network like a storage that
 * holds nothing, and storages beyond it don't connect through it.
 */
public class StorageIOTileEntity extends TileEntity implements IControllableStorage, ILinkable {
    private static final String LINKED_TO_CONTROLLER_TAG = "isLinkedToController";

    @Nullable
    private BlockPos controllerPos = null;
    private boolean isLinkedToController = false;

    private StorageIOBlock.Mode getMode() {
        return getBlockType() instanceof StorageIOBlock ? ((StorageIOBlock) getBlockType()).getMode() : StorageIOBlock.Mode.IO;
    }

    @Nullable
    private ControllerTileEntityBase getController() {
        return world == null ? null : IControllerBoundable.getLoadedController(world, controllerPos);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return getController() != null;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            ControllerTileEntityBase controller = getController();
            if (controller == null) {
                return null;
            }
            switch (getMode()) {
                case INPUT:
                    return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new InputOnlyHandler(controller));
                case OUTPUT:
                    return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new OutputOnlyHandler(controller));
                default:
                    return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(controller);
            }
        }
        return super.getCapability(capability, facing);
    }

    // ---- In the network ------------------------------------------------------------------

    /** Never asked: hasStorageData is false, so the controller doesn't use it as a storage. */
    @Nullable
    @Override
    public IStorageWrapper getStorageWrapper() {
        return null;
    }

    @Override
    public boolean hasStorageData() {
        return false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            registerWithControllerOnLoad();
        }
    }

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

    /** Storages beyond it don't join the network through it, as in the original. */
    @Override
    public boolean canConnectStorages() {
        return false;
    }

    @Override
    public boolean canBeConnected() {
        return isLinked() || controllerPos == null;
    }

    @Override
    public void linkToController(BlockPos controllerPos) {
        if (this.controllerPos != null) {
            return;
        }
        isLinkedToController = true;
        ILinkable.super.linkToController(controllerPos);
        markDirty();
    }

    @Override
    public boolean isLinked() {
        return isLinkedToController && controllerPos != null;
    }

    @Override
    public void setNotLinked() {
        isLinkedToController = false;
        markDirty();
    }

    @Override
    public Set<BlockPos> getConnectablePositions() {
        return Collections.emptySet();
    }

    @Override
    public boolean connectLinkedSelf() {
        return true;
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        saveControllerPos(compound);
        if (isLinkedToController) {
            compound.setBoolean(LINKED_TO_CONTROLLER_TAG, true);
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        controllerPos = null;
        loadControllerPos(compound);
        isLinkedToController = compound.getBoolean(LINKED_TO_CONTROLLER_TAG);
    }

    /** Storage Input: one slot that takes anything and routes it into the network, never giving anything out. */
    private static final class InputOnlyHandler implements IItemHandler {
        private final ControllerTileEntityBase controller;

        InputOnlyHandler(ControllerTileEntityBase controller) {
            this.controller = controller;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return controller.insertItem(stack, simulate, true);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    }

    /** Storage Output: the network's slots to take from, taking nothing in. */
    private static final class OutputOnlyHandler implements IItemHandler {
        private final ControllerTileEntityBase controller;

        OutputOnlyHandler(ControllerTileEntityBase controller) {
            this.controller = controller;
        }

        @Override
        public int getSlots() {
            return controller.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return controller.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return controller.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return controller.getSlotLimit(slot);
        }
    }
}
