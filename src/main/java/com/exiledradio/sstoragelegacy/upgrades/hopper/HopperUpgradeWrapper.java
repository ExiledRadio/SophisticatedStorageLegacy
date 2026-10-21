/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.upgrades.hopper;

import com.exiledradio.scorelegacy.api.IStorageWrapper;
import com.exiledradio.scorelegacy.api.ITickableUpgrade;
import com.exiledradio.scorelegacy.upgrades.ContentsFilterLogic;
import com.exiledradio.scorelegacy.upgrades.FilterLogic;
import com.exiledradio.scorelegacy.upgrades.UpgradeWrapperBase;
import com.exiledradio.scorelegacy.util.InventoryHelper;
import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.common.gui.BlockSide;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Moves items between a placed storage and the blocks next to it (the original's
 * HopperUpgradeWrapper): every few ticks one push to a push side and one pull from a pull side,
 * through the input and output filters. The basic hopper always pulls from the storage's top
 * and pushes to its bottom; the advanced one starts that way and can be set per side.
 *
 * <p>1.12 vanilla inventories already expose an item handler capability, so the original's
 * WorldlyContainer path is not needed, and neighbours are looked up on each transfer instead of
 * cached (there is no LazyOptional to invalidate the cache).
 */
public class HopperUpgradeWrapper extends UpgradeWrapperBase<HopperUpgradeWrapper, HopperUpgradeItem> implements ITickableUpgrade {
    private static final String PULL_DIRECTIONS_TAG = "pullDirections";
    private static final String PUSH_DIRECTIONS_TAG = "pushDirections";

    private final Set<EnumFacing> pullDirections = EnumSet.noneOf(EnumFacing.class);
    private final Set<EnumFacing> pushDirections = EnumSet.noneOf(EnumFacing.class);
    private boolean directionsInitialized = false;
    private final ContentsFilterLogic inputFilterLogic;
    private final ContentsFilterLogic outputFilterLogic;
    private long coolDownTime = 0;

