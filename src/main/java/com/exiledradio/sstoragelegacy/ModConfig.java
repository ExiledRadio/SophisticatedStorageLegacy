package com.exiledradio.sstoragelegacy;

import com.exiledradio.scorelegacy.config.AutoCookingUpgradeConfig;
import com.exiledradio.scorelegacy.config.CookingUpgradeConfig;
import com.exiledradio.scorelegacy.config.FilteredUpgradeConfig;
import com.exiledradio.scorelegacy.config.MagnetUpgradeConfig;
import com.exiledradio.scorelegacy.config.PumpUpgradeConfig;
import com.exiledradio.scorelegacy.config.XpPumpUpgradeConfig;
import com.exiledradio.scorelegacy.settings.Setting;
import com.exiledradio.sstoragelegacy.block.StorageTier;
import com.exiledradio.sstoragelegacy.config.LimitedBarrelConfig;
import com.exiledradio.sstoragelegacy.config.StorageSlotsConfig;
import com.exiledradio.sstoragelegacy.config.UpgradeLimitsConfig;
import com.exiledradio.sstoragelegacy.upgrades.compression.CompressionInventoryPart;
import com.exiledradio.sstoragelegacy.upgrades.compression.CompressionUpgradeConfig;
import com.exiledradio.sstoragelegacy.upgrades.hopper.HopperUpgradeConfig;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Storage's own settings; the defaults are the original's. */
@Config(modid = SophisticatedStorageLegacy.MODID)
@Mod.EventBusSubscriber(modid = SophisticatedStorageLegacy.MODID)
public class ModConfig {

    @Config.Comment({"Slots of each storage. A storage keeps the counts it was first used with (as in the original),",
            "so changes apply to storages used for the first time afterwards; a tier upgrade adds the difference between tiers."})
    @Setting.Feature(icon = "sstoragelegacy:barrel")
    public static final Storages storages = new Storages();

