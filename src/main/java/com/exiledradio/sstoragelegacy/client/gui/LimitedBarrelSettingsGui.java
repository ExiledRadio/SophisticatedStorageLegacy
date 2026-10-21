/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.gui;

import com.exiledradio.scorelegacy.client.gui.settings.SettingsScreenBase;
import com.exiledradio.sstoragelegacy.common.gui.LimitedBarrelSettingsContainer;

/** A limited barrel's settings screen: the slots framed as in its storage screen. */
public class LimitedBarrelSettingsGui extends SettingsScreenBase {
    private final LimitedBarrelSettingsContainer container;

    public LimitedBarrelSettingsGui(LimitedBarrelSettingsContainer container) {
        super(container);
        this.container = container;
    }

    @Override
    protected void drawStorageSlotsBackground() {
        LimitedBarrelGui.drawSlotsBackground(guiLeft, guiTop, xSize, container.getNumberOfSlots());
    }
}
