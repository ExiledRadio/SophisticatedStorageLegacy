/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.render;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.BarrelBlock;
import com.exiledradio.sstoragelegacy.block.BarrelTileEntity;
import com.exiledradio.sstoragelegacy.block.StorageTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import com.exiledradio.scorelegacy.settings.itemdisplay.DisplaySide;

import java.util.List;

/**
 * Draws what a storage shows on its front, like the original's DisplayItemRenderer: display
 * items (one centred, two stacked, three as one over two, four in a square; flat 2D items
 * smaller than blocks, which stand out by half their depth), small upgrade icons along the
 * bottom, and the lock. The original bakes most of these into the block model; a TESR is
 * simpler on 1.12, and returns at once for barrels that show nothing.
 *
 * <p>The helpers work in the "front frame": {@link #rotateNorthTo} turned the storage so its
 * front faces north, and the origin moved to the front's bottom corner, so x and y run 0..1
 * across the face (x = 1 is the viewer's left) and the viewer looks from -z.
 */
public class BarrelDisplayItemRenderer extends TileEntitySpecialRenderer<BarrelTileEntity> {
    private static final float SMALL_3D_ITEM_SCALE = 0.5F;
    private static final float BIG_2D_ITEM_SCALE = 0.5F;
    private static final float SMALL_2D_ITEM_SCALE = 0.25F;
    /** Items sit around the face's middle; the original passes 0.5 for barrels. */
    static final float CENTER_Y = 0.5F;
    /** A block item in the FIXED transform is half a block deep; it stands out by half of that. */
    private static final float BLOCK_ITEM_HALF_DEPTH = 0.25F;
    /** How thick a block item stands out from the front once pressed flat: one pixel, as later originals show it. */
    static final float FLAT_BLOCK_THICKNESS = 1 / 16F;
    private static final float UPGRADE_ITEM_SCALE = 0.125F;
    private static final ResourceLocation LOCK_TEXTURE = new ResourceLocation(SophisticatedStorageLegacy.MODID, "textures/block/lock.png");
    private static final ResourceLocation UPGRADE_BASE = new ResourceLocation(SophisticatedStorageLegacy.MODID, "upgrade_base");

    @Override
    public void render(BarrelTileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        IBlockState state = te.getWorld().getBlockState(te.getPos());
        if (!(state.getBlock() instanceof BarrelBlock) || te.isPacked()) {
            return;
        }
        boolean showItems = !te.getDisplayItems().isEmpty();
        boolean showUpgrades = shouldShowUpgrades(te);
        if (!showItems && !showUpgrades) {
            return;
        }
        EnumFacing facing = state.getValue(BarrelBlock.FACING);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
        rotateNorthTo(facing);
        // The lid of a normal barrel is recessed one pixel; a flat top is flush.
        GlStateManager.translate(-0.5, -0.5, -(0.5 - (te.isFlatTop() ? 0 : 1 / 16.0)));
        applyFrontLight(te, facing);
        if (showItems) {
            renderDisplayItemsOnFront(te, CENTER_Y, 0);
        }
        if (showUpgrades) {
            renderUpgradesOnFront(te, te.isFlatTop() ? 0 : 1 / 16.0);
        }
        GlStateManager.popMatrix();
    }

    /**
     * Draws a storage's display items on its front face (chests).
     *
     * @param frontRecess how far the front face is inside the block's edge (chests: 1/16)
     * @param centerY     the height items centre on (barrels 0.5; chests a little lower)
     */
    public static void renderDisplayItems(StorageTileEntity te, double x, double y, double z, EnumFacing facing, double frontRecess, float centerY) {
        renderDisplayItems(te, x, y, z, facing, frontRecess, centerY, 0, 0);
    }

