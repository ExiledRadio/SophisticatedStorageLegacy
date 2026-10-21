/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client;

import com.exiledradio.sstoragelegacy.block.LimitedBarrelBlock;
import com.exiledradio.sstoragelegacy.item.StorageToolItem;
import com.exiledradio.sstoragelegacy.network.ScrolledToolMessage;
import com.exiledradio.sstoragelegacy.network.StoragePacketHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Client input and HUD for limited barrels and the storage tool (the original's ClientEventHandler). */
public class ClientEventHandler {
    /** PlayerControllerMP.blockHitDelay. */
    private static final String BLOCK_HIT_DELAY = "field_78781_i";
    private static final int OVERLAY_COLOR = 0xFFFFFF;

    /**
     * Left-clicking a limited barrel's front takes items, so it shouldn't also start breaking it:
     * never in creative, and in survival not unless the tool breaks it quickly (an axe).
     */
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!event.getWorld().isRemote) {
            return;
        }
        IBlockState state = event.getWorld().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof LimitedBarrelBlock) || !((LimitedBarrelBlock) state.getBlock()).isLookingAtFront(player, event.getPos(), state)) {
            return;
        }
        if (player.capabilities.isCreativeMode) {
            event.setCanceled(true);
        } else if (player.getDigSpeed(state, event.getPos()) < 2) {
            event.setUseItem(Event.Result.DENY);
            // Without a delay the next tick's click would take another item at once.
            PlayerControllerMP controller = Minecraft.getMinecraft().playerController;
            ObfuscationReflectionHelper.setPrivateValue(PlayerControllerMP.class, controller, 5, BLOCK_HIT_DELAY);
        }
    }

    /**
     * Right-click in an inventory with a shulker box carried, or on a shulker box with items
     * carried, stashes them in the box (see StorageItemContents). Not in the creative inventory.
     */
    @SubscribeEvent
    public void onGuiMouse(net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof net.minecraft.client.gui.inventory.GuiContainer)
                || event.getGui() instanceof net.minecraft.client.gui.inventory.GuiContainerCreative
                || org.lwjgl.input.Mouse.getEventButton() != 1 || !org.lwjgl.input.Mouse.getEventButtonState()) {
            return;
        }
        net.minecraft.client.gui.inventory.GuiContainer gui = (net.minecraft.client.gui.inventory.GuiContainer) event.getGui();
        net.minecraft.inventory.Slot slot = gui.getSlotUnderMouse();
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (slot == null || player == null || !com.exiledradio.sstoragelegacy.item.StorageItemContents.tryStash(slot, player, true)) {
            return;
        }
        StoragePacketHandler.INSTANCE.sendToServer(new com.exiledradio.sstoragelegacy.network.StashMessage(gui.inventorySlots.windowId, slot.slotNumber));
        event.setCanceled(true);
    }

    private static net.minecraft.util.math.BlockPos paintbrushCachePos = null;
    private static ItemStack paintbrushCacheStack = null;
    private static com.exiledradio.sstoragelegacy.item.PaintbrushItem.ItemRequirements paintbrushCache = null;

    /** Looking at a storage or controller with a paintbrush: the items it still needs (the original's PaintbrushOverlay). */
    @SubscribeEvent
    public void onRenderPaintbrushOverlay(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.player == null || mc.world == null || mc.currentScreen != null
                || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
            return;
        }
        ItemStack paintbrush = mc.player.getHeldItemMainhand().getItem() instanceof com.exiledradio.sstoragelegacy.item.PaintbrushItem ? mc.player.getHeldItemMainhand()
                : mc.player.getHeldItemOffhand().getItem() instanceof com.exiledradio.sstoragelegacy.item.PaintbrushItem ? mc.player.getHeldItemOffhand() : ItemStack.EMPTY;
        net.minecraft.util.math.BlockPos pos = mc.objectMouseOver.getBlockPos();
        net.minecraft.block.Block block = mc.world.getBlockState(pos).getBlock();
        if (paintbrush.isEmpty() || !(block instanceof com.exiledradio.sstoragelegacy.block.StorageBlockBase || block instanceof com.exiledradio.sstoragelegacy.block.ControllerBlock)) {
            return;
        }
        if (!pos.equals(paintbrushCachePos) || paintbrush != paintbrushCacheStack || mc.world.getTotalWorldTime() % 20 == 0) {
            paintbrushCache = com.exiledradio.sstoragelegacy.item.PaintbrushItem.getItemRequirements(paintbrush, mc.player, mc.world, pos);
            paintbrushCachePos = pos;
            paintbrushCacheStack = paintbrush;
        }
        if (paintbrushCache == null || paintbrushCache.getItemsMissing().isEmpty()) {
            return;
        }
        net.minecraft.client.gui.ScaledResolution resolution = event.getResolution();
        String text = net.minecraft.client.resources.I18n.format("item.sstoragelegacy.paintbrush.overlay.missing_items");
        int y = resolution.getScaledHeight() - 75 - 10;
        mc.fontRenderer.drawStringWithShadow(text, (resolution.getScaledWidth() - mc.fontRenderer.getStringWidth(text)) / 2F + 1, y, 0xFFFFFF);
        int x = (resolution.getScaledWidth() - paintbrushCache.getItemsMissing().size() * 18) / 2;
        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
        for (ItemStack missing : paintbrushCache.getItemsMissing()) {
            mc.getRenderItem().renderItemAndEffectIntoGUI(missing, x, y + 10);
            mc.getRenderItem().renderItemOverlays(mc.fontRenderer, missing, x, y + 10);
            x += 18;
        }
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
    }

    /** Sneak + scroll with the tool in the main hand changes its mode. */
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (event.getDwheel() == 0 || mc.currentScreen != null || player == null || !player.isSneaking()
                || !(player.getHeldItemMainhand().getItem() instanceof StorageToolItem)) {
            return;
        }
        StoragePacketHandler.INSTANCE.sendToServer(new ScrolledToolMessage(event.getDwheel() > 0));
        event.setCanceled(true);
    }

    /** The tool's mode, above the hotbar while it is held. */
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        ItemStack tool = mc.player.getHeldItemMainhand();
        if (!(tool.getItem() instanceof StorageToolItem)) {
            tool = mc.player.getHeldItemOffhand();
            if (!(tool.getItem() instanceof StorageToolItem)) {
                return;
            }
        }
        String message = StorageToolItem.getOverlayMessage(tool);
        FontRenderer font = mc.fontRenderer;
        ScaledResolution resolution = event.getResolution();
        font.drawStringWithShadow(message, (resolution.getScaledWidth() - font.getStringWidth(message)) / 2.0F,
                resolution.getScaledHeight() - 75, OVERLAY_COLOR);
    }
}
