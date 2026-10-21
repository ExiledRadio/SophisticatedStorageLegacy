/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.render;

import com.exiledradio.scorelegacy.util.CountAbbreviator;
import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelBlock;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelTileEntity;
import com.exiledradio.sstoragelegacy.block.VerticalFacing;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

/**
 * A limited barrel's front (the original's LimitedBarrelRenderer): each slot's item where the
 * slot is, its count below it in the slot's colour, fill bars at the edge, and the upgrades.
 */
public class LimitedBarrelRenderer extends TileEntitySpecialRenderer<LimitedBarrelTileEntity> {
    private static final ResourceLocation FILL_INDICATORS = new ResourceLocation(SophisticatedStorageLegacy.MODID, "textures/entity/fill_indicators.png");
    private static final float SINGLE_ITEM_FONT_SCALE = 1 / 48F;
    private static final float MULTIPLE_ITEMS_FONT_SCALE = 1 / 96F;
    /** Counts sit this far out from the recessed front of a normal (not flat-top) barrel (the original: 0.25 of a pixel). */
    private static final double COUNT_OUTWARDS = 0.25 / 16.0 + 0.001;

    @Override
    public void render(LimitedBarrelTileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        IBlockState state = te.getWorld().getBlockState(te.getPos());
        if (te.isPacked() || !(state.getBlock() instanceof LimitedBarrelBlock)) {
            return;
        }
        LimitedBarrelBlock block = (LimitedBarrelBlock) state.getBlock();
        boolean showItems = !te.getDisplayItems().isEmpty();
        boolean showCounts = te.shouldShowCounts();
        boolean showUpgrades = BarrelDisplayItemRenderer.shouldShowUpgrades(te);
        boolean showFillLevels = te.shouldShowFillLevels() || StorageRenderState.holdsItemThatShowsFillLevels();
        if (!showItems && !showCounts && !showUpgrades && !showFillLevels) {
            return;
        }
        EnumFacing front = block.getFacing(state);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
        BarrelDisplayItemRenderer.rotateNorthTo(state.getValue(LimitedBarrelBlock.FACING));
        VerticalFacing vertical = state.getValue(LimitedBarrelBlock.VERTICAL_FACING);
        if (vertical != VerticalFacing.NO) {
            BarrelDisplayItemRenderer.rotateNorthTo(vertical.getDirection());
        }
        GlStateManager.translate(-0.5, -0.5, -(0.5 - (te.isFlatTop() ? 0 : 1 / 16.0)));
        BarrelDisplayItemRenderer.applyFrontLight(te, front);

        if (showItems) {
            BarrelDisplayItemRenderer.renderDisplayItemsOnFront(te, BarrelDisplayItemRenderer.CENTER_Y, block.getSlotCount());
        }
        if (showCounts) {
            renderCounts(te, block.getSlotCount());
        }
        if (showUpgrades) {
            BarrelDisplayItemRenderer.renderUpgradesOnFront(te, te.isFlatTop() ? 0 : 1 / 16.0);
        }
        if (showFillLevels) {
            // Faded while only the tool (in fill level mode) shows them.
            renderFillLevels(te, block.getSlotCount(), !te.shouldShowFillLevels() && StorageRenderState.holdsToolInToggleFillLevelDisplay() ? 0.5F : 1.0F);
        }
        GlStateManager.popMatrix();
    }

    /** Whether the slot's display item is a block, which stands out from the front. */
    private static boolean showsBlockItem(LimitedBarrelTileEntity te, int slot) {
        for (com.exiledradio.sstoragelegacy.block.StorageTileEntity.DisplayItem displayItem : te.getDisplayItems()) {
            if (displayItem.getSlotIndex() == slot) {
                return Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(displayItem.getItem(), te.getWorld(), null).isGui3d();
            }
        }
        return false;
    }

