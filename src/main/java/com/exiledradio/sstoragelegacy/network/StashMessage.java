/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.network;

import com.exiledradio.sstoragelegacy.item.StorageItemContents;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client to server: the player right-clicked a slot to stash items in a shulker box (see StorageItemContents). */
public class StashMessage implements IMessage {
    private int windowId;
    private int slotNumber;

    public StashMessage() {
    }

    public StashMessage(int windowId, int slotNumber) {
        this.windowId = windowId;
        this.slotNumber = slotNumber;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        slotNumber = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        buf.writeInt(slotNumber);
    }

    public static class Handler implements IMessageHandler<StashMessage, IMessage> {
        @Override
        public IMessage onMessage(StashMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Container container = player.openContainer;
                if (container == null || container.windowId != message.windowId || message.slotNumber < 0
                        || message.slotNumber >= container.inventorySlots.size()) {
                    return;
                }
                if (StorageItemContents.tryStash(container.getSlot(message.slotNumber), player, false)) {
                    container.detectAndSendChanges();
                    player.updateHeldItem();
                }
            });
            return null;
        }
    }
}
