/*
 * Written for Sophisticated Storage Legacy by ExiledRadio, GPLv3, after the original 1.20 Core's ColorPicker.
 */
package com.exiledradio.sstoragelegacy.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.EnumDyeColor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.function.IntConsumer;

/**
 * A colour picker drawn over a screen (the original 1.20 Core's ColorPicker, rewritten for 1.12):
 * a saturation/brightness square, a hue bar, a preview, a hex field, the 16 dye colours, and
 * buttons to clear the colour (none), cancel or confirm. The screen routes its input here while it
 * is open.
 */
public class ColorPicker {
    public static final int WIDTH = 150;
    public static final int HEIGHT = 96;
    private static final int SQUARE = 64;
    private static final int HUE_WIDTH = 8;
    private static final int SWATCH = 9;
    private static final int BUTTON_HEIGHT = 12;

    private final int x;
    private final int y;
    private final IntConsumer onDone;
    private final GuiTextField hexField;
    private float hue;
    private float saturation;
    private float brightness;
    /** -1 when "none" is chosen. */
    private int color;
    private boolean draggingSquare = false;
    private boolean draggingHue = false;

    public ColorPicker(int x, int y, int color, IntConsumer onDone) {
        this.x = x;
        this.y = y;
        this.onDone = onDone;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        hexField = new GuiTextField(0, font, x + 80, y + 24, 64, 12);
        hexField.setMaxStringLength(7);
        setColor(color);
    }

    private void setColor(int newColor) {
        color = newColor;
        if (newColor != -1) {
            float[] hsb = Color.RGBtoHSB(newColor >> 16 & 255, newColor >> 8 & 255, newColor & 255, null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
            hexField.setText(String.format("#%06X", newColor & 0xFFFFFF));
        } else {
            hexField.setText("");
        }
    }

    private void updateFromHsb() {
        color = Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
        hexField.setText(String.format("#%06X", color));
    }

    // ---- Drawing ---------------------------------------------------------------------------

    public void draw(int mouseX, int mouseY) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        Gui.drawRect(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xFF000000);
        Gui.drawRect(x, y, x + WIDTH, y + HEIGHT, 0xFFC6C6C6);
        drawSquare(x + 4, y + 4);
        drawHueBar(x + 4 + SQUARE + 4, y + 4);
        // Markers.
        int markerX = x + 4 + Math.round(saturation * (SQUARE - 1));
        int markerY = y + 4 + Math.round((1 - brightness) * (SQUARE - 1));
        Gui.drawRect(markerX - 1, markerY - 1, markerX + 2, markerY + 2, 0xFFFFFFFF);
        int hueY = y + 4 + Math.round(hue * (SQUARE - 1));
        Gui.drawRect(x + 4 + SQUARE + 3, hueY - 1, x + 4 + SQUARE + 4 + HUE_WIDTH + 1, hueY + 1, 0xFFFFFFFF);
        // Preview and hex.
        int previewX = x + 80;
        Gui.drawRect(previewX, y + 4, previewX + 64, y + 20, 0xFF000000);
        if (color != -1) {
            Gui.drawRect(previewX + 1, y + 5, previewX + 63, y + 19, 0xFF000000 | color);
        } else {
            Minecraft.getMinecraft().fontRenderer.drawString(I18n.format("gui.sstoragelegacy.color_picker.none"), previewX + 4, y + 8, 0xFFFFFF);
        }
        hexField.drawTextBox();
        // Dye swatches.
        for (EnumDyeColor dye : EnumDyeColor.values()) {
            int sx = previewX + (dye.getMetadata() % 8) * (SWATCH - 1);
            int sy = y + 40 + (dye.getMetadata() / 8) * (SWATCH - 1);
            Gui.drawRect(sx, sy, sx + SWATCH, sy + SWATCH, 0xFF000000);
            Gui.drawRect(sx + 1, sy + 1, sx + SWATCH - 1, sy + SWATCH - 1, 0xFF000000 | dye.getColorValue());
        }
        // Buttons: none, cancel, confirm.
        drawButton(x + 4, y + HEIGHT - BUTTON_HEIGHT - 4, 44, "gui.sstoragelegacy.color_picker.none", mouseX, mouseY);
        drawButton(x + 52, y + HEIGHT - BUTTON_HEIGHT - 4, 46, "gui.cancel", mouseX, mouseY);
        drawButton(x + 102, y + HEIGHT - BUTTON_HEIGHT - 4, 44, "gui.done", mouseX, mouseY);
        GlStateManager.enableDepth();
    }

    private void drawButton(int bx, int by, int width, String key, int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + width && mouseY >= by && mouseY < by + BUTTON_HEIGHT;
        Gui.drawRect(bx, by, bx + width, by + BUTTON_HEIGHT, 0xFF000000);
        Gui.drawRect(bx + 1, by + 1, bx + width - 1, by + BUTTON_HEIGHT - 1, hovered ? 0xFF8B8B8B : 0xFF6F6F6F);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String text = I18n.format(key);
        font.drawStringWithShadow(text, bx + (width - font.getStringWidth(text)) / 2F, by + 2, 0xFFFFFF);
    }

