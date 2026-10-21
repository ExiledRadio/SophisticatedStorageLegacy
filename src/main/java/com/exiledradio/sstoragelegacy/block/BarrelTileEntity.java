/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** A barrel: shows up to 4 front items, can have a flat top, and its lid is a block state. */
public class BarrelTileEntity extends StorageTileEntity {
    private boolean flatTop = false;
    /** Blocks whose look parts of the barrel take (compact, see BarrelMaterials); empty for the wood look. */
    private java.util.Map<BarrelMaterial, String> materials = java.util.Collections.emptyMap();

    public boolean isFlatTop() {
        return flatTop;
    }

    public java.util.Map<BarrelMaterial, String> getMaterials() {
        return materials;
    }

    public void setMaterials(java.util.Map<BarrelMaterial, String> materials) {
        this.materials = materials.isEmpty() ? java.util.Collections.emptyMap() : new java.util.EnumMap<>(materials);
        markDirty();
        notifyClients();
    }

    @Override
    public int getDisplayItemLimit() {
        return MAX_DISPLAY_ITEMS;
    }

    @Override
    protected void onOpenStateChanged(boolean open) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BarrelBlock) || state.getValue(BarrelBlock.OPEN) == open) {
            return;
        }
        // 1.12 has no barrel sounds; the chest's, from the barrel's lid, at the original's volume/pitch.
        playSound(open ? SoundEvents.BLOCK_CHEST_OPEN : SoundEvents.BLOCK_CHEST_CLOSE, state.getValue(BarrelBlock.FACING), 0.5F);
        world.setBlockState(pos, state.withProperty(BarrelBlock.OPEN, open), 3);
    }

    @Override
    protected void writeExtraSyncData(NBTTagCompound tag) {
        tag.setBoolean(StorageItemData.FLAT_TOP_TAG, flatTop);
        com.exiledradio.sstoragelegacy.item.BarrelMaterials.write(tag, materials);
    }

    @Override
    protected void readExtraSyncData(NBTTagCompound tag) {
        flatTop = tag.getBoolean(StorageItemData.FLAT_TOP_TAG);
        java.util.Map<BarrelMaterial, String> read = com.exiledradio.sstoragelegacy.item.BarrelMaterials.read(tag);
        materials = read.isEmpty() ? java.util.Collections.emptyMap() : read;
    }

    @Override
    protected void readExtraFromStack(ItemStack stack) {
        flatTop = StorageItemData.isFlatTop(stack);
        java.util.Map<BarrelMaterial, String> read = com.exiledradio.sstoragelegacy.item.BarrelMaterials.get(stack);
        materials = read.isEmpty() ? java.util.Collections.emptyMap() : read;
    }

    @Override
    protected void writeExtraToStack(ItemStack stack) {
        StorageItemData.setFlatTop(stack, flatTop);
        com.exiledradio.sstoragelegacy.item.BarrelMaterials.set(stack, materials);
    }
}
