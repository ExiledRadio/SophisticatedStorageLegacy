/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.upgrades;

/** What a side of a storage does for an upgrade that moves items through it. */
public enum IOMode {
    PUSH("push"),
    PULL("pull"),
    PUSH_PULL("push_pull"),
    OFF("off"),
    DISABLED("disabled");

    private final String name;

    IOMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public IOMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static IOMode fromName(String name) {
        for (IOMode mode : values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return OFF;
    }
}