    public static class Storages {
        @Setting.Feature(icon = "sstoragelegacy:barrel")
        public final StorageSlotsConfig woodBarrel = new StorageSlotsConfig(27, 1);
        @Setting.Feature(icon = "sstoragelegacy:copper_barrel")
        public final StorageSlotsConfig copperBarrel = new StorageSlotsConfig(45, 1);
        @Setting.Feature(icon = "sstoragelegacy:iron_barrel")
        public final StorageSlotsConfig ironBarrel = new StorageSlotsConfig(54, 2);
        @Setting.Feature(icon = "sstoragelegacy:gold_barrel")
        public final StorageSlotsConfig goldBarrel = new StorageSlotsConfig(81, 3);
        @Setting.Feature(icon = "sstoragelegacy:diamond_barrel")
        public final StorageSlotsConfig diamondBarrel = new StorageSlotsConfig(108, 4);
        @Setting.Feature(icon = "sstoragelegacy:netherite_barrel")
        public final StorageSlotsConfig netheriteBarrel = new StorageSlotsConfig(132, 5);
        @Setting.Feature(icon = "sstoragelegacy:nether_star_barrel")
        public final StorageSlotsConfig netherStarBarrel = new StorageSlotsConfig(132, 5);
        @Setting.Feature(icon = "sstoragelegacy:chest")
        public final StorageSlotsConfig woodChest = new StorageSlotsConfig(27, 1);
        @Setting.Feature(icon = "sstoragelegacy:copper_chest")
        public final StorageSlotsConfig copperChest = new StorageSlotsConfig(45, 1);
        @Setting.Feature(icon = "sstoragelegacy:iron_chest")
        public final StorageSlotsConfig ironChest = new StorageSlotsConfig(54, 2);
        @Setting.Feature(icon = "sstoragelegacy:gold_chest")
        public final StorageSlotsConfig goldChest = new StorageSlotsConfig(81, 3);
        @Setting.Feature(icon = "sstoragelegacy:diamond_chest")
        public final StorageSlotsConfig diamondChest = new StorageSlotsConfig(108, 4);
        @Setting.Feature(icon = "sstoragelegacy:netherite_chest")
        public final StorageSlotsConfig netheriteChest = new StorageSlotsConfig(132, 5);
        @Setting.Feature(icon = "sstoragelegacy:nether_star_chest")
        public final StorageSlotsConfig netherStarChest = new StorageSlotsConfig(132, 5);
        @Setting.Feature(icon = "sstoragelegacy:shulker_box")
        public final StorageSlotsConfig shulkerBox = new StorageSlotsConfig(27, 1);
        @Setting.Feature(icon = "sstoragelegacy:copper_shulker_box")
        public final StorageSlotsConfig copperShulkerBox = new StorageSlotsConfig(45, 1);
        @Setting.Feature(icon = "sstoragelegacy:iron_shulker_box")
        public final StorageSlotsConfig ironShulkerBox = new StorageSlotsConfig(54, 2);
        @Setting.Feature(icon = "sstoragelegacy:gold_shulker_box")
        public final StorageSlotsConfig goldShulkerBox = new StorageSlotsConfig(81, 3);
        @Setting.Feature(icon = "sstoragelegacy:diamond_shulker_box")
        public final StorageSlotsConfig diamondShulkerBox = new StorageSlotsConfig(108, 4);
        @Setting.Feature(icon = "sstoragelegacy:netherite_shulker_box")
        public final StorageSlotsConfig netheriteShulkerBox = new StorageSlotsConfig(132, 5);
        @Setting.Feature(icon = "sstoragelegacy:nether_star_shulker_box")
        public final StorageSlotsConfig netherStarShulkerBox = new StorageSlotsConfig(132, 5);
        @Setting.Feature(icon = "sstoragelegacy:limited_barrel_1")
        public final LimitedBarrelConfig limitedBarrel1 = new LimitedBarrelConfig(32, 1);
        @Setting.Feature(icon = "sstoragelegacy:limited_copper_barrel_1")
        public final LimitedBarrelConfig limitedCopperBarrel1 = new LimitedBarrelConfig(53, 1);
        @Setting.Feature(icon = "sstoragelegacy:limited_iron_barrel_1")
        public final LimitedBarrelConfig limitedIronBarrel1 = new LimitedBarrelConfig(64, 2);
        @Setting.Feature(icon = "sstoragelegacy:limited_gold_barrel_1")
        public final LimitedBarrelConfig limitedGoldBarrel1 = new LimitedBarrelConfig(96, 3);
        @Setting.Feature(icon = "sstoragelegacy:limited_diamond_barrel_1")
        public final LimitedBarrelConfig limitedDiamondBarrel1 = new LimitedBarrelConfig(128, 4);
        @Setting.Feature(icon = "sstoragelegacy:limited_netherite_barrel_1")
        public final LimitedBarrelConfig limitedNetheriteBarrel1 = new LimitedBarrelConfig(160, 5);
        @Setting.Feature(icon = "sstoragelegacy:limited_nether_star_barrel_1")
        public final LimitedBarrelConfig limitedNetherStarBarrel1 = new LimitedBarrelConfig(160, 5);
        @Setting.Feature(icon = "sstoragelegacy:limited_barrel_2")
        public final LimitedBarrelConfig limitedBarrel2 = new LimitedBarrelConfig(16, 1);
        @Setting.Feature(icon = "sstoragelegacy:limited_copper_barrel_2")
        public final LimitedBarrelConfig limitedCopperBarrel2 = new LimitedBarrelConfig(27, 1);
        @Setting.Feature(icon = "sstoragelegacy:limited_iron_barrel_2")
        public final LimitedBarrelConfig limitedIronBarrel2 = new LimitedBarrelConfig(32, 2);
        @Setting.Feature(icon = "sstoragelegacy:limited_gold_barrel_2")
        public final LimitedBarrelConfig limitedGoldBarrel2 = new LimitedBarrelConfig(48, 3);
        @Setting.Feature(icon = "sstoragelegacy:limited_diamond_barrel_2")
        public final LimitedBarrelConfig limitedDiamondBarrel2 = new LimitedBarrelConfig(64, 4);
        @Setting.Feature(icon = "sstoragelegacy:limited_netherite_barrel_2")
        public final LimitedBarrelConfig limitedNetheriteBarrel2 = new LimitedBarrelConfig(80, 5);
        @Setting.Feature(icon = "sstoragelegacy:limited_nether_star_barrel_2")
        public final LimitedBarrelConfig limitedNetherStarBarrel2 = new LimitedBarrelConfig(80, 5);
        @Setting.Feature(icon = "sstoragelegacy:limited_barrel_3")
        public final LimitedBarrelConfig limitedBarrel3 = new LimitedBarrelConfig(10, 1);
        @Setting.Feature(icon = "sstoragelegacy:limited_copper_barrel_3")
        public final LimitedBarrelConfig limitedCopperBarrel3 = new LimitedBarrelConfig(17, 1);
        @Setting.Feature(icon = "sstoragelegacy:limited_iron_barrel_3")
        public final LimitedBarrelConfig limitedIronBarrel3 = new LimitedBarrelConfig(20, 2);
        @Setting.Feature(icon = "sstoragelegacy:limited_gold_barrel_3")
        public final LimitedBarrelConfig limitedGoldBarrel3 = new LimitedBarrelConfig(30, 3);
        @Setting.Feature(icon = "sstoragelegacy:limited_diamond_barrel_3")
        public final LimitedBarrelConfig limitedDiamondBarrel3 = new LimitedBarrelConfig(40, 4);
        @Setting.Feature(icon = "sstoragelegacy:limited_netherite_barrel_3")
        public final LimitedBarrelConfig limitedNetheriteBarrel3 = new LimitedBarrelConfig(50, 5);
        @Setting.Feature(icon = "sstoragelegacy:limited_nether_star_barrel_3")
        public final LimitedBarrelConfig limitedNetherStarBarrel3 = new LimitedBarrelConfig(50, 5);
        @Setting.Feature(icon = "sstoragelegacy:limited_barrel_4")
        public final LimitedBarrelConfig limitedBarrel4 = new LimitedBarrelConfig(8, 1);
        @Setting.Feature(icon = "sstoragelegacy:limited_copper_barrel_4")
        public final LimitedBarrelConfig limitedCopperBarrel4 = new LimitedBarrelConfig(13, 1);
        @Setting.Feature(icon = "sstoragelegacy:limited_iron_barrel_4")
        public final LimitedBarrelConfig limitedIronBarrel4 = new LimitedBarrelConfig(16, 2);
        @Setting.Feature(icon = "sstoragelegacy:limited_gold_barrel_4")
        public final LimitedBarrelConfig limitedGoldBarrel4 = new LimitedBarrelConfig(24, 3);
        @Setting.Feature(icon = "sstoragelegacy:limited_diamond_barrel_4")
        public final LimitedBarrelConfig limitedDiamondBarrel4 = new LimitedBarrelConfig(32, 4);
        @Setting.Feature(icon = "sstoragelegacy:limited_netherite_barrel_4")
        public final LimitedBarrelConfig limitedNetheriteBarrel4 = new LimitedBarrelConfig(40, 5);
        @Setting.Feature(icon = "sstoragelegacy:limited_nether_star_barrel_4")
        public final LimitedBarrelConfig limitedNetherStarBarrel4 = new LimitedBarrelConfig(40, 5);