    /** White to the hue left to right, darkening to black downwards. */
    private void drawSquare(int sx, int sy) {
        int hueColor = Color.HSBtoRGB(hue, 1, 1);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, sx, sy, 0xFFFFFF, 255);
        vertex(buffer, sx, sy + SQUARE, 0xFFFFFF, 255);
        vertex(buffer, sx + SQUARE, sy + SQUARE, hueColor, 255);
        vertex(buffer, sx + SQUARE, sy, hueColor, 255);
        tessellator.draw();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, sx, sy, 0, 0);
        vertex(buffer, sx, sy + SQUARE, 0, 255);
        vertex(buffer, sx + SQUARE, sy + SQUARE, 0, 255);
        vertex(buffer, sx + SQUARE, sy, 0, 0);
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    private void drawHueBar(int hx, int hy) {
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int steps = 6;
        for (int i = 0; i < steps; i++) {
            int top = Color.HSBtoRGB(i / (float) steps, 1, 1);
            int bottom = Color.HSBtoRGB((i + 1) / (float) steps, 1, 1);
            float y0 = hy + SQUARE * i / (float) steps;
            float y1 = hy + SQUARE * (i + 1) / (float) steps;
            vertex(buffer, hx, y0, top, 255);
            vertex(buffer, hx, y1, bottom, 255);
            vertex(buffer, hx + HUE_WIDTH, y1, bottom, 255);
            vertex(buffer, hx + HUE_WIDTH, y0, top, 255);
        }
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    private static void vertex(BufferBuilder buffer, float vx, float vy, int rgb, int alpha) {
        buffer.pos(vx, vy, 0).color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, alpha).endVertex();
    }

    // ---- Input -----------------------------------------------------------------------------

    public void mouseClicked(int mouseX, int mouseY, int button) {
        hexField.mouseClicked(mouseX, mouseY, button);
        if (inside(mouseX, mouseY, x + 4, y + 4, SQUARE, SQUARE)) {
            draggingSquare = true;
            dragSquare(mouseX, mouseY);
        } else if (inside(mouseX, mouseY, x + 4 + SQUARE + 4, y + 4, HUE_WIDTH, SQUARE)) {
            draggingHue = true;
            dragHue(mouseY);
        }
        for (EnumDyeColor dye : EnumDyeColor.values()) {
            int sx = x + 80 + (dye.getMetadata() % 8) * (SWATCH - 1);
            int sy = y + 40 + (dye.getMetadata() / 8) * (SWATCH - 1);
            if (inside(mouseX, mouseY, sx, sy, SWATCH, SWATCH)) {
                setColor(dye.getColorValue());
            }
        }
        int buttonY = y + HEIGHT - BUTTON_HEIGHT - 4;
        if (inside(mouseX, mouseY, x + 4, buttonY, 44, BUTTON_HEIGHT)) {
            onDone.accept(-1);
        } else if (inside(mouseX, mouseY, x + 52, buttonY, 46, BUTTON_HEIGHT)) {
            onDone.accept(Integer.MIN_VALUE);
        } else if (inside(mouseX, mouseY, x + 102, buttonY, 44, BUTTON_HEIGHT)) {
            onDone.accept(color);
        }
    }

    public void mouseDragged(int mouseX, int mouseY) {
        if (draggingSquare) {
            dragSquare(mouseX, mouseY);
        } else if (draggingHue) {
            dragHue(mouseY);
        }
    }

    public void mouseReleased() {
        draggingSquare = false;
        draggingHue = false;
    }

    private void dragSquare(int mouseX, int mouseY) {
        saturation = clamp((mouseX - (x + 4)) / (float) (SQUARE - 1));
        brightness = 1 - clamp((mouseY - (y + 4)) / (float) (SQUARE - 1));
        updateFromHsb();
    }

    private void dragHue(int mouseY) {
        hue = clamp((mouseY - (y + 4)) / (float) (SQUARE - 1));
        updateFromHsb();
    }

    /** Escape cancels; typing a full hex colour sets it. */
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            onDone.accept(Integer.MIN_VALUE);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            onDone.accept(color);
            return;
        }
        if (hexField.textboxKeyTyped(typedChar, keyCode)) {
            String text = hexField.getText().startsWith("#") ? hexField.getText().substring(1) : hexField.getText();
            if (text.length() == 6) {
                try {
                    int parsed = Integer.parseInt(text, 16);
                    String keep = hexField.getText();
                    setColor(parsed);
                    hexField.setText(keep);
                } catch (NumberFormatException e) {
                    // not a colour yet
                }
            }
        }
    }

    private static boolean inside(int mouseX, int mouseY, int rx, int ry, int width, int height) {
        return mouseX >= rx && mouseX < rx + width && mouseY >= ry && mouseY < ry + height;
    }

    private static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }
}
