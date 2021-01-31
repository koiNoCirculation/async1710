package org.tgt.async1710;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.PingResponseHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerHandshakeTCP;
import net.minecraft.util.MessageDeserializer;
import net.minecraft.util.MessageDeserializer2;
import net.minecraft.util.MessageSerializer;
import net.minecraft.util.MessageSerializer2;

import java.lang.reflect.Field;
import java.util.List;

public class ChannelInitializerNetworkSystem extends ChannelInitializer {
    @Override
    protected void initChannel(Channel channel) throws Exception {
        NetworkSystem networkSystem = MinecraftServer.getServer().getNetworkSystem();
        channel.config().setOption(ChannelOption.IP_TOS, 24);
        channel.config().setOption(ChannelOption.TCP_NODELAY, false);
        channel.pipeline().addLast("timeout", new ReadTimeoutHandler(FMLNetworkHandler.READ_TIMEOUT)).
                addLast("legacy_query", new PingResponseHandler(networkSystem)).
                addLast("splitter", new MessageDeserializer2()).
                addLast("decoder", new MessageDeserializer(NetworkManager.STATISTICS)).
                addLast("prepender", new MessageSerializer2()).
                addLast("encoder", new MessageSerializer(NetworkManager.STATISTICS));
        NetworkManager networkmanager = new NetworkManager(false);
        Field networkManagers = NetworkSystem.class.getDeclaredField("networkManagers");
        networkManagers.setAccessible(true);
        ((List<NetworkManager>) networkManagers.get(networkSystem)).add(networkmanager);
        channel.pipeline().addLast("packet_handler", networkmanager);
        networkmanager.setNetHandler(new NetHandlerHandshakeTCP(MinecraftServer.getServer(), networkmanager));
    }
}
