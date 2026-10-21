/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.inventory.InventoryHandler;
import com.exiledradio.scorelegacy.settings.memory.MemorySettingsCategory;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * A limited barrel (the original's LimitedBarrelBlockEntity): 1 to 4 big slots, each shown on
 * the front with its count (in a dye colour of the player's choice) and optionally a fill level.
 * Counts and fill levels go to clients only when they change.
 */
public class LimitedBarrelTileEntity extends BarrelTileEntity {
    private static final String SLOT_COUNTS_TAG = "slotCounts";
    private static final String SLOT_FILL_LEVELS_TAG = "slotFillLevels";
    private static final String SHOW_COUNTS_TAG = "showCounts";
    private static final String SHOW_FILL_LEVELS_TAG = "showFillLevels";
    private static final String SLOT_COLORS_TAG = "slotColors";
    /** Ticks within which a second right-click counts as a double click. */
    private static final int DOUBLE_CLICK_TICKS = 10;
    /** Dye text colours of 1.14+ (DyeColor.getTextColor), which the original colours counts with. */
    private static final Map<EnumDyeColor, Integer> TEXT_COLORS = new EnumMap<>(EnumDyeColor.class);

    static {
        int[] colors = {0xFFFFFF, 0xFF681F, 0xFF00FF, 0x9AC0CD, 0xFFFF00, 0xBFFF00, 0xFF69B4, 0x808080,
                0xD3D3D3, 0x00FFFF, 0xA020F0, 0x0000FF, 0x8B4513, 0x00FF00, 0xFF0000, 0x000000};
        for (EnumDyeColor color : EnumDyeColor.values()) {
            TEXT_COLORS.put(color, colors[color.getMetadata()]);
        }
    }

    private long lastDepositTime = -100;
    private Map<Integer, EnumDyeColor> slotColors = new HashMap<>();
    private boolean showCounts = true;
    private boolean showFillLevels = false;

    /** Client: what the server last sent. Server: what it last sent, to send again only on change. */
    private int[] slotCounts = new int[0];
    private float[] slotFillLevels = new float[0];

    // ---- What a limited barrel changes ---------------------------------------------------

    @Override
    public int getInventorySlotCount() {
        Block block = world == null ? null : getBlockType();
        return block instanceof LimitedBarrelBlock ? ((LimitedBarrelBlock) block).getSlotCount() : 1;
    }

    @Override
    public int getUpgradeSlotCount() {
        Block block = world == null ? null : getBlockType();
        return block instanceof LimitedBarrelBlock ? ((LimitedBarrelBlock) block).getDefaultUpgradeSlots() : 1;
    }

    @Override
    public int getBaseStackSizeMultiplier() {
        Block block = world == null ? null : getBlockType();
        return block instanceof LimitedBarrelBlock ? ((LimitedBarrelBlock) block).getBaseStackSizeMultiplier() : 1;
    }

    @Override
    public int getDisplayItemLimit() {
        return getInventorySlotCount();
    }

    @Override
    public boolean memorizesItemsWhenLocked() {
        return true;
    }

    @Override
    public boolean allowsEmptySlotsMatchingItemInsertsWhenLocked() {
        return false;
    }

    @Override
    protected void onOpenStateChanged(boolean open) {
        // No lid to open.
    }

    // ---- Counts, fill levels and their colours -------------------------------------------

    public boolean shouldShowCounts() {
        return showCounts;
    }

    public void toggleCountVisibility() {
        showCounts = !showCounts;
        markDirty();
        notifyClients();
    }

    public boolean shouldShowFillLevels() {
        return showFillLevels;
    }

    public void toggleFillLevelVisibility() {
        showFillLevels = !showFillLevels;
        markDirty();
        notifyClients();
    }

    public int getSlotCount(int slot) {
        return slot >= 0 && slot < slotCounts.length ? slotCounts[slot] : 0;
    }

    public float getSlotFillPercentage(int slot) {
        return slot >= 0 && slot < slotFillLevels.length ? slotFillLevels[slot] : 0;
    }

    public int getSlotColor(int slot) {
        return TEXT_COLORS.get(slotColors.getOrDefault(slot, EnumDyeColor.WHITE));
    }

    /** Dyes one slot's count, or every slot's; uses one dye if anything changed. */
    public boolean applyDye(int slot, ItemStack dyeStack, EnumDyeColor color, boolean applyToAll) {
        InventoryHandler inventory = getStorageWrapper().getInventoryHandler();
        if (slot < 0 || slot >= inventory.getSlots()) {
            return false;
        }
        boolean changed = false;
        if (applyToAll) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                changed |= applyDye(i, color, inventory);
            }
        } else {
            changed = applyDye(slot, color, inventory);
        }
        if (!changed) {
            return false;
        }
        dyeStack.shrink(1);
        markDirty();
        notifyClients();
        return true;
    }

    private boolean applyDye(int slot, EnumDyeColor color, InventoryHandler inventory) {
        if (inventory.getStackInSlot(slot).isEmpty() || color == slotColors.get(slot)) {
            return false;
        }
        slotColors.put(slot, color);
        return true;
    }

    // ---- Front interaction ---------------------------------------------------------------

    /** Right-click on a slot: the held stack goes in; a quick second click brings all matching items from the inventory. */
    public boolean depositItem(EntityPlayer player, EnumHand hand, ItemStack held, int slot) {
        long time = world.getTotalWorldTime();
        boolean doubleClick = time - lastDepositTime < DOUBLE_CLICK_TICKS;
        lastDepositTime = time;

        InventoryHandler inventory = getStorageWrapper().getInventoryHandler();
        MemorySettingsCategory memory = getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
        if (doubleClick) {
            return depositFromPlayerInventory(player, slot, inventory, memory);
        }
        if (held.isEmpty()) {
            return false;
        }
        ItemStack remaining = inventory.insertItem(slot, held, true);
        if (remaining.getCount() == held.getCount()) {
            return false;
        }
        remaining = inventory.insertItem(slot, held, false);
        if (isLocked() && memory != null) {
            memory.selectSlot(slot);
        }
        player.setHeldItem(hand, remaining);
        return true;
    }

    private boolean depositFromPlayerInventory(EntityPlayer player, int slot, InventoryHandler inventory, MemorySettingsCategory memory) {
        boolean success = false;
        for (int playerSlot = 0; playerSlot < player.inventory.mainInventory.size(); playerSlot++) {
            ItemStack playerStack = player.inventory.mainInventory.get(playerSlot);
            if (playerStack.isEmpty()) {
                continue;
            }
            ItemStack inSlot = inventory.getStackInSlot(slot);
            boolean matches = inSlot.isEmpty()
                    ? memory != null && memory.isSlotSelected(slot) && memory.matchesFilter(slot, playerStack)
                    : ItemHandlerHelper.canItemStacksStack(inSlot, playerStack);
            if (!matches) {
                continue;
            }
            ItemStack remaining = inventory.insertItem(slot, playerStack, false);
            if (remaining.getCount() != playerStack.getCount()) {
                player.inventory.mainInventory.set(playerSlot, remaining);
                success = true;
            }
        }
        if (success) {
            player.inventory.markDirty();
        }
        return success;
    }

    /** Left-click on a slot: one item, or a stack when sneaking, into the player's inventory. */
    boolean tryToTakeItem(EntityPlayer player, int slot) {
        InventoryHandler inventory = getStorageWrapper().getInventoryHandler();
        ItemStack inSlot = inventory.getStackInSlot(slot);
        if (inSlot.isEmpty()) {
            return false;
        }
        int count = player.isSneaking() ? Math.min(inSlot.getMaxStackSize(), inSlot.getCount()) : 1;
        ItemStack taken = inventory.extractItem(slot, count, false);
        if (player.inventory.addItemStackToInventory(taken)) {
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F,
                    ((world.rand.nextFloat() - world.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
        }
        if (!taken.isEmpty()) {
            player.dropItem(taken, false);
        }
        return true;
    }

    // ---- Ticking: send counts when they change -------------------------------------------

    @Override
    public void update() {
        super.update();
        if (world == null || world.isRemote) {
            return;
        }
        InventoryHandler inventory = getStorageWrapper().getInventoryHandler();
        int slots = inventory.getSlots();
        boolean changed = slotCounts.length != slots;
        if (!changed) {
            for (int slot = 0; slot < slots; slot++) {
                if (inventory.getStackInSlot(slot).getCount() != slotCounts[slot]) {
                    changed = true;
                    break;
                }
            }
        }
        if (changed) {
            updateCounts(inventory);
            notifyClients();
        }
    }

    private void updateCounts(InventoryHandler inventory) {
        int slots = inventory.getSlots();
        slotCounts = new int[slots];
        slotFillLevels = new float[slots];
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            slotCounts[slot] = stack.getCount();
            slotFillLevels[slot] = stack.isEmpty() ? 0 : stack.getCount() / (float) inventory.getMaxStackSizeFor(slot, stack);
        }
    }

    // ---- Saving and syncing --------------------------------------------------------------

    @Override
    protected void writeExtraSyncData(NBTTagCompound tag) {
        super.writeExtraSyncData(tag);
        if (!showCounts) {
            tag.setBoolean(SHOW_COUNTS_TAG, false);
        }
        if (showFillLevels) {
            tag.setBoolean(SHOW_FILL_LEVELS_TAG, true);
        }
        if (!slotColors.isEmpty()) {
            NBTTagCompound colors = new NBTTagCompound();
            slotColors.forEach((slot, color) -> colors.setInteger(String.valueOf(slot), color.getMetadata()));
            tag.setTag(SLOT_COLORS_TAG, colors);
        }
    }

    @Override
    protected void writeClientSyncData(NBTTagCompound tag) {
        super.writeClientSyncData(tag);
        if (world != null && !world.isRemote) {
            updateCounts(getStorageWrapper().getInventoryHandler());
        }
        tag.setIntArray(SLOT_COUNTS_TAG, slotCounts);
        NBTTagList fillLevels = new NBTTagList();
        for (float fillLevel : slotFillLevels) {
            fillLevels.appendTag(new NBTTagFloat(fillLevel));
        }
        tag.setTag(SLOT_FILL_LEVELS_TAG, fillLevels);
    }

    @Override
    protected void readExtraSyncData(NBTTagCompound tag) {
        super.readExtraSyncData(tag);
        showCounts = !tag.hasKey(SHOW_COUNTS_TAG) || tag.getBoolean(SHOW_COUNTS_TAG);
        showFillLevels = tag.getBoolean(SHOW_FILL_LEVELS_TAG);
        slotColors = new HashMap<>();
        NBTTagCompound colors = tag.getCompoundTag(SLOT_COLORS_TAG);
        for (String key : colors.getKeySet()) {
            try {
                slotColors.put(Integer.parseInt(key), EnumDyeColor.byMetadata(colors.getInteger(key)));
            } catch (NumberFormatException ignored) {
                // Not a slot number: skip it.
            }
        }
        if (tag.hasKey(SLOT_COUNTS_TAG)) {
            slotCounts = Arrays.copyOf(tag.getIntArray(SLOT_COUNTS_TAG), tag.getIntArray(SLOT_COUNTS_TAG).length);
        }
        if (tag.hasKey(SLOT_FILL_LEVELS_TAG)) {
            NBTTagList fillLevels = tag.getTagList(SLOT_FILL_LEVELS_TAG, Constants.NBT.TAG_FLOAT);
            slotFillLevels = new float[fillLevels.tagCount()];
            for (int i = 0; i < fillLevels.tagCount(); i++) {
                slotFillLevels[i] = fillLevels.getFloatAt(i);
            }
        }
    }
}
