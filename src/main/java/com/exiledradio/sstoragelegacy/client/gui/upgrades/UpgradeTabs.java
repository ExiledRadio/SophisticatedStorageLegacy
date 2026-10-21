/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.gui.upgrades;

import com.exiledradio.scorelegacy.client.gui.StorageScreenBase;
import com.exiledradio.scorelegacy.client.gui.UpgradeGuiManager;
import com.exiledradio.scorelegacy.client.gui.upgrades.CookingUpgradeTabs;
import com.exiledradio.scorelegacy.client.gui.upgrades.CoreUpgradeTabs;
import com.exiledradio.scorelegacy.client.gui.upgrades.CraftingUpgradeTab;
import com.exiledradio.scorelegacy.client.gui.upgrades.JukeboxUpgradeTab;
import com.exiledradio.scorelegacy.client.gui.upgrades.PumpUpgradeTab;
import com.exiledradio.scorelegacy.client.gui.upgrades.XpPumpUpgradeTab;
import com.exiledradio.scorelegacy.client.gui.utils.Position;
import com.exiledradio.scorelegacy.upgrades.ContentsFilteredUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.compacting.CompactingUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.feeding.FeedingUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.filter.FilterUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.magnet.MagnetUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.pickup.PickupUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.pump.PumpUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.voiding.VoidUpgradeContainer;
import com.exiledradio.sstoragelegacy.ModConfig;
import com.exiledradio.sstoragelegacy.init.ModUpgrades;
import com.exiledradio.sstoragelegacy.upgrades.hopper.HopperUpgradeContainer;

/** The settings tab of every Storage upgrade, registered against its container type. */
public final class UpgradeTabs {

    private UpgradeTabs() {}

    public static void register() {
        ModConfig.Upgrades c = ModConfig.upgrades;
        UpgradeGuiManager.registerTab(ModUpgrades.PICKUP_BASIC, (ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.ContentsTab<>(container, p, s, "pickup", false, c.pickup.slotsInRow));
        UpgradeGuiManager.registerTab(ModUpgrades.PICKUP_ADVANCED, (ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.ContentsTab<>(container, p, s, "advanced_pickup", true, c.advancedPickup.slotsInRow));
        UpgradeGuiManager.registerTab(FilterUpgradeContainer.BASIC_TYPE, (FilterUpgradeContainer container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.FilterTab(container, p, s, "filter", false, c.filter.slotsInRow));
        UpgradeGuiManager.registerTab(FilterUpgradeContainer.ADVANCED_TYPE, (FilterUpgradeContainer container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.FilterTab(container, p, s, "advanced_filter", true, c.advancedFilter.slotsInRow));
        UpgradeGuiManager.registerTab(ModUpgrades.MAGNET_BASIC, (MagnetUpgradeContainer container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.MagnetTab(container, p, s, "magnet", false, c.magnet.slotsInRow));
        UpgradeGuiManager.registerTab(ModUpgrades.MAGNET_ADVANCED, (MagnetUpgradeContainer container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.MagnetTab(container, p, s, "advanced_magnet", true, c.advancedMagnet.slotsInRow));
        UpgradeGuiManager.registerTab(ModUpgrades.VOID_BASIC, (VoidUpgradeContainer container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.VoidTab(container, p, s, "void", false, c.voiding.slotsInRow));
        UpgradeGuiManager.registerTab(ModUpgrades.VOID_ADVANCED, (VoidUpgradeContainer container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.VoidTab(container, p, s, "advanced_void", true, c.advancedVoiding.slotsInRow));
        UpgradeGuiManager.registerTab(ModUpgrades.FEEDING_BASIC, (FeedingUpgradeContainer container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.FeedingTab(container, p, s, "feeding", false, c.feeding.slotsInRow));
        UpgradeGuiManager.registerTab(ModUpgrades.FEEDING_ADVANCED, (FeedingUpgradeContainer container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.FeedingTab(container, p, s, "advanced_feeding", true, c.advancedFeeding.slotsInRow));
        UpgradeGuiManager.registerTab(ModUpgrades.COMPACTING_BASIC, (CompactingUpgradeContainer container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.CompactingTab(container, p, s, "compacting", false, c.compacting.slotsInRow));
        UpgradeGuiManager.registerTab(ModUpgrades.COMPACTING_ADVANCED, (CompactingUpgradeContainer container, Position p, StorageScreenBase s) ->
                new CoreUpgradeTabs.CompactingTab(container, p, s, "advanced_compacting", true, c.advancedCompacting.slotsInRow));
        UpgradeGuiManager.registerTab(ModUpgrades.CRAFTING, CraftingUpgradeTab::new);
        UpgradeGuiManager.registerTab(ModUpgrades.SMELTING, CookingUpgradeTabs.SmeltingTab::new);
        UpgradeGuiManager.registerTab(ModUpgrades.AUTO_SMELTING, CookingUpgradeTabs.AutoSmeltingTab::new);
        UpgradeGuiManager.registerTab(ModUpgrades.JUKEBOX, JukeboxUpgradeTab::new);
        UpgradeGuiManager.registerTab(ModUpgrades.PUMP_BASIC, (PumpUpgradeContainer container, Position p, StorageScreenBase s) -> new PumpUpgradeTab(container, p, s, false));
        UpgradeGuiManager.registerTab(ModUpgrades.PUMP_ADVANCED, (PumpUpgradeContainer container, Position p, StorageScreenBase s) -> new PumpUpgradeTab(container, p, s, true));
        UpgradeGuiManager.registerTab(ModUpgrades.XP_PUMP, XpPumpUpgradeTab::new);
        UpgradeGuiManager.registerTab(ModUpgrades.HOPPER_BASIC, (HopperUpgradeContainer container, Position p, StorageScreenBase s) -> new HopperUpgradeTab(container, p, s, false));
        UpgradeGuiManager.registerTab(ModUpgrades.HOPPER_ADVANCED, (HopperUpgradeContainer container, Position p, StorageScreenBase s) -> new HopperUpgradeTab(container, p, s, true));
    }
}