        /** The slots of a barrel, chest or shulker box of a tier ("barrel", "chest", "shulker_box"). */
        public StorageSlotsConfig get(String storageName, StorageTier tier) {
            StorageSlotsConfig[] byTier;
            switch (storageName) {
                case "chest":
                    byTier = new StorageSlotsConfig[] {woodChest, copperChest, ironChest, goldChest, diamondChest, netheriteChest, netherStarChest};
                    break;
                case "shulker_box":
                    byTier = new StorageSlotsConfig[] {shulkerBox, copperShulkerBox, ironShulkerBox, goldShulkerBox, diamondShulkerBox, netheriteShulkerBox, netherStarShulkerBox};
                    break;
                case "barrel":
                default:
                    byTier = new StorageSlotsConfig[] {woodBarrel, copperBarrel, ironBarrel, goldBarrel, diamondBarrel, netheriteBarrel, netherStarBarrel};
            }
            return byTier[tier.ordinal()];
        }

        /** A limited barrel of a tier and slot count (1 to 4). */
        public LimitedBarrelConfig getLimited(StorageTier tier, int slotCount) {
            LimitedBarrelConfig[][] bySlots = {
                    {limitedBarrel1, limitedCopperBarrel1, limitedIronBarrel1, limitedGoldBarrel1, limitedDiamondBarrel1, limitedNetheriteBarrel1, limitedNetherStarBarrel1},
                    {limitedBarrel2, limitedCopperBarrel2, limitedIronBarrel2, limitedGoldBarrel2, limitedDiamondBarrel2, limitedNetheriteBarrel2, limitedNetherStarBarrel2},
                    {limitedBarrel3, limitedCopperBarrel3, limitedIronBarrel3, limitedGoldBarrel3, limitedDiamondBarrel3, limitedNetheriteBarrel3, limitedNetherStarBarrel3},
                    {limitedBarrel4, limitedCopperBarrel4, limitedIronBarrel4, limitedGoldBarrel4, limitedDiamondBarrel4, limitedNetheriteBarrel4, limitedNetherStarBarrel4}};
            return bySlots[Math.max(0, Math.min(3, slotCount - 1))][tier.ordinal()];
        }
    }

