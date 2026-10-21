/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.render;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.ChestBlock;
import com.exiledradio.sstoragelegacy.block.ChestTileEntity;
import com.exiledradio.sstoragelegacy.block.StorageTier;
import com.exiledradio.sstoragelegacy.block.StorageTileEntity;
import com.exiledradio.sstoragelegacy.block.WoodType;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * The original's chest: a 14x10x14 body, a 14x5x14 lid and a lock, drawn in layers from its
 * textures: the wood (unless both colours are dyed), the tinted main and accent overlays, the
 * tier trim, the lock (unless an item is displayed), and packing tape over all of it. The model
 * keeps the original's Y-up coordinates, so its UVs match the original's textures.
 *
 * <p>Each half of a double chest is 15 pixels wide towards the other half, with its own left_ or
 * right_ textures and half the latch; the main half also draws the display item, the lock icon
 * and the upgrades, centred on (or running along) the whole front.
 */
public class ChestRenderer extends TileEntitySpecialRenderer<ChestTileEntity> {
    private static final String TEXTURES = SophisticatedStorageLegacy.MODID + ":textures/entity/chest/";
    /** Display items sit centred a little below the middle of the chest's front (the original's 0.5 * 14.01 / 16). */
    private static final float DISPLAY_CENTER_Y = 0.5F * (14.01F / 16F);

    private static final Map<ChestBlock.Type, Textures> TEXTURES_BY_TYPE = new EnumMap<>(ChestBlock.Type.class);
    private static final Map<ChestBlock.Type, Model> MODELS = new EnumMap<>(ChestBlock.Type.class);

    static {
        for (ChestBlock.Type type : ChestBlock.Type.values()) {
            TEXTURES_BY_TYPE.put(type, new Textures(type == ChestBlock.Type.SINGLE ? "" : type.getName() + "_"));
            MODELS.put(type, new Model(type));
        }
    }

    /** One chest type's textures: plain names for a single chest, left_ / right_ for the halves. */
    private static final class Textures {
        final ResourceLocation tintableMain;
        final ResourceLocation tintableAccent;
        final ResourceLocation packed;
        final Map<WoodType, ResourceLocation> woods = new EnumMap<>(WoodType.class);
        final Map<StorageTier, ResourceLocation> tiers = new EnumMap<>(StorageTier.class);

        Textures(String prefix) {
            tintableMain = new ResourceLocation(TEXTURES + prefix + "tintable_main.png");
            tintableAccent = new ResourceLocation(TEXTURES + prefix + "tintable_accent.png");
            packed = new ResourceLocation(TEXTURES + prefix + "packed.png");
            for (WoodType woodType : WoodType.values()) {
                woods.put(woodType, new ResourceLocation(TEXTURES + prefix + woodType.getName() + ".png"));
            }
            for (StorageTier tier : StorageTier.values()) {
                String name = tier == StorageTier.WOOD ? "wood_tier" : tier.getModelPrefix() + "tier";
                tiers.put(tier, new ResourceLocation(TEXTURES + prefix + name + ".png"));
            }
        }
    }

