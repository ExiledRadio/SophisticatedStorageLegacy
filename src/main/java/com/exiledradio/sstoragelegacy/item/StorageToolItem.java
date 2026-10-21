/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.scorelegacy.controller.ILinkable;
import com.exiledradio.sstoragelegacy.block.ControllerTileEntity;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelTileEntity;
import com.exiledradio.sstoragelegacy.block.StorageTileEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * The storage tool (the original's StorageToolItem): sneak + scroll picks a mode, right-clicking
 * a storage applies it. Held in the off hand while placing a storage, the lock mode locks it.
 * Link mode: right-click a controller to remember it, then a storage or storage link to link it
 * (with no controller remembered: unlink it); sneak + right-click the air forgets the controller.
 */
public class StorageToolItem extends Item {
    private static final String CONTROLLER_POS_TAG = "controllerPos";
    private static final String MODE_TAG = "mode";

    public StorageToolItem(CreativeTabs tab) {
        setRegistryName(SophisticatedStorageLegacy.MODID, "storage_tool");
        setTranslationKey(SophisticatedStorageLegacy.MODID + ".storage_tool");
        setMaxStackSize(1);
        setCreativeTab(tab);
    }

    @SideOnly(Side.CLIENT)
    @Override
    @SuppressWarnings("deprecation")
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        tooltip.add(TextFormatting.DARK_GRAY + I18n.translateToLocal("item.sstoragelegacy.storage_tool.tooltip"));
        tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted("item.sstoragelegacy.storage_tool.tooltip.controls",
                TextFormatting.AQUA + I18n.translateToLocal("item.sstoragelegacy.storage_tool.tooltip.controls.combination") + TextFormatting.GRAY));
    }

    /** The tool in the off hand when a storage is placed: lock mode locks it. */
    public static void useOffHandOnPlaced(ItemStack tool, StorageTileEntity tile) {
        if (tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        if (getMode(tool) == Mode.LOCK) {
            tile.toggleLock();
        }
    }

    @Nonnull
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        ItemStack tool = player.getHeldItem(hand);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof ControllerTileEntity) {
            return useOnController(world, tool, pos, (ControllerTileEntity) te);
        }
        switch (getMode(tool)) {
            case LINK:
                if (!(te instanceof ILinkable)) {
                    return EnumActionResult.PASS;
                }
                if (!world.isRemote) {
                    BlockPos controller = getControllerLink(tool);
                    if (controller != null) {
                        ((ILinkable) te).linkToController(controller);
                    } else {
                        ((ILinkable) te).unlinkFromController();
                    }
                }
                return EnumActionResult.SUCCESS;
            case LOCK:
                return toggle(world, te, StorageTileEntity.class, StorageTileEntity::toggleLock);
            case COUNT_DISPLAY:
                return toggle(world, te, LimitedBarrelTileEntity.class, LimitedBarrelTileEntity::toggleCountVisibility);
            case LOCK_DISPLAY:
                return toggle(world, te, StorageTileEntity.class, StorageTileEntity::toggleLockVisibility);
            case TIER_DISPLAY:
                return toggle(world, te, StorageTileEntity.class, StorageTileEntity::toggleTierVisibility);
            case UPGRADES_DISPLAY:
                return toggle(world, te, StorageTileEntity.class, StorageTileEntity::toggleUpgradesVisibility);
            case FILL_LEVEL_DISPLAY:
                return toggle(world, te, LimitedBarrelTileEntity.class, LimitedBarrelTileEntity::toggleFillLevelVisibility);
            default:
                return EnumActionResult.PASS;
        }
    }

    /** On a controller: Link mode remembers it; the other modes apply to every connected storage. */
    private static EnumActionResult useOnController(World world, ItemStack tool, BlockPos pos, ControllerTileEntity controller) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        switch (getMode(tool)) {
            case LINK:
                tool.setTagInfo(CONTROLLER_POS_TAG, new net.minecraft.nbt.NBTTagLong(pos.toLong()));
                break;
            case LOCK:
                controller.toggleLock();
                break;
            case COUNT_DISPLAY:
                controller.toggleCountVisibility();
                break;
            case LOCK_DISPLAY:
                controller.toggleLockVisibility();
                break;
            case TIER_DISPLAY:
                controller.toggleTierVisibility();
                break;
            case UPGRADES_DISPLAY:
                controller.toggleUpgradesVisibility();
                break;
            default:
                controller.toggleFillLevelVisibility();
                break;
        }
        return EnumActionResult.SUCCESS;
    }

    private static <T> EnumActionResult toggle(World world, @Nullable TileEntity te, Class<T> type, Consumer<T> toggle) {
        if (!type.isInstance(te)) {
            return EnumActionResult.PASS;
        }
        if (!world.isRemote) {
            toggle.accept(type.cast(te));
        }
        return EnumActionResult.SUCCESS;
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack tool = player.getHeldItem(hand);
        if (player.isSneaking() && tool.getTagCompound() != null && tool.getTagCompound().hasKey(CONTROLLER_POS_TAG)) {
            tool.getTagCompound().removeTag(CONTROLLER_POS_TAG);
            return new ActionResult<>(EnumActionResult.SUCCESS, tool);
        }
        return super.onItemRightClick(world, player, hand);
    }

    @Nullable
    public static BlockPos getControllerLink(ItemStack tool) {
        NBTTagCompound tag = tool.getTagCompound();
        return tag != null && tag.hasKey(CONTROLLER_POS_TAG) ? BlockPos.fromLong(tag.getLong(CONTROLLER_POS_TAG)) : null;
    }

    /** The line shown above the hotbar while the tool is held. */
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("deprecation")
    public static String getOverlayMessage(ItemStack tool) {
        String prefix = "item.sstoragelegacy.storage_tool.overlay.";
        Mode mode = getMode(tool);
        if (mode == Mode.LINK) {
            BlockPos controller = getControllerLink(tool);
            return controller == null ? I18n.translateToLocal(prefix + "unlinking")
                    : I18n.translateToLocalFormatted(prefix + "linking", controller.getX(), controller.getY(), controller.getZ());
        }
        return I18n.translateToLocal(prefix + mode.getOverlayKey());
    }

    public static Mode getMode(ItemStack tool) {
        NBTTagCompound tag = tool.getTagCompound();
        if (tag != null && tag.hasKey(MODE_TAG)) {
            try {
                return Mode.valueOf(tag.getString(MODE_TAG));
            } catch (IllegalArgumentException ignored) {
                // An unknown mode: back to the default.
            }
        }
        return Mode.LINK;
    }

    public static void cycleMode(ItemStack tool, boolean next) {
        Mode mode = getMode(tool);
        tool.setTagInfo(MODE_TAG, new net.minecraft.nbt.NBTTagString((next ? mode.next() : mode.previous()).name()));
    }

    public enum Mode {
        LINK("linking"),
        LOCK("toggling_lock"),
        COUNT_DISPLAY("toggling_count_display"),
        LOCK_DISPLAY("toggling_lock_display"),
        TIER_DISPLAY("toggling_tier_display"),
        UPGRADES_DISPLAY("toggling_upgrades_display"),
        FILL_LEVEL_DISPLAY("toggling_fill_level_display");

        private final String overlayKey;

        Mode(String overlayKey) {
            this.overlayKey = overlayKey;
        }

        String getOverlayKey() {
            return overlayKey;
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Mode previous() {
            return values()[Math.floorMod(ordinal() - 1, values().length)];
        }
    }
}
