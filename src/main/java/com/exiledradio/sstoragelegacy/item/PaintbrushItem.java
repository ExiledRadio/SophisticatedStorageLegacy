/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.BarrelMaterial;
import com.exiledradio.sstoragelegacy.block.BarrelTileEntity;
import com.exiledradio.sstoragelegacy.block.ControllerTileEntity;
import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.block.StorageTileEntity;
import com.exiledradio.sstoragelegacy.util.DecorationHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies a decoration to placed storages (the original 1.20's PaintbrushItem): the barrel
 * materials or the two tint colours stored on it at the decoration table. Right-click a storage,
 * or a controller for every connected storage. The blocks and dyes it needs come from the
 * player's inventory, including the inventories of items carried (backpacks), with leftover parts
 * kept on the brush.
 */
public class PaintbrushItem extends Item {
    private static final String BARREL_MATERIALS_TAG = "barrelMaterials";
    private static final String MAIN_COLOR_TAG = "mainColor";
    private static final String ACCENT_COLOR_TAG = "accentColor";
    private static final String REMAINING_PARTS_TAG = "remainingParts";

    public PaintbrushItem(CreativeTabs tab) {
        setRegistryName(SophisticatedStorageLegacy.MODID, "paintbrush");
        setTranslationKey(SophisticatedStorageLegacy.MODID + ".paintbrush");
        setMaxStackSize(1);
        setCreativeTab(tab);
    }

    // ---- What it carries -------------------------------------------------------------------

    public static void setBarrelMaterials(ItemStack paintbrush, Map<BarrelMaterial, String> materials) {
        NBTTagCompound tag = getOrCreateTag(paintbrush);
        NBTTagCompound materialsTag = new NBTTagCompound();
        materials.forEach((material, block) -> materialsTag.setString(material.getName(), block));
        tag.setTag(BARREL_MATERIALS_TAG, materialsTag);
        tag.removeTag(MAIN_COLOR_TAG);
        tag.removeTag(ACCENT_COLOR_TAG);
    }

    public static Map<BarrelMaterial, String> getBarrelMaterials(ItemStack paintbrush) {
        NBTTagCompound tag = paintbrush.getTagCompound();
        if (tag == null || !tag.hasKey(BARREL_MATERIALS_TAG)) {
            return Collections.emptyMap();
        }
        Map<BarrelMaterial, String> materials = new EnumMap<>(BarrelMaterial.class);
        NBTTagCompound materialsTag = tag.getCompoundTag(BARREL_MATERIALS_TAG);
        for (String key : materialsTag.getKeySet()) {
            BarrelMaterial material = BarrelMaterial.fromName(key);
            if (material != null) {
                materials.put(material, materialsTag.getString(key));
            }
        }
        return materials;
    }

    /** -1 when it carries no main colour. */
    public static int getMainColor(ItemStack paintbrush) {
        NBTTagCompound tag = paintbrush.getTagCompound();
        return tag != null && tag.hasKey(MAIN_COLOR_TAG) ? tag.getInteger(MAIN_COLOR_TAG) : -1;
    }

    public static int getAccentColor(ItemStack paintbrush) {
        NBTTagCompound tag = paintbrush.getTagCompound();
        return tag != null && tag.hasKey(ACCENT_COLOR_TAG) ? tag.getInteger(ACCENT_COLOR_TAG) : -1;
    }

    public static void setMainColor(ItemStack paintbrush, int color) {
        getOrCreateTag(paintbrush).setInteger(MAIN_COLOR_TAG, color);
        getOrCreateTag(paintbrush).removeTag(BARREL_MATERIALS_TAG);
    }

    public static void setAccentColor(ItemStack paintbrush, int color) {
        getOrCreateTag(paintbrush).setInteger(ACCENT_COLOR_TAG, color);
        getOrCreateTag(paintbrush).removeTag(BARREL_MATERIALS_TAG);
    }

    public static void resetMainColor(ItemStack paintbrush) {
        getOrCreateTag(paintbrush).removeTag(MAIN_COLOR_TAG);
    }

    public static void resetAccentColor(ItemStack paintbrush) {
        getOrCreateTag(paintbrush).removeTag(ACCENT_COLOR_TAG);
    }

    private static boolean hasDecoration(ItemStack paintbrush) {
        NBTTagCompound tag = paintbrush.getTagCompound();
        return tag != null && (tag.hasKey(BARREL_MATERIALS_TAG) || tag.hasKey(MAIN_COLOR_TAG) || tag.hasKey(ACCENT_COLOR_TAG));
    }

    public static Map<String, Integer> getRemainingParts(ItemStack paintbrush) {
        Map<String, Integer> parts = new HashMap<>();
        NBTTagCompound tag = paintbrush.getTagCompound();
        if (tag != null) {
            NBTTagCompound partsTag = tag.getCompoundTag(REMAINING_PARTS_TAG);
            for (String key : partsTag.getKeySet()) {
                parts.put(key, partsTag.getInteger(key));
            }
        }
        return parts;
    }

    public static void setRemainingParts(ItemStack paintbrush, Map<String, Integer> parts) {
        NBTTagCompound partsTag = new NBTTagCompound();
        parts.forEach(partsTag::setInteger);
        getOrCreateTag(paintbrush).setTag(REMAINING_PARTS_TAG, partsTag);
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    // ---- Using it --------------------------------------------------------------------------

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        ItemStack paintbrush = player.getHeldItem(hand);
        if (!hasDecoration(paintbrush)) {
            return EnumActionResult.PASS;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof StorageTileEntity) {
            if (!world.isRemote) {
                paintStorage(player, paintbrush, (StorageTileEntity) te, 1.0F);
            }
            return EnumActionResult.SUCCESS;
        }
        if (te instanceof ControllerTileEntity) {
            if (!world.isRemote) {
                for (StorageTileEntity storage : ((ControllerTileEntity) te).getConnectedStorageTiles()) {
                    paintStorage(player, paintbrush, storage, 0.6F);
                }
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    private static void paintStorage(EntityPlayer player, ItemStack paintbrush, StorageTileEntity storage, float soundVolume) {
        StorageTileEntity main = storage.getMainTile();
        IBlockState state = main.getWorld().getBlockState(main.getPos());
        EnumFacing front = state.getBlock() instanceof StorageBlockBase ? ((StorageBlockBase) state.getBlock()).getFacing(state) : EnumFacing.UP;
        boolean painted = getBarrelMaterials(paintbrush).isEmpty() ? setColors(player, paintbrush, main)
                : main instanceof BarrelTileEntity && applyMaterials(player, paintbrush, (BarrelTileEntity) main);
        if (painted) {
            playSoundAndParticles(main.getWorld(), main.getPos(), front, soundVolume, state);
        }
    }

    /** Sets the tints if the dyes are there; a barrel's materials go with it. */
    private static boolean setColors(EntityPlayer player, ItemStack paintbrush, StorageTileEntity storage) {
        int main = getMainColor(paintbrush) == -1 ? -1 : getMainColor(paintbrush) & 0xFFFFFF;
        int accent = getAccentColor(paintbrush) == -1 ? -1 : getAccentColor(paintbrush) & 0xFFFFFF;
        int originalMain = storage.getMainColor();
        int originalAccent = storage.getAccentColor();
        if (originalMain == main && originalAccent == accent) {
            return false;
        }
        Map<String, Integer> remainingParts = getRemainingParts(paintbrush);
        List<IItemHandler> handlers = getItemHandlers(player);
        if (!DecorationHelper.consumeDyes(main, accent, remainingParts, handlers, originalMain, originalAccent, true)) {
            return false;
        }
        DecorationHelper.consumeDyes(main, accent, remainingParts, handlers, originalMain, originalAccent, false);
        setRemainingParts(paintbrush, remainingParts);
        storage.setColors(main, accent);
        if (storage instanceof BarrelTileEntity) {
            ((BarrelTileEntity) storage).setMaterials(Collections.emptyMap());
        }
        return true;
    }

    /** Applies the materials if the blocks are there; the tints go. */
    private static boolean applyMaterials(EntityPlayer player, ItemStack paintbrush, BarrelTileEntity barrel) {
        Map<BarrelMaterial, String> original = barrel.getMaterials();
        Map<BarrelMaterial, String> toApply = getBarrelMaterials(paintbrush);
        if (original.equals(toApply)) {
            return false;
        }
        Map<String, Integer> remainingParts = getRemainingParts(paintbrush);
        List<IItemHandler> handlers = getItemHandlers(player);
        if (!DecorationHelper.consumeMaterials(remainingParts, handlers, original, toApply, true)) {
            return false;
        }
        DecorationHelper.consumeMaterials(remainingParts, handlers, original, toApply, false);
        setRemainingParts(paintbrush, remainingParts);
        barrel.setColors(-1, -1);
        barrel.setMaterials(BarrelMaterials.compact(BarrelMaterials.uncompact(toApply)));
        return true;
    }

    private static void playSoundAndParticles(World world, BlockPos pos, EnumFacing front, float volume, IBlockState state) {
        world.playSound(null, pos, state.getBlock().getSoundType(state, world, pos, null).getPlaceSound(), SoundCategory.BLOCKS, volume, 1.0F);
        if (world instanceof WorldServer) {
            double x = pos.getX() + 0.5D + front.getXOffset() * 0.6D;
            double y = pos.getY() + 0.5D + front.getYOffset() * 0.6D;
            double z = pos.getZ() + 0.5D + front.getZOffset() * 0.6D;
            boolean vertical = front.getAxis() == EnumFacing.Axis.Y;
            double dx = vertical ? 0.4D : 0.1D + front.getZOffset() * 0.3D;
            double dy = vertical ? 0.1D : 0.4D;
            double dz = vertical ? 0.4D : 0.1D + front.getXOffset() * 0.3D;
            ((WorldServer) world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x, y, z, 4, dx, dy, dz, 1.0D);
        }
    }

    /** The player's inventory, then every carried item that has an inventory (the original's ...IncludingContainers). */
    public static List<IItemHandler> getItemHandlers(EntityPlayer player) {
        List<IItemHandler> handlers = new ArrayList<>();
        handlers.add(new PlayerMainInvWrapper(player.inventory));
        for (ItemStack stack : player.inventory.mainInventory) {
            if (!stack.isEmpty() && !(stack.getItem() instanceof PaintbrushItem) && stack.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                IItemHandler handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                if (handler != null) {
                    handlers.add(handler);
                }
            }
        }
        return handlers;
    }

    // ---- What it would take (the overlay) --------------------------------------------------

    /** Items the decoration takes at a storage or controller, split into those present and missing; null if nothing to do. */
    @Nullable
    public static ItemRequirements getItemRequirements(ItemStack paintbrush, EntityPlayer player, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        List<StorageTileEntity> storages = new ArrayList<>();
        if (te instanceof StorageTileEntity) {
            storages.add(((StorageTileEntity) te).getMainTile());
        } else if (te instanceof ControllerTileEntity) {
            storages.addAll(((ControllerTileEntity) te).getConnectedStorageTiles());
        } else {
            return null;
        }
        Map<String, Integer> partsNeeded = new HashMap<>();
        Map<BarrelMaterial, String> materials = getBarrelMaterials(paintbrush);
        boolean dyes = materials.isEmpty();
        for (StorageTileEntity storage : storages) {
            Map<String, Integer> storageParts;
            if (dyes) {
                int main = getMainColor(paintbrush) == -1 ? -1 : getMainColor(paintbrush) & 0xFFFFFF;
                int accent = getAccentColor(paintbrush) == -1 ? -1 : getAccentColor(paintbrush) & 0xFFFFFF;
                storageParts = DecorationHelper.getDyePartsNeeded(main, accent, storage.getMainColor(), storage.getAccentColor());
            } else if (storage instanceof BarrelTileEntity) {
                storageParts = DecorationHelper.getMaterialPartsNeeded(((BarrelTileEntity) storage).getMaterials(), materials);
            } else {
                continue;
            }
            storageParts.forEach((key, count) -> partsNeeded.merge(key, count, Integer::sum));
        }
        if (partsNeeded.isEmpty()) {
            return null;
        }
        Map<String, Integer> remainingParts = getRemainingParts(paintbrush);
        List<IItemHandler> handlers = getItemHandlers(player);
        DecorationHelper.ConsumptionResult result = dyes ? DecorationHelper.consumeDyePartsNeeded(partsNeeded, handlers, remainingParts, true)
                : DecorationHelper.consumeMaterialPartsNeeded(partsNeeded, remainingParts, handlers, true);
        List<ItemStack> present = new ArrayList<>();
        List<ItemStack> missing = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : partsNeeded.entrySet()) {
            int count = ceilDiv(entry.getValue() - remainingParts.getOrDefault(entry.getKey(), 0), DecorationHelper.BLOCK_TOTAL_PARTS);
            int missingCount = ceilDiv(result.getMissingParts().getOrDefault(entry.getKey(), 0), DecorationHelper.BLOCK_TOTAL_PARTS);
            ItemStack item = dyes ? getDyeStack(entry.getKey()) : BarrelMaterials.getStack(entry.getKey());
            if (item.isEmpty()) {
                continue;
            }
            if (missingCount > 0) {
                missing.add(copyWithCount(item, missingCount));
            }
            if (count - missingCount > 0) {
                present.add(copyWithCount(item, count - missingCount));
            }
        }
        return new ItemRequirements(present, missing);
    }

    private static ItemStack getDyeStack(String dyeOreName) {
        EnumDyeColor color = DecorationHelper.RED_DYE.equals(dyeOreName) ? EnumDyeColor.RED
                : DecorationHelper.GREEN_DYE.equals(dyeOreName) ? EnumDyeColor.GREEN : DecorationHelper.BLUE_DYE.equals(dyeOreName) ? EnumDyeColor.BLUE : null;
        return color == null ? ItemStack.EMPTY : new ItemStack(Items.DYE, 1, color.getDyeDamage());
    }

    private static ItemStack copyWithCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private static int ceilDiv(int x, int y) {
        return (int) Math.ceil((double) x / y);
    }

    /** The items a paint job takes that the player has, and those they lack. */
    public static final class ItemRequirements {
        private final List<ItemStack> itemsPresent;
        private final List<ItemStack> itemsMissing;

        ItemRequirements(List<ItemStack> itemsPresent, List<ItemStack> itemsMissing) {
            this.itemsPresent = itemsPresent;
            this.itemsMissing = itemsMissing;
        }

        public List<ItemStack> getItemsPresent() {
            return itemsPresent;
        }

        public List<ItemStack> getItemsMissing() {
            return itemsMissing;
        }
    }

    // ---- Tooltip ---------------------------------------------------------------------------

    // Item.addInformation is itself client-only, so FML strips this override on a dedicated server.
    @SideOnly(Side.CLIENT)
    @Override
    @SuppressWarnings("deprecation")
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        String prefix = "item.sstoragelegacy.paintbrush.tooltip";
        for (String line : I18n.translateToLocal(prefix).split("\\\\n")) {
            tooltip.add(TextFormatting.DARK_GRAY + line);
        }
        Map<BarrelMaterial, String> materials = getBarrelMaterials(stack);
        if (!materials.isEmpty()) {
            tooltip.add(TextFormatting.GRAY + I18n.translateToLocal(prefix + ".materials"));
            materials.forEach((material, name) -> {
                ItemStack block = BarrelMaterials.getStack(name);
                if (!block.isEmpty()) {
                    tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted(prefix + ".material",
                            I18n.translateToLocal("gui.sstoragelegacy.barrel_part." + material.getName()), TextFormatting.DARK_AQUA + block.getDisplayName()));
                }
            });
        }
        if (getMainColor(stack) != -1) {
            tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted(prefix + ".main_color", hex(getMainColor(stack))));
        }
        if (getAccentColor(stack) != -1) {
            tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted(prefix + ".accent_color", hex(getAccentColor(stack))));
        }
    }

    private static String hex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
}
