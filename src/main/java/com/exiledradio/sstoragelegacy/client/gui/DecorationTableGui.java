/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.gui;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.DecorationTableTileEntity;
import com.exiledradio.sstoragelegacy.common.gui.DecorationTableContainer;
import com.exiledradio.sstoragelegacy.item.BarrelMaterials;
import com.exiledradio.sstoragelegacy.item.PaintbrushItem;
import com.exiledradio.sstoragelegacy.network.DecorationTableMessage;
import com.exiledradio.sstoragelegacy.network.StoragePacketHandler;
import com.exiledradio.sstoragelegacy.util.DecorationHelper;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The decoration table's screen (the original 1.20's DecorationTableScreen): part slots with
 * their part icons and reuse arrows, dye slots, colour buttons that open a colour picker, a
 * turning preview of the result, and tooltips listing the parts the result takes and those the
 * table has stored.
 */
public class DecorationTableGui extends GuiContainer {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(SophisticatedStorageLegacy.MODID, "textures/gui/decoration_table.png");
    private static final ResourceLocation ELEMENTS = new ResourceLocation(SophisticatedStorageLegacy.MODID, "textures/gui/decoration_table_elements.png");
    private static final ResourceLocation EMPTY_MATERIAL = new ResourceLocation(SophisticatedStorageLegacy.MODID, "textures/item/empty_material_slot.png");
    private static final ResourceLocation[] EMPTY_DYES = {
            new ResourceLocation(SophisticatedStorageLegacy.MODID, "textures/item/empty_red_dye_slot.png"),
            new ResourceLocation(SophisticatedStorageLegacy.MODID, "textures/item/empty_green_dye_slot.png"),
            new ResourceLocation(SophisticatedStorageLegacy.MODID, "textures/item/empty_blue_dye_slot.png")};
    /** Part icons in the elements texture (u, v), by part slot, then main and accent tint. */
    private static final int[][] PART_ICONS = {{0, 0}, {0, 16}, {0, 32}, {0, 48}, {16, 16}, {16, 32}, {16, 48}};
    private static final String[] PART_NAMES = {"top_inner_trim", "top_trim", "side_trim", "bottom_trim", "top", "side", "bottom"};
    private static final int[] MAIN_TINT_ICON = {48, 48};
    private static final int[] ACCENT_TINT_ICON = {32, 48};
    private static final int[] STORED_PARTS_ICON = {32, 16};
    /** Preview rotations while hovering a part slot (x, y), as in the original. */
    private static final int[][] SLOT_ROTATIONS = {{90, 180}, {90, 180}, {0, 180}, {-90, 180}, {90, 180}, {0, 180}, {-90, 180}};

    private final DecorationTableContainer container;
    private final DecorationTableTileEntity tile;
    @Nullable
    private ColorPicker colorPicker = null;
    private List<ItemStack> previewStacks = new ArrayList<>();
    private int selectedPreview = 0;
    private float xRotation = 30;
    private float yRotation = 45;
    private float targetXRotation = 30;
    private float targetYRotation = 45;
    private long hoverRotationTime = 0;
    private String lastPreviewKey = "";

    public DecorationTableGui(DecorationTableContainer container) {
        super(container);
        this.container = container;
        this.tile = container.getTile();
        xSize = 250;
        ySize = 226;
    }

    // ---- Layout helpers --------------------------------------------------------------------

    private Slot slot(int index) {
        return container.getSlot(index);
    }

    private int previewX() {
        return guiLeft + slot(DecorationTableContainer.FIRST_DYE_SLOT + 2).xPos + 26;
    }

    private int previewY() {
        return guiTop + slot(DecorationTableContainer.FIRST_DYE_SLOT).yPos;
    }

    private int previewHeight() {
        return container.getResultSlot().yPos - slot(DecorationTableContainer.FIRST_DYE_SLOT).yPos + 20;
    }

    /** Where a part slot's reuse arrow is (vertical above the slot; the top core's points from the left), or null. */
    @Nullable
    private int[] arrowPosition(int partSlot) {
        if (DecorationTableTileEntity.getSlotInheritedFrom(partSlot) == -1) {
            return null;
        }
        Slot s = slot(partSlot);
        return partSlot == DecorationTableTileEntity.TOP_CORE_SLOT ? new int[] {guiLeft + s.xPos - 11, guiTop + s.yPos + 4, 1}
                : new int[] {guiLeft + s.xPos + 4, guiTop + s.yPos - 11, 0};
    }