    @Config.Comment("Client-side settings")
    @Setting.Feature(icon = "sstoragelegacy:iron_barrel")
    public static final Client client = new Client();

    public static class Client {
        @Config.Comment({"Determines whether JEI and creative tab will show tinted storage items for iron and higher tiers.",
                "Can help with easier navigation in JEI."})
        @Config.RequiresMcRestart
        @Setting.Client
        public boolean showHigherTierTintedVariants = true;
    }

    @Config.Comment("Items shulker boxes don't take")
    @Setting.Feature(icon = "sstoragelegacy:shulker_box")
    public static final ShulkerBoxDisallowedItems shulkerBoxDisallowedItems = new ShulkerBoxDisallowedItems();

    public static class ShulkerBoxDisallowedItems {
        @Config.Comment("List of items that are not allowed to be put in shulkerboxes - e.g. \"minecraft:ender_chest\"")
        @Setting.ListOf(Setting.Kind.ITEM)
        public String[] disallowedItems = new String[0];

        @Config.Comment({"Determines if container items are able to fit in shulker boxes. In 1.12 these are items that",
                "carry an inventory of their own (an item handler capability), such as backpacks."})
        public boolean containerItemsDisallowed = false;

        public boolean isItemDisallowed(net.minecraft.item.ItemStack stack) {
            if (containerItemsDisallowed && stack.hasCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                return true;
            }
            net.minecraft.util.ResourceLocation name = stack.getItem().getRegistryName();
            if (name == null) {
                return false;
            }
            for (String disallowed : disallowedItems) {
                if (disallowed.trim().equals(name.toString())) {
                    return true;
                }
            }
            return false;
        }
    }

    @Config.Comment({"Threshold of number of item entities dropped from chest / barrel above which break is canceled",
            "(unless shift key is pressed) and message is displayed explaining to player many drops and packing tape use"})
    @Config.RangeInt(min = 0, max = 1000)
    public static int tooManyItemEntityDrops = 200;

    @Config.Comment("Upgrade settings")
    @Setting.Feature(icon = "sstoragelegacy:upgrade_base")
    public static final Upgrades upgrades = new Upgrades();

    public static class Upgrades {
        @Setting.Feature(icon = "sstoragelegacy:pickup_upgrade")
        public final FilteredUpgradeConfig pickup = new FilteredUpgradeConfig(9, 3);
        @Setting.Feature(icon = "sstoragelegacy:advanced_pickup_upgrade")
        public final FilteredUpgradeConfig advancedPickup = new FilteredUpgradeConfig(16, 4);
        @Setting.Feature(icon = "sstoragelegacy:filter_upgrade")
        public final FilteredUpgradeConfig filter = new FilteredUpgradeConfig(9, 3);
        @Setting.Feature(icon = "sstoragelegacy:advanced_filter_upgrade")
        public final FilteredUpgradeConfig advancedFilter = new FilteredUpgradeConfig(16, 4);
        @Setting.Feature(icon = "sstoragelegacy:magnet_upgrade")
        public final MagnetUpgradeConfig magnet = new MagnetUpgradeConfig(9, 3, 3);
        @Setting.Feature(icon = "sstoragelegacy:advanced_magnet_upgrade")
        public final MagnetUpgradeConfig advancedMagnet = new MagnetUpgradeConfig(16, 4, 5);
        @Setting.Feature(icon = "sstoragelegacy:feeding_upgrade")
        public final FilteredUpgradeConfig feeding = new FilteredUpgradeConfig(9, 3);
        @Setting.Feature(icon = "sstoragelegacy:advanced_feeding_upgrade")
        public final FilteredUpgradeConfig advancedFeeding = new FilteredUpgradeConfig(16, 4);
        @Setting.Feature(icon = "sstoragelegacy:compacting_upgrade")
        public final FilteredUpgradeConfig compacting = new FilteredUpgradeConfig(9, 3);
        @Setting.Feature(icon = "sstoragelegacy:advanced_compacting_upgrade")
        public final FilteredUpgradeConfig advancedCompacting = new FilteredUpgradeConfig(16, 4);