    protected HopperUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
        inputFilterLogic = new ContentsFilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getInputFilterSlotCount(), storageWrapper::getInventoryHandler, "inputFilter");
        outputFilterLogic = new ContentsFilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getOutputFilterSlotCount(), storageWrapper::getInventoryHandler, "outputFilter");
        deserialize();
    }

    @Override
    public void tick(@Nullable EntityLivingBase entity, World world, BlockPos pos) {
        if (entity != null) {
            // Only placed storages have sides.
            return;
        }
        initDirections(world, pos);
        if (coolDownTime > world.getTotalWorldTime()) {
            return;
        }
        push:
        for (EnumFacing direction : pushDirections) {
            for (IItemHandler neighbour : getItemHandlers(world, pos, direction)) {
                if (moveItems(storageWrapper.getInventoryForUpgradeProcessing(), neighbour, outputFilterLogic)) {
                    break push;
                }
            }
        }
        pull:
        for (EnumFacing direction : pullDirections) {
            for (IItemHandler neighbour : getItemHandlers(world, pos, direction)) {
                if (moveItems(neighbour, storageWrapper.getInventoryForUpgradeProcessing(), inputFilterLogic)) {
                    break pull;
                }
            }
        }
        coolDownTime = world.getTotalWorldTime() + upgradeItem.getTransferSpeedTicks();
    }

    /**
     * The basic hopper follows the storage's top and bottom every time it loads (the storage may
     * have been turned); the advanced one only until its sides were saved once.
     */
    private void initDirections(World world, BlockPos pos) {
        NBTTagCompound tag = upgrade.getTagCompound();
        boolean saved = tag != null && (tag.hasKey(PULL_DIRECTIONS_TAG) || tag.hasKey(PUSH_DIRECTIONS_TAG));
        if (directionsInitialized || (upgradeItem.isAdvanced() && saved)) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof StorageBlockBase) {
            StorageBlockBase block = (StorageBlockBase) state.getBlock();
            EnumFacing horizontal = block.getHorizontalDirection(state);
            pullDirections.clear();
            pushDirections.clear();
            setPushingTo(BlockSide.BOTTOM.toDirection(horizontal, block.getVerticalFacing(state)), true);
            setPullingFrom(BlockSide.TOP.toDirection(horizontal, block.getVerticalFacing(state)), true);
            directionsInitialized = true;
        }
    }

    /** The inventories on a side; a double chest has two blocks there (one beyond its other half on that side). */
    private static java.util.List<IItemHandler> getItemHandlers(World world, BlockPos pos, EnumFacing direction) {
        IBlockState state = world.getBlockState(pos);
        java.util.List<BlockPos> positions = state.getBlock() instanceof StorageBlockBase
                ? ((StorageBlockBase) state.getBlock()).getNeighborPositions(state, pos, direction)
                : java.util.Collections.singletonList(pos.offset(direction));
        java.util.List<IItemHandler> handlers = new java.util.ArrayList<>();
        for (BlockPos neighbourPos : positions) {
            IItemHandler handler = getItemHandler(world, neighbourPos, direction);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    @Nullable
    private static IItemHandler getItemHandler(World world, BlockPos neighbourPos, EnumFacing direction) {
        if (!world.isBlockLoaded(neighbourPos)) {
            return null;
        }
        TileEntity te = world.getTileEntity(neighbourPos);
        if (te == null || !te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite())) {
            return null;
        }
        return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite());
    }

    private boolean moveItems(IItemHandler fromHandler, IItemHandler toHandler, FilterLogic filterLogic) {
        for (int slot = 0; slot < fromHandler.getSlots(); slot++) {
            ItemStack slotStack = fromHandler.getStackInSlot(slot);
            if (slotStack.isEmpty() || !filterLogic.matchesFilter(slotStack)) {
                continue;
            }
            ItemStack extracted = fromHandler.extractItem(slot, upgradeItem.getMaxTransferStackSize(), true);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack remainder = InventoryHelper.insertIntoInventory(extracted, toHandler, true);
            if (remainder.getCount() < extracted.getCount()) {
                InventoryHelper.insertIntoInventory(fromHandler.extractItem(slot, extracted.getCount() - remainder.getCount(), false), toHandler, false);
                return true;
            }
        }
        return false;
    }

    public ContentsFilterLogic getInputFilterLogic() {
        return inputFilterLogic;
    }

    public ContentsFilterLogic getOutputFilterLogic() {
        return outputFilterLogic;
    }

    public boolean isPullingFrom(EnumFacing direction) {
        return pullDirections.contains(direction);
    }

    public boolean isPushingTo(EnumFacing direction) {
        return pushDirections.contains(direction);
    }

    public void setPullingFrom(EnumFacing direction, boolean shouldPull) {
        if (shouldPull) {
            pullDirections.add(direction);
        } else {
            pullDirections.remove(direction);
        }
        serializeDirections(PULL_DIRECTIONS_TAG, pullDirections);
    }

    public void setPushingTo(EnumFacing direction, boolean shouldPush) {
        if (shouldPush) {
            pushDirections.add(direction);
        } else {
            pushDirections.remove(direction);
        }
        serializeDirections(PUSH_DIRECTIONS_TAG, pushDirections);
    }

    private void serializeDirections(String key, Set<EnumFacing> directions) {
        NBTTagList list = new NBTTagList();
        for (EnumFacing direction : directions) {
            list.appendTag(new NBTTagString(direction.getName()));
        }
        upgrade.setTagInfo(key, list);
        save();
    }

    private void deserialize() {
        pullDirections.clear();
        pushDirections.clear();
        NBTTagCompound tag = upgrade.getTagCompound();
        if (tag == null) {
            return;
        }
        readDirections(tag.getTagList(PULL_DIRECTIONS_TAG, Constants.NBT.TAG_STRING), pullDirections);
        readDirections(tag.getTagList(PUSH_DIRECTIONS_TAG, Constants.NBT.TAG_STRING), pushDirections);
    }

    private static void readDirections(NBTTagList list, Set<EnumFacing> directions) {
        for (int i = 0; i < list.tagCount(); i++) {
            EnumFacing direction = EnumFacing.byName(list.getStringTagAt(i));
            if (direction != null) {
                directions.add(direction);
            }
        }
    }
}
