package com.exiledradio.sstoragelegacy.client;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.client.render.StorageItemStackRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import com.exiledradio.sstoragelegacy.block.WoodType;
import com.exiledradio.sstoragelegacy.init.ModContent;
import com.exiledradio.sstoragelegacy.init.ModUpgrades;
import com.exiledradio.sstoragelegacy.item.BarrelItem;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

/** Item model bindings. Each barrel block (limited too) uses its own multipart blockstate (barrel.json, limited_iron_barrel_2.json...) directly. */
@Mod.EventBusSubscriber(modid = SophisticatedStorageLegacy.MODID, value = Side.CLIENT)
public final class ModelRegistration {
    private ModelRegistration() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (StorageBlockBase block : ModContent.getAllStorages()) {
            if (!(Item.getItemFromBlock(block) instanceof BarrelItem)) {
                continue;
            }
            BarrelItem barrel = (BarrelItem) Item.getItemFromBlock(block);
            String prefix = barrel.getModelKeyPrefix();
            // One merged model per look and tier, see tools/gen_barrel_models.py.
            List<ModelResourceLocation> variants = new ArrayList<>();
            for (String flat : new String[] {"", "_flat"}) {
                variants.add(barrelModel(prefix + "tinted" + flat));
                for (WoodType wood : WoodType.values()) {
                    for (String tint : new String[] {"", "_main", "_accent"}) {
                        variants.add(barrelModel(prefix + wood.getName() + tint + flat));
                    }
                }
            }
            variants.add(com.exiledradio.sstoragelegacy.client.render.BarrelMaterialModel.ITEM_MODEL);
            ModelBakery.registerItemVariants(barrel, variants.toArray(new ModelResourceLocation[0]));
            // With materials the item is built per stack (BarrelMaterialModel).
            ModelLoader.setCustomMeshDefinition(barrel, stack -> com.exiledradio.sstoragelegacy.item.BarrelMaterials.get(stack).isEmpty()
                    ? barrelModel(barrel.getModelKey(stack)) : com.exiledradio.sstoragelegacy.client.render.BarrelMaterialModel.ITEM_MODEL);
        }
        // Chests and shulker boxes are drawn by TESRs (items too); their blockstates only give particles.
        StorageItemStackRenderer itemRenderer = new StorageItemStackRenderer();
        for (StorageBlockBase block : ModContent.getAllStorages()) {
            if (Item.getItemFromBlock(block) instanceof BarrelItem) {
                continue;
            }
            String kind = block.getStorageName();
            ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                @Override
                protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                    return new ModelResourceLocation(SophisticatedStorageLegacy.MODID + ":" + kind + "_block", "normal");
                }
            });
            Item item = Item.getItemFromBlock(block);
            item.setTileEntityItemStackRenderer(itemRenderer);
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(SophisticatedStorageLegacy.MODID + ":" + kind, "inventory"));
        }
        List<Block> plainBlocks = new ArrayList<>(java.util.Arrays.asList(ModContent.CONTROLLER, ModContent.STORAGE_LINK));
        plainBlocks.addAll(ModContent.STORAGE_IO_BLOCKS);
        plainBlocks.add(ModContent.DECORATION_TABLE);
        ModelLoader.setCustomModelResourceLocation(ModContent.PAINTBRUSH, 0, new ModelResourceLocation(ModContent.PAINTBRUSH.getRegistryName(), "inventory"));
        for (Block block : plainBlocks) {
            //noinspection ConstantConditions - registered blocks have a registry name
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
        for (Item item : ModUpgrades.getUpgradeItems()) {
            //noinspection ConstantConditions - registered items always have a registry name
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }

    @SubscribeEvent
    public static void onModelBake(net.minecraftforge.client.event.ModelBakeEvent event) {
        com.exiledradio.sstoragelegacy.client.render.BarrelMaterialModel.onModelBake(event);
    }

    private static ModelResourceLocation barrelModel(String key) {
        return new ModelResourceLocation(SophisticatedStorageLegacy.MODID + ":barrel/" + key, "inventory");
    }
}