    /**
     * The same, moved across the front: a double chest draws its items and lock centred on both
     * halves, and its upgrades from the double chest's left edge (+x is the viewer's left).
     */
    public static void renderDisplayItems(StorageTileEntity te, double x, double y, double z, EnumFacing facing, double frontRecess, float centerY,
                                          double centerShift, double upgradesShift) {
        boolean showItems = !te.getDisplayItems().isEmpty();
        boolean showUpgrades = shouldShowUpgrades(te);
        boolean showLock = shouldShowLock(te);
        if (te.isPacked() || (!showItems && !showUpgrades && !showLock)) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
        rotateNorthTo(facing);
        GlStateManager.translate(-0.5, -0.5, -(0.5 - frontRecess));
        applyFrontLight(te, facing);
        if (showUpgrades) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(upgradesShift, 0, 0);
            renderUpgradesOnFront(te, 0);
            GlStateManager.popMatrix();
        }
        GlStateManager.translate(centerShift, 0, 0);
        if (showLock) {
            renderLockOnFront(te, 13F / 16F);
        }
        GlStateManager.popMatrix();
        if (showItems) {
            renderDisplayItemsOnSide(te, x, y, z, facing, frontRecess, centerY, centerShift);
        }
    }

    /**
     * The display items on the face their setting picks (the original ChestRenderer's
     * renderDisplayItem): the front, centred across a double chest, or one of a double chest's
     * ends (+x, the viewer's left, is LEFT). centerShift says where the other half is: below 0
     * it is on the viewer's right, above 0 on the left, 0 for a single storage.
     */
    private static void renderDisplayItemsOnSide(StorageTileEntity te, double x, double y, double z, EnumFacing facing, double frontRecess,
                                                 float centerY, double centerShift) {
        DisplaySide side = centerShift == 0 ? DisplaySide.FRONT : te.getDisplayItems().get(0).getDisplaySide();
        BlockPos lightPos = te.getPos().offset(facing);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
        rotateNorthTo(facing);
        if (side == DisplaySide.LEFT) {
            EnumFacing left = facing.rotateY();
            GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            BlockPos end = te.getPos();
            if (centerShift > 0) {
                GlStateManager.translate(0, 0, -1);
                end = end.offset(left);
            }
            lightPos = end.offset(left);
        } else if (side == DisplaySide.RIGHT) {
            EnumFacing right = facing.rotateYCCW();
            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            BlockPos end = te.getPos();
            if (centerShift < 0) {
                GlStateManager.translate(0, 0, -1);
                end = end.offset(right);
            }
            lightPos = end.offset(right);
        } else {
            GlStateManager.translate(centerShift, 0, 0);
        }
        GlStateManager.translate(-0.5, -0.5, -(0.5 - frontRecess));
        applyLight(te, lightPos);
        renderDisplayItemsOnFront(te, centerY, 0);
        GlStateManager.popMatrix();
    }

    /** Light from the block in front, not the storage's own position. */
    static void applyFrontLight(StorageTileEntity te, EnumFacing front) {
        applyLight(te, te.getPos().offset(front));
    }

    static void applyLight(StorageTileEntity te, BlockPos lightPos) {
        int light = te.getWorld().getCombinedLight(lightPos, 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536.0F);
    }

    /**
     * The display items, in the front frame.
     *
     * @param fixedSlotCount 0 to lay items out by how many there are; otherwise every item goes
     *                       where its slot is among this many (limited barrels)
     */
    static void renderDisplayItemsOnFront(StorageTileEntity te, float centerY, int fixedSlotCount) {
        List<StorageTileEntity.DisplayItem> displayItems = te.getDisplayItems();
        Minecraft mc = Minecraft.getMinecraft();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        int count = fixedSlotCount > 0 ? fixedSlotCount : displayItems.size();
        for (int i = 0; i < displayItems.size(); i++) {
            StorageTileEntity.DisplayItem displayItem = displayItems.get(i);
            int index = fixedSlotCount > 0 ? displayItem.getSlotIndex() : i;
            if (index >= count) {
                continue;
            }
            ItemStack stack = displayItem.getItem();
            IBakedModel model = mc.getRenderItem().getItemModelWithOverrides(stack, te.getWorld(), null);
            boolean is3d = model.isGui3d();
            float itemScale = count == 1 ? (is3d ? 1.0F : BIG_2D_ITEM_SCALE) : (is3d ? SMALL_3D_ITEM_SCALE : SMALL_2D_ITEM_SCALE);
            // Deviation from the 1.18 original (which stands block items out like an item frame):
            // blocks are pressed onto the face as a one-pixel-thick tile, as later versions do.
            // The FIXED cube is 2 * BLOCK_ITEM_HALF_DEPTH * itemScale deep; squash it to the set thickness.
            float depth = is3d ? FLAT_BLOCK_THICKNESS / (2 * BLOCK_ITEM_HALF_DEPTH * itemScale) : 1.0F;
            float outwards = is3d ? FLAT_BLOCK_THICKNESS / 2 + 0.001F : 0.0F;
            float[] front = getFrontOffset(index, count, centerY);

            GlStateManager.pushMatrix();
            GlStateManager.translate(front[0], front[1], -outwards);
            GlStateManager.rotate(displayItem.getRotation(), 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(itemScale, itemScale, itemScale * depth);
            if (is3d) {
                // The squash is non-uniform, so normals need renormalising or the faces shade wrong.
                GlStateManager.enableNormalize();
            }
            mc.getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
            if (is3d) {
                GlStateManager.disableNormalize();
            }
            GlStateManager.popMatrix();
        }
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    /** Whether the upgrades go on the front: turned on with the storage tool, or while holding the tool or an upgrade. */
    static boolean shouldShowUpgrades(StorageTileEntity te) {
        return te.shouldShowUpgrades() || StorageRenderState.holdsItemThatShowsUpgrades();
    }

    /**
     * One small icon per upgrade slot along the bottom, from the viewer's left; empty slots only
     * while holding something that shows them.
     *
     * @param outwards how far the outer surface is in front of the front frame (a barrel's recessed
     *                 lid: 1/16); the icons sit on the frame there, as in the original
     */
    static void renderUpgradesOnFront(StorageTileEntity te, double outwards) {
        Minecraft mc = Minecraft.getMinecraft();
        boolean renderEmptySlots = StorageRenderState.holdsItemThatShowsUpgrades();
        Item upgradeBase = Item.REGISTRY.getObject(UPGRADE_BASE);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        int i = 0;
        for (ItemStack upgrade : te.getRenderUpgradeItems()) {
            if (upgrade.isEmpty() && (!renderEmptySlots || upgradeBase == null)) {
                continue;
            }
            ItemStack toRender = upgrade.isEmpty() ? new ItemStack(upgradeBase) : upgrade;
            GlStateManager.pushMatrix();
            GlStateManager.translate(1F - i * 2 / 16F - 1 / 16F, 1 / 16F, -outwards - 0.001F);
            GlStateManager.scale(UPGRADE_ITEM_SCALE, UPGRADE_ITEM_SCALE, UPGRADE_ITEM_SCALE);
            mc.getRenderItem().renderItem(toRender, ItemCameraTransforms.TransformType.FIXED);
            GlStateManager.popMatrix();
            i++;
        }
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    /** A locked storage shows a lock unless the tool hid it; with the tool in a lock mode a hidden one shows faded. */
    static boolean shouldShowLock(StorageTileEntity te) {
        return te.isLocked() && (te.shouldShowLock() || StorageRenderState.holdsToolInToggleLockOrLockDisplay());
    }

    /** The lock, one pixel square, centred across the front at the given height. */
    static void renderLockOnFront(StorageTileEntity te, float yOffset) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(LOCK_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5 - 0.5 / 16.0, yOffset, -0.001);
        GlStateManager.scale(1 / 16F, 1 / 16F, 1 / 16F);
        drawQuad(0, 0, 1, 1, te.shouldShowLock() ? 1.0F : 0.5F);
        GlStateManager.popMatrix();
    }

    /**
     * A unit square in the xy plane facing the viewer (-z), with the texture the right way up
     * for them (the original's RenderHelper.renderQuad). Bind the texture first.
     */
    static void drawQuad(float minU, float minV, float maxU, float maxV, float alpha) {
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.003921569F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0, 1, 0).tex(maxU, minV).endVertex();
        buffer.pos(0, 0, 0).tex(maxU, maxV).endVertex();
        buffer.pos(1, 0, 0).tex(minU, maxV).endVertex();
        buffer.pos(1, 1, 0).tex(minU, minV).endVertex();
        tessellator.draw();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
    }

    /** From a north-facing front to the given facing (the original's getNorthBasedRotation). */
    static void rotateNorthTo(EnumFacing facing) {
        switch (facing) {
            case DOWN:
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                break;
            case UP:
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                break;
            case SOUTH:
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                break;
            case WEST:
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case EAST:
                GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                break;
            default:
                break;
        }
    }

    /** Where on the face (x, y from the corner) item number index of count goes. */
    static float[] getFrontOffset(int index, int count, float centerY) {
        float half = centerY / 2;
        if (count == 1) {
            return new float[] {0.5F, centerY};
        }
        if (count == 2) {
            return new float[] {0.5F, index == 0 ? centerY + half : half};
        }
        if (count == 3) {
            float xOffset = index == 0 ? 0.5F : 0.75F - (index - 1) * 0.5F;
            return new float[] {xOffset, index == 0 ? centerY + half : half};
        }
        return new float[] {index == 0 || index == 2 ? centerY + half : half, index == 0 || index == 1 ? centerY + half : half};
    }
}
