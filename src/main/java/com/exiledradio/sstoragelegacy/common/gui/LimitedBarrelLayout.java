/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.common.gui;

/** Where a limited barrel's 1 to 4 slots sit in its GUIs (the original's LimitedBarrelScreen.updateSlotPositions). */
public final class LimitedBarrelLayout {
    /** Height of the slots area; the storage GUIs are 176 wide. */
    public static final int STORAGE_SLOTS_HEIGHT = 82;
    private static final int HALF_WIDTH = 88;
    private static final int MIDDLE_OF_STORAGE_SLOTS = 18 + STORAGE_SLOTS_HEIGHT / 2;

    private LimitedBarrelLayout() {}

    public static int[] getSlotPosition(int slot, int slotCount) {
        int top = MIDDLE_OF_STORAGE_SLOTS - 29;
        int bottom = MIDDLE_OF_STORAGE_SLOTS + 11;
        int left = HALF_WIDTH - 29;
        int right = HALF_WIDTH + 11;
        int center = HALF_WIDTH - 9;
        switch (slotCount) {
            case 1:
                return new int[] {center, MIDDLE_OF_STORAGE_SLOTS - 9};
            case 2:
                return new int[] {center, slot == 0 ? top : bottom};
            case 3:
                return slot == 0 ? new int[] {center, top} : new int[] {slot == 1 ? left : right, bottom};
            default:
                return new int[] {slot % 2 == 0 ? left : right, slot < 2 ? top : bottom};
        }
    }
}