        @Config.Comment({"Items the compacting upgrades compact even though no recipe crafts them back, so the basic",
                "upgrade handles them too (registry name, optionally @metadata; a trailing * matches any name",
                "starting with the rest). Default: NoTreePunching's rocks."})
        @Setting.ListOf(Setting.Kind.ITEM)
        public String[] alwaysCompactedItems = {"notreepunching:rock/*"};
        @Setting.Feature(icon = "sstoragelegacy:void_upgrade")
        public final com.exiledradio.scorelegacy.config.VoidUpgradeConfig voiding = new com.exiledradio.scorelegacy.config.VoidUpgradeConfig(9, 3);
        @Setting.Feature(icon = "sstoragelegacy:advanced_void_upgrade")
        public final com.exiledradio.scorelegacy.config.VoidUpgradeConfig advancedVoiding = new com.exiledradio.scorelegacy.config.VoidUpgradeConfig(16, 4);
        @Setting.Feature(icon = "sstoragelegacy:smelting_upgrade")
        public final CookingUpgradeConfig smelting = new CookingUpgradeConfig();
        @Setting.Feature(icon = "sstoragelegacy:auto_smelting_upgrade")
        public final AutoCookingUpgradeConfig autoSmelting = new AutoCookingUpgradeConfig();
        @Setting.Feature(icon = "sstoragelegacy:smoking_upgrade")
        public final CookingUpgradeConfig smoking = new CookingUpgradeConfig();
        @Setting.Feature(icon = "sstoragelegacy:auto_smoking_upgrade")
        public final AutoCookingUpgradeConfig autoSmoking = new AutoCookingUpgradeConfig();
        @Setting.Feature(icon = "sstoragelegacy:blasting_upgrade")
        public final CookingUpgradeConfig blasting = new CookingUpgradeConfig();
        @Setting.Feature(icon = "sstoragelegacy:auto_blasting_upgrade")
        public final AutoCookingUpgradeConfig autoBlasting = new AutoCookingUpgradeConfig();
        @Setting.Feature(icon = "sstoragelegacy:pump_upgrade")
        public final PumpUpgradeConfig pump = new PumpUpgradeConfig();
        @Setting.Feature(icon = "sstoragelegacy:xp_pump_upgrade")
        public final XpPumpUpgradeConfig xpPump = new XpPumpUpgradeConfig();
        @Setting.Feature(icon = "sstoragelegacy:hopper_upgrade")
        public final HopperUpgradeConfig hopper = new HopperUpgradeConfig(2, 2, 2, 2, 8, 1);
        @Setting.Feature(icon = "sstoragelegacy:advanced_hopper_upgrade")
        public final HopperUpgradeConfig advancedHopper = new HopperUpgradeConfig(4, 4, 4, 4, 2, 4);
        @Setting.Feature(icon = "sstoragelegacy:compression_upgrade")
        public final CompressionUpgradeConfig compression = new CompressionUpgradeConfig();

        @Config.Comment({"Limit of maximum number of upgrades of type per storage in format of \"StorageType|UpgradeRegistryName[or UpgradeGroup]|MaxNumber\"",
                "Storage types: barrel, chest, shulker_box, limited_barrel. Groups: stack_upgrades, cooking_upgrades."})
        public String[] maxUpgradesPerStorage = {
                "chest|stack_upgrades|2", "chest|cooking_upgrades|1", "chest|jukebox_upgrade|1",
                "barrel|stack_upgrades|2", "barrel|cooking_upgrades|1", "barrel|jukebox_upgrade|1",
                "shulker_box|stack_upgrades|2", "shulker_box|cooking_upgrades|1", "shulker_box|jukebox_upgrade|1",
                "limited_barrel|cooking_upgrades|1", "limited_barrel|jukebox_upgrade|1"};

        @Config.Comment("Items that never stack beyond their normal size, even with stack upgrades (registry names, e.g. minecraft:shulker_box)")
        @Setting.ListOf(Setting.Kind.ITEM)
        public String[] nonStackableItems = {"minecraft:white_shulker_box", "minecraft:orange_shulker_box", "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box", "minecraft:pink_shulker_box", "minecraft:gray_shulker_box", "minecraft:silver_shulker_box", "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box", "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box", "minecraft:red_shulker_box", "minecraft:black_shulker_box"};
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(SophisticatedStorageLegacy.MODID)) {
            ConfigManager.sync(SophisticatedStorageLegacy.MODID, Config.Type.INSTANCE);
            CompressionInventoryPart.clearConfigCache();
            UpgradeLimitsConfig.clearCache();
        }
    }
}
