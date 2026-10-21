/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.item;

import com.exiledradio.sstoragelegacy.block.BarrelMaterial;
import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Barrel materials in NBT (the original BarrelBlockItem's materials helpers): a "materials"
 * compound of part name to block. A 1.12 block is its registry name, plus "@meta" when the look
 * depends on metadata (granite is "minecraft:stone@1"); names without "@" read as the original's.
 */
public final class BarrelMaterials {
    public static final String MATERIALS_TAG = "materials";

    private BarrelMaterials() {}

    /** The saved (compact) materials of a stack or tile tag; empty if none. */
    public static Map<BarrelMaterial, String> read(@Nullable NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(MATERIALS_TAG)) {
            return Collections.emptyMap();
        }
        NBTTagCompound materialsTag = tag.getCompoundTag(MATERIALS_TAG);
        Map<BarrelMaterial, String> materials = new EnumMap<>(BarrelMaterial.class);
        for (String key : materialsTag.getKeySet()) {
            BarrelMaterial material = BarrelMaterial.fromName(key);
            if (material != null) {
                materials.put(material, materialsTag.getString(key));
            }
        }
        return materials;
    }

    public static void write(NBTTagCompound tag, Map<BarrelMaterial, String> materials) {
        if (materials.isEmpty()) {
            tag.removeTag(MATERIALS_TAG);
            return;
        }
        NBTTagCompound materialsTag = new NBTTagCompound();
        materials.forEach((material, block) -> materialsTag.setString(material.getName(), block));
        tag.setTag(MATERIALS_TAG, materialsTag);
    }

    public static Map<BarrelMaterial, String> get(ItemStack stack) {
        return read(stack.getTagCompound());
    }

    public static void set(ItemStack stack, Map<BarrelMaterial, String> materials) {
        if (materials.isEmpty()) {
            remove(stack);
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        write(stack.getTagCompound(), materials);
    }

    public static void remove(ItemStack stack) {
        if (stack.getTagCompound() != null) {
            stack.getTagCompound().removeTag(MATERIALS_TAG);
            if (stack.getTagCompound().isEmpty()) {
                stack.setTagCompound(null);
            }
        }
    }

    /** Groups replaced by the leaves they stand for. */
    public static Map<BarrelMaterial, String> uncompact(Map<BarrelMaterial, String> materials) {
        Map<BarrelMaterial, String> leaves = new EnumMap<>(BarrelMaterial.class);
        materials.forEach((material, block) -> {
            for (BarrelMaterial child : material.getChildren()) {
                leaves.put(child, block);
            }
        });
        return leaves;
    }

    /** Leaves that all share a block replaced by their group, largest groups first (the enum order). */
    public static Map<BarrelMaterial, String> compact(Map<BarrelMaterial, String> leaves) {
        Map<BarrelMaterial, String> materials = new EnumMap<>(BarrelMaterial.class);
        materials.putAll(leaves);
        for (BarrelMaterial material : BarrelMaterial.values()) {
            if (material.isLeaf()) {
                continue;
            }
            String shared = null;
            boolean same = true;
            for (BarrelMaterial child : material.getChildren()) {
                String block = materials.get(child);
                if (block == null || (shared != null && !shared.equals(block))) {
                    same = false;
                    break;
                }
                shared = block;
            }
            if (same && shared != null) {
                for (BarrelMaterial child : material.getChildren()) {
                    materials.remove(child);
                }
                materials.put(material, shared);
            }
        }
        return materials;
    }

    /** Unset leaves take their block from related ones (the original's fillEmptyMaterialsWithDefaults). */
    public static void fillEmptyFromDefaults(Map<BarrelMaterial, String> leaves) {
        for (BarrelMaterial material : BarrelMaterial.values()) {
            if (material.isLeaf() && !leaves.containsKey(material)) {
                for (BarrelMaterial source : BarrelMaterial.getFillFromDefaults(material)) {
                    if (leaves.containsKey(source)) {
                        leaves.put(material, leaves.get(source));
                        break;
                    }
                }
            }
        }
    }

    /** Tints a material covers go (the original's removeCoveredTints): main under all cores, accent under all trims. */
    public static void removeCoveredTints(ItemStack stack, Map<BarrelMaterial, String> materials) {
        if (StorageItemData.getMainColor(stack) != StorageItemData.NO_COLOR
                && (materials.containsKey(BarrelMaterial.ALL) || materials.containsKey(BarrelMaterial.ALL_BUT_TRIM))) {
            StorageItemData.setMainColor(stack, StorageItemData.NO_COLOR);
        }
        if (StorageItemData.getAccentColor(stack) != StorageItemData.NO_COLOR
                && (materials.containsKey(BarrelMaterial.ALL) || materials.containsKey(BarrelMaterial.ALL_TRIM))) {
            StorageItemData.setAccentColor(stack, StorageItemData.NO_COLOR);
        }
    }

    /**
     * The material name of a block item, or null if it can't be one: a plain full cube drawn by
     * its model, and not a storage (the original accepts full-block block items).
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static String getMaterialName(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemBlock)) {
            return null;
        }
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        if (block instanceof StorageBlockBase || block.getRegistryName() == null) {
            return null;
        }
        int meta = stack.getItem().getMetadata(stack.getMetadata());
        IBlockState state = block.getStateFromMeta(meta);
        if (!isFullBlockShape(state) || state.getRenderType() != EnumBlockRenderType.MODEL || block.hasTileEntity(state)) {
            return null;
        }
        return meta == 0 ? block.getRegistryName().toString() : block.getRegistryName() + "@" + meta;
    }

    /**
     * Whether the block fills its whole space (the original's isShapeFullBlock): glass counts, though
     * 1.12 says it isn't a "full cube". Bounding boxes rarely need the world; if one does, fall back.
     */
    @SuppressWarnings("deprecation")
    private static boolean isFullBlockShape(IBlockState state) {
        try {
            return Block.FULL_BLOCK_AABB.equals(state.getBoundingBox(null, net.minecraft.util.math.BlockPos.ORIGIN));
        } catch (RuntimeException e) {
            return state.isFullCube();
        }
    }

    /** The block state a material name stands for, or null if that block is gone. */
    @Nullable
    @SuppressWarnings("deprecation")
    public static IBlockState getState(String materialName) {
        int at = materialName.lastIndexOf('@');
        String name = at < 0 ? materialName : materialName.substring(0, at);
        Block block = Block.REGISTRY.containsKey(new ResourceLocation(name)) ? Block.REGISTRY.getObject(new ResourceLocation(name)) : null;
        if (block == null) {
            return null;
        }
        int meta = 0;
        if (at >= 0) {
            try {
                meta = Integer.parseInt(materialName.substring(at + 1));
            } catch (NumberFormatException e) {
                meta = 0;
            }
        }
        return block.getStateFromMeta(meta);
    }

    /** The item for a material name (for the decoration table and paintbrush costs). */
    public static ItemStack getStack(String materialName) {
        IBlockState state = getState(materialName);
        if (state == null) {
            return ItemStack.EMPTY;
        }
        int at = materialName.lastIndexOf('@');
        int meta = at < 0 ? 0 : state.getBlock().getMetaFromState(state);
        return new ItemStack(state.getBlock(), 1, meta);
    }
}