    private int[] colorButton(boolean main) {
        Slot green = slot(DecorationTableContainer.FIRST_DYE_SLOT + 1);
        Slot row = slot(main ? DecorationTableTileEntity.TOP_TRIM_SLOT : DecorationTableTileEntity.SIDE_TRIM_SLOT);
        return new int[] {guiLeft + green.xPos - 1, guiTop + row.yPos - 1};
    }

    // ---- Drawing ---------------------------------------------------------------------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        updatePreview(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (colorPicker != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 400);
            colorPicker.draw(mouseX, mouseY);
            GlStateManager.popMatrix();
            return;
        }
        drawPreview();
        renderHoveredToolTip(mouseX, mouseY);
        drawWidgetTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        // Empty slot icons.
        for (int partSlot = 0; partSlot < 7; partSlot++) {
            if (!slot(partSlot).getHasStack() && tile.getInheritedItem(partSlot).isEmpty()) {
                drawIcon(EMPTY_MATERIAL, guiLeft + slot(partSlot).xPos, guiTop + slot(partSlot).yPos);
            }
        }
        for (int dye = 0; dye < 3; dye++) {
            Slot s = slot(DecorationTableContainer.FIRST_DYE_SLOT + dye);
            if (!s.getHasStack()) {
                drawIcon(EMPTY_DYES[dye], guiLeft + s.xPos, guiTop + s.yPos);
            }
            int tint = dye == 0 ? 0x33FF0000 : dye == 1 ? 0x3300FF00 : 0x330000FF;
            Gui.drawRect(guiLeft + s.xPos, guiTop + s.yPos, guiLeft + s.xPos + 16, guiTop + s.yPos + 16, tint);
        }
        // Part icons next to the part slots.
        mc.getTextureManager().bindTexture(ELEMENTS);
        GlStateManager.color(1, 1, 1, 1);
        for (int partSlot = 0; partSlot < 7; partSlot++) {
            Slot s = slot(partSlot);
            drawModalRectWithCustomSizedTexture(guiLeft + s.xPos + 18, guiTop + s.yPos, PART_ICONS[partSlot][0], PART_ICONS[partSlot][1], 16, 16, 64, 64);
            int[] arrow = arrowPosition(partSlot);
            if (arrow != null) {
                boolean hovered = inside(mouseX, mouseY, arrow[0], arrow[1], 8, 8);
                int rowV = arrow[2] == 1 ? 8 : 0;
                drawModalRectWithCustomSizedTexture(arrow[0], arrow[1], hovered ? 48 : 56, rowV, 8, 8, 64, 64);
                drawModalRectWithCustomSizedTexture(arrow[0], arrow[1], tile.isSlotMaterialInherited(partSlot) ? 32 : 40, rowV, 8, 8, 64, 64);
            }
        }
        // Colour buttons with their tint icons.
        for (boolean main : new boolean[] {true, false}) {
            int[] button = colorButton(main);
            int color = main ? tile.getMainColor() : tile.getAccentColor();
            Gui.drawRect(button[0], button[1], button[0] + 18, button[1] + 18, 0xFF373737);
            Gui.drawRect(button[0] + 1, button[1] + 1, button[0] + 17, button[1] + 17, color == -1 ? 0xFF8B8B8B : 0xFF000000 | color);
            if (color == -1) {
                fontRenderer.drawString("-", button[0] + 7, button[1] + 5, 0x404040);
            }
            mc.getTextureManager().bindTexture(ELEMENTS);
            GlStateManager.color(1, 1, 1, 1);
            int[] icon = main ? MAIN_TINT_ICON : ACCENT_TINT_ICON;
            drawModalRectWithCustomSizedTexture(button[0] + 19, button[1] + 1, icon[0], icon[1], 16, 16, 64, 64);
        }
        // Stored parts.
        if (!tile.getPartsStored().isEmpty()) {
            Slot s = slot(DecorationTableTileEntity.TOP_INNER_TRIM_SLOT);
            drawModalRectWithCustomSizedTexture(guiLeft + s.xPos + 57, guiTop + s.yPos, STORED_PARTS_ICON[0], STORED_PARTS_ICON[1], 16, 16, 64, 64);
        }
        // The preview's black box.
        Gui.drawRect(previewX(), previewY(), previewX() + 80, previewY() + previewHeight(), 0xFF000000);
        // Inherited blocks, faded, in empty slots that reuse them.
        RenderHelper.enableGUIStandardItemLighting();
        for (int partSlot = 0; partSlot < 7; partSlot++) {
            Slot s = slot(partSlot);
            ItemStack inherited = s.getHasStack() ? ItemStack.EMPTY : tile.getInheritedItem(partSlot);
            if (!inherited.isEmpty()) {
                itemRender.renderItemAndEffectIntoGUI(inherited, guiLeft + s.xPos, guiTop + s.yPos);
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                Gui.drawRect(guiLeft + s.xPos, guiTop + s.yPos, guiLeft + s.xPos + 16, guiTop + s.yPos + 16, 0x99C6C6C6);
                GlStateManager.enableDepth();
            }
        }
        RenderHelper.disableStandardItemLighting();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("tile.sstoragelegacy.decoration_table.name"), 8, 6, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"), 45, slot(DecorationTableTileEntity.BOTTOM_TRIM_SLOT).yPos + 20, 0x404040);
    }

    private void drawIcon(ResourceLocation texture, int ix, int iy) {
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        drawModalRectWithCustomSizedTexture(ix, iy, 0, 0, 16, 16, 16, 16);
    }

    // ---- Preview ---------------------------------------------------------------------------

    private void updatePreview(int mouseX, int mouseY) {
        StringBuilder key = new StringBuilder();
        for (int index = 0; index < DecorationTableContainer.RESULT_SLOT; index++) {
            key.append(slot(index).getStack()).append(slot(index).getStack().getTagCompound());
        }
        key.append(tile.getMainColor()).append(tile.getAccentColor());
        for (int partSlot = 0; partSlot < 7; partSlot++) {
            key.append(tile.isSlotMaterialInherited(partSlot));
        }
        if (!key.toString().equals(lastPreviewKey)) {
            lastPreviewKey = key.toString();
            previewStacks = tile.getDecoratedPreviewStacks();
            selectedPreview = Math.min(selectedPreview, Math.max(0, previewStacks.size() - 1));
        }
        for (int partSlot = 0; partSlot < 7; partSlot++) {
            Slot s = slot(partSlot);
            if (inside(mouseX, mouseY, guiLeft + s.xPos, guiTop + s.yPos, 34, 16)) {
                targetXRotation = SLOT_ROTATIONS[partSlot][0];
                targetYRotation = SLOT_ROTATIONS[partSlot][1];
                hoverRotationTime = System.currentTimeMillis();
            }
        }
        if (hoverRotationTime != 0 && System.currentTimeMillis() - hoverRotationTime > 1000) {
            targetXRotation = 30;
            targetYRotation = 45;
            hoverRotationTime = 0;
        }
        xRotation += (targetXRotation - xRotation) * 0.15F;
        yRotation += (targetYRotation - yRotation) * 0.15F;
    }

    private void drawPreview() {
        if (previewStacks.isEmpty()) {
            return;
        }
        ItemStack stack = previewStacks.get(selectedPreview);
        int buttonsHeight = previewStacks.size() > 1 ? 20 : 0;
        GlStateManager.pushMatrix();
        GlStateManager.translate(previewX() + 40, previewY() + (previewHeight() - buttonsHeight) / 2F, 150);
        GlStateManager.scale(48, -48, 48);
        GlStateManager.rotate(-xRotation, 1, 0, 0);
        GlStateManager.rotate(yRotation, 0, 1, 0);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        mc.getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        if (previewStacks.size() > 1) {
            RenderHelper.enableGUIStandardItemLighting();
            int bx = previewX() + (80 - previewStacks.size() * 20) / 2;
            int by = previewY() + previewHeight() - 19;
            for (int i = 0; i < previewStacks.size(); i++) {
                itemRender.renderItemAndEffectIntoGUI(previewStacks.get(i), bx + i * 20 + 1, by + 1);
            }
            RenderHelper.disableStandardItemLighting();
        }
    }

    // ---- Tooltips --------------------------------------------------------------------------

    @Override
    public List<String> getItemToolTip(ItemStack stack) {
        List<String> tooltip = super.getItemToolTip(stack);
        Slot hovered = getSlotUnderMouse();
        if (hovered == container.getResultSlot() && !(stack.getItem() instanceof PaintbrushItem)) {
            addPartCounts(tile.getPartsNeeded(), tooltip, key -> tile.getMissingDyes().contains(key) ? TextFormatting.RED : TextFormatting.DARK_GRAY);
        }
        return tooltip;
    }

    private void drawWidgetTooltips(int mouseX, int mouseY) {
        for (int partSlot = 0; partSlot < 7; partSlot++) {
            Slot s = slot(partSlot);
            if (inside(mouseX, mouseY, guiLeft + s.xPos + 18, guiTop + s.yPos, 16, 16)) {
                drawHoveringText(I18n.format("gui.sstoragelegacy.barrel_part." + PART_NAMES[partSlot]), mouseX, mouseY);
            }
            int[] arrow = arrowPosition(partSlot);
            if (arrow != null && inside(mouseX, mouseY, arrow[0], arrow[1], 8, 8)) {
                drawHoveringText(I18n.format("gui.sstoragelegacy.buttons.decoration_inheritance_" + (tile.isSlotMaterialInherited(partSlot) ? "on" : "off")), mouseX, mouseY);
            }
        }
        for (boolean main : new boolean[] {true, false}) {
            int[] button = colorButton(main);
            if (inside(mouseX, mouseY, button[0], button[1], 18, 18)) {
                drawHoveringText(I18n.format("gui.sstoragelegacy.buttons.pick_color"), mouseX, mouseY);
            } else if (inside(mouseX, mouseY, button[0] + 19, button[1] + 1, 16, 16)) {
                drawHoveringText(I18n.format(main ? "gui.sstoragelegacy.tint.main" : "gui.sstoragelegacy.tint.accent"), mouseX, mouseY);
            }
        }
        Slot innerTrim = slot(DecorationTableTileEntity.TOP_INNER_TRIM_SLOT);
        if (!tile.getPartsStored().isEmpty() && inside(mouseX, mouseY, guiLeft + innerTrim.xPos + 57, guiTop + innerTrim.yPos, 16, 16)) {
            List<String> lines = new ArrayList<>();
            lines.add(I18n.format("gui.sstoragelegacy.tooltip.parts_stored"));
            addPartCounts(tile.getPartsStored(), lines, key -> TextFormatting.GRAY);
            drawHoveringText(lines, mouseX, mouseY);
        }
    }

    /** "6/24 (25%) of Stone", sorted by item name (the original's addPartCountInfo). */
    private static void addPartCounts(Map<String, Integer> parts, List<String> tooltip, java.util.function.Function<String, TextFormatting> format) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(parts.entrySet());
        entries.sort(Comparator.comparing(e -> partItem(e.getKey()).getDisplayName()));
        for (Map.Entry<String, Integer> entry : entries) {
            ItemStack item = partItem(entry.getKey());
            if (item.isEmpty()) {
                continue;
            }
            int count = entry.getValue();
            tooltip.add(format.apply(entry.getKey()).toString() + count + "/" + DecorationHelper.BLOCK_TOTAL_PARTS + " ("
                    + Math.round((float) count / DecorationHelper.BLOCK_TOTAL_PARTS * 100) + "%) of " + item.getDisplayName());
        }
    }

    private static ItemStack partItem(String key) {
        return key.startsWith("dye") ? DecorationTableTileEntity.getDyeItem(key) : BarrelMaterials.getStack(key);
    }

    // ---- Input -----------------------------------------------------------------------------

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (colorPicker != null) {
            colorPicker.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        for (int partSlot = 0; partSlot < 7; partSlot++) {
            int[] arrow = arrowPosition(partSlot);
            if (arrow != null && inside(mouseX, mouseY, arrow[0], arrow[1], 8, 8)) {
                send(DecorationTableMessage.INHERITANCE, partSlot, tile.isSlotMaterialInherited(partSlot) ? 0 : 1);
                return;
            }
        }
        for (boolean main : new boolean[] {true, false}) {
            int[] button = colorButton(main);
            if (inside(mouseX, mouseY, button[0], button[1], 18, 18)) {
                openColorPicker(main);
                return;
            }
        }
        if (previewStacks.size() > 1) {
            int bx = previewX() + (80 - previewStacks.size() * 20) / 2;
            int by = previewY() + previewHeight() - 19;
            for (int i = 0; i < previewStacks.size(); i++) {
                if (inside(mouseX, mouseY, bx + i * 20, by, 18, 18)) {
                    selectedPreview = i;
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void openColorPicker(boolean main) {
        int current = main ? tile.getMainColor() : tile.getAccentColor();
        colorPicker = new ColorPicker(guiLeft + (xSize - ColorPicker.WIDTH) / 2, guiTop + (ySize - ColorPicker.HEIGHT) / 2, current, chosen -> {
            colorPicker = null;
            if (chosen != Integer.MIN_VALUE) {
                send(main ? DecorationTableMessage.MAIN_COLOR : DecorationTableMessage.ACCENT_COLOR, 0, chosen);
            }
        });
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (colorPicker != null) {
            colorPicker.mouseDragged(mouseX, mouseY);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (colorPicker != null) {
            colorPicker.mouseReleased();
            return;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (colorPicker != null) {
            colorPicker.keyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    /** Changes the table here at once and tells the server. */
    private void send(int action, int partSlot, int value) {
        DecorationTableMessage.apply(tile, action, partSlot, value);
        StoragePacketHandler.INSTANCE.sendToServer(new DecorationTableMessage(action, partSlot, value));
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
