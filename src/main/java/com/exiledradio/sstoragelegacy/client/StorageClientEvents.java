/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.upgrades.hopper.HopperUpgradeContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Sprites Storage's GUIs draw from the block atlas (empty filter slot icons). */
@Mod.EventBusSubscriber(modid = SophisticatedStorageLegacy.MODID, value = Side.CLIENT)
public final class StorageClientEvents {
    private StorageClientEvents() {}

    @SubscribeEvent
    public static void registerSprites(TextureStitchEvent.Pre event) {
        if (event.getMap() != Minecraft.getMinecraft().getTextureMapBlocks()) {
            return;
        }
        event.getMap().registerSprite(new ResourceLocation(HopperUpgradeContainer.EMPTY_INPUT_FILTER_SLOT_SPRITE));
        event.getMap().registerSprite(new ResourceLocation(HopperUpgradeContainer.EMPTY_OUTPUT_FILTER_SLOT_SPRITE));
    }
}
