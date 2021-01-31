package org.tgt.async1710.mixins.net.minecraft.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.minecraft.network.NetworkSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.tgt.async1710.ChannelInitializerNetworkSystem;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

@Mixin(NetworkSystem.class)
public class MixinNetworkSystem {
    @Shadow @Final private List endpoints;
    private static NioEventLoopGroup boss = new NioEventLoopGroup(2, new ThreadFactoryBuilder().setNameFormat("Netty Boss #%d").setDaemon(true).build());

    /**
     * Connection per thread, no limit.
     */
    private static NioEventLoopGroup worker = new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Worker #%d").setDaemon(true).build());

    /**
     * @author
     */
    @Overwrite
    /**
     * Adds a channel that listens on publicly accessible network ports
     */
    public void addLanEndpoint(InetAddress address, int port) throws IOException
    {
        synchronized (this.endpoints)
        {
            ChannelFuture channelFuture = new ServerBootstrap().
                    channel(NioServerSocketChannel.class).
                    childHandler(new ChannelInitializerNetworkSystem()).
                    group(boss, worker).
                    localAddress(address, port).
                    bind().
                    syncUninterruptibly();
            this.endpoints.add(channelFuture);
        }
    }
}
