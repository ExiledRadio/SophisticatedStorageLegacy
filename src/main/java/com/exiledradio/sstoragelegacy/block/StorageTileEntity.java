/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.api.ITickableUpgrade;
import com.exiledradio.scorelegacy.controller.IControllableStorage;
import com.exiledradio.scorelegacy.controller.ILinkable;
import com.exiledradio.scorelegacy.settings.memory.MemorySettingsCategory;
import com.exiledradio.sstoragelegacy.common.gui.StorageBlockContainer;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Any placed storage (barrel, chest, shulker box): its storage (inventory, upgrades, settings;
 * see {@link StorageBlockWrapper}), wood type, tint colours, custom name, packing and the items
 * shown on its front. Ticks its upgrades on the server. Subclasses add how they open and close.
 */
public abstract class StorageTileEntity extends TileEntity implements ITickable, IControllableStorage, ILinkable {
    /** The most display items any storage shows (barrels: 4; chests and shulker boxes: 1). */
    public static final int MAX_DISPLAY_ITEMS = 4;

    /** Saved storage: inventory, upgrades, settings (barrels before S3 only "inventory", at the top level). */
    private static final String CONTENTS_TAG = "contents";
    private static final String LEGACY_INVENTORY_TAG = "inventory";
    private static final String DISPLAY_ITEMS_TAG = "displayItems";
    private static final String ITEM_TAG = "item";
    private static final String ROTATION_TAG = "rotation";
    private static final String SLOT_TAG = "slot";
    private static final String DISPLAY_SIDE_TAG = "displaySide";
    private static final String CUSTOM_NAME_TAG = "customName";
    private static final String LOCKED_TAG = "locked";
    private static final String SHOW_LOCK_TAG = "showLock";
    private static final String SHOW_TIER_TAG = "showTier";
    private static final String SHOW_UPGRADES_TAG = "showUpgrades";
    private static final String LINKED_TO_CONTROLLER_TAG = "isLinkedToController";
    private static final String RENDER_UPGRADES_TAG = "renderUpgrades";
    private static final String CLIENT_UPGRADE_INVENTORY_TAG = "upgradeInventory";
    /** Ticks between checks whether the upgrades drawn on the front changed. */
    private static final int UPGRADE_RENDER_CHECK_TICKS = 10;
    /** Ticks between checks that whoever opened the storage still has it open. */
    private static final int RECHECK_OPEN_TICKS = 5;

    private final StorageBlockWrapper storageWrapper = new StorageBlockWrapper(this, new NBTTagCompound());
    private List<DisplayItem> displayItems = Collections.emptyList();
    /** One stack per upgrade slot (empty for an empty slot). Client: as last synced; server: as last sent. */
    private List<ItemStack> renderUpgradeItems = Collections.emptyList();

    @Nullable
    private WoodType woodType = null;
    private int mainColor = StorageItemData.NO_COLOR;
    private int accentColor = StorageItemData.NO_COLOR;
    @Nullable
    private String customName = null;

    /** Server only: players with this storage's GUI open. Not saved. */
    private int openCount = 0;
    /** Set while a tier upgrade item replaces the block, so breaking the old one spills nothing. */
    private boolean upgrading = false;
    /** Packed with packing tape: can't be opened, and breaks into an item that keeps the contents. */
    private boolean packed = false;
    /** Locked with the storage tool: only takes items it already has (see the wrapper's policy). */
    private boolean locked = false;
    private boolean showLock = true;
    private boolean showTier = true;
    /** The controller whose network this storage is in, if any (saved, not synced). */
    @Nullable
    private BlockPos controllerPos = null;
    /** Joined by the storage tool's link rather than by touching the network. */
    private boolean isLinkedToController = false;
    /** Upgrades drawn small along the bottom of the front (the storage tool turns it on). */
    private boolean showUpgrades = false;

    // ---- What differs per storage --------------------------------------------------------

    /** How many items the item display setting can show (the original: barrels 4, others 1). */
    public int getDisplayItemLimit() {
        return 1;
    }

    /** Whether a stack may go in at all (shulker boxes refuse shulker boxes). */
    public boolean isAllowedInStorage(ItemStack stack) {
        return true;
    }

    /** How many inventory slots (limited barrels: 1 to 4; everything else: the tier's). */
    /** Inventory slots a new storage here gets; the wrapper keeps the count it was first used with. */
    public int getInventorySlotCount() {
        Block block = getBlockType();
        return block instanceof StorageBlockBase ? ((StorageBlockBase) block).getDefaultInventorySlots() : getTier().getInventorySlots();
    }

