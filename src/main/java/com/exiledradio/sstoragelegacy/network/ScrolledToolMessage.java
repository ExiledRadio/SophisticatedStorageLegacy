/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.network;

import com.exiledradio.sstoragelegacy.item.StorageToolItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Sneak + scroll with the storage tool in the main hand: the next or previous mode. */
public class ScrolledToolMessage implements IMessage {
    private boolean next;

    public ScrolledToolMessage() {
    }

    public ScrolledToolMessage(boolean next) {
        this.next = next;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        next = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(next);
    }

    public static class Handler implements IMessageHandler<ScrolledToolMessage, IMessage> {
        @Override
        public IMessage onMessage(ScrolledToolMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack stack = player.getHeldItemMainhand();
                if (stack.getItem() instanceof StorageToolItem) {
                    StorageToolItem.cycleMode(stack, message.next);
                }
            });
            return null;
        }
    }
}
