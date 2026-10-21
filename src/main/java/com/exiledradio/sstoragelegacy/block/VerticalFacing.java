/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

/** Whether a limited barrel's front faces up, down or (NO) its horizontal facing. */
public enum VerticalFacing implements IStringSerializable {
    NO("no", EnumFacing.NORTH),
    UP("up", EnumFacing.UP),
    DOWN("down", EnumFacing.DOWN);

    private final String name;
    private final EnumFacing direction;

    VerticalFacing(String name, EnumFacing direction) {
        this.name = name;
        this.direction = direction;
    }

    public EnumFacing getDirection() {
        return direction;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    public static VerticalFacing fromDirection(EnumFacing direction) {
        if (direction.getAxis().isHorizontal()) {
            return NO;
        }
        return direction == EnumFacing.UP ? UP : DOWN;
    }
}