    @Override
    public void render(ChestTileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        IBlockState state = te.hasWorld() ? te.getWorld().getBlockState(te.getPos()) : null;
        boolean isChest = state != null && state.getBlock() instanceof ChestBlock;
        EnumFacing facing = isChest ? state.getValue(ChestBlock.FACING) : EnumFacing.SOUTH;
        ChestBlock.Type type = isChest ? state.getValue(ChestBlock.TYPE) : ChestBlock.Type.SINGLE;
        if (isChest && type != ChestBlock.Type.SINGLE) {
            // Both halves take the brighter light of the two (vanilla's BrightnessCombiner, 1.15+):
            // lit per block, the halves show a seam wherever light changes across them (lava, torches).
            int own = te.getWorld().getCombinedLight(te.getPos(), 0);
            int other = te.getWorld().getCombinedLight(te.getPos().offset(ChestBlock.getConnectedDirection(state)), 0);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, Math.max(own % 65536, other % 65536), Math.max(own / 65536, other / 65536));
        }
        // The latch is hidden on both halves when the main half shows an item on the front, as in the original.
        StorageTileEntity main = te.hasWorld() ? te.getMainTile() : te;
        renderChest(this, x, y, z, facing, type, te.getLidAngle(partialTicks), te.getWoodType(), te.getMainColor(), te.getAccentColor(),
                te.getTier(), te.isPacked(), !main.getDisplayItems().isEmpty()
                        && main.getDisplayItems().get(0).getDisplaySide() == com.exiledradio.scorelegacy.settings.itemdisplay.DisplaySide.FRONT, te.shouldShowTier() ? 1.0F
                        : StorageRenderState.holdsItemThatShowsHiddenTiers() ? 0.5F : 0.0F, destroyStage);
        if (te.hasWorld() && te.isMainChest()) {
            double centerShift = type == ChestBlock.Type.RIGHT ? -0.5 : type == ChestBlock.Type.LEFT ? 0.5 : 0;
            double upgradesShift = type == ChestBlock.Type.LEFT ? 1 : 0;
            BarrelDisplayItemRenderer.renderDisplayItems(te, x, y, z, facing, 1 / 16.0, DISPLAY_CENTER_Y, centerShift, upgradesShift);
        }
    }

    /**
     * Draws a chest (or one half of a double chest) at (x, y, z). Used by the placed chest and, with
     * no renderer, by the item.
     *
     * @param lidAngle  0 closed to 1 open
     * @param tierAlpha 1 shows the tier trim, 0 hides it (the storage tool), 0.5 shows a hidden one faded
     */
    static void renderChest(@Nullable ChestRenderer renderer, double x, double y, double z, EnumFacing facing, ChestBlock.Type type, float lidAngle,
                            @Nullable WoodType woodType, int mainColor, int accentColor, StorageTier tier, boolean packed,
                            boolean showsItem, float tierAlpha, int destroyStage) {
        Textures textures = TEXTURES_BY_TYPE.get(type);
        Model model = MODELS.get(type);
        boolean hasMain = mainColor != StorageItemData.NO_COLOR;
        boolean hasAccent = accentColor != StorageItemData.NO_COLOR;
        float eased = 1.0F - lidAngle;
        eased = 1.0F - eased * eased * eased;

        GlStateManager.enableDepth();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
        // The original turns its model by -facing.toYRot(); 1.12's horizontal angle is the same number.
        GlStateManager.rotate(-facing.getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5, -0.5, -0.5);

        if (destroyStage >= 0 && renderer != null) {
            renderer.bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 4.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
            model.render(eased, true, false);
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        } else {
            int n = 0;
            if (woodType != null || !(hasMain && hasAccent)) {
                beginLayer(n);
                layer(model, textures.woods.get(woodType != null ? woodType : WoodType.DEFAULT), -1, eased, false);
                endLayer(n++);
            }
            if (hasMain) {
                beginLayer(n);
                layer(model, textures.tintableMain, mainColor, eased, false);
                endLayer(n++);
            }
            if (hasAccent) {
                beginLayer(n);
                layer(model, textures.tintableAccent, accentColor, eased, false);
                endLayer(n++);
            }
            beginLayer(n);
            ResourceLocation tierTexture = textures.tiers.get(tier);
            if (tierAlpha >= 1.0F) {
                layer(model, tierTexture, -1, eased, true, !showsItem);
                endLayer(n);
            } else {
                // The latch stays when the trim is hidden, as in the original.
                layer(model, tierTexture, -1, eased, false, !showsItem);
                endLayer(n);
                if (tierAlpha > 0.0F) {
                    renderFadedTier(model, tierTexture, eased, tierAlpha);
                }
            }
            if (packed) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(-0.005, -0.005, -0.005);
                GlStateManager.scale(1.01F, 1.01F, 1.01F);
                layer(model, textures.packed, -1, eased, false);
                GlStateManager.popMatrix();
            }
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
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

    /** A hidden tier trim, slightly larger and see-through, while holding the tool in tier mode or a tier upgrade. */
    private static void renderFadedTier(Model model, ResourceLocation texture, float lidAngle, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.005, -0.005, -0.005);
        GlStateManager.scale(1.01F, 1.01F, 1.01F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        model.render(lidAngle, true, false);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void layer(Model model, ResourceLocation texture, int color, float lidAngle, boolean withLock) {
        layer(model, texture, color, lidAngle, true, withLock);
    }

    private static void layer(Model model, ResourceLocation texture, int color, float lidAngle, boolean withBody, boolean withLock) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        if (color >= 0) {
            GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
        } else {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        model.render(lidAngle, withBody, withLock);
    }

    /**
     * The original's chest layers: bottom, lid and lock, in its (Y-up) pixel coordinates. A single
     * chest is 14 wide with a 2-wide latch; each double half is 15 wide towards the other half,
     * with a 1-wide half latch at that edge (createDoubleBodyLeftLayer / RightLayer).
     */
    private static class Model extends ModelBase {
        private final ModelRenderer bottom;
        private final ModelRenderer lid;
        private final ModelRenderer lock;

        Model(ChestBlock.Type type) {
            textureWidth = 64;
            textureHeight = 64;
            bottom = new ModelRenderer(this, 0, 19);
            lid = new ModelRenderer(this, 0, 0);
            lock = new ModelRenderer(this, 0, 0);
            lid.setRotationPoint(0.0F, 9.0F, 1.0F);
            if (type == ChestBlock.Type.SINGLE) {
                bottom.addBox(1.0F, 0.0F, 1.0F, 14, 10, 14);
                lid.addBox(1.0F, 0.0F, 0.0F, 14, 5, 14);
                lock.addBox(7.0F, -1.0F, 15.0F, 2, 4, 1);
                lock.setRotationPoint(0.0F, 8.0F, 0.0F);
            } else {
                float x = type == ChestBlock.Type.RIGHT ? 1.0F : 0.0F;
                bottom.addBox(x, 0.0F, 1.0F, 15, 10, 14);
                lid.addBox(x, 0.0F, 0.0F, 15, 5, 14);
                lock.addBox(type == ChestBlock.Type.RIGHT ? 15.0F : 0.0F, -2.0F, 14.0F, 1, 4, 1);
                lock.setRotationPoint(0.0F, 9.0F, 1.0F);
            }
        }

        void render(float lidAngle, boolean withBody, boolean withLock) {
            lid.rotateAngleX = -(lidAngle * ((float) Math.PI / 2F));
            if (withBody) {
                lid.render(0.0625F);
                bottom.render(0.0625F);
            }
            if (withLock) {
                lock.rotateAngleX = lid.rotateAngleX;
                lock.render(0.0625F);
            }
        }
    }
}
