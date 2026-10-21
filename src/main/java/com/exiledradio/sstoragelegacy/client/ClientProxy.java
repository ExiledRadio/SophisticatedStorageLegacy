package com.exiledradio.sstoragelegacy.client;

import com.exiledradio.sstoragelegacy.CommonProxy;
import com.exiledradio.sstoragelegacy.block.BarrelTileEntity;
import com.exiledradio.sstoragelegacy.block.ChestTileEntity;
import com.exiledradio.sstoragelegacy.block.ControllerTileEntity;
import com.exiledradio.sstoragelegacy.client.render.ControllerRenderer;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelTileEntity;
import com.exiledradio.sstoragelegacy.client.render.LimitedBarrelRenderer;
import com.exiledradio.sstoragelegacy.block.ShulkerBoxTileEntity;
import com.exiledradio.sstoragelegacy.client.render.ChestRenderer;
import com.exiledradio.sstoragelegacy.client.render.ShulkerBoxRenderer;
import com.exiledradio.sstoragelegacy.client.gui.StorageBlockGui;
import com.exiledradio.sstoragelegacy.client.render.BarrelDisplayItemRenderer;
import com.exiledradio.sstoragelegacy.common.gui.LimitedBarrelContainer;
import com.exiledradio.sstoragelegacy.common.gui.LimitedBarrelSettingsContainer;
import com.exiledradio.sstoragelegacy.client.gui.LimitedBarrelGui;
import com.exiledradio.sstoragelegacy.client.gui.LimitedBarrelSettingsGui;
import com.exiledradio.sstoragelegacy.common.gui.StorageBlockContainer;
import com.exiledradio.sstoragelegacy.common.gui.StorageBlockSettingsContainer;
import com.exiledradio.sstoragelegacy.client.gui.upgrades.UpgradeTabs;
import com.exiledradio.scorelegacy.client.gui.settings.SettingsScreenBase;
import com.exiledradio.sstoragelegacy.init.ModContent;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        Minecraft mc = Minecraft.getMinecraft();
        ClientRegistry.bindTileEntitySpecialRenderer(BarrelTileEntity.class, new BarrelDisplayItemRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(LimitedBarrelTileEntity.class, new LimitedBarrelRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(ControllerTileEntity.class, new ControllerRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(ChestTileEntity.class, new ChestRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(ShulkerBoxTileEntity.class, new ShulkerBoxRenderer());
        UpgradeTabs.register();
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        List<Block> barrelList = new ArrayList<>(ModContent.getBarrels());
        barrelList.addAll(ModContent.getLimitedBarrels());
        Block[] barrels = barrelList.toArray(new Block[0]);
        // Tint index 0 is the main colour, 1 the accent. Undyed parts never ask (only tint parts carry an index).
        mc.getBlockColors().registerBlockColorHandler((state, world, pos, tintIndex) -> {
            if (world == null || pos == null) {
                return -1;
            }
            TileEntity te = world.getTileEntity(pos);
            if (!(te instanceof BarrelTileEntity)) {
                return -1;
            }
            BarrelTileEntity barrel = (BarrelTileEntity) te;
            return tintIndex == 0 ? barrel.getMainColor() : tintIndex == 1 ? barrel.getAccentColor() : -1;
        }, barrels);
        mc.getItemColors().registerItemColorHandler((stack, tintIndex) ->
                tintIndex == 0 ? StorageItemData.getMainColor(stack) : tintIndex == 1 ? StorageItemData.getAccentColor(stack) : -1,
                barrels);
    }

    @Nullable
    @Override
    public Object createGui(Object container) {
        if (container instanceof LimitedBarrelContainer) {
            return new LimitedBarrelGui((LimitedBarrelContainer) container);
        }
        if (container instanceof LimitedBarrelSettingsContainer) {
            return new LimitedBarrelSettingsGui((LimitedBarrelSettingsContainer) container);
        }
        if (container instanceof StorageBlockContainer) {
            return new StorageBlockGui((StorageBlockContainer) container);
        }
        if (container instanceof com.exiledradio.sstoragelegacy.common.gui.DecorationTableContainer) {
            return new com.exiledradio.sstoragelegacy.client.gui.DecorationTableGui((com.exiledradio.sstoragelegacy.common.gui.DecorationTableContainer) container);
        }
        if (container instanceof StorageBlockSettingsContainer) {
            return new SettingsScreenBase((StorageBlockSettingsContainer) container);
        }
        return null;
    }
}
