/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.upgrades.hopper;

import com.exiledradio.scorelegacy.common.gui.StorageContainerBase;
import com.exiledradio.scorelegacy.common.gui.UpgradeContainerBase;
import com.exiledradio.scorelegacy.common.gui.UpgradeContainerType;
import com.exiledradio.scorelegacy.inventory.UpgradeHandler;
import com.exiledradio.scorelegacy.upgrades.ContentsFilterLogicContainer;
import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.block.VerticalFacing;
import com.exiledradio.sstoragelegacy.common.gui.SideIOContainer;
import com.exiledradio.sstoragelegacy.upgrades.IOMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

/** The hopper's settings tab: input and output filters, and (advanced) what each side does. */
public class HopperUpgradeContainer extends UpgradeContainerBase<HopperUpgradeWrapper, HopperUpgradeContainer> {
    public static final String EMPTY_INPUT_FILTER_SLOT_SPRITE = SophisticatedStorageLegacy.MODID + ":item/empty_input_filter_slot";
    public static final String EMPTY_OUTPUT_FILTER_SLOT_SPRITE = SophisticatedStorageLegacy.MODID + ":item/empty_output_filter_slot";

    private final ContentsFilterLogicContainer inputFilterLogicContainer;
    private final ContentsFilterLogicContainer outputFilterLogicContainer;
    private final SideIOContainer sideIOContainer;
    @Nullable
    private EnumFacing horizontalDirection;
    @Nullable
    private VerticalFacing verticalFacing;

    public HopperUpgradeContainer(EntityPlayer player, int containerId, HopperUpgradeWrapper wrapper, UpgradeHandler upgradeHandler,
                                  UpgradeContainerType<HopperUpgradeWrapper, HopperUpgradeContainer> type) {
        super(player, containerId, wrapper, upgradeHandler, type);
        inputFilterLogicContainer = new ContentsFilterLogicContainer(() -> getUpgradeWrapper().getInputFilterLogic(), this, slot -> addFilterSlot(slot, EMPTY_INPUT_FILTER_SLOT_SPRITE));
        outputFilterLogicContainer = new ContentsFilterLogicContainer(() -> getUpgradeWrapper().getOutputFilterLogic(), this, slot -> addFilterSlot(slot, EMPTY_OUTPUT_FILTER_SLOT_SPRITE));
        sideIOContainer = new SideIOContainer(this, this::getHorizontalDirection, this::getVerticalFacing, this::getDirectionIOMode, this::setDirectionIOMode, false);
    }

    private void addFilterSlot(Slot slot, String sprite) {
        slot.setBackgroundName(sprite);
        slots.add(slot);
    }

    private EnumFacing getHorizontalDirection() {
        if (horizontalDirection == null) {
            initBlockRotation();
        }
        return horizontalDirection;
    }

    private VerticalFacing getVerticalFacing() {
        if (verticalFacing == null) {
            initBlockRotation();
        }
        return verticalFacing;
    }

    /** Read when first needed: while this is built the storage container isn't the player's open one yet. */
    private void initBlockRotation() {
        horizontalDirection = EnumFacing.NORTH;
        verticalFacing = VerticalFacing.NO;
        if (player.openContainer instanceof StorageContainerBase) {
            IBlockState state = player.world.getBlockState(((StorageContainerBase) player.openContainer).getStorageBlockPos());
            if (state.getBlock() instanceof StorageBlockBase) {
                StorageBlockBase block = (StorageBlockBase) state.getBlock();
                horizontalDirection = block.getHorizontalDirection(state);
                verticalFacing = block.getVerticalFacing(state);
            }
        }
    }

    private void setDirectionIOMode(EnumFacing direction, IOMode ioMode) {
        HopperUpgradeWrapper wrapper = getUpgradeWrapper();
        wrapper.setPullingFrom(direction, ioMode == IOMode.PULL || ioMode == IOMode.PUSH_PULL);
        wrapper.setPushingTo(direction, ioMode == IOMode.PUSH || ioMode == IOMode.PUSH_PULL);
    }

    private IOMode getDirectionIOMode(EnumFacing direction) {
        boolean pulling = getUpgradeWrapper().isPullingFrom(direction);
        boolean pushing = getUpgradeWrapper().isPushingTo(direction);
        if (pulling && pushing) {
            return IOMode.PUSH_PULL;
        }
        return pulling ? IOMode.PULL : pushing ? IOMode.PUSH : IOMode.OFF;
    }

    @Override
    public void handleMessage(NBTTagCompound data) {
        if (!inputFilterLogicContainer.handleMessage(data) && !outputFilterLogicContainer.handleMessage(data)) {
            sideIOContainer.handleMessage(data);
        }
    }

    public ContentsFilterLogicContainer getInputFilterLogicContainer() {
        return inputFilterLogicContainer;
    }

    public ContentsFilterLogicContainer getOutputFilterLogicContainer() {
        return outputFilterLogicContainer;
    }

    public SideIOContainer getSideIOContainer() {
        return sideIOContainer;
    }
}