    /** Upgrade slots a new storage here gets; the wrapper keeps the count it was first used with. */
    public int getUpgradeSlotCount() {
        Block block = getBlockType();
        return block instanceof StorageBlockBase ? ((StorageBlockBase) block).getDefaultUpgradeSlots() : getTier().getUpgradeSlots();
    }

    /** Normal stacks per slot before stack upgrades (limited barrels: 8 to 160). */
    public int getBaseStackSizeMultiplier() {
        return 1;
    }

    /** Whether locking also memorizes every slot's item (limited barrels). */
    public boolean memorizesItemsWhenLocked() {
        return false;
    }

    /**
     * Whether a locked storage still fills empty slots with items it already holds elsewhere. True
     * for everything but limited barrels, whose locked empty slots stay empty unless memorized.
     */
    public boolean allowsEmptySlotsMatchingItemInsertsWhenLocked() {
        return true;
    }

    /** Server side, when the first player opens it or the last one closes it. */
    protected abstract void onOpenStateChanged(boolean open);

    /** Extra look saved, synced and carried by the item (barrel: flat top). */
    protected void writeExtraSyncData(NBTTagCompound tag) {
    }

    protected void readExtraSyncData(NBTTagCompound tag) {
    }

    /** Sent to clients but not saved (upgrades on the front; limited barrels: slot counts). Subclasses read theirs back in readExtraSyncData. */
    protected void writeClientSyncData(NBTTagCompound tag) {
        if (world != null && !world.isRemote) {
            renderUpgradeItems = collectRenderUpgradeItems();
        }
        NBTTagList upgrades = new NBTTagList();
        for (ItemStack stack : renderUpgradeItems) {
            upgrades.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        tag.setTag(RENDER_UPGRADES_TAG, upgrades);
        // The client builds its storage container from this tile, and upgrade tabs add slots: without
        // the server's upgrades its container would come out shorter than the server's.
        tag.setTag(CLIENT_UPGRADE_INVENTORY_TAG, storageWrapper.getUpgradeHandler().serializeNBT());
        tag.setInteger(StorageBlockWrapper.INVENTORY_SLOTS_TAG, storageWrapper.getNumberOfInventorySlots());
        tag.setInteger(StorageBlockWrapper.UPGRADE_SLOTS_TAG, storageWrapper.getNumberOfUpgradeSlots());
    }

    protected void readExtraFromStack(ItemStack stack) {
    }

    protected void writeExtraToStack(ItemStack stack) {
    }

    // ---- State ---------------------------------------------------------------------------

    public boolean isPacked() {
        return packed;
    }

    public void setPacked(boolean packed) {
        this.packed = packed;
        markDirty();
        notifyClients();
    }

    public boolean isLocked() {
        return locked;
    }

    /** The storage tool's lock mode, as in the original. */
    public void toggleLock() {
        locked = !locked;
        if (memorizesItemsWhenLocked()) {
            MemorySettingsCategory memory = storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
            if (memory != null) {
                if (locked) {
                    memory.selectSlots(0, storageWrapper.getInventoryHandler().getSlots());
                } else {
                    memory.unselectAllSlots();
                }
            }
        }
        storageWrapper.refreshInventoryForInputOutput();
        markDirty();
        notifyClients();
    }

    public boolean shouldShowLock() {
        return showLock;
    }

    public void toggleLockVisibility() {
        showLock = !showLock;
        markDirty();
        notifyClients();
    }

    public boolean shouldShowTier() {
        return showTier;
    }

    public void toggleTierVisibility() {
        showTier = !showTier;
        markDirty();
        notifyClients();
    }

    public boolean shouldShowUpgrades() {
        return showUpgrades;
    }

    public void toggleUpgradesVisibility() {
        showUpgrades = !showUpgrades;
        markDirty();
        notifyClients();
    }

    public boolean isOpen() {
        return openCount > 0;
    }

    public boolean isUpgrading() {
        return upgrading;
    }

    public void setUpgrading(boolean upgrading) {
        this.upgrading = upgrading;
    }

    public StorageBlockWrapper getStorageWrapper() {
        return storageWrapper;
    }

    /**
     * The tile whose storage this one stands for: itself, except for the half of a double chest
     * that keeps nothing (see {@link ChestTileEntity}). Opening, comparators and pipes go there.
     */
    public StorageTileEntity getMainTile() {
        return this;
    }

    /**
     * Makes this a tile outside any world for a storage item's contents (see StorageItemContents):
     * the block then gives the tier and default slot counts that the world would otherwise give.
     */
    void setDetachedBlock(Block block) {
        blockType = block;
    }

    /** Server: closes this storage's screen for everyone who has it open (its slots are about to change). */
    public void closeOpenContainers() {
        if (world == null || world.isRemote) {
            return;
        }
        for (EntityPlayer player : new ArrayList<>(world.playerEntities)) {
            if ((player.openContainer instanceof StorageBlockContainer && ((StorageBlockContainer) player.openContainer).getTile() == this)
                    || (player.openContainer instanceof com.exiledradio.sstoragelegacy.common.gui.StorageBlockSettingsContainer
                    && ((com.exiledradio.sstoragelegacy.common.gui.StorageBlockSettingsContainer) player.openContainer).getTile() == this)) {
                player.closeScreen();
            }
        }
    }

    /** The tier of the block this tile belongs to (wood until the tile knows its block). */
    public StorageTier getTier() {
        Block block = getBlockType();
        return block instanceof StorageBlockBase ? ((StorageBlockBase) block).getTier() : StorageTier.WOOD;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        for (ITickableUpgrade upgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class)) {
            upgrade.tick(null, world, pos);
        }
        if (Math.floorMod(world.getTotalWorldTime() + pos.hashCode(), UPGRADE_RENDER_CHECK_TICKS) == 0 && !collectRenderUpgradeItems().equals(renderUpgradeItems)) {
            notifyClients();
        }
    }

