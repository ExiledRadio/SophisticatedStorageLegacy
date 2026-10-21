/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.common.gui;

import com.exiledradio.sstoragelegacy.block.VerticalFacing;
import net.minecraft.util.EnumFacing;

/** A side of a storage relative to its front, turned into a world direction (the original's BlockSide). */
public enum BlockSide {
    TOP("top"),
    BOTTOM("bottom"),
    FRONT("front"),
    BACK("back"),
    LEFT("left"),
    RIGHT("right");

    private final String name;

    BlockSide(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static BlockSide fromName(String name) {
        for (BlockSide side : values()) {
            if (side.name.equals(name)) {
                return side;
            }
        }
        return FRONT;
    }

    /** 1.18's getClockWise / getCounterClockWise are 1.12's rotateY / rotateYCCW. */
    public EnumFacing toDirection(EnumFacing horizontal, VerticalFacing vertical) {
        switch (this) {
            case TOP:
                if (vertical == VerticalFacing.NO) {
                    return EnumFacing.UP;
                }
                return vertical == VerticalFacing.UP ? horizontal.getOpposite() : horizontal;
            case BOTTOM:
                if (vertical == VerticalFacing.NO) {
                    return EnumFacing.DOWN;
                }
                return vertical == VerticalFacing.UP ? horizontal : horizontal.getOpposite();
            case FRONT:
                return vertical == VerticalFacing.NO ? horizontal : vertical.getDirection();
            case BACK:
                return vertical == VerticalFacing.NO ? horizontal.getOpposite() : vertical.getDirection().getOpposite();
            case LEFT:
                return horizontal.rotateY();
            default:
                return horizontal.rotateYCCW();
        }
    }
}
