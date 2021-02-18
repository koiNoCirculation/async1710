package org.tgt.async1710.mixins.cpw.mods.fml.common.network;

import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleChannelHandlerWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.tgt.async1710.world.TaskSubmitter;

import java.lang.reflect.Constructor;

@Mixin(SimpleChannelHandlerWrapper.class)
public class MixinSimpleChannelHandlerWrapper<REQ extends IMessage, REPLY extends IMessage> {
    @Shadow @Final private IMessageHandler<? super REQ, ? extends REPLY> messageHandler;

    @Shadow @Final private Side side;

    static Constructor<MessageContext> ctor;

    static {
        try {
            ctor = MessageContext.class.getDeclaredConstructor(INetHandler.class, Side.class);
            ctor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * @author lyt
     * used future
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Overwrite(remap = false)
    protected void channelRead0(ChannelHandlerContext ctx, REQ msg) throws Exception
    {
        INetHandler iNetHandler = ctx.channel().attr(NetworkRegistry.NET_HANDLER).get();
        MessageContext messageContext = ctor.newInstance(iNetHandler, side);
        ((TaskSubmitter)((NetHandlerPlayServer)iNetHandler).playerEntity.worldObj).
                submit(() ->  messageHandler.onMessage(msg, messageContext)).
                map(f  -> f.thenAcceptAsync(result -> {
            if (result != null)
            {
                ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.REPLY);
                ctx.writeAndFlush(result).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            }
        }));

    }
}
