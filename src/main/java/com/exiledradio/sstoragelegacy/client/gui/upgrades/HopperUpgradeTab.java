/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.gui.upgrades;

import com.exiledradio.scorelegacy.client.gui.StorageScreenBase;
import com.exiledradio.scorelegacy.client.gui.UpgradeSettingsTab;
import com.exiledradio.scorelegacy.client.gui.filter.ContentsFilterControl;
import com.exiledradio.scorelegacy.client.gui.utils.Position;
import com.exiledradio.sstoragelegacy.ModConfig;
import com.exiledradio.sstoragelegacy.client.gui.SideIOControl;
import com.exiledradio.sstoragelegacy.upgrades.hopper.HopperUpgradeContainer;

/**
 * The hopper's tab (the original's HopperUpgradeTab): input filter on top, output filter below;
 * the advanced one has the side buttons between them.
 */
public class HopperUpgradeTab extends UpgradeSettingsTab<HopperUpgradeContainer> {
    private final ContentsFilterControl inputFilterLogicControl;
    private final ContentsFilterControl outputFilterLogicControl;

    public HopperUpgradeTab(HopperUpgradeContainer container, Position position, StorageScreenBase screen, boolean advanced) {
        super(container, position, screen, advanced ? "advanced_hopper" : "hopper");
        if (advanced) {
            int inRow = ModConfig.upgrades.advancedHopper.inputFilterSlotsInRow;
            inputFilterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(screen, new Position(x + 3, y + 24),
                    container.getInputFilterLogicContainer(), inRow));
            SideIOControl sideIOControl = addHideableChild(new SideIOControl(container.getSideIOContainer(),
                    new Position(x + 3 + 9, inputFilterLogicControl.getY() + inputFilterLogicControl.getHeight() + 4)));
            outputFilterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(screen,
                    new Position(x + 3, sideIOControl.getY() + sideIOControl.getHeight() + 4), container.getOutputFilterLogicContainer(),
                    ModConfig.upgrades.advancedHopper.outputFilterSlotsInRow));
        } else {
            inputFilterLogicControl = addHideableChild(new ContentsFilterControl.Basic(screen, new Position(x + 3, y + 24),
                    container.getInputFilterLogicContainer(), ModConfig.upgrades.hopper.inputFilterSlotsInRow));
            outputFilterLogicControl = addHideableChild(new ContentsFilterControl.Basic(screen,
                    new Position(x + 3, inputFilterLogicControl.getY() + inputFilterLogicControl.getHeight() + 4),
                    container.getOutputFilterLogicContainer(), ModConfig.upgrades.hopper.outputFilterSlotsInRow));
        }
    }

    @Override
    protected void moveSlotsToTab() {
        inputFilterLogicControl.moveSlotsToView();
        outputFilterLogicControl.moveSlotsToView();
    }
}
