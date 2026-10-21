/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.block;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The parts of a barrel that can take another block's look (the original's BarrelMaterial). The
 * leaves are the real parts: each face's core and its one-pixel trim, plus the rings on a limited
 * barrel's front. The others are groups, used to save a map compactly when parts share a block.
 */
public enum BarrelMaterial {
    SIDE("side", Part.CORE),
    SIDE_TRIM("side_trim", Part.TRIM),
    BOTTOM("bottom", Part.CORE),
    BOTTOM_TRIM("bottom_trim", Part.TRIM),
    TOP("top", Part.CORE),
    TOP_TRIM("top_trim", Part.TRIM),
    TOP_INNER_TRIM("top_inner_trim", Part.TRIM),
    ALL("all", Part.BOTH, SIDE, SIDE_TRIM, BOTTOM, BOTTOM_TRIM, TOP, TOP_TRIM, TOP_INNER_TRIM),
    ALL_TRIM("all_trim", Part.TRIM, SIDE_TRIM, BOTTOM_TRIM, TOP_TRIM, TOP_INNER_TRIM),
    ALL_BUT_TRIM("all_but_trim", Part.CORE, SIDE, BOTTOM, TOP),
    TOP_ALL("top_all", Part.BOTH, TOP, TOP_TRIM, TOP_INNER_TRIM),
    SIDE_ALL("side_all", Part.BOTH, SIDE, SIDE_TRIM),
    BOTTOM_ALL("bottom_all", Part.BOTH, BOTTOM, BOTTOM_TRIM);

    /** Which half of the barrel model a material covers. */
    public enum Part {
        BOTH,
        TRIM,
        CORE
    }

    private static final Map<String, BarrelMaterial> BY_NAME = new HashMap<>();

    static {
        for (BarrelMaterial material : values()) {
            BY_NAME.put(material.name, material);
        }
    }

    private final String name;
    private final Part part;
    private final BarrelMaterial[] children;

    BarrelMaterial(String name, Part part, BarrelMaterial... children) {
        this.name = name;
        this.part = part;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public Part getPart() {
        return part;
    }

    /** The leaves a group stands for; a leaf stands for itself. */
    public List<BarrelMaterial> getChildren() {
        return children.length > 0 ? Arrays.asList(children) : Collections.singletonList(this);
    }

    public boolean isLeaf() {
        return children.length == 0;
    }

    /** By saved name; unknown names give null. */
    public static BarrelMaterial fromName(String name) {
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    /** Where an unset leaf takes its block from, first match wins (the original's getFillFromDefaults). */
    public static List<BarrelMaterial> getFillFromDefaults(BarrelMaterial material) {
        switch (material) {
            case SIDE:
                return Arrays.asList(BOTTOM, TOP, TOP_INNER_TRIM);
            case SIDE_TRIM:
                return Arrays.asList(BOTTOM_TRIM, TOP_TRIM);
            case BOTTOM:
                return Arrays.asList(SIDE, TOP, TOP_INNER_TRIM);
            case BOTTOM_TRIM:
                return Arrays.asList(SIDE_TRIM, TOP_TRIM);
            case TOP:
                return Arrays.asList(TOP_INNER_TRIM, SIDE, BOTTOM);
            case TOP_TRIM:
                return Arrays.asList(SIDE_TRIM, BOTTOM_TRIM);
            case TOP_INNER_TRIM:
                return Arrays.asList(TOP_TRIM, SIDE_TRIM, BOTTOM_TRIM);
            default:
                return Collections.emptyList();
        }
    }
}
