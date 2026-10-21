/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.api.IStorageWrapper;
import com.exiledradio.scorelegacy.util.IItemHandlerSimpleInserter;
import com.exiledradio.scorelegacy.inventory.InventoryHandler;
import com.exiledradio.scorelegacy.inventory.StorageContents;
import com.exiledradio.scorelegacy.inventory.UpgradeHandler;
import com.exiledradio.scorelegacy.settings.SettingsHandler;
import com.exiledradio.scorelegacy.settings.itemdisplay.ItemDisplaySettingsCategory;
import com.exiledradio.scorelegacy.settings.memory.MemorySettingsCategory;
import com.exiledradio.scorelegacy.util.SortBy;
import com.exiledradio.sstoragelegacy.ModConfig;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A placed barrel as Core sees it: inventory, upgrades and settings, all kept in one NBT
 * compound the tile saves (the original's StorageWrapper inside StorageBlockEntity).
 */
public class StorageBlockWrapper implements IStorageWrapper {
    private static final String OPEN_TAB_ID_TAG = "openTabId";
    private static final String CONTENTS_UUID_TAG = "contentsUuid";

    /** Which items never stack past their normal size (Storage's config), and what the storage refuses. */
    private final StorageContents.Policy policy = new StorageContents.Policy() {
        @Override
        public boolean isItemValid(StorageContents contents, int slot, ItemStack stack) {
            if (!tile.isAllowedInStorage(stack)) {
                return false;
            }
            // A locked limited barrel's empty slots take nothing (memorized slots are let through
            // by the memory filter before this is asked).
            if (tile.isLocked() && !tile.allowsEmptySlotsMatchingItemInsertsWhenLocked()) {
                MemorySettingsCategory memory = getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
                return !getInventoryHandler().getStackInSlot(slot).isEmpty() || (memory != null && memory.isSlotSelected(slot));
            }
            return true;
        }

        @Override
        public int getBaseStackSizeMultiplier() {
            return tile.getBaseStackSizeMultiplier();
        }

        @Override
        public String[] getNonStackableItems() {
            return ModConfig.upgrades.nonStackableItems;
        }
    };

    public static final String INVENTORY_SLOTS_TAG = "numberOfInventorySlots";
    public static final String UPGRADE_SLOTS_TAG = "numberOfUpgradeSlots";
    private final StorageTileEntity tile;
    private NBTTagCompound contentsNbt;
    @Nullable
    private StorageContents contents = null;
    @Nullable
    private LockedInputHandler lockedInputOutput = null;

    public StorageBlockWrapper(StorageTileEntity tile, NBTTagCompound contentsNbt) {
        this.tile = tile;
        this.contentsNbt = contentsNbt;
    }

    public NBTTagCompound getContentsNbt() {
        return contentsNbt;
    }

    /** Replaces everything with saved contents (loading the tile); handlers are rebuilt on next use. */
    public void load(NBTTagCompound nbt) {
        contentsNbt = nbt;
        contents = null;
    }

    /** Client side: the server's upgrades, so a container opened here gets the same upgrade tabs and slots. */
    public void applyUpgradeInventoryFromServer(NBTTagCompound upgradeInventory) {
        getContents().applyUpgradeInventoryFromServer(upgradeInventory);
    }

    private StorageContents getContents() {
        if (contents == null) {
            contents = new StorageContents(getContentsUuid(), contentsNbt, tile::markDirty, policy);
        }
        return contents;
    }

    @Override
    public InventoryHandler getInventoryHandler() {
        // Upgrades and settings first: the inventory's insert hooks, slot limit and memory slots come from them.
        getUpgradeHandler();
        getSettingsHandler();
        return getContents().getInventoryHandler(getNumberOfInventorySlots());
    }

    @Override
    public UpgradeHandler getUpgradeHandler() {
        return getContents().getUpgradeHandler(getNumberOfUpgradeSlots(), this);
    }

    @Override
    public IItemHandlerSimpleInserter getInventoryForUpgradeProcessing() {
        getInventoryHandler();
        return getContents().getInventoryForUpgradeProcessing(this);
    }

    @Override
    public IItemHandlerSimpleInserter getInventoryForInputOutput() {
        getInventoryHandler();
        IItemHandlerSimpleInserter inventory = getContents().getInventoryForInputOutput(this);
        if (tile.isLocked() && tile.allowsEmptySlotsMatchingItemInsertsWhenLocked()) {
            // Locked: hoppers and pipes may only add more of what is already inside.
            if (lockedInputOutput == null || lockedInputOutput.getDelegate() != inventory) {
                lockedInputOutput = new LockedInputHandler(inventory, getInventoryHandler());
            }
            return lockedInputOutput;
        }
        return inventory;
    }

    @Override
    public void refreshInventoryForInputOutput() {
        getContents().refreshInventoryForInputOutput();
    }

    @Override
    public void refreshInventoryForUpgradeProcessing() {
        getContents().refreshInventoryForUpgradeProcessing();
    }

    @Override
    public int getOpenTabId() {
        return contentsNbt.hasKey(OPEN_TAB_ID_TAG) ? contentsNbt.getInteger(OPEN_TAB_ID_TAG) : -1;
    }

    @Override
    public void setOpenTabId(int openTabId) {
        if (openTabId < 0) {
            contentsNbt.removeTag(OPEN_TAB_ID_TAG);
        } else {
            contentsNbt.setInteger(OPEN_TAB_ID_TAG, openTabId);
        }
        tile.markDirty();
    }

    /**
     * The original's StorageWrapper: a storage keeps the slot counts it was first used with, so a
     * config change applies to new storages only. Saved by the server; the client gets them with
     * the tile's sync data, since its container must match the server's.
     */
    @Override
    public int getNumberOfInventorySlots() {
        return getSavedCount(INVENTORY_SLOTS_TAG, tile.getInventorySlotCount());
    }

    @Override
    public int getNumberOfUpgradeSlots() {
        return getSavedCount(UPGRADE_SLOTS_TAG, tile.getUpgradeSlotCount());
    }

    private int getSavedCount(String tag, int defaultCount) {
        if (contentsNbt.hasKey(tag)) {
            return contentsNbt.getInteger(tag);
        }
        if (tile.getWorld() != null && !tile.getWorld().isRemote) {
            contentsNbt.setInteger(tag, defaultCount);
            tile.markDirty();
        }
        return defaultCount;
    }

    /** Client side: the counts the server uses. */
    public void applySlotCountsFromServer(int inventorySlots, int upgradeSlots) {
        if (contentsNbt.getInteger(INVENTORY_SLOTS_TAG) != inventorySlots || contentsNbt.getInteger(UPGRADE_SLOTS_TAG) != upgradeSlots) {
            contentsNbt.setInteger(INVENTORY_SLOTS_TAG, inventorySlots);
            contentsNbt.setInteger(UPGRADE_SLOTS_TAG, upgradeSlots);
            contents = null;
        }
    }

    /**
     * A tier upgrade: adds the difference between the two tiers' counts to saved counts (the
     * original's increaseSize; never shrinks). Unsaved counts follow the new tier on their own.
     */
    public void increaseSize(int additionalInventorySlots, int additionalUpgradeSlots) {
        increaseSavedCount(INVENTORY_SLOTS_TAG, additionalInventorySlots);
        increaseSavedCount(UPGRADE_SLOTS_TAG, additionalUpgradeSlots);
        contents = null;
    }

    /**
     * Sets the inventory size outright (a double chest joining or splitting). Shrinking forgets
     * what was in the slots it removes, so empty them first.
     */
    public void setNumberOfInventorySlots(int slots) {
        NBTTagCompound inventory = contentsNbt.getCompoundTag(InventoryHandler.INVENTORY_TAG);
        net.minecraft.nbt.NBTTagList items = inventory.getTagList("Items", net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND);
        for (int i = items.tagCount() - 1; i >= 0; i--) {
            if (items.getCompoundTagAt(i).getInteger("Slot") >= slots) {
                items.removeTag(i);
            }
        }
        contentsNbt.setInteger(INVENTORY_SLOTS_TAG, slots);
        contents = null;
        lockedInputOutput = null;
        tile.markDirty();
    }

    /** Whether no upgrade is in the storage. */
    public boolean hasNoUpgrades() {
        UpgradeHandler upgrades = getUpgradeHandler();
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            if (!upgrades.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void increaseSavedCount(String tag, int additional) {
        if (additional > 0 && contentsNbt.hasKey(tag)) {
            contentsNbt.setInteger(tag, contentsNbt.getInteger(tag) + additional);
        }
    }

    /** The same for a storage's contents carried in an item (a shulker box or packed storage crafted to a higher tier). */
    public static void increaseSize(NBTTagCompound contentsNbt, int additionalInventorySlots, int additionalUpgradeSlots) {
        for (String tag : new String[] {INVENTORY_SLOTS_TAG, UPGRADE_SLOTS_TAG}) {
            int additional = tag.equals(INVENTORY_SLOTS_TAG) ? additionalInventorySlots : additionalUpgradeSlots;
            if (additional > 0 && contentsNbt.hasKey(tag)) {
                contentsNbt.setInteger(tag, contentsNbt.getInteger(tag) + additional);
            }
        }
    }

    /** The jukebox tracks what it plays by this; made on the server the first time it's needed. */
    @Nullable
    @Override
    public UUID getContentsUuid() {
        if (!contentsNbt.hasUniqueId(CONTENTS_UUID_TAG)) {
            if (tile.getWorld() == null || tile.getWorld().isRemote) {
                return null;
            }
            contentsNbt.setUniqueId(CONTENTS_UUID_TAG, UUID.randomUUID());
            tile.markDirty();
        }
        return contentsNbt.getUniqueId(CONTENTS_UUID_TAG);
    }

    @Override
    public int getMainColor() {
        int color = tile.getMainColor();
        return color == StorageItemData.NO_COLOR ? -1 : color;
    }

    @Override
    public int getAccentColor() {
        int color = tile.getAccentColor();
        return color == StorageItemData.NO_COLOR ? -1 : color;
    }

    @Override
    public SettingsHandler getSettingsHandler() {
        return getContents().getSettingsHandler(this);
    }

    @Override
    public void addSettingsCategories(SettingsHandler handler) {
        IStorageWrapper.super.addSettingsCategories(handler);
        // Reached from getInventoryHandler before the settings exist, so the inventory is taken from
        // the contents directly rather than through getInventoryHandler.
        InventoryHandler inventory = getContents().getInventoryHandler(getNumberOfInventorySlots());
        handler.addCategory(ItemDisplaySettingsCategory.NAME, (nbt, save) -> {
            ItemDisplaySettingsCategory category = new ItemDisplaySettingsCategory(() -> inventory, this::setItemDisplay, nbt, save,
                    tile.getDisplayItemLimit(), () -> handler.getTypeCategory(MemorySettingsCategory.class));
            inventory.addListener(category::itemChanged);
            return category;
        });
    }

    /** The item display setting changed: the front shows those items (the tile syncs them to clients). */
    private void setItemDisplay(List<ItemDisplaySettingsCategory.DisplayedItem> displayed) {
        List<StorageTileEntity.DisplayItem> items = new ArrayList<>();
        for (ItemDisplaySettingsCategory.DisplayedItem item : displayed) {
            items.add(new StorageTileEntity.DisplayItem(item.getItem(), item.getRotation(), item.getSlot(), item.getDisplaySide()));
        }
        if (!items.equals(tile.getDisplayItems())) {
            tile.setDisplayItems(items);
        }
    }

    // The settings screen's wording from Storage's own lang file ("Settings", "Back To Storage").

    /** Double chests can show their items on either end (the original ChestBlock's canChangeDisplaySide). */
    @Override
    public boolean canChangeDisplaySide() {
        if (tile.getWorld() == null) {
            return false;
        }
        net.minecraft.block.state.IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        return state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestBlock.Type.SINGLE;
    }

    /** Back to the front: a chest that stops being double (the original resets it on every split). */
    public void resetDisplaySide() {
        ItemDisplaySettingsCategory display = getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
        if (display != null) {
            display.setDisplaySide(com.exiledradio.scorelegacy.settings.itemdisplay.DisplaySide.FRONT);
        }
    }

    @Override
    public String getSettingsTitleKey() {
        return "gui.sstoragelegacy.settings.title";
    }

    @Override
    public String getSettingsTooltipKey() {
        return "gui.sstoragelegacy.settings.tooltip";
    }

    @Override
    public String getBackToStorageTooltipKey() {
        return "gui.sstoragelegacy.back_to_storage.tooltip";
    }

    /** The original's storage types, which the upgrade limits config names: "barrel", "chest", "shulker_box", "limited_barrel". */
    @Override
    public String getStorageType() {
        if (tile instanceof LimitedBarrelTileEntity) {
            return "limited_barrel";
        }
        net.minecraft.block.Block block = tile.getBlockType();
        return block instanceof StorageBlockBase ? ((StorageBlockBase) block).getStorageName() : "";
    }

    @Override
    public String getMainSettingsTabKey() {
        return "gui.sstoragelegacy.settings.storage";
    }

    @Override
    public ItemStack getMainSettingsTabIcon() {
        if (tile.getWorld() != null && tile.getWorld().getBlockState(tile.getPos()).getBlock() instanceof StorageBlockBase) {
            return ((StorageBlockBase) tile.getWorld().getBlockState(tile.getPos()).getBlock()).getStorageStack(tile.getWorld(), tile.getPos());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public String getMatchContentsButtonKey() {
        return "gui.sstoragelegacy.upgrades.buttons.match_storage_contents";
    }

    @Override
    public String getStorageContextButtonKey() {
        return "gui.sstoragelegacy.settings.buttons.context_storage";
    }

    @Override
    public String getPlayerSettingsTagName() {
        return "sophisticatedStorageSettings";
    }

    @Override
    public SortBy getSortBy() {
        return getContents().getSortBy();
    }

    @Override
    public void setSortBy(SortBy sortBy) {
        getContents().setSortBy(sortBy);
    }
}
