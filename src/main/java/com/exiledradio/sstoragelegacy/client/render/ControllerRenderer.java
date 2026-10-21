/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.render;

import com.exiledradio.sstoragelegacy.block.ControllerTileEntity;
import com.exiledradio.sstoragelegacy.item.StorageToolItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * While the storage tool in link mode is held (the original's ControllerRenderer): the blocks
 * linked to this controller in lime, with a line to each, and the controller in light blue when
 * it is the one the tool links to.
 */
public class ControllerRenderer extends TileEntitySpecialRenderer<ControllerTileEntity> {
    private static final float[] LIME = {0.5F, 0.8F, 0.1F};
    private static final float[] LIGHT_BLUE = {0.23F, 0.7F, 0.85F};

    @Override
    public void render(ControllerTileEntity controller, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        ItemStack tool = player == null ? ItemStack.EMPTY : getTool(player);
        if (tool.isEmpty() || StorageToolItem.getMode(tool) != StorageToolItem.Mode.LINK) {
            return;
        }
        BlockPos controllerPos = controller.getPos();
        GlStateManager.pushMatrix();
        // Draw in coordinates relative to the controller's corner.
        GlStateManager.translate(x, y, z);
        // The lightmap unit stays on in a TESR and would darken the colours to black.
        Minecraft.getMinecraft().entityRenderer.disableLightmap();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        if (controllerPos.equals(StorageToolItem.getControllerLink(tool))) {
            RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(0, 0, 0, 1, 1, 1).grow(0.002), LIGHT_BLUE[0], LIGHT_BLUE[1], LIGHT_BLUE[2], 1.0F);
        }
        for (BlockPos linkedPos : controller.getLinkedBlocks()) {
            IBlockState state = controller.getWorld().getBlockState(linkedPos);
            AxisAlignedBB box = state.getBoundingBox(controller.getWorld(), linkedPos)
                    .offset(linkedPos.getX() - controllerPos.getX(), linkedPos.getY() - controllerPos.getY(), linkedPos.getZ() - controllerPos.getZ());
            RenderGlobal.drawSelectionBoundingBox(box.grow(0.002), LIME[0], LIME[1], LIME[2], 1.0F);
            drawLine(new Vec3d(0.5, 0.5, 0.5), box.getCenter());
        }
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().entityRenderer.enableLightmap();
        GlStateManager.popMatrix();
    }

    private static ItemStack getTool(EntityPlayerSP player) {
        if (player.getHeldItemMainhand().getItem() instanceof StorageToolItem) {
            return player.getHeldItemMainhand();
        }
        return player.getHeldItemOffhand().getItem() instanceof StorageToolItem ? player.getHeldItemOffhand() : ItemStack.EMPTY;
    }

    private static void drawLine(Vec3d from, Vec3d to) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(1, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(from.x, from.y, from.z).color(LIME[0], LIME[1], LIME[2], 1.0F).endVertex();
        buffer.pos(to.x, to.y, to.z).color(LIME[0], LIME[1], LIME[2], 1.0F).endVertex();
        tessellator.draw();
    }

    @Override
    public boolean isGlobalRenderer(ControllerTileEntity te) {
        return true;
    }
}
