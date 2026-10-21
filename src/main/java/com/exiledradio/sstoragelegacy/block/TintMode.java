package com.exiledradio.sstoragelegacy.block;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

/**
 * Which tint overlays a storage block renders. Used as an actual-state property so the
 * multipart blockstate can pick parts: the wood base unless BOTH, the accent overlay for
 * ACCENT and BOTH, the main overlay for MAIN and BOTH.
 */
public enum TintMode implements IStringSerializable {
    NONE("none"),
    MAIN("main"),
    ACCENT("accent"),
    BOTH("both");

    private final String name;

    TintMode(String name) {
        this.name = name;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    public static TintMode of(boolean hasMain, boolean hasAccent) {
        if (hasMain) {
            return hasAccent ? BOTH : MAIN;
        }
        return hasAccent ? ACCENT : NONE;
    }
}
