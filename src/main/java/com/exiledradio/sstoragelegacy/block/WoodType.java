/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import com.exiledradio.scorelegacy.compat.MaterialCompat;

import net.minecraft.block.BlockPlanks;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The wood looks a wooden storage can have. Crimson and warped don't exist in vanilla 1.12: they
 * are listed and craftable only when a Nether backport adds them (see {@link MaterialCompat}).
 * Names match the original's lower-cased {@code woodType} NBT values, so stack NBT reads the same.
 */
public enum WoodType implements IStringSerializable {
    OAK("oak", BlockPlanks.EnumType.OAK, null),
    SPRUCE("spruce", BlockPlanks.EnumType.SPRUCE, null),
    BIRCH("birch", BlockPlanks.EnumType.BIRCH, null),
    JUNGLE("jungle", BlockPlanks.EnumType.JUNGLE, null),
    ACACIA("acacia", BlockPlanks.EnumType.ACACIA, null),
    DARK_OAK("dark_oak", BlockPlanks.EnumType.DARK_OAK, null),
    CRIMSON("crimson", null, MaterialCompat.Material.CRIMSON),
    WARPED("warped", null, MaterialCompat.Material.WARPED);

    /** What a storage without any saved wood type (and without both colours) looks like, as in the original. */
    public static final WoodType DEFAULT = ACACIA;

    private final String name;
    @Nullable
    private final BlockPlanks.EnumType planks;
    @Nullable
    private final MaterialCompat.Material material;

    WoodType(String name, @Nullable BlockPlanks.EnumType planks, @Nullable MaterialCompat.Material material) {
        this.name = name;
        this.planks = planks;
        this.material = material;
    }

    /** Whether storages in this wood are listed and craftable (crimson and warped: when a mod adds them). */
    public boolean isAvailable() {
        return material == null || material.isAvailable();
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    /** Vanilla's planks of this wood; null for crimson and warped. */
    @Nullable
    public BlockPlanks.EnumType getPlanks() {
        return planks;
    }

    public String getTranslationKey() {
        return "wood_name.sstoragelegacy." + name;
    }

    @Nullable
    public static WoodType fromName(String name) {
        for (WoodType type : values()) {
            // The original writes WoodType.name(), which is lower case for vanilla woods; accept either.
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
