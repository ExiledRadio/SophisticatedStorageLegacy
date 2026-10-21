package com.exiledradio.sstoragelegacy.init;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.BarrelBlock;
import com.exiledradio.sstoragelegacy.block.ChestBlock;
import com.exiledradio.sstoragelegacy.block.ChestTileEntity;
import com.exiledradio.sstoragelegacy.block.ControllerBlock;
import com.exiledradio.sstoragelegacy.block.ControllerTileEntity;
import com.exiledradio.sstoragelegacy.block.StorageLinkBlock;
import com.exiledradio.sstoragelegacy.block.StorageLinkTileEntity;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelBlock;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelTileEntity;
import com.exiledradio.sstoragelegacy.block.ShulkerBoxBlock;
import com.exiledradio.sstoragelegacy.block.ShulkerBoxTileEntity;
import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.block.StorageTier;
import com.exiledradio.sstoragelegacy.block.BarrelTileEntity;
import com.exiledradio.sstoragelegacy.block.WoodType;
import com.exiledradio.sstoragelegacy.crafting.FlatTopBarrelToggleRecipe;
import com.exiledradio.sstoragelegacy.crafting.StorageDyeRecipe;
import com.exiledradio.sstoragelegacy.item.BarrelItem;
import com.exiledradio.sstoragelegacy.item.LimitedBarrelItem;
import com.exiledradio.sstoragelegacy.item.ShulkerBoxItem;
import com.exiledradio.sstoragelegacy.item.StorageBlockItem;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import com.exiledradio.sstoragelegacy.item.TooltipBlockItem;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SophisticatedStorageLegacy.MODID)
public final class ModContent {
    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(SophisticatedStorageLegacy.MODID) {
        @Nonnull
        @Override
        public ItemStack createIcon() {
            return StorageItemData.setWoodType(new ItemStack(BARREL), WoodType.SPRUCE);
        }
    };

    /** The wood barrel, which most code (recipes, dye, JEI examples) starts from. */
    public static BarrelBlock BARREL;
    private static final Map<StorageTier, BarrelBlock> BARRELS = new EnumMap<>(StorageTier.class);
    /** Limited barrels by tier, then slot count (index 0 = 1 slot). */
    private static final Map<StorageTier, LimitedBarrelBlock[]> LIMITED_BARRELS = new EnumMap<>(StorageTier.class);
    private static final Map<StorageTier, ChestBlock> CHESTS = new EnumMap<>(StorageTier.class);
    private static final Map<StorageTier, ShulkerBoxBlock> SHULKER_BOXES = new EnumMap<>(StorageTier.class);
    /** Every storage block, barrels, limited barrels, chests, shulker boxes, each wood tier first. */
    private static final List<StorageBlockBase> ALL_STORAGES = new ArrayList<>();

    public static ControllerBlock CONTROLLER;
    public static StorageLinkBlock STORAGE_LINK;
    public static com.exiledradio.sstoragelegacy.block.DecorationTableBlock DECORATION_TABLE;
    public static com.exiledradio.sstoragelegacy.item.PaintbrushItem PAINTBRUSH;
    /** Storage I/O, Input and Output (from the original 1.20). */
    public static final List<com.exiledradio.sstoragelegacy.block.StorageIOBlock> STORAGE_IO_BLOCKS = new ArrayList<>();

