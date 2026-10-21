/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.sstoragelegacy.item.BarrelItem;
import com.exiledradio.sstoragelegacy.item.BarrelMaterials;
import com.exiledradio.sstoragelegacy.item.LimitedBarrelItem;
import com.exiledradio.sstoragelegacy.item.PaintbrushItem;
import com.exiledradio.sstoragelegacy.item.StorageBlockItem;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import com.exiledradio.sstoragelegacy.util.DecorationHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The decoration table (the original 1.20's DecorationTableBlockEntity): blocks in seven part
 * slots and red, green and blue dye decorate what is in the storage slot. Barrels and limited
 * barrels take materials; any storage, a leather armour piece or a backpack takes the two tint
 * colours; a paintbrush takes either as the decoration it then applies (nothing is used up for
 * a paintbrush). Each empty part slot reuses the block of the slot above it unless its arrow is
 * switched off. Leftover parts of a used block or dye are kept for later.
 */
public class DecorationTableTileEntity extends TileEntity {
    public static final int TOP_INNER_TRIM_SLOT = 0;
    public static final int TOP_TRIM_SLOT = 1;
    public static final int SIDE_TRIM_SLOT = 2;
    public static final int BOTTOM_TRIM_SLOT = 3;
    public static final int TOP_CORE_SLOT = 4;
    public static final int SIDE_CORE_SLOT = 5;
    public static final int BOTTOM_CORE_SLOT = 6;
    public static final int RED_DYE_SLOT = 0;
    public static final int GREEN_DYE_SLOT = 1;
    public static final int BLUE_DYE_SLOT = 2;
    /** Backpacks Legacy's colour tags (the original's SBCompat): main colour is the cloth, accent the border. */
    private static final String BACKPACK_CLOTH_COLOR_TAG = "clothColor";
    private static final String BACKPACK_BORDER_COLOR_TAG = "borderColor";

    private final Map<String, Integer> remainingParts = new HashMap<>();
    private final Map<Integer, Boolean> slotMaterialInheritance = new HashMap<>();
    private final Set<String> missingDyes = new HashSet<>();
    private ItemStack result = ItemStack.EMPTY;
    private int mainColor = -1;
    private int accentColor = -1;

    private final ItemStackHandler decorativeBlocks = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) {
            updateResultAndMarkDirty();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return BarrelMaterials.getMaterialName(stack) != null;
        }
    };

    private final ItemStackHandler dyes = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            updateResultAndMarkDirty();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            switch (slot) {
                case RED_DYE_SLOT:
                    return DecorationHelper.hasOreName(stack, DecorationHelper.RED_DYE);
                case GREEN_DYE_SLOT:
                    return DecorationHelper.hasOreName(stack, DecorationHelper.GREEN_DYE);
                case BLUE_DYE_SLOT:
                    return DecorationHelper.hasOreName(stack, DecorationHelper.BLUE_DYE);
                default:
                    return false;
            }
        }
    };

    private final ItemStackHandler storageBlock = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            updateResultAndMarkDirty();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return isDecoratable(stack);
        }
    };

    public static boolean isDecoratable(ItemStack stack) {
        return stack.getItem() instanceof StorageBlockItem || stack.getItem() instanceof PaintbrushItem || isLeatherArmor(stack) || isBackpack(stack);
    }

    private static boolean isLeatherArmor(ItemStack stack) {
        return stack.getItem() instanceof ItemArmor && ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER;
    }

    private static boolean isBackpack(ItemStack stack) {
        return stack.getItem().getRegistryName() != null && "sbackpackslegacy".equals(stack.getItem().getRegistryName().getNamespace())
                && stack.getItem().getRegistryName().getPath().endsWith("backpack");
    }

    private static boolean isBarrel(ItemStack stack) {
        return stack.getItem() instanceof BarrelItem || stack.getItem() instanceof LimitedBarrelItem;
    }

    /** Barrels and Limited Barrel I have no inner trim on their front (the original's STORAGES_WIHOUT_TOP_INNER_TRIM). */
    private static boolean supportsTopInnerTrim(ItemStack stack) {
        if (stack.getItem() instanceof PaintbrushItem) {
            return true;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        return block instanceof LimitedBarrelBlock && ((LimitedBarrelBlock) block).getSlotCount() > 1;
    }

    // ---- Result ----------------------------------------------------------------------------

    public void updateResultAndMarkDirty() {
        updateResult();
        markDirty();
    }

    private void updateResult() {
        missingDyes.clear();
        result = ItemStack.EMPTY;
        ItemStack input = storageBlock.getStackInSlot(0);
        if (input.isEmpty()) {
            return;
        }
        Decoration decoration = decorate(input);
        result = decoration.result;
        missingDyes.addAll(calculateMissingDyes(decoration.requiredDyeParts));
    }

    private static final class Decoration {
        static final Decoration EMPTY = new Decoration(ItemStack.EMPTY, Collections.emptyMap());
        final ItemStack result;
        final Map<String, Integer> requiredDyeParts;

        Decoration(ItemStack result, Map<String, Integer> requiredDyeParts) {
            this.result = result;
            this.requiredDyeParts = requiredDyeParts;
        }
    }

    private Decoration decorate(ItemStack input) {
        boolean paintbrush = input.getItem() instanceof PaintbrushItem;
        boolean supportsMaterials = paintbrush || (isBarrel(input) && StorageItemData.getMainColor(input) == StorageItemData.NO_COLOR
                && StorageItemData.getAccentColor(input) == StorageItemData.NO_COLOR);
        if (supportsMaterials) {
            Map<BarrelMaterial, String> toApply = getMaterialsToApply(supportsTopInnerTrim(input));
            if (!toApply.isEmpty()) {
                return new Decoration(paintbrush ? decoratePaintbrushWithMaterials(input, toApply) : decorateWithMaterials(input, toApply), Collections.emptyMap());
            }
        }
        if (paintbrush) {
            return decoratePaintbrushWithTints(input);
        }
        if (isLeatherArmor(input)) {
            return decorateLeatherArmor(input);
        }
        if (isBackpack(input)) {
            return decorateBackpack(input);
        }
        if (input.getItem() instanceof StorageBlockItem && BarrelMaterials.get(input).isEmpty()) {
            return decorateStorageWithTints(input);
        }
        return Decoration.EMPTY;
    }

    private static ItemStack decorateWithMaterials(ItemStack input, Map<BarrelMaterial, String> toApply) {
        Map<BarrelMaterial, String> materials = new EnumMap<>(BarrelMaterial.class);
        materials.putAll(BarrelMaterials.uncompact(BarrelMaterials.get(input)));
        materials.putAll(toApply);
        Map<BarrelMaterial, String> compacted = BarrelMaterials.compact(materials);
        if (compacted.equals(BarrelMaterials.get(input))) {
            return ItemStack.EMPTY;
        }
        ItemStack result = input.copy();
        result.setCount(1);
        BarrelMaterials.removeCoveredTints(result, compacted);
        BarrelMaterials.set(result, compacted);
        return result;
    }

    private static ItemStack decoratePaintbrushWithMaterials(ItemStack input, Map<BarrelMaterial, String> toApply) {
        Map<BarrelMaterial, String> compacted = BarrelMaterials.compact(toApply);
        if (compacted.equals(PaintbrushItem.getBarrelMaterials(input))) {
            return ItemStack.EMPTY;
        }
        ItemStack result = input.copy();
        result.setCount(1);
        PaintbrushItem.setBarrelMaterials(result, compacted);
        return result;
    }

    private Decoration decoratePaintbrushWithTints(ItemStack input) {
        if ((mainColor == -1 && accentColor == -1) || (mainColor == PaintbrushItem.getMainColor(input) && accentColor == PaintbrushItem.getAccentColor(input))) {
            return Decoration.EMPTY;
        }
        ItemStack result = input.copy();
        result.setCount(1);
        if (mainColor != -1) {
            PaintbrushItem.setMainColor(result, mainColor);
        }
        if (accentColor != -1) {
            PaintbrushItem.setAccentColor(result, accentColor);
        }
        return new Decoration(result, Collections.emptyMap());
    }

    private Decoration decorateStorageWithTints(ItemStack input) {
        int currentMain = StorageItemData.getMainColor(input);
        int currentAccent = StorageItemData.getAccentColor(input);
        if ((mainColor == -1 || mainColor == currentMain) && (accentColor == -1 || accentColor == currentAccent)) {
            return Decoration.EMPTY;
        }
        ItemStack result = input.copy();
        result.setCount(1);
        if (mainColor != -1) {
            StorageItemData.setMainColor(result, mainColor & 0xFFFFFF);
        }
        if (accentColor != -1) {
            StorageItemData.setAccentColor(result, accentColor & 0xFFFFFF);
        }
        return new Decoration(result, DecorationHelper.getDyePartsNeeded(mainColor, accentColor, currentMain, currentAccent));
    }

    private Decoration decorateLeatherArmor(ItemStack input) {
        ItemArmor armor = (ItemArmor) input.getItem();
        int current = armor.hasColor(input) ? armor.getColor(input) : -1;
        if (mainColor == -1 || current == mainColor) {
            return Decoration.EMPTY;
        }
        ItemStack result = input.copy();
        result.setCount(1);
        armor.setColor(result, mainColor & 0xFFFFFF);
        return new Decoration(result, DecorationHelper.getDyePartsNeeded(mainColor, -1, current, -1, 24, 0));
    }

    private Decoration decorateBackpack(ItemStack input) {
        NBTTagCompound tag = input.getTagCompound();
        int currentCloth = tag != null && tag.hasKey(BACKPACK_CLOTH_COLOR_TAG) ? tag.getInteger(BACKPACK_CLOTH_COLOR_TAG) : -1;
        int currentBorder = tag != null && tag.hasKey(BACKPACK_BORDER_COLOR_TAG) ? tag.getInteger(BACKPACK_BORDER_COLOR_TAG) : -1;
        if ((mainColor == -1 || mainColor == currentCloth) && (accentColor == -1 || accentColor == currentBorder)) {
            return Decoration.EMPTY;
        }
        ItemStack result = input.copy();
        result.setCount(1);
        if (!result.hasTagCompound()) {
            result.setTagCompound(new NBTTagCompound());
        }
        if (mainColor != -1) {
            result.getTagCompound().setInteger(BACKPACK_CLOTH_COLOR_TAG, mainColor & 0xFFFFFF);
        }
        if (accentColor != -1) {
            result.getTagCompound().setInteger(BACKPACK_BORDER_COLOR_TAG, accentColor & 0xFFFFFF);
        }
        return new Decoration(result, DecorationHelper.getDyePartsNeeded(mainColor, accentColor, currentCloth, currentBorder, 20, 4));
    }

    private Set<String> calculateMissingDyes(Map<String, Integer> requiredDyeParts) {
        Set<String> missing = new HashSet<>();
        if (!dyes.getStackInSlot(RED_DYE_SLOT).isEmpty() && !dyes.getStackInSlot(GREEN_DYE_SLOT).isEmpty() && !dyes.getStackInSlot(BLUE_DYE_SLOT).isEmpty()) {
            return missing;
        }
        for (String dye : requiredDyeParts.keySet()) {
            int slot = DecorationHelper.RED_DYE.equals(dye) ? RED_DYE_SLOT : DecorationHelper.GREEN_DYE.equals(dye) ? GREEN_DYE_SLOT : BLUE_DYE_SLOT;
            if (dyes.getStackInSlot(slot).isEmpty() && remainingParts.getOrDefault(dye, 0) < requiredDyeParts.get(dye)) {
                missing.add(dye);
            }
        }
        return missing;
    }

    /** The decorated stacks the preview shows: the result, or for a paintbrush what it would paint. */
    public List<ItemStack> getDecoratedPreviewStacks() {
        ItemStack input = storageBlock.getStackInSlot(0);
        if (input.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> previews = new ArrayList<>();
        List<ItemStack> inputs = new ArrayList<>();
        if (input.getItem() instanceof PaintbrushItem) {
            inputs.add(storageStack("limited_barrel_3"));
            if (!hasMaterials()) {
                inputs.add(storageStack("chest"));
                inputs.add(storageStack("shulker_box"));
            }
        } else {
            inputs.add(input);
        }
        for (ItemStack previewInput : inputs) {
            if (previewInput.isEmpty()) {
                continue;
            }
            Decoration decoration = decorate(previewInput);
            if (!decoration.result.isEmpty()) {
                previews.add(decoration.result);
            }
        }
        return previews;
    }

    /** A plain storage of ours, which a paintbrush's preview is shown on. */
    private static ItemStack storageStack(String name) {
        net.minecraft.item.Item item = net.minecraft.item.Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation("sstoragelegacy", name));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ---- Materials from the part slots -----------------------------------------------------

    private Map<BarrelMaterial, String> getMaterialsToApply(boolean supportsInnerTrim) {
        Map<BarrelMaterial, String> materials = new EnumMap<>(BarrelMaterial.class);
        String topInnerTrim = setMaterialFromSlot(TOP_INNER_TRIM_SLOT, null, materials, BarrelMaterial.TOP_INNER_TRIM, supportsInnerTrim);
        String topTrim = setMaterialFromSlot(TOP_TRIM_SLOT, topInnerTrim, materials, BarrelMaterial.TOP_TRIM, true);
        String sideTrim = setMaterialFromSlot(SIDE_TRIM_SLOT, topTrim, materials, BarrelMaterial.SIDE_TRIM, true);
        setMaterialFromSlot(BOTTOM_TRIM_SLOT, sideTrim, materials, BarrelMaterial.BOTTOM_TRIM, true);
        String top = setMaterialFromSlot(TOP_CORE_SLOT, topTrim, materials, BarrelMaterial.TOP, true);
        String side = setMaterialFromSlot(SIDE_CORE_SLOT, top, materials, BarrelMaterial.SIDE, true);
        setMaterialFromSlot(BOTTOM_CORE_SLOT, side, materials, BarrelMaterial.BOTTOM, true);
        return materials;
    }

    @Nullable
    private String setMaterialFromSlot(int slot, @Nullable String inherited, Map<BarrelMaterial, String> materials, BarrelMaterial material, boolean add) {
        String name = BarrelMaterials.getMaterialName(decorativeBlocks.getStackInSlot(slot));
        if (name == null && isSlotMaterialInherited(slot)) {
            name = inherited;
        }
        if (name != null && add) {
            materials.put(material, name);
        }
        return name;
    }

    public boolean hasMaterials() {
        for (int slot = 0; slot < decorativeBlocks.getSlots(); slot++) {
            if (!decorativeBlocks.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean isSlotMaterialInherited(int slot) {
        return slotMaterialInheritance.getOrDefault(slot, true);
    }

    public void setSlotMaterialInheritance(int slot, boolean value) {
        if (value) {
            slotMaterialInheritance.remove(slot);
        } else {
            slotMaterialInheritance.put(slot, false);
        }
        updateResultAndMarkDirty();
    }

    /** The slot an empty part slot takes its block from, or -1. */
    public static int getSlotInheritedFrom(int slot) {
        switch (slot) {
            case TOP_TRIM_SLOT:
                return TOP_INNER_TRIM_SLOT;
            case SIDE_TRIM_SLOT:
            case TOP_CORE_SLOT:
                return TOP_TRIM_SLOT;
            case BOTTOM_TRIM_SLOT:
                return SIDE_TRIM_SLOT;
            case SIDE_CORE_SLOT:
                return TOP_CORE_SLOT;
            case BOTTOM_CORE_SLOT:
                return SIDE_CORE_SLOT;
            default:
                return -1;
        }
    }

    /** The block an empty, inheriting slot shows faded. */
    public ItemStack getInheritedItem(int slot) {
        while (isSlotMaterialInherited(slot)) {
            int parent = getSlotInheritedFrom(slot);
            if (parent == -1) {
                return ItemStack.EMPTY;
            }
            if (!decorativeBlocks.getStackInSlot(parent).isEmpty()) {
                return decorativeBlocks.getStackInSlot(parent);
            }
            slot = parent;
        }
        return ItemStack.EMPTY;
    }

    // ---- Taking the result -----------------------------------------------------------------

    public ItemStack getResult() {
        return result;
    }

    public Set<String> getMissingDyes() {
        return missingDyes;
    }

    /** Uses up the blocks or dyes for the result just taken (not for a paintbrush). */
    public void consumeIngredientsOnCraft() {
        ItemStack input = storageBlock.getStackInSlot(0);
        if (input.isEmpty() || input.getItem() instanceof PaintbrushItem) {
            return;
        }
        List<IItemHandler> dyeHandlers = Collections.singletonList(dyes);
        boolean noBlocks = !hasMaterials();
        boolean supportsMaterials = isBarrel(input) && StorageItemData.getMainColor(input) == StorageItemData.NO_COLOR
                && StorageItemData.getAccentColor(input) == StorageItemData.NO_COLOR;
        if (noBlocks || !supportsMaterials) {
            int[] current = getCurrentColors(input);
            Map<String, Integer> needed = isLeatherArmor(input) ? DecorationHelper.getDyePartsNeeded(mainColor, -1, current[0], -1, 24, 0)
                    : isBackpack(input) ? DecorationHelper.getDyePartsNeeded(mainColor, accentColor, current[0], current[1], 20, 4)
                    : DecorationHelper.getDyePartsNeeded(mainColor, accentColor, current[0], current[1]);
            DecorationHelper.consumeDyePartsNeeded(needed, dyeHandlers, remainingParts, false);
        } else {
            DecorationHelper.consumeMaterials(remainingParts, Collections.singletonList(decorativeBlocks), BarrelMaterials.get(input),
                    getMaterialsToApply(supportsTopInnerTrim(input)), false);
        }
        markDirty();
        notifyClients();
    }

    private static int[] getCurrentColors(ItemStack input) {
        if (isLeatherArmor(input)) {
            ItemArmor armor = (ItemArmor) input.getItem();
            return new int[] {armor.hasColor(input) ? armor.getColor(input) : -1, -1};
        }
        if (isBackpack(input)) {
            NBTTagCompound tag = input.getTagCompound();
            return new int[] {tag != null && tag.hasKey(BACKPACK_CLOTH_COLOR_TAG) ? tag.getInteger(BACKPACK_CLOTH_COLOR_TAG) : -1,
                    tag != null && tag.hasKey(BACKPACK_BORDER_COLOR_TAG) ? tag.getInteger(BACKPACK_BORDER_COLOR_TAG) : -1};
        }
        return new int[] {StorageItemData.getMainColor(input), StorageItemData.getAccentColor(input)};
    }

    /** Parts the result takes, for its tooltip: dye ore names or material names to part counts. */
    public Map<String, Integer> getPartsNeeded() {
        ItemStack input = storageBlock.getStackInSlot(0);
        if (!hasMaterials() || !isBarrel(input)) {
            int[] current = getCurrentColors(input);
            return DecorationHelper.getDyePartsNeeded(mainColor, accentColor, current[0], current[1]);
        }
        return DecorationHelper.getMaterialPartsNeeded(BarrelMaterials.get(input), getMaterialsToApply(supportsTopInnerTrim(input)));
    }

    public Map<String, Integer> getPartsStored() {
        return remainingParts;
    }

    // ---- Colours ---------------------------------------------------------------------------

    public int getMainColor() {
        return mainColor;
    }

    public void setMainColor(int mainColor) {
        this.mainColor = mainColor;
        updateResultAndMarkDirty();
    }

    public int getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
        updateResultAndMarkDirty();
    }

    // ---- Inventories, saving, syncing ------------------------------------------------------

    public ItemStackHandler getDecorativeBlocks() {
        return decorativeBlocks;
    }

    public ItemStackHandler getDyes() {
        return dyes;
    }

    public ItemStackHandler getStorageBlock() {
        return storageBlock;
    }

    public void dropContents() {
        for (ItemStackHandler handler : new ItemStackHandler[] {decorativeBlocks, dyes, storageBlock}) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
    }

    private void notifyClients() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeData(tag);
        return tag;
    }

    private void writeData(NBTTagCompound tag) {
        tag.setTag("decorativeBlocks", decorativeBlocks.serializeNBT());
        tag.setTag("dyes", dyes.serializeNBT());
        tag.setTag("storageBlock", storageBlock.serializeNBT());
        if (!result.isEmpty()) {
            tag.setTag("result", result.writeToNBT(new NBTTagCompound()));
        }
        NBTTagList inheritance = new NBTTagList();
        slotMaterialInheritance.forEach((slot, value) -> {
            NBTTagCompound slotTag = new NBTTagCompound();
            slotTag.setInteger("slot", slot);
            slotTag.setBoolean("value", value);
            inheritance.appendTag(slotTag);
        });
        tag.setTag("slotMaterialInheritance", inheritance);
        NBTTagList parts = new NBTTagList();
        remainingParts.forEach((key, value) -> {
            NBTTagCompound partTag = new NBTTagCompound();
            partTag.setString("key", key);
            partTag.setInteger("value", value);
            parts.appendTag(partTag);
        });
        tag.setTag("remainingParts", parts);
        tag.setInteger("mainColor", mainColor);
        tag.setInteger("accentColor", accentColor);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readData(tag);
    }

    private void readData(NBTTagCompound tag) {
        decorativeBlocks.deserializeNBT(tag.getCompoundTag("decorativeBlocks"));
        dyes.deserializeNBT(tag.getCompoundTag("dyes"));
        storageBlock.deserializeNBT(tag.getCompoundTag("storageBlock"));
        result = tag.hasKey("result") ? new ItemStack(tag.getCompoundTag("result")) : ItemStack.EMPTY;
        slotMaterialInheritance.clear();
        NBTTagList inheritance = tag.getTagList("slotMaterialInheritance", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < inheritance.tagCount(); i++) {
            slotMaterialInheritance.put(inheritance.getCompoundTagAt(i).getInteger("slot"), inheritance.getCompoundTagAt(i).getBoolean("value"));
        }
        remainingParts.clear();
        NBTTagList parts = tag.getTagList("remainingParts", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < parts.tagCount(); i++) {
            remainingParts.put(parts.getCompoundTagAt(i).getString("key"), parts.getCompoundTagAt(i).getInteger("value"));
        }
        mainColor = tag.hasKey("mainColor") ? tag.getInteger("mainColor") : -1;
        accentColor = tag.hasKey("accentColor") ? tag.getInteger("accentColor") : -1;
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeData(tag);
        return new SPacketUpdateTileEntity(pos, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readData(packet.getNbtCompound());
        updateResult();
    }

    /** Dyes as items (for tooltips and the overlay): the plain red, green and blue dyes. */
    public static ItemStack getDyeItem(String dyeOreName) {
        int meta = DecorationHelper.RED_DYE.equals(dyeOreName) ? 1 : DecorationHelper.GREEN_DYE.equals(dyeOreName) ? 2 : 4;
        return new ItemStack(Items.DYE, 1, meta);
    }
}
