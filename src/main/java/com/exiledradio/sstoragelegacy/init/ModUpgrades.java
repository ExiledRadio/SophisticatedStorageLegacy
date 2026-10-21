/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.init;

import com.exiledradio.scorelegacy.common.gui.UpgradeContainerRegistry;
import com.exiledradio.scorelegacy.common.gui.UpgradeContainerType;
import com.exiledradio.scorelegacy.upgrades.ContentsFilteredUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.compacting.CompactingUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.compacting.CompactingUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.compacting.CompactingUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.cooking.AutoSmeltingUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.cooking.AutoSmeltingUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.cooking.AutoSmeltingUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.cooking.SmeltingUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.cooking.SmeltingUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.cooking.SmeltingUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.crafting.CraftingUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.crafting.CraftingUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.crafting.CraftingUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.feeding.FeedingUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.feeding.FeedingUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.feeding.FeedingUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.filter.FilterUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.filter.FilterUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.jukebox.JukeboxUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.jukebox.JukeboxUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.jukebox.JukeboxUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.magnet.MagnetUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.magnet.MagnetUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.magnet.MagnetUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.pickup.PickupUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.pickup.PickupUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.pump.PumpUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.pump.PumpUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.pump.PumpUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.stack.StackUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.voiding.VoidUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.voiding.VoidUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.voiding.VoidUpgradeWrapper;
import com.exiledradio.scorelegacy.upgrades.xppump.XpPumpUpgradeContainer;
import com.exiledradio.scorelegacy.upgrades.xppump.XpPumpUpgradeItem;
import com.exiledradio.scorelegacy.upgrades.xppump.XpPumpUpgradeWrapper;
import com.exiledradio.sstoragelegacy.ModConfig;
import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.item.PackingTapeItem;
import com.exiledradio.sstoragelegacy.upgrades.compression.CompressionUpgradeItem;
import com.exiledradio.sstoragelegacy.upgrades.hopper.HopperUpgradeContainer;
import com.exiledradio.sstoragelegacy.upgrades.hopper.HopperUpgradeItem;
import com.exiledradio.sstoragelegacy.upgrades.hopper.HopperUpgradeWrapper;
import com.exiledradio.sstoragelegacy.item.StorageToolItem;
import com.exiledradio.sstoragelegacy.item.StorageTierUpgradeItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Storage's own upgrade items (the original registers its own copies of Core's upgrades), and
 * which settings-tab container each one gets. Smoking, blasting and stonecutter upgrades are
 * dropped (no such blocks in 1.12).
 */