    private ModContent() {}

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        for (StorageTier tier : StorageTier.values()) {
            BARRELS.put(tier, register(event, new BarrelBlock(tier)));
        }
        for (StorageTier tier : StorageTier.values()) {
            LimitedBarrelBlock[] limited = new LimitedBarrelBlock[4];
            for (int slots = 1; slots <= 4; slots++) {
                limited[slots - 1] = register(event, new LimitedBarrelBlock(tier, slots));
            }
            LIMITED_BARRELS.put(tier, limited);
        }
        for (StorageTier tier : StorageTier.values()) {
            CHESTS.put(tier, register(event, new ChestBlock(tier)));
        }
        for (StorageTier tier : StorageTier.values()) {
            SHULKER_BOXES.put(tier, register(event, new ShulkerBoxBlock(tier)));
        }
        BARREL = BARRELS.get(StorageTier.WOOD);
        CONTROLLER = new ControllerBlock();
        CONTROLLER.setCreativeTab(CREATIVE_TAB);
        event.getRegistry().register(CONTROLLER);
        STORAGE_LINK = new StorageLinkBlock();
        STORAGE_LINK.setCreativeTab(CREATIVE_TAB);
        event.getRegistry().register(STORAGE_LINK);
        for (com.exiledradio.sstoragelegacy.block.StorageIOBlock.Mode mode : com.exiledradio.sstoragelegacy.block.StorageIOBlock.Mode.values()) {
            com.exiledradio.sstoragelegacy.block.StorageIOBlock io = new com.exiledradio.sstoragelegacy.block.StorageIOBlock(mode);
            io.setCreativeTab(CREATIVE_TAB);
            event.getRegistry().register(io);
            STORAGE_IO_BLOCKS.add(io);
        }
        DECORATION_TABLE = new com.exiledradio.sstoragelegacy.block.DecorationTableBlock();
        DECORATION_TABLE.setCreativeTab(CREATIVE_TAB);
        event.getRegistry().register(DECORATION_TABLE);
        GameRegistry.registerTileEntity(com.exiledradio.sstoragelegacy.block.DecorationTableTileEntity.class, new ResourceLocation(SophisticatedStorageLegacy.MODID, "decoration_table"));
        GameRegistry.registerTileEntity(com.exiledradio.sstoragelegacy.block.StorageIOTileEntity.class, new ResourceLocation(SophisticatedStorageLegacy.MODID, "storage_io"));
        GameRegistry.registerTileEntity(ControllerTileEntity.class, new ResourceLocation(SophisticatedStorageLegacy.MODID, "controller"));
        GameRegistry.registerTileEntity(StorageLinkTileEntity.class, new ResourceLocation(SophisticatedStorageLegacy.MODID, "storage_link"));
        GameRegistry.registerTileEntity(BarrelTileEntity.class, new ResourceLocation(SophisticatedStorageLegacy.MODID, "barrel"));
        GameRegistry.registerTileEntity(LimitedBarrelTileEntity.class, new ResourceLocation(SophisticatedStorageLegacy.MODID, "limited_barrel"));
        GameRegistry.registerTileEntity(ChestTileEntity.class, new ResourceLocation(SophisticatedStorageLegacy.MODID, "chest"));
        GameRegistry.registerTileEntity(ShulkerBoxTileEntity.class, new ResourceLocation(SophisticatedStorageLegacy.MODID, "shulker_box"));
    }

    private static <B extends StorageBlockBase> B register(RegistryEvent.Register<Block> event, B block) {
        block.setCreativeTab(CREATIVE_TAB);
        event.getRegistry().register(block);
        ALL_STORAGES.add(block);
        return block;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (BarrelBlock barrel : BARRELS.values()) {
            event.getRegistry().register(new BarrelItem(barrel));
        }
        for (LimitedBarrelBlock[] limited : LIMITED_BARRELS.values()) {
            for (LimitedBarrelBlock barrel : limited) {
                event.getRegistry().register(new LimitedBarrelItem(barrel));
            }
        }
        for (ChestBlock chest : CHESTS.values()) {
            event.getRegistry().register(new StorageBlockItem(chest));
        }
        for (ShulkerBoxBlock shulkerBox : SHULKER_BOXES.values()) {
            event.getRegistry().register(new ShulkerBoxItem(shulkerBox));
        }
        event.getRegistry().register(new TooltipBlockItem(CONTROLLER));
        event.getRegistry().register(new TooltipBlockItem(STORAGE_LINK));
        for (com.exiledradio.sstoragelegacy.block.StorageIOBlock io : STORAGE_IO_BLOCKS) {
            event.getRegistry().register(new TooltipBlockItem(io));
        }
        event.getRegistry().register(new net.minecraft.item.ItemBlock(DECORATION_TABLE).setRegistryName(DECORATION_TABLE.getRegistryName()));
        PAINTBRUSH = new com.exiledradio.sstoragelegacy.item.PaintbrushItem(CREATIVE_TAB);
        event.getRegistry().register(PAINTBRUSH);
        ModUpgrades.registerItems(event.getRegistry(), CREATIVE_TAB);
    }

    public static BarrelBlock getBarrel(StorageTier tier) {
        return BARRELS.get(tier);
    }

    /** Every tier's barrel, wood first. */
    public static Collection<BarrelBlock> getBarrels() {
        return Collections.unmodifiableCollection(BARRELS.values());
    }

    /** Every limited barrel, by tier then slot count. */
    public static List<LimitedBarrelBlock> getLimitedBarrels() {
        List<LimitedBarrelBlock> all = new ArrayList<>();
        for (LimitedBarrelBlock[] limited : LIMITED_BARRELS.values()) {
            Collections.addAll(all, limited);
        }
        return all;
    }

    public static Collection<ChestBlock> getChests() {
        return Collections.unmodifiableCollection(CHESTS.values());
    }

    public static Collection<ShulkerBoxBlock> getShulkerBoxes() {
        return Collections.unmodifiableCollection(SHULKER_BOXES.values());
    }

    public static List<StorageBlockBase> getAllStorages() {
        return Collections.unmodifiableList(ALL_STORAGES);
    }

    /** The storage of a kind ("barrel", "chest", "shulker_box") and tier, or null. */
    @Nullable
    public static StorageBlockBase getStorage(String storageName, StorageTier tier) {
        for (StorageBlockBase block : ALL_STORAGES) {
            if (block.getStorageName().equals(storageName) && block.getTier() == tier) {
                return block;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new StorageDyeRecipe().setRegistryName(SophisticatedStorageLegacy.MODID, "storage_dye"));
        event.getRegistry().register(new FlatTopBarrelToggleRecipe().setRegistryName(SophisticatedStorageLegacy.MODID, "flat_top_barrel_toggle"));
    }
}
