/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.gui;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelTileEntity;
import com.exiledradio.sstoragelegacy.common.gui.LimitedBarrelContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** A limited barrel's screen (the original's LimitedBarrelScreen): its slots framed like the front, each with a fill bar. */
public class LimitedBarrelGui extends StorageBlockGui {
    static final ResourceLocation GUI_BACKGROUNDS = new ResourceLocation(SophisticatedStorageLegacy.MODID, "textures/gui/limited_barrels.png");
    private static final int BACKGROUND_WIDTH = 84;
    private static final int BACKGROUND_HEIGHT = 82;
    private static final int BAR_WIDTH = 3;
    private static final int SMALL_BAR_HEIGHT = 28;
    private static final int LARGE_BAR_HEIGHT = 68;
    private static final int PERCENTAGE_COLOR = 0x2c2c2c;

    private final LimitedBarrelContainer container;

    public LimitedBarrelGui(LimitedBarrelContainer container) {
        super(container);
        this.container = container;
    }

    @Override
    protected void drawStorageSlotsBackground() {
        drawSlotsBackground(guiLeft, guiTop, xSize, container.getNumberOfInventorySlots());
    }

    /** The frame for 1 to 4 slots, centred under the title. */
    static void drawSlotsBackground(int guiLeft, int guiTop, int xSize, int slots) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(GUI_BACKGROUNDS);
        int index = Math.max(0, Math.min(3, slots - 1));
        int u = (index % 2) * BACKGROUND_WIDTH;
        int v = (index / 2) * BACKGROUND_HEIGHT;
        Gui.drawModalRectWithCustomSizedTexture(guiLeft + xSize / 2 - BACKGROUND_WIDTH / 2 - 1, guiTop + 17, u, v, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 256, 256);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        LimitedBarrelTileEntity tile = (LimitedBarrelTileEntity) container.getTile();
        int right = xSize / 2 + 37;
        int left = xSize / 2 - 37 - 5;
        int top = 18 + 6;
        int bottom = 18 + 6 + 40;
        switch (container.getNumberOfInventorySlots()) {
            case 1:
                renderBar(right, top, tile.getSlotFillPercentage(0), true, false);
                break;
            case 2:
                renderBar(right, top, tile.getSlotFillPercentage(0), false, false);
                renderBar(right, bottom, tile.getSlotFillPercentage(1), false, false);
                break;
            case 3:
                renderBar(right, top, tile.getSlotFillPercentage(0), false, false);
                renderBar(left, bottom, tile.getSlotFillPercentage(1), false, true);
                renderBar(right, bottom, tile.getSlotFillPercentage(2), false, false);
                break;
            default:
                renderBar(left, top, tile.getSlotFillPercentage(0), false, true);
                renderBar(right, top, tile.getSlotFillPercentage(1), false, false);
                renderBar(left, bottom, tile.getSlotFillPercentage(2), false, true);
                renderBar(right, bottom, tile.getSlotFillPercentage(3), false, false);
                break;
        }
    }

    /** A bar filling from the bottom, with the percentage beside it (on its left for left-hand bars). */
    private void renderBar(int x, int y, float percentage, boolean large, boolean left) {
        int height = large ? LARGE_BAR_HEIGHT : SMALL_BAR_HEIGHT;
        int barHeight = (int) (height * percentage);
        int yOffset = height - barHeight;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(GUI_BACKGROUNDS);
        Gui.drawModalRectWithCustomSizedTexture(x, y + yOffset, large ? 168 : 171, yOffset, BAR_WIDTH, barHeight, 256, 256);
        String text = (int) (percentage * 100) + "%";
        int textX = left ? x - 2 - fontRenderer.getStringWidth(text) : x + 6;
        fontRenderer.drawString(text, textX, y + height / 2 - 3, PERCENTAGE_COLOR);
    }

    @Override
    protected boolean shouldShowSortButtons() {
        return false;
    }
}
