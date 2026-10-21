/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.render;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.ShulkerBoxBlock;
import com.exiledradio.sstoragelegacy.block.ShulkerBoxTileEntity;
import com.exiledradio.sstoragelegacy.block.StorageTier;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelShulker;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * The original's shulker box: vanilla's shulker shell (same model and texture layout), drawn in
 * layers: the plain look unless both colours are dyed, the tinted main and accent overlays, then
 * the tier trim. The transform is vanilla 1.12's shulker box renderer's.
 */
public class ShulkerBoxRenderer extends TileEntitySpecialRenderer<ShulkerBoxTileEntity> {
    private static final String TEXTURES = SophisticatedStorageLegacy.MODID + ":textures/entity/shulker_box/";
    private static final ResourceLocation NO_TINT = new ResourceLocation(TEXTURES + "no_tint.png");
    private static final ResourceLocation TINTABLE_MAIN = new ResourceLocation(TEXTURES + "tintable_main.png");
    private static final ResourceLocation TINTABLE_ACCENT = new ResourceLocation(TEXTURES + "tintable_accent.png");
    private static final Map<StorageTier, ResourceLocation> TIER_TEXTURES = new EnumMap<>(StorageTier.class);
    private static final ModelShulker MODEL = new ModelShulker();

    static {
        for (StorageTier tier : StorageTier.values()) {
            String name = tier == StorageTier.WOOD ? "base_tier" : tier.getModelPrefix() + "tier";
            TIER_TEXTURES.put(tier, new ResourceLocation(TEXTURES + name + ".png"));
        }
    }

    @Override
    public void render(ShulkerBoxTileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        IBlockState state = te.hasWorld() ? te.getWorld().getBlockState(te.getPos()) : null;
        EnumFacing facing = state != null && state.getBlock() instanceof ShulkerBoxBlock ? state.getValue(ShulkerBoxBlock.FACING) : EnumFacing.UP;
        float progress = te.getProgress(partialTicks);
        renderShulkerBox(this, x, y, z, facing, progress, te.getMainColor(), te.getAccentColor(), te.getTier(), destroyStage);
        if (te.hasWorld() && progress <= 0.0F) {
            // Items show on the closed lid's face; the original also turns them with the opening lid.
            BarrelDisplayItemRenderer.renderDisplayItems(te, x, y, z, facing, 0, 0.5F);
        }
    }

    static void renderShulkerBox(@Nullable ShulkerBoxRenderer renderer, double x, double y, double z, EnumFacing facing, float progress,
                                 int mainColor, int accentColor, StorageTier tier, int destroyStage) {
        boolean hasMain = mainColor != StorageItemData.NO_COLOR;
        boolean hasAccent = accentColor != StorageItemData.NO_COLOR;
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);
        GlStateManager.disableCull();
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        GlStateManager.translate(0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.9995F, 0.9995F, 0.9995F);
        GlStateManager.translate(0.0F, -1.0F, 0.0F);
        switch (facing) {
            case DOWN:
                GlStateManager.translate(0.0F, 2.0F, 0.0F);
                GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                break;
            case NORTH:
                GlStateManager.translate(0.0F, 1.0F, 1.0F);
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
                break;
            case SOUTH:
                GlStateManager.translate(0.0F, 1.0F, -1.0F);
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                break;
            case WEST:
                GlStateManager.translate(-1.0F, 1.0F, 0.0F);
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                break;
            case EAST:
                GlStateManager.translate(1.0F, 1.0F, 0.0F);
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                break;
            case UP:
            default:
                break;
        }

        if (destroyStage >= 0 && renderer != null) {
            renderer.bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 4.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
            renderModel(progress);
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        } else {
            int n = 0;
            if (!hasMain || !hasAccent) {
                beginLayer(n);
                layer(NO_TINT, -1, progress);
                endLayer(n++);
            }
            if (hasMain) {
                beginLayer(n);
                layer(TINTABLE_MAIN, mainColor, progress);
                endLayer(n++);
            }
            if (hasAccent) {
                beginLayer(n);
                layer(TINTABLE_ACCENT, accentColor, progress);
                endLayer(n++);
            }
            beginLayer(n);
            layer(TIER_TEXTURES.get(tier), -1, progress);
            endLayer(n);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableCull();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    /**
     * The layers (wood, tints, tier trim) share one geometry. Equal depth alone decides which one
     * shows, and OptiFine shaders don't keep that stable: the trim z-fights. Each layer after the
     * first is nudged towards the camera in depth instead (n is the layer's place in the stack).
     */
    private static void beginLayer(int n) {
        if (n > 0) {
            GlStateManager.enablePolygonOffset();
            GlStateManager.doPolygonOffset(-1.0F * n, -10.0F * n);
        }
    }

    private static void endLayer(int n) {
        if (n > 0) {
            GlStateManager.doPolygonOffset(0.0F, 0.0F);
            GlStateManager.disablePolygonOffset();
        }
    }

    private static void layer(ResourceLocation texture, int color, float progress) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        if (color >= 0) {
            GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
        } else {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        renderModel(progress);
    }

    private static void renderModel(float progress) {
        MODEL.base.render(0.0625F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, -progress * 0.5F, 0.0F);
        GlStateManager.rotate(270.0F * progress, 0.0F, 1.0F, 0.0F);
        MODEL.lid.render(0.0625F);
        GlStateManager.popMatrix();
    }
}