    private void renderCounts(LimitedBarrelTileEntity te, int slots) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        float yOffset = -(slots == 1 ? 0.25F : 0.11F);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        for (int slot = 0; slot < slots; slot++) {
            int count = te.getSlotCount(slot);
            if (count <= 0) {
                continue;
            }
            float[] front = BarrelDisplayItemRenderer.getFrontOffset(slot, slots, BarrelDisplayItemRenderer.CENTER_Y);
            // An endless slot (infinity upgrade) shows a bigger "∞" a little higher, as in the original.
            boolean infinite = count == Integer.MAX_VALUE;
            float scale = (slots == 1 ? SINGLE_ITEM_FONT_SCALE : MULTIPLE_ITEMS_FONT_SCALE) * (infinite ? 2 : 1);
            String text = infinite ? "\u221E" : TextFormatting.BOLD + CountAbbreviator.abbreviate(count, slots == 1 ? 6 : 5);
            // Deviation: the original leaves the count under a block shown in the slot, whose bottom
            // edge then covers the top of the digits; here it goes just in front of the block.
            double outwards = (te.isFlatTop() ? 0.001 : COUNT_OUTWARDS)
                    + (showsBlockItem(te, slot) ? BarrelDisplayItemRenderer.FLAT_BLOCK_THICKNESS + 0.001 : 0);
            GlStateManager.pushMatrix();
            GlStateManager.translate(front[0], front[1] + (infinite ? yOffset / 1.8F : yOffset), -outwards);
            // Text runs to the viewer's right (-x) and down (-y), facing them (-z).
            GlStateManager.scale(-scale, -scale, -scale);
            font.drawString(text, -font.getStringWidth(text) / 2, 0, te.getSlotColor(slot));
            GlStateManager.popMatrix();
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** One bar per slot at its outer edge: tall for a single slot, short otherwise (the original's positions). */
    private void renderFillLevels(LimitedBarrelTileEntity te, int slots, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(FILL_INDICATORS);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, -0.001);
        switch (slots) {
            case 1:
                renderFillLevel(te.getSlotFillPercentage(0), 1 / 16F, 1 / 16F, true, alpha);
                break;
            case 2:
                renderFillLevel(te.getSlotFillPercentage(0), 1 / 16F, 9 / 16F, false, alpha);
                renderFillLevel(te.getSlotFillPercentage(1), 1 / 16F, 1 / 16F, false, alpha);
                break;
            case 3:
                renderFillLevel(te.getSlotFillPercentage(0), 1 / 16F, 9 / 16F, false, alpha);
                renderFillLevel(te.getSlotFillPercentage(1), 14 / 16F, 1 / 16F, false, alpha);
                renderFillLevel(te.getSlotFillPercentage(2), 1 / 16F, 1 / 16F, false, alpha);
                break;
            default:
                renderFillLevel(te.getSlotFillPercentage(0), 14 / 16F, 9 / 16F, false, alpha);
                renderFillLevel(te.getSlotFillPercentage(1), 1 / 16F, 9 / 16F, false, alpha);
                renderFillLevel(te.getSlotFillPercentage(2), 14 / 16F, 1 / 16F, false, alpha);
                renderFillLevel(te.getSlotFillPercentage(3), 1 / 16F, 1 / 16F, false, alpha);
                break;
        }
        GlStateManager.popMatrix();
    }

    private void renderFillLevel(float fillLevel, float x, float y, boolean large, float alpha) {
        if (fillLevel <= 0) {
            return;
        }
        int barHeight = large ? 14 : 6;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 1 / 16F / 5F, y + 1 / 16F / 5F, 0);
        GlStateManager.scale(1 / 16F / 5F * 3, fillLevel / 16F / 5F * (barHeight * 5 - 2), 1);
        float minU = large ? 0 : 3 / 128F;
        float maxV = large ? 68 / 128F : 28 / 128F;
        BarrelDisplayItemRenderer.drawQuad(minU, (1 - fillLevel) * maxV, minU + 3 / 128F, maxV, alpha);
        GlStateManager.popMatrix();
    }
}
