/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.gui;

import com.exiledradio.scorelegacy.client.gui.controls.ButtonDefinition;
import com.exiledradio.scorelegacy.client.gui.controls.ButtonDefinitions;
import com.exiledradio.scorelegacy.client.gui.controls.CompositeWidget;
import com.exiledradio.scorelegacy.client.gui.controls.ToggleButton;
import com.exiledradio.scorelegacy.client.gui.controls.Widget;
import com.exiledradio.scorelegacy.client.gui.utils.Dimension;
import com.exiledradio.scorelegacy.client.gui.utils.GuiHelper;
import com.exiledradio.scorelegacy.client.gui.utils.Position;
import com.exiledradio.scorelegacy.client.gui.utils.UV;
import com.exiledradio.sstoragelegacy.common.gui.BlockSide;
import com.exiledradio.sstoragelegacy.common.gui.SideIOContainer;
import com.exiledradio.sstoragelegacy.upgrades.IOMode;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Six buttons laid out like an unfolded block (top; left, front, right; bottom, back), each
 * cycling what that side does (the original's SideIOControl).
 */
public class SideIOControl extends CompositeWidget<Widget> {
    private static final String BUTTONS = "gui.sstoragelegacy.upgrades.buttons.";
    public static final ButtonDefinition.Toggle<IOMode> IO_MODE = ButtonDefinitions.createToggleButtonDefinition(ImmutableMap.of(
            IOMode.PUSH, state(208, "io_mode_push"),
            IOMode.PULL, state(192, "io_mode_pull"),
            IOMode.PUSH_PULL, state(176, "io_mode_push_pull"),
            IOMode.OFF, state(160, "io_mode_off"),
            IOMode.DISABLED, state(224, "io_mode_disabled")));

    public SideIOControl(SideIOContainer container, Position position) {
        super(position);
        addSideButton(container, new Position(x + 18, y), BlockSide.TOP);
        addSideButton(container, new Position(x, y + 18), BlockSide.LEFT);
        addSideButton(container, new Position(x + 18, y + 18), BlockSide.FRONT);
        addSideButton(container, new Position(x + 36, y + 18), BlockSide.RIGHT);
        addSideButton(container, new Position(x + 18, y + 36), BlockSide.BOTTOM);
        addSideButton(container, new Position(x + 36, y + 36), BlockSide.BACK);
    }

    private static ToggleButton.StateData state(int u, String key) {
        return GuiHelper.getButtonStateData(new UV(u, 48), BUTTONS + key, Dimension.SQUARE_16, new Position(1, 1));
    }

    private void addSideButton(SideIOContainer container, Position position, BlockSide side) {
        addChild(new SideButton(position, container, side));
    }

    @Override
    protected void renderBg(int mouseX, int mouseY) {
        // Only the buttons.
    }

    @Override
    public int getWidth() {
        return 54;
    }

    @Override
    public int getHeight() {
        return 54;
    }

    /** A side's button; its tooltip also says which side and world direction it is. */
    private static class SideButton extends ToggleButton<IOMode> {
        private final SideIOContainer container;
        private final BlockSide side;

        SideButton(Position position, SideIOContainer container, BlockSide side) {
            super(position, IO_MODE, button -> container.toggleSideIO(side), () -> container.getSideIOMode(side));
            this.container = container;
            this.side = side;
        }

        @Override
        protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
            super.renderWidget(mouseX, mouseY, partialTicks);
            if (isMouseOver(mouseX, mouseY)) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add(I18n.format(BUTTONS + "io_mode_" + container.getSideIOMode(side).getName()));
                tooltip.add(TextFormatting.DARK_GRAY + I18n.format(BUTTONS + "io_mode_side_info",
                        I18n.format("gui.sstoragelegacy.block_side." + side.getName()),
                        I18n.format("gui.sstoragelegacy.direction." + container.toDirection(side).getName())));
                GuiHelper.setTooltipToRender(tooltip);
            }
        }
    }
}