    /** The upgrades to draw on the front (storage tool or an upgrade in hand), one per slot. */
    public List<ItemStack> getRenderUpgradeItems() {
        return renderUpgradeItems;
    }

    private List<ItemStack> collectRenderUpgradeItems() {
        List<ItemStack> items = new ArrayList<>();
        IItemHandler upgrades = storageWrapper.getUpgradeHandler();
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            ItemStack stack = upgrades.getStackInSlot(slot);
            items.add(stack.isEmpty() ? ItemStack.EMPTY : new ItemStack(stack.getItem(), 1, stack.getMetadata()));
        }
        return new ItemStackList(items);
    }

    /** The wood shown. Like the original, a storage with no saved wood shows acacia. */
    public WoodType getRenderWoodType() {
        return woodType != null ? woodType : WoodType.DEFAULT;
    }

    @Nullable
    public WoodType getWoodType() {
        return woodType;
    }

    public int getMainColor() {
        return mainColor;
    }

    public int getAccentColor() {
        return accentColor;
    }

    /** Sets both tint colours (-1 for none), as the paintbrush and decoration table do. */
    public void setColors(int mainColor, int accentColor) {
        this.mainColor = mainColor;
        this.accentColor = accentColor;
        markDirty();
        notifyClients();
    }

    public TintMode getTintMode() {
        return TintMode.of(mainColor != StorageItemData.NO_COLOR, accentColor != StorageItemData.NO_COLOR);
    }

    /** What the front shows, in display order (at most {@link #getDisplayItemLimit()}). */
    public List<DisplayItem> getDisplayItems() {
        return displayItems;
    }

    public void setDisplayItems(List<DisplayItem> items) {
        displayItems = items.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(items.subList(0, Math.min(items.size(), getDisplayItemLimit()))));
        markDirty();
        notifyClients();
    }

    public boolean hasCustomName() {
        return customName != null && !customName.isEmpty();
    }

    protected void notifyClients() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    /** Copies the look, name and any carried contents from the stack that placed this storage. */
    public void readFromStack(ItemStack stack) {
        woodType = StorageItemData.getWoodType(stack);
        mainColor = StorageItemData.getMainColor(stack);
        accentColor = StorageItemData.getAccentColor(stack);
        customName = stack.hasDisplayName() ? stack.getDisplayName() : null;
        readExtraFromStack(stack);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(StorageItemData.CONTENTS_TAG)) {
            // A packed storage or a shulker box placed again: its contents come back.
            storageWrapper.load(tag.getCompoundTag(StorageItemData.CONTENTS_TAG).copy());
        }
        markDirty();
    }

    /** Writes the look and name onto a stack (drops, pick-block); with withContents, the contents too. */
    public void writeToStack(ItemStack stack, boolean withContents) {
        StorageItemData.setWoodType(stack, woodType);
        StorageItemData.setMainColor(stack, mainColor);
        StorageItemData.setAccentColor(stack, accentColor);
        writeExtraToStack(stack);
        if (hasCustomName()) {
            stack.setStackDisplayName(customName);
        }
        if (withContents) {
            if (packed) {
                stack.setTagInfo(StorageItemData.PACKED_TAG, new NBTTagByte((byte) 1));
            }
            stack.setTagInfo(StorageItemData.CONTENTS_TAG, storageWrapper.getContentsNbt().copy());
        }
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName() {
        if (hasCustomName()) {
            return new TextComponentString(customName);
        }
        String key = getBlockType().getTranslationKey() + ".name";
        if (woodType == null) {
            return new TextComponentTranslation(key, "", "");
        }
        return new TextComponentTranslation(key, new TextComponentTranslation(woodType.getTranslationKey()), " ");
    }

    // ---- Opening and closing -------------------------------------------------------------

    public void startOpen(EntityPlayer player) {
        if (player.isSpectator() || world == null || world.isRemote) {
            return;
        }
        openCount++;
        if (openCount == 1) {
            onOpenStateChanged(true);
            world.scheduleUpdate(pos, getBlockType(), RECHECK_OPEN_TICKS);
        }
    }

    public void stopOpen(EntityPlayer player) {
        if (player.isSpectator() || world == null || world.isRemote || openCount <= 0) {
            return;
        }
        openCount--;
        if (openCount == 0) {
            onOpenStateChanged(false);
        }
    }

    /**
     * Called from the block's scheduled tick while open: recounts who really has the GUI open,
     * so a player who logged out or was moved away can't leave the storage open forever.
     */
    public void recheckOpen() {
        if (world == null || world.isRemote) {
            return;
        }
        int count = 0;
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(5.0D);
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, area)) {
            if (player.openContainer instanceof StorageBlockContainer && ((StorageBlockContainer) player.openContainer).getTile() == this) {
                count++;
            }
        }
        boolean wasOpen = openCount > 0;
        openCount = count;
        if (count > 0) {
            world.scheduleUpdate(pos, getBlockType(), RECHECK_OPEN_TICKS);
        } else if (wasOpen) {
            onOpenStateChanged(false);
        }
    }

    /** Plays an open/close sound from the given face of the block. */
    protected void playSound(SoundEvent sound, EnumFacing facing, float volume) {
        double x = pos.getX() + 0.5D + facing.getXOffset() / 2.0D;
        double y = pos.getY() + 0.5D + facing.getYOffset() / 2.0D;
        double z = pos.getZ() + 0.5D + facing.getZOffset() / 2.0D;
        world.playSound(null, x, y, z, sound, SoundCategory.BLOCKS, volume, world.rand.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, @Nonnull IBlockState oldState, @Nonnull IBlockState newState) {
        // Opening and closing may change the state; that must not replace the tile and its items.
        return oldState.getBlock() != newState.getBlock();
    }

    // ---- Capabilities --------------------------------------------------------------------

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return storageWrapper.getFluidHandler() != null;
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return storageWrapper.getEnergyStorage() != null;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(storageWrapper.getInventoryForInputOutput());
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            IFluidHandler fluidHandler = storageWrapper.getFluidHandler();
            if (fluidHandler != null) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidHandler);
            }
        }
        if (capability == CapabilityEnergy.ENERGY) {
            IEnergyStorage energyStorage = storageWrapper.getEnergyStorage();
            if (energyStorage != null) {
                return CapabilityEnergy.ENERGY.cast(energyStorage);
            }
        }
        return super.getCapability(capability, facing);
    }

    // ---- Saving and syncing --------------------------------------------------------------

    // ---- Controller network ------------------------------------------------------------

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

    /** A linked storage is its own little network: its neighbours don't join through it. */
    @Override
    public boolean canConnectStorages() {
        return !isLinkedToController;
    }

    @Override
    public Set<BlockPos> getConnectablePositions() {
        return Collections.emptySet();
    }

    @Override
    public boolean connectLinkedSelf() {
        return true;
    }

    @Override
    public boolean canBeConnected() {
        return isLinked() || controllerPos == null;
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        saveControllerPos(compound);
        if (isLinkedToController) {
            compound.setBoolean(LINKED_TO_CONTROLLER_TAG, true);
        }
        compound.setTag(CONTENTS_TAG, storageWrapper.getContentsNbt());
        writeSynchronizedData(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        controllerPos = null;
        loadControllerPos(compound);
        isLinkedToController = compound.getBoolean(LINKED_TO_CONTROLLER_TAG);
        NBTTagCompound contents;
        if (compound.hasKey(CONTENTS_TAG)) {
            contents = compound.getCompoundTag(CONTENTS_TAG);
        } else {
            // A barrel saved before Phase S3: its plain inventory is in the same item format.
            contents = new NBTTagCompound();
            if (compound.hasKey(LEGACY_INVENTORY_TAG)) {
                contents.setTag(LEGACY_INVENTORY_TAG, compound.getCompoundTag(LEGACY_INVENTORY_TAG));
            }
        }
        storageWrapper.load(contents);
        readSynchronizedData(compound);
    }

    /** What the client needs to render and name the storage. Not the items. */
    private void writeSynchronizedData(NBTTagCompound tag) {
        if (woodType != null) {
            tag.setString(StorageItemData.WOOD_TYPE_TAG, woodType.getName());
        }
        tag.setInteger(StorageItemData.MAIN_COLOR_TAG, mainColor);
        tag.setInteger(StorageItemData.ACCENT_COLOR_TAG, accentColor);
        tag.setBoolean(StorageItemData.PACKED_TAG, packed);
        if (locked) {
            tag.setBoolean(LOCKED_TAG, true);
        }
        if (!showLock) {
            tag.setBoolean(SHOW_LOCK_TAG, false);
        }
        if (!showTier) {
            tag.setBoolean(SHOW_TIER_TAG, false);
        }
        if (showUpgrades) {
            tag.setBoolean(SHOW_UPGRADES_TAG, true);
        }
        if (hasCustomName()) {
            tag.setString(CUSTOM_NAME_TAG, customName);
        }
        if (!displayItems.isEmpty()) {
            NBTTagList list = new NBTTagList();
            for (DisplayItem displayItem : displayItems) {
                list.appendTag(displayItem.serialize());
            }
            tag.setTag(DISPLAY_ITEMS_TAG, list);
        }
        writeExtraSyncData(tag);
    }

    private void readSynchronizedData(NBTTagCompound tag) {
        woodType = tag.hasKey(StorageItemData.WOOD_TYPE_TAG) ? WoodType.fromName(tag.getString(StorageItemData.WOOD_TYPE_TAG)) : null;
        mainColor = tag.hasKey(StorageItemData.MAIN_COLOR_TAG) ? tag.getInteger(StorageItemData.MAIN_COLOR_TAG) : StorageItemData.NO_COLOR;
        accentColor = tag.hasKey(StorageItemData.ACCENT_COLOR_TAG) ? tag.getInteger(StorageItemData.ACCENT_COLOR_TAG) : StorageItemData.NO_COLOR;
        packed = tag.getBoolean(StorageItemData.PACKED_TAG);
        locked = tag.getBoolean(LOCKED_TAG);
        showLock = !tag.hasKey(SHOW_LOCK_TAG) || tag.getBoolean(SHOW_LOCK_TAG);
        showTier = !tag.hasKey(SHOW_TIER_TAG) || tag.getBoolean(SHOW_TIER_TAG);
        showUpgrades = tag.getBoolean(SHOW_UPGRADES_TAG);
        customName = tag.hasKey(CUSTOM_NAME_TAG) ? tag.getString(CUSTOM_NAME_TAG) : null;
        List<DisplayItem> items = new ArrayList<>();
        NBTTagList list = tag.getTagList(DISPLAY_ITEMS_TAG, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount() && items.size() < MAX_DISPLAY_ITEMS; i++) {
            DisplayItem displayItem = DisplayItem.deserialize(list.getCompoundTagAt(i));
            if (!displayItem.getItem().isEmpty()) {
                items.add(displayItem);
            }
        }
        displayItems = items.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(items);
        if (tag.hasKey(RENDER_UPGRADES_TAG)) {
            List<ItemStack> upgrades = new ArrayList<>();
            NBTTagList upgradeList = tag.getTagList(RENDER_UPGRADES_TAG, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < upgradeList.tagCount(); i++) {
                upgrades.add(new ItemStack(upgradeList.getCompoundTagAt(i)));
            }
            renderUpgradeItems = new ItemStackList(upgrades);
        }
        if (world != null && world.isRemote && tag.hasKey(StorageBlockWrapper.INVENTORY_SLOTS_TAG)) {
            storageWrapper.applySlotCountsFromServer(tag.getInteger(StorageBlockWrapper.INVENTORY_SLOTS_TAG), tag.getInteger(StorageBlockWrapper.UPGRADE_SLOTS_TAG));
        }
        if (world != null && world.isRemote && tag.hasKey(CLIENT_UPGRADE_INVENTORY_TAG)) {
            storageWrapper.applyUpgradeInventoryFromServer(tag.getCompoundTag(CLIENT_UPGRADE_INVENTORY_TAG));
        }
        readExtraSyncData(tag);
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.writeToNBT(new NBTTagCompound());
        writeSynchronizedData(tag);
        writeClientSyncData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(@Nonnull NBTTagCompound tag) {
        super.readFromNBT(tag);
        readSynchronizedData(tag);
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeSynchronizedData(tag);
        writeClientSyncData(tag);
        return new SPacketUpdateTileEntity(pos, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readSynchronizedData(packet.getNbtCompound());
        if (world != null) {
            // Wood, tint and the like are actual-state properties: re-render with the new values.
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    /** Stacks compared by item and metadata, so a list can tell whether the upgrades changed. */
    private static final class ItemStackList extends java.util.AbstractList<ItemStack> {
        private final List<ItemStack> stacks;

        ItemStackList(List<ItemStack> stacks) {
            this.stacks = Collections.unmodifiableList(stacks);
        }

        @Override
        public ItemStack get(int index) {
            return stacks.get(index);
        }

        @Override
        public int size() {
            return stacks.size();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof List) || ((List<?>) o).size() != size()) {
                return false;
            }
            for (int i = 0; i < size(); i++) {
                Object other = ((List<?>) o).get(i);
                if (!(other instanceof ItemStack) || !ItemStack.areItemStacksEqual(get(i), (ItemStack) other)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return size();
        }
    }

    /** One item on the front: the item, its rotation in degrees and the slot it stands for. */
    public static final class DisplayItem {
        private final ItemStack item;
        private final int rotation;
        private final int slotIndex;
        private final com.exiledradio.scorelegacy.settings.itemdisplay.DisplaySide displaySide;

        public DisplayItem(ItemStack item, int rotation, int slotIndex) {
            this(item, rotation, slotIndex, com.exiledradio.scorelegacy.settings.itemdisplay.DisplaySide.FRONT);
        }

        public DisplayItem(ItemStack item, int rotation, int slotIndex, com.exiledradio.scorelegacy.settings.itemdisplay.DisplaySide displaySide) {
            this.item = item;
            this.rotation = rotation;
            this.slotIndex = slotIndex;
            this.displaySide = displaySide;
        }

        /** The face the items are on: the front, or a double chest's left or right end. */
        public com.exiledradio.scorelegacy.settings.itemdisplay.DisplaySide getDisplaySide() {
            return displaySide;
        }

        public ItemStack getItem() {
            return item;
        }

        public int getRotation() {
            return rotation;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        NBTTagCompound serialize() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag(ITEM_TAG, item.writeToNBT(new NBTTagCompound()));
            tag.setInteger(ROTATION_TAG, rotation);
            tag.setInteger(SLOT_TAG, slotIndex);
            if (displaySide != com.exiledradio.scorelegacy.settings.itemdisplay.DisplaySide.FRONT) {
                tag.setString(DISPLAY_SIDE_TAG, displaySide.getName());
            }
            return tag;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DisplayItem)) {
                return false;
            }
            DisplayItem other = (DisplayItem) o;
            return rotation == other.rotation && slotIndex == other.slotIndex && displaySide == other.displaySide && ItemStack.areItemStacksEqual(item, other.item);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * item.getItem().hashCode() + rotation) + slotIndex;
        }

        static DisplayItem deserialize(NBTTagCompound tag) {
            return new DisplayItem(new ItemStack(tag.getCompoundTag(ITEM_TAG)), tag.getInteger(ROTATION_TAG), tag.getInteger(SLOT_TAG),
                    com.exiledradio.scorelegacy.settings.itemdisplay.DisplaySide.fromName(tag.getString(DISPLAY_SIDE_TAG)));
        }
    }
}
