package org.tgt.async1710.mixins.net.minecraft.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ReportedException;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.net.SocketAddress;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager {
    @Shadow private INetHandler packetListener;

    @Shadow protected abstract void dispatchPacket(Packet inPacket, GenericFutureListener<?>[] futureListeners);

    @Shadow private Channel channel;

    @Shadow @Final public static AttributeKey<?> attrKeyConnectionState;

    @Shadow private EnumConnectionState connectionState;

    @Shadow public abstract boolean isLocalChannel();

    @Shadow @Final private static Logger logger;

    @Shadow public abstract SocketAddress getRemoteAddress();

    @Shadow public abstract void disableAutoRead();

    @Shadow public abstract void closeChannel(IChatComponent message);

    /**
     * @author
     * 为了分散等待的压力，反正都不在一个线程了，又在乎多少别的呢
     */
    @Overwrite
    protected void channelRead0(ChannelHandlerContext ctx, Packet packetIn)
    {
        EnumConnectionState enumconnectionstate = (EnumConnectionState)this.channel.attr(attrKeyConnectionState).get();

        if (this.connectionState != enumconnectionstate)
        {
            if (this.connectionState != null)
            {
                this.packetListener.onConnectionStateTransition(this.connectionState, enumconnectionstate);
            }

            this.connectionState = enumconnectionstate;
        }
        if (this.packetListener != null)
        {
            try {
                packetIn.processPacket(this.packetListener);
                channel.flush();
            } catch (Exception e) {
                if (isLocalChannel())
                {
                    CrashReport crashreport = CrashReport.makeCrashReport(e, "Ticking memory connection");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Ticking connection");
                    crashreportcategory.addCrashSectionCallable("Connection", MixinNetworkManager.this::toString);
                    throw new ReportedException(crashreport);
                }

                logger.warn("Failed to handle packet for " + getRemoteAddress(), e);
                final ChatComponentText chatcomponenttext = new ChatComponentText("Internal server error");
                scheduleOutboundPacket(new S40PacketDisconnect(chatcomponenttext), (f) -> closeChannel(chatcomponenttext));
                disableAutoRead();
            }
        }
    }

    /**
     * 发包也扔进线程池里面
     * @author
     */
    @Overwrite
    public void scheduleOutboundPacket(Packet inPacket, GenericFutureListener<?>... futureListeners)
    {
        dispatchPacket(inPacket, futureListeners);
    }

    /**
     * @author
     * zhe ge bu yong le
     */
    @Overwrite
    public void processReceivedPackets() {
        /**
         * do nothing
         */
        if(packetListener != null) {
            packetListener.onNetworkTick();
        }
    }
}
