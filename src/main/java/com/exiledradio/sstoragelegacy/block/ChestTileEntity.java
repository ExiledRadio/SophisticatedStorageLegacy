/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.inventory.InventoryHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A chest: its lid swings open like a vanilla chest's. Whether it is open goes to clients as a
 * block event (as vanilla chests do); both sides then animate the lid.
 *
 * <p>Two chests side by side form a double chest (the original 1.20.x's, GPL source a41c8065).
 * Only one half, the main, keeps contents and upgrades: its inventory holds both halves' slots,
 * the other half's first. The other half remembers where the main is and passes everything to it
 * (opening, pipes, the controller); its own storage stays empty until the pair splits.
 */
public class ChestTileEntity extends StorageTileEntity {
    private static final int OPEN_EVENT = 1;
    private static final String DOUBLE_MAIN_POS_TAG = "doubleMainPos";

    private boolean lidOpen = false;
    private float lidAngle = 0.0F;
    private float prevLidAngle = 0.0F;
    /** Set on the half that keeps nothing: where the main half is. */
    @Nullable
    private BlockPos doubleMainPos = null;
    /** Server: check once, on the first tick after loading, that a single chest isn't still set up as a half. */
    private boolean checkSingleOnTick = true;

    // ---- Double chest --------------------------------------------------------------------

    public boolean isMainChest() {
        return doubleMainPos == null;
    }

    @Nullable
    public BlockPos getDoubleMainPos() {
        return doubleMainPos;
    }

    void setMainPos(@Nullable BlockPos mainPos) {
        doubleMainPos = mainPos;
        markDirty();
        notifyClients();
    }

    @Override
    public StorageTileEntity getMainTile() {
        if (doubleMainPos != null && world != null && world.isBlockLoaded(doubleMainPos)) {
            TileEntity te = world.getTileEntity(doubleMainPos);
            if (te instanceof ChestTileEntity) {
                return (ChestTileEntity) te;
            }
        }
        return this;
    }

    private ChestBlock.Type getType() {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof ChestBlock ? state.getValue(ChestBlock.TYPE) : ChestBlock.Type.SINGLE;
    }

    /** The other half of a double chest, or null. */
    @Nullable
    ChestTileEntity getOtherPart() {
        if (world == null) {
            return null;
        }
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestBlock.Type.SINGLE) {
            return null;
        }
        BlockPos otherPos = pos.offset(ChestBlock.getConnectedDirection(state));
        TileEntity te = world.isBlockLoaded(otherPos) ? world.getTileEntity(otherPos) : null;
        return te instanceof ChestTileEntity ? (ChestTileEntity) te : null;
    }

    /**
     * This half stops keeping anything: its items and per-slot settings go into the main's slots
     * after the main's own, and it joins the controller as a part without contents.
     */
    public void joinWith(ChestTileEntity main) {
        StorageBlockWrapper mainWrapper = main.getStorageWrapper();
        StorageBlockWrapper own = getStorageWrapper();
        main.closeOpenContainers();
        closeOpenContainers();
        int offset = mainWrapper.getNumberOfInventorySlots();
        InventoryHandler ownInventory = own.getInventoryHandler();
        int ownSlots = ownInventory.getSlots();
        mainWrapper.setNumberOfInventorySlots(offset + ownSlots);
        InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
        for (int slot = 0; slot < ownSlots; slot++) {
            ItemStack stack = ownInventory.getRawStack(slot);
            if (!stack.isEmpty()) {
                mainInventory.setRawStack(slot + offset, stack.copy());
                ownInventory.setRawStack(slot, ItemStack.EMPTY);
            }
        }
        own.getSettingsHandler().copySlotSettingsTo(mainWrapper.getSettingsHandler(), 0, offset);
        own.getSettingsHandler().deleteSlotSettingsFrom(0);
        setMainPos(main.getPos());
        removeFromController();
        setNotLinked();
        tryToAddToController();
        main.markDirty();
        main.notifyClients();
    }

    /** The lock and what the storage tool shows, the same on both halves. */
    void syncTogglesFrom(ChestTileEntity other) {
        if (other.isLocked() != isLocked()) {
            super.toggleLock();
        }
        if (other.shouldShowLock() != shouldShowLock()) {
            super.toggleLockVisibility();
        }
        if (other.shouldShowTier() != shouldShowTier()) {
            super.toggleTierVisibility();
        }
        if (other.shouldShowUpgrades() != shouldShowUpgrades()) {
            super.toggleUpgradesVisibility();
        }
    }

    /**
     * The half without contents is being broken: it takes back the main's last slots (its own
     * share, which then spills from it) and the main is a single chest again.
     */
    void takeShareFromMain() {
        StorageTileEntity mainTile = getMainTile();
        if (mainTile == this) {
            doubleMainPos = null;
            return;
        }
        ChestTileEntity main = (ChestTileEntity) mainTile;
        main.closeOpenContainers();
        StorageBlockWrapper mainWrapper = main.getStorageWrapper();
        InventoryHandler mainInventory = mainWrapper.getInventoryHandler();
        InventoryHandler ownInventory = getStorageWrapper().getInventoryHandler();
        int mainSlots = mainInventory.getSlots();
        int first = Math.max(0, mainSlots - ownInventory.getSlots());
        for (int slot = first; slot < mainSlots; slot++) {
            ItemStack stack = mainInventory.getRawStack(slot);
            if (!stack.isEmpty()) {
                ownInventory.setRawStack(slot - first, stack.copy());
                mainInventory.setRawStack(slot, ItemStack.EMPTY);
            }
        }
        mainWrapper.getSettingsHandler().deleteSlotSettingsFrom(first);
        mainWrapper.resetDisplaySide();
        mainWrapper.setNumberOfInventorySlots(first);
        doubleMainPos = null;
        main.markDirty();
        main.notifyClients();
    }

    /**
     * The main half is being broken: the other half becomes a chest of its own with the main's last
     * slots, each cut to a normal stack (the rest spills with the main, as in the original).
     */
    void handOverTo(ChestTileEntity other) {
        if (!pos.equals(other.doubleMainPos)) {
            return;
        }
        closeOpenContainers();
        InventoryHandler inventory = getStorageWrapper().getInventoryHandler();
        InventoryHandler otherInventory = other.getStorageWrapper().getInventoryHandler();
        int slots = inventory.getSlots();
        int first = Math.max(0, slots - otherInventory.getSlots());
        for (int slot = first; slot < slots; slot++) {
            ItemStack remaining = inventory.getRawStack(slot).copy();
            if (!remaining.isEmpty()) {
                otherInventory.setRawStack(slot - first, remaining.splitStack(remaining.getMaxStackSize()));
                inventory.setRawStack(slot, remaining);
            }
        }
        getStorageWrapper().getSettingsHandler().copySlotSettingsTo(other.getStorageWrapper().getSettingsHandler(), first, -first);
        other.getStorageWrapper().resetDisplaySide();
        other.doubleMainPos = null;
        other.removeFromController();
        other.tryToAddToController();
        other.markDirty();
        other.notifyClients();
    }

    /**
     * The other half is gone without this one hearing of it (replaced by a command): a main spills
     * the slots past a single chest's, the other half just forgets its main.
     */
    void becomeSingle() {
        if (!isMainChest()) {
            setMainPos(null);
            return;
        }
        Object block = getBlockType();
        int single = block instanceof StorageBlockBase ? ((StorageBlockBase) block).getDefaultInventorySlots() : getInventorySlotCount();
        StorageBlockWrapper wrapper = getStorageWrapper();
        wrapper.resetDisplaySide();
        InventoryHandler inventory = wrapper.getInventoryHandler();
        if (inventory.getSlots() <= single) {
            return;
        }
        closeOpenContainers();
        for (int slot = single; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getRawStack(slot);
            if (!stack.isEmpty()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                inventory.setRawStack(slot, ItemStack.EMPTY);
            }
        }
        wrapper.getSettingsHandler().deleteSlotSettingsFrom(single);
        wrapper.setNumberOfInventorySlots(single);
        markDirty();
        notifyClients();
    }

    @Override
    public boolean hasStorageData() {
        return isMainChest();
    }

    @Override
    public boolean canBeLinked() {
        return isMainChest();
    }

    // The lock and the tool's display toggles apply to both halves, as in the original.

    @Override
    public void toggleLock() {
        super.toggleLock();
        ChestTileEntity other = getOtherPart();
        if (other != null) {
            other.toggleJustMyLock();
        }
    }

    private void toggleJustMyLock() {
        super.toggleLock();
    }

    @Override
    public void toggleLockVisibility() {
        super.toggleLockVisibility();
        ChestTileEntity other = getOtherPart();
        if (other != null) {
            other.toggleJustMyLockVisibility();
        }
    }

    private void toggleJustMyLockVisibility() {
        super.toggleLockVisibility();
    }

    @Override
    public void toggleTierVisibility() {
        super.toggleTierVisibility();
        ChestTileEntity other = getOtherPart();
        if (other != null) {
            other.toggleJustMyTierVisibility();
        }
    }

    private void toggleJustMyTierVisibility() {
        super.toggleTierVisibility();
    }

    @Override
    public void toggleUpgradesVisibility() {
        super.toggleUpgradesVisibility();
        ChestTileEntity other = getOtherPart();
        if (other != null) {
            other.toggleJustMyUpgradesVisibility();
        }
    }

    private void toggleJustMyUpgradesVisibility() {
        super.toggleUpgradesVisibility();
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        StorageTileEntity main = getMainTile();
        return main != this ? main.hasCapability(capability, facing) : super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        StorageTileEntity main = getMainTile();
        return main != this ? main.getCapability(capability, facing) : super.getCapability(capability, facing);
    }

    @Override
    protected void writeExtraSyncData(NBTTagCompound tag) {
        super.writeExtraSyncData(tag);
        if (doubleMainPos != null) {
            tag.setLong(DOUBLE_MAIN_POS_TAG, doubleMainPos.toLong());
        }
    }

    @Override
    protected void readExtraSyncData(NBTTagCompound tag) {
        super.readExtraSyncData(tag);
        doubleMainPos = tag.hasKey(DOUBLE_MAIN_POS_TAG) ? BlockPos.fromLong(tag.getLong(DOUBLE_MAIN_POS_TAG)) : null;
    }

    // ---- Lid -----------------------------------------------------------------------------

    @Override
    protected void onOpenStateChanged(boolean open) {
        world.addBlockEvent(pos, getBlockType(), OPEN_EVENT, open ? 1 : 0);
        ChestTileEntity other = getOtherPart();
        ChestTileEntity soundSource = this;
        if (other != null) {
            // Both lids swing; the sound comes once, from the right half, as in the original.
            world.addBlockEvent(other.getPos(), other.getBlockType(), OPEN_EVENT, open ? 1 : 0);
            if (getType() == ChestBlock.Type.LEFT) {
                soundSource = other;
            }
        }
        soundSource.playSound(open ? SoundEvents.BLOCK_CHEST_OPEN : SoundEvents.BLOCK_CHEST_CLOSE, EnumFacing.UP, 0.5F);
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        if (id == OPEN_EVENT) {
            lidOpen = type > 0;
            return true;
        }
        return super.receiveClientEvent(id, type);
    }

    @Override
    public void update() {
        super.update();
        if (checkSingleOnTick && world != null && !world.isRemote) {
            // As the original's load(): a single chest with a main, or with a double's slots, is put right.
            // Only twice a single's slots counts as a double's: a lowered slot config must not spill chests.
            checkSingleOnTick = false;
            if (getType() == ChestBlock.Type.SINGLE && !isUpgrading()) {
                Object block = getBlockType();
                int single = block instanceof StorageBlockBase ? ((StorageBlockBase) block).getDefaultInventorySlots() : getInventorySlotCount();
                if (!isMainChest() || getStorageWrapper().getNumberOfInventorySlots() >= 2 * single) {
                    becomeSingle();
                }
            }
        }
        prevLidAngle = lidAngle;
        if (lidOpen && lidAngle < 1.0F) {
            lidAngle = Math.min(1.0F, lidAngle + 0.1F);
        } else if (!lidOpen && lidAngle > 0.0F) {
            lidAngle = Math.max(0.0F, lidAngle - 0.1F);
        }
    }

    /** How far the lid is open, 0 to 1, between ticks. */
    public float getLidAngle(float partialTicks) {
        return prevLidAngle + (lidAngle - prevLidAngle) * partialTicks;
    }
}