public final class ModUpgrades {
    public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> PICKUP_BASIC = new UpgradeContainerType<>(ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> PICKUP_ADVANCED = new UpgradeContainerType<>(ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> MAGNET_BASIC = new UpgradeContainerType<>(MagnetUpgradeContainer::new);
    public static final UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> MAGNET_ADVANCED = new UpgradeContainerType<>(MagnetUpgradeContainer::new);
    public static final UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> FEEDING_BASIC = new UpgradeContainerType<>(FeedingUpgradeContainer::new);
    public static final UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> FEEDING_ADVANCED = new UpgradeContainerType<>(FeedingUpgradeContainer::new);
    public static final UpgradeContainerType<CompactingUpgradeWrapper, CompactingUpgradeContainer> COMPACTING_BASIC = new UpgradeContainerType<>(CompactingUpgradeContainer::new);
    public static final UpgradeContainerType<CompactingUpgradeWrapper, CompactingUpgradeContainer> COMPACTING_ADVANCED = new UpgradeContainerType<>(CompactingUpgradeContainer::new);
    public static final UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> VOID_BASIC = new UpgradeContainerType<>(VoidUpgradeContainer::new);
    public static final UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> VOID_ADVANCED = new UpgradeContainerType<>(VoidUpgradeContainer::new);
    public static final UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> CRAFTING = new UpgradeContainerType<>(CraftingUpgradeContainer::new);
    public static final UpgradeContainerType<SmeltingUpgradeWrapper, SmeltingUpgradeContainer> SMELTING = new UpgradeContainerType<>(SmeltingUpgradeContainer::new);
    public static final UpgradeContainerType<AutoSmeltingUpgradeWrapper, AutoSmeltingUpgradeContainer> AUTO_SMELTING = new UpgradeContainerType<>(AutoSmeltingUpgradeContainer::new);
    public static final UpgradeContainerType<JukeboxUpgradeWrapper, JukeboxUpgradeContainer> JUKEBOX = new UpgradeContainerType<>(JukeboxUpgradeContainer::new);
    public static final UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> PUMP_BASIC = new UpgradeContainerType<>(PumpUpgradeContainer::new);
    public static final UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> PUMP_ADVANCED = new UpgradeContainerType<>(PumpUpgradeContainer::new);
    public static final UpgradeContainerType<XpPumpUpgradeWrapper, XpPumpUpgradeContainer> XP_PUMP = new UpgradeContainerType<>(XpPumpUpgradeContainer::new);

    public static final UpgradeContainerType<HopperUpgradeWrapper, HopperUpgradeContainer> HOPPER_BASIC = new UpgradeContainerType<>(HopperUpgradeContainer::new);
    public static final UpgradeContainerType<HopperUpgradeWrapper, HopperUpgradeContainer> HOPPER_ADVANCED = new UpgradeContainerType<>(HopperUpgradeContainer::new);

    private static final List<Item> UPGRADE_ITEMS = new ArrayList<>();

    private ModUpgrades() {}

    public static List<Item> getUpgradeItems() {
        return Collections.unmodifiableList(UPGRADE_ITEMS);
    }

    public static void registerItems(IForgeRegistry<Item> registry, CreativeTabs tab) {
        ModConfig.Upgrades config = ModConfig.upgrades;
        register(registry, new BaseItem("upgrade_base", tab));
        register(registry, new PickupUpgradeItem("pickup_upgrade", tab, () -> config.pickup.filterSlots), PICKUP_BASIC);
        register(registry, new PickupUpgradeItem("advanced_pickup_upgrade", tab, () -> config.advancedPickup.filterSlots), PICKUP_ADVANCED);
        register(registry, new FilterUpgradeItem("filter_upgrade", tab, () -> config.filter.filterSlots), FilterUpgradeContainer.BASIC_TYPE);
        register(registry, new FilterUpgradeItem("advanced_filter_upgrade", tab, () -> config.advancedFilter.filterSlots), FilterUpgradeContainer.ADVANCED_TYPE);
        register(registry, new MagnetUpgradeItem("magnet_upgrade", tab, () -> config.magnet.magnetRange, () -> config.magnet.filterSlots), MAGNET_BASIC);
        register(registry, new MagnetUpgradeItem("advanced_magnet_upgrade", tab, () -> config.advancedMagnet.magnetRange, () -> config.advancedMagnet.filterSlots), MAGNET_ADVANCED);
        register(registry, new FeedingUpgradeItem("feeding_upgrade", tab, () -> config.feeding.filterSlots), FEEDING_BASIC);
        register(registry, new FeedingUpgradeItem("advanced_feeding_upgrade", tab, () -> config.advancedFeeding.filterSlots), FEEDING_ADVANCED);
        register(registry, new CompactingUpgradeItem("compacting_upgrade", tab, false, () -> config.compacting.filterSlots), COMPACTING_BASIC);
        register(registry, new CompactingUpgradeItem("advanced_compacting_upgrade", tab, true, () -> config.advancedCompacting.filterSlots), COMPACTING_ADVANCED);
        register(registry, new VoidUpgradeItem("void_upgrade", tab, () -> config.voiding.filterSlots, () -> config.voiding.voidAnythingEnabled), VOID_BASIC);
        register(registry, new VoidUpgradeItem("advanced_void_upgrade", tab, () -> config.advancedVoiding.filterSlots, () -> config.advancedVoiding.voidAnythingEnabled), VOID_ADVANCED);
        register(registry, new SmeltingUpgradeItem("smelting_upgrade", tab, config.smelting), SMELTING);
        register(registry, new AutoSmeltingUpgradeItem("auto_smelting_upgrade", tab, config.autoSmelting), AUTO_SMELTING);
        register(registry, new CraftingUpgradeItem("crafting_upgrade", tab), CRAFTING);
        register(registry, new StackUpgradeItem("stack_upgrade_tier_1", tab, 2));
        register(registry, new StackUpgradeItem("stack_upgrade_tier_1_plus", tab, 3));
        register(registry, new StackUpgradeItem("stack_upgrade_tier_2", tab, 4));
        register(registry, new StackUpgradeItem("stack_upgrade_tier_3", tab, 8));
        register(registry, new StackUpgradeItem("stack_upgrade_tier_4", tab, 16));
        // From the original 1.20: tier 5, omega and the downgrades.
        register(registry, new StackUpgradeItem("stack_upgrade_tier_5", tab, 32));
        register(registry, new StackUpgradeItem("stack_upgrade_omega_tier", tab, Integer.MAX_VALUE));
        register(registry, new StackUpgradeItem("stack_downgrade_tier_1", tab, 0.125));
        register(registry, new StackUpgradeItem("stack_downgrade_tier_2", tab, 0.0625));
        register(registry, new StackUpgradeItem("stack_downgrade_tier_3", tab, 0.03125));
        register(registry, new com.exiledradio.scorelegacy.upgrades.infinity.InfinityUpgradeItem("infinity_upgrade", tab, true));
        register(registry, new com.exiledradio.scorelegacy.upgrades.infinity.InfinityUpgradeItem("survival_infinity_upgrade", tab, false));
        register(registry, new JukeboxUpgradeItem("jukebox_upgrade", tab), JUKEBOX);
        register(registry, new PumpUpgradeItem("pump_upgrade", tab, false, false, config.pump), PUMP_BASIC);
        register(registry, new PumpUpgradeItem("advanced_pump_upgrade", tab, true, true, config.pump), PUMP_ADVANCED);
        register(registry, new XpPumpUpgradeItem("xp_pump_upgrade", tab, config.xpPump), XP_PUMP);
        register(registry, new HopperUpgradeItem("hopper_upgrade", tab, () -> config.hopper, false), HOPPER_BASIC);
        register(registry, new HopperUpgradeItem("advanced_hopper_upgrade", tab, () -> config.advancedHopper, true), HOPPER_ADVANCED);
        register(registry, new CompressionUpgradeItem("compression_upgrade", tab));
        register(registry, new PackingTapeItem(tab));
        register(registry, new StorageToolItem(tab));
        for (StorageTierUpgradeItem.TierUpgrade tierUpgrade : StorageTierUpgradeItem.TierUpgrade.values()) {
            register(registry, new StorageTierUpgradeItem(tierUpgrade, tab));
        }
    }

    private static void register(IForgeRegistry<Item> registry, Item item) {
        registry.register(item);
        UPGRADE_ITEMS.add(item);
    }

    private static void register(IForgeRegistry<Item> registry, Item item, UpgradeContainerType<?, ?> containerType) {
        register(registry, item);
        UpgradeContainerRegistry.register(item, containerType);
    }

    /** The plain crafting ingredient every upgrade starts from. */
    private static class BaseItem extends Item {
        BaseItem(String name, CreativeTabs tab) {
            setRegistryName(SophisticatedStorageLegacy.MODID, name);
            setTranslationKey(SophisticatedStorageLegacy.MODID + "." + name);
            setCreativeTab(tab);
        }
    }
}
