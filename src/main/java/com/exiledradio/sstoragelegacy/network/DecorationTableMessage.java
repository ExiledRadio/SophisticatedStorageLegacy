/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.network;

import com.exiledradio.sstoragelegacy.block.DecorationTableTileEntity;
import com.exiledradio.sstoragelegacy.common.gui.DecorationTableContainer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client to server: a decoration table screen changed a slot's reuse arrow or a colour (the original's SyncContainerClientDataMessage use). */
public class DecorationTableMessage implements IMessage {
    public static final int INHERITANCE = 0;
    public static final int MAIN_COLOR = 1;
    public static final int ACCENT_COLOR = 2;

    private int action;
    private int slot;
    private int value;

    public DecorationTableMessage() {
    }

    public DecorationTableMessage(int action, int slot, int value) {
        this.action = action;
        this.slot = slot;
        this.value = value;
    }

    /** Applies the change to a table (the client does too, so its screen updates at once). */
    public static void apply(DecorationTableTileEntity tile, int action, int slot, int value) {
        switch (action) {
            case INHERITANCE:
                if (slot >= 0 && slot < 7) {
                    tile.setSlotMaterialInheritance(slot, value != 0);
                }
                break;
            case MAIN_COLOR:
                tile.setMainColor(value);
                break;
            case ACCENT_COLOR:
                tile.setAccentColor(value);
                break;
            default:
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        slot = buf.readByte();
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeByte(slot);
        buf.writeInt(value);
    }

    public static class Handler implements IMessageHandler<DecorationTableMessage, IMessage> {
        @Override
        public IMessage onMessage(DecorationTableMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof DecorationTableContainer) {
                    DecorationTableTileEntity tile = ((DecorationTableContainer) player.openContainer).getTile();
                    apply(tile, message.action, message.slot, message.value);
                    player.openContainer.detectAndSendChanges();
                }
            });
            return null;
        }
    }
}
