/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.upgrades.compression;

import com.exiledradio.scorelegacy.inventory.IInventoryPart;
import com.exiledradio.scorelegacy.inventory.InventoryHandler;
import com.exiledradio.scorelegacy.settings.memory.MemorySettingsCategory;
import com.exiledradio.scorelegacy.util.ItemStackKey;
import com.exiledradio.scorelegacy.util.RecipeHelper;
import com.exiledradio.sstoragelegacy.ModConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The slots a compression upgrade runs (the original's CompressionInventoryPart): a chain of an
 * item and its compressed forms, most compressed first (iron block, iron ingot, iron nugget).
 * Each slot shows everything in the chain counted in its own item, and taking from or adding to
 * any of them moves the others with it.
 *
 * <p>The original keeps a second "calculated" stack per slot and updates both sides on every
 * change. Here a chain is one total counted in its smallest item; the slots show that total
 * divided by their size, and after every change the real stacks are laid out again from the
 * most compressed item down (each up to its normal stack limit), which is what the original's
 * internal compaction ends up with. The layout is worked out from the stored stacks (or, while
 * the slots are empty, from memorised items) each time, so it can't drift from what is saved.
 */
public class CompressionInventoryPart implements IInventoryPart {
    private static final String DECOMPRESSIBLE_MATCHER = "([a-z0-9_.-]+:[a-z0-9_/.-]+)(?::(\\d+))?=(\\d+)x([a-z0-9_.-]+:[a-z0-9_/.-]+)(?::(\\d+))?";
    @Nullable
    private static Map<ItemStackKey, RecipeHelper.UncompactingResult> configDecompressions = null;

    private final InventoryHandler handler;
    private final int firstSlot;
    private final int slots;
    private final Supplier<MemorySettingsCategory> memorySettings;

    public CompressionInventoryPart(InventoryHandler handler, int firstSlot, int slots, Supplier<MemorySettingsCategory> memorySettings) {
        this.handler = handler;
        this.firstSlot = firstSlot;
        this.slots = slots;
        this.memorySettings = memorySettings;
    }

    @Override
    public int getFirstSlot() {
        return firstSlot;
    }

    @Override
    public int getSlots() {
        return slots;
    }

    // ---- Layout --------------------------------------------------------------------------

    /**
     * What each slot of the range holds. group[i] is -1 for a slot that takes nothing; slots of
     * one group share a total counted in units of the group's smallest item.
     */
    static final class Layout {
        final ItemStack[] items;
        final long[] units;
        final int[] group;
        /** Nothing stored or memorised yet: any item may start a chain in any slot. */
        final boolean open;
        /** A stored item doesn't fit the chain the others make (the original then compresses nothing). */
        final boolean incompatible;

        Layout(int slots, boolean open, boolean incompatible) {
            items = new ItemStack[slots];
            Arrays.fill(items, ItemStack.EMPTY);
            units = new long[slots];
            group = new int[slots];
            Arrays.fill(group, -1);
            this.open = open;
            this.incompatible = incompatible;
        }
    }

    Layout getLayout() {
        return getLayout(-1, ItemStack.EMPTY);
    }

    /** The layout, as if extraSlot held extraStack (to start a chain in an empty range). */
    private Layout getLayout(int extraSlot, ItemStack extraStack) {
        Map<Integer, ItemStack> existing = new HashMap<>();
        for (int i = 0; i < slots; i++) {
            ItemStack raw = handler.getRawStack(firstSlot + i);
            if (!raw.isEmpty()) {
                existing.put(i, raw);
            }
        }
        if (existing.isEmpty() && extraSlot < 0) {
            MemorySettingsCategory memory = memorySettings.get();
            if (memory != null) {
                for (int i = 0; i < slots; i++) {
                    ItemStack remembered = memory.getSlotFilterStack(firstSlot + i);
                    if (!remembered.isEmpty()) {
                        existing.put(i, remembered);
                    }
                }
            }
        }
        if (extraSlot >= 0) {
            existing.put(extraSlot - firstSlot, extraStack);
        }
        if (existing.isEmpty()) {
            return new Layout(slots, true, false);
        }
        int anchor = -1;
        for (int i = slots - 1; i >= 0; i--) {
            if (existing.containsKey(i)) {
                anchor = i;
                break;
            }
        }
        Layout chain = buildChain(anchor, single(existing.get(anchor)), existing);
        return chain != null ? chain : plainLayout(existing);
    }

    /** Each stored stack a slot of its own; empty slots take nothing. */
    private Layout plainLayout(Map<Integer, ItemStack> existing) {
        Layout layout = new Layout(slots, false, true);
        for (Map.Entry<Integer, ItemStack> entry : existing.entrySet()) {
            int i = entry.getKey();
            layout.items[i] = single(entry.getValue());
            layout.units[i] = 1;
            layout.group[i] = i;
        }
        return layout;
    }

    /**
     * The chain through the anchor slot: its uncompressed forms after it, its compressed forms
     * before it (the original's getSlotDefinitions and addPreviousItems). Null when a stored
     * item doesn't belong.
     */
    @Nullable
    private Layout buildChain(int anchor, ItemStack anchorItem, Map<Integer, ItemStack> existing) {
        // An item with NBT never compresses (RecipeHelper skips those), so it is a chain of one.
        Layout layout = new Layout(slots, false, false);
        int[] multiplier = new int[slots];
        // Smaller forms after the anchor.
        ItemStack current = anchorItem;
        int bottom = anchor;
        for (int i = anchor + 1; i < slots; i++) {
            RecipeHelper.UncompactingResult uncompacted = getUncompactingResult(current);
            if (uncompacted == null) {
                break;
            }
            layout.items[i] = uncompacted.getResult();
            multiplier[i] = uncompacted.getCompactUsingShape() == RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE ? 4 : 9;
            current = layout.items[i];
            bottom = i;
        }
        // The anchor and bigger forms before it.
        current = anchorItem;
        int top = anchor;
        for (int i = anchor; i >= 0; i--) {
            if (existing.containsKey(i) && !sameItem(existing.get(i), current)) {
                return null;
            }
            layout.items[i] = current;
            top = i;
            int shapeSize = getCompressionSize(current);
            if (shapeSize == 0) {
                break;
            }
            multiplier[i] = shapeSize * shapeSize;
            if (i > 0) {
                current = single(RecipeHelper.getCompactingResult(current, shapeSize).getResult());
                if (current.isEmpty()) {
                    break;
                }
            }
        }
        // Stored items outside the chain make it incompatible, as in the original.
        for (int i : existing.keySet()) {
            if (i < top || i > bottom) {
                return null;
            }
        }
        long units = 1;
        for (int i = bottom; i >= top; i--) {
            layout.units[i] = units;
            layout.group[i] = 0;
            units *= Math.max(1, multiplier[i]);
        }
        return layout;
    }

    /** 3 or 2 if this item compresses into something that uncompresses back (or the config says so), else 0. */
    private static int getCompressionSize(ItemStack item) {
        java.util.Set<RecipeHelper.CompactingShape> shapes = RecipeHelper.getItemCompactingShapes(item);
        if (shapes.contains(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)) {
            return 3;
        }
        if (shapes.contains(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE)) {
            return 2;
        }
        if (shapes.contains(RecipeHelper.CompactingShape.THREE_BY_THREE)
                && getConfigDecompression(single(RecipeHelper.getCompactingResult(item, 3).getResult())) != null) {
            return 3;
        }
        if (shapes.contains(RecipeHelper.CompactingShape.TWO_BY_TWO)
                && getConfigDecompression(single(RecipeHelper.getCompactingResult(item, 2).getResult())) != null) {
            return 2;
        }
        return 0;
    }

    @Nullable
    private static RecipeHelper.UncompactingResult getUncompactingResult(ItemStack item) {
        RecipeHelper.UncompactingResult result = RecipeHelper.getUncompactingResult(item);
        return result != null ? result : getConfigDecompression(item);
    }

    /** The config's extra decompressions (glowstone into dust and the like, which have no uncrafting recipe). */
    @Nullable
    private static RecipeHelper.UncompactingResult getConfigDecompression(ItemStack item) {
        if (item.isEmpty()) {
            return null;
        }
        if (configDecompressions == null) {
            Map<ItemStackKey, RecipeHelper.UncompactingResult> map = new HashMap<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(DECOMPRESSIBLE_MATCHER);
            for (String entry : ModConfig.upgrades.compression.additionalDecompressibleItems) {
                java.util.regex.Matcher matcher = pattern.matcher(entry);
                if (!matcher.matches()) {
                    continue;
                }
                Item from = Item.REGISTRY.getObject(new ResourceLocation(matcher.group(1)));
                Item to = Item.REGISTRY.getObject(new ResourceLocation(matcher.group(4)));
                int count = Integer.parseInt(matcher.group(3));
                if (from != null && to != null && (count == 4 || count == 9)) {
                    int fromMeta = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
                    int toMeta = matcher.group(5) == null ? 0 : Integer.parseInt(matcher.group(5));
                    map.put(new ItemStackKey(new ItemStack(from, 1, fromMeta)), new RecipeHelper.UncompactingResult(new ItemStack(to, 1, toMeta),
                            count == 4 ? RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE : RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
                }
            }
            configDecompressions = map;
        }
        return configDecompressions.get(new ItemStackKey(single(item)));
    }

    /** The config was edited: read the extra decompressions again. */
    public static void clearConfigCache() {
        configDecompressions = null;
    }

    private static ItemStack single(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return ItemHandlerHelper.canItemStacksStack(single(a), single(b));
    }

    // ---- Totals --------------------------------------------------------------------------

    private long getTotal(Layout layout, int group) {
        long total = 0;
        for (int i = 0; i < slots; i++) {
            if (layout.group[i] == group) {
                total += handler.getRawStack(firstSlot + i).getCount() * layout.units[i];
            }
        }
        return total;
    }

    private long getCapacity(Layout layout, int group) {
        long capacity = 0;
        for (int i = 0; i < slots; i++) {
            if (layout.group[i] == group) {
                capacity += (long) handler.getBaseStackLimit(layout.items[i]) * layout.units[i];
            }
        }
        return capacity;
    }

    /** Stores a group's total again: as many of the biggest form as fit, then the next, and so on. */
    private void writeGroup(Layout layout, int group, long total) {
        int last = -1;
        for (int i = 0; i < slots; i++) {
            if (layout.group[i] == group) {
                last = i;
            }
        }
        long remaining = total;
        for (int i = 0; i < slots; i++) {
            if (layout.group[i] != group) {
                continue;
            }
            long count = remaining / layout.units[i];
            if (i != last) {
                count = Math.min(count, handler.getBaseStackLimit(layout.items[i]));
            }
            count = Math.min(count, Integer.MAX_VALUE);
            remaining -= count * layout.units[i];
            ItemStack raw = handler.getRawStack(firstSlot + i);
            if (count == 0) {
                if (!raw.isEmpty()) {
                    handler.setRawStack(firstSlot + i, ItemStack.EMPTY);
                }
            } else if (raw.isEmpty() || raw.getCount() != count || !sameItem(raw, layout.items[i])) {
                ItemStack stack = layout.items[i].copy();
                stack.setCount((int) count);
                handler.setRawStack(firstSlot + i, stack);
            }
        }
    }

    private static int toInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    // ---- IInventoryPart --------------------------------------------------------------------

    @Override
    public ItemStack getStackInSlot(int slot) {
        Layout layout = getLayout();
        int i = slot - firstSlot;
        int group = layout.group[i];
        if (group < 0) {
            return ItemStack.EMPTY;
        }
        int count = toInt(getTotal(layout, group) / layout.units[i]);
        if (count == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = layout.items[i].copy();
        stack.setCount(count);
        return stack;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        Layout layout = getLayout();
        int i = slot - firstSlot;
        if (layout.open) {
            return true;
        }
        return layout.group[i] >= 0 && sameItem(layout.items[i], stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        Layout layout = getLayout();
        int i = slot - firstSlot;
        if (layout.open) {
            return handler.getBaseSlotLimit();
        }
        return layout.group[i] < 0 ? 0 : toInt(getCapacity(layout, layout.group[i]) / layout.units[i]);
    }

    @Override
    public int getStackLimit(int slot, ItemStack stack) {
        Layout layout = getLayout();
        int i = slot - firstSlot;
        if (layout.open) {
            return handler.getBaseStackLimit(stack);
        }
        return layout.group[i] < 0 ? 0 : toInt(getCapacity(layout, layout.group[i]) / layout.units[i]);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return stack;
        }
        Layout layout = getLayout();
        if (layout.open) {
            layout = getLayout(slot, stack);
        }
        int i = slot - firstSlot;
        int group = layout.group[i];
        if (group < 0 || !sameItem(layout.items[i], stack)) {
            return stack;
        }
        long total = getTotal(layout, group);
        long free = getCapacity(layout, group) - total;
        int inserted = toInt(Math.min(stack.getCount(), free / layout.units[i]));
        if (inserted <= 0) {
            return stack;
        }
        if (!simulate) {
            writeGroup(layout, group, total + inserted * layout.units[i]);
        }
        return inserted == stack.getCount() ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - inserted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractItem(slot, amount, simulate, true);
    }

    private ItemStack extractItem(int slot, int amount, boolean simulate, boolean limitToStackSize) {
        Layout layout = getLayout();
        int i = slot - firstSlot;
        int group = layout.group[i];
        if (group < 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        long total = getTotal(layout, group);
        long available = total / layout.units[i];
        int extracted = toInt(Math.min(amount, available));
        if (limitToStackSize) {
            extracted = Math.min(extracted, layout.items[i].getMaxStackSize());
        }
        if (extracted <= 0) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            writeGroup(layout, group, total - extracted * layout.units[i]);
        }
        return ItemHandlerHelper.copyStackWithSize(layout.items[i], extracted);
    }

    /** Sets what the slot shows by adding or taking the difference, as in the original. */
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        ItemStack current = getStackInSlot(slot);
        if (stack.isEmpty()) {
            if (!current.isEmpty()) {
                extractItem(slot, current.getCount(), false, false);
            }
            return;
        }
        if (!current.isEmpty() && !sameItem(current, stack)) {
            return;
        }
        int currentCount = current.getCount();
        if (stack.getCount() > currentCount) {
            insertItem(slot, ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - currentCount), false);
        } else if (stack.getCount() < currentCount) {
            extractItem(slot, currentCount - stack.getCount(), false, false);
        }
    }

    /** Whether the stored items form a chain the compression can run (checked before the upgrade goes in). */
    boolean isCompatible() {
        return !getLayout().incompatible;
    }
}
