/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client;

import com.exiledradio.scorelegacy.client.gui.ContentsTooltipPanel;
import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.item.ShulkerBoxItem;
import com.exiledradio.sstoragelegacy.item.StorageBlockItem;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hold Shift over a shulker box or a packed storage to see what is in it (the original's
 * StorageContentsTooltip). The contents travel in the item's own NBT, so unlike backpacks
 * nothing has to be asked of the server.
 */
@Mod.EventBusSubscriber(modid = SophisticatedStorageLegacy.MODID, value = Side.CLIENT)
public final class StorageTooltipRenderer {
    public static final String TOOLTIP = "item." + SophisticatedStorageLegacy.MODID + ".storage.tooltip.";
    private static final String UPGRADE_INVENTORY_TAG = "upgradeInventory";
    private static final String INVENTORY_TAG = "inventory";
    private static final String REAL_COUNT_TAG = "realCount";

    private StorageTooltipRenderer() {}

    /** Shulker boxes always (as the original); wooden storages only when packed. */
    public static boolean showsContents(ItemStack stack) {
        if (!(stack.getItem() instanceof StorageBlockItem)) {
            return false;
        }
        return stack.getItem() instanceof ShulkerBoxItem
                || (stack.getTagCompound() != null && stack.getTagCompound().getBoolean(StorageItemData.PACKED_TAG));
    }

    /** The "Press <Left Shift> to View Contents" line. */
    public static String getShiftHint() {
        return TextFormatting.GRAY + I18n.format(TOOLTIP + "press_for_contents", TextFormatting.AQUA + I18n.format(TOOLTIP + "shift") + TextFormatting.GRAY);
    }

    @SubscribeEvent
    public static void onTooltipDrawn(RenderTooltipEvent.PostText event) {
        ItemStack stack = event.getStack();
        if (!showsContents(stack) || !GuiScreen.isShiftKeyDown()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        NBTTagCompound contents = tag == null ? new NBTTagCompound() : tag.getCompoundTag(StorageItemData.CONTENTS_TAG);
        ContentsTooltipPanel.draw(Minecraft.getMinecraft(), event.getFontRenderer(), event.getX(), event.getY(), event.getHeight(), summarize(contents), TOOLTIP);
    }

    /** Upgrades as they are; the inventory merged by kind (item, metadata, NBT), most first. */
    private static NBTTagCompound summarize(NBTTagCompound contents) {
        NBTTagCompound summary = new NBTTagCompound();
        NBTTagList upgrades = new NBTTagList();
        NBTTagList upgradeItems = contents.getCompoundTag(UPGRADE_INVENTORY_TAG).getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < upgradeItems.tagCount(); i++) {
            ItemStack upgrade = new ItemStack(upgradeItems.getCompoundTagAt(i));
            if (!upgrade.isEmpty()) {
                upgrades.appendTag(upgrade.writeToNBT(new NBTTagCompound()));
            }
        }
        summary.setTag("upgrades", upgrades);

        Map<String, ItemStack> merged = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        NBTTagList inventoryItems = contents.getCompoundTag(INVENTORY_TAG).getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < inventoryItems.tagCount(); i++) {
            NBTTagCompound entry = inventoryItems.getCompoundTagAt(i);
            ItemStack item = new ItemStack(entry);
            if (item.isEmpty()) {
                continue;
            }
            int count = entry.hasKey(REAL_COUNT_TAG) ? entry.getInteger(REAL_COUNT_TAG) : item.getCount();
            item.setCount(1);
            String key = item.getItem().getRegistryName() + "@" + item.getMetadata() + (item.getTagCompound() == null ? "" : item.getTagCompound().toString());
            merged.putIfAbsent(key, item);
            counts.merge(key, count, Integer::sum);
        }
        List<String> keys = new ArrayList<>(merged.keySet());
        keys.sort((a, b) -> Integer.compare(counts.get(b), counts.get(a)));
        NBTTagList items = new NBTTagList();
        for (String key : keys) {
            NBTTagCompound entry = merged.get(key).writeToNBT(new NBTTagCompound());
            entry.setInteger(REAL_COUNT_TAG, counts.get(key));
            items.appendTag(entry);
        }
        summary.setTag("items", items);
        return summary;
    }
}
