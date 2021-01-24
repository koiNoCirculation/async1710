package org.tgt.async1710.mixins.net.minecraft.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tgt.async1710.TaskSubmitter;

import javax.vecmath.Vector3d;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayerServer {
    @Shadow @Final private static Logger logger;

    @Shadow public EntityPlayerMP playerEntity;

    @Shadow @Final private MinecraftServer serverController;

    @Shadow public abstract void processPlayer(C03PacketPlayer packetIn);

    @Shadow public abstract void processPlayerDigging(C07PacketPlayerDigging packetIn);

    @Shadow public abstract void processPlayerBlockPlacement(C08PacketPlayerBlockPlacement packetIn);

    @Shadow public abstract void processUseEntity(C02PacketUseEntity packetIn);

    @Shadow public abstract void processUpdateSign(C12PacketUpdateSign packetIn);

    @Inject(method = "processPlayer", cancellable = true,at = @At("HEAD"))
    public void _processPlayer(C03PacketPlayer packetIn, CallbackInfo ci) {
        WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        ((TaskSubmitter) worldserver).submit(() -> processPlayer(packetIn), ci);
    }

    @Inject(method = "processPlayerDigging", cancellable = true,at = @At("HEAD"))
    public void _processPlayerDigging(C07PacketPlayerDigging packetIn, CallbackInfo ci) {
        WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        ((TaskSubmitter) worldserver).submit(() -> processPlayerDigging(packetIn), ci);
    }
    @Inject(method = "processPlayerBlockPlacement", cancellable = true,at = @At("HEAD"))
    public void _processPlayerBlockPlacement(C08PacketPlayerBlockPlacement packetIn, CallbackInfo ci) {
        WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        ((TaskSubmitter) worldserver).submit(() -> processPlayerBlockPlacement(packetIn), ci);
    }
    @Inject(method = "processUseEntity", cancellable = true,at = @At("HEAD"))
    public void _processUseEntity(C02PacketUseEntity packetIn, CallbackInfo ci) {
        WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        ((TaskSubmitter) worldserver).submit(() -> processUseEntity(packetIn), ci);
    }
    @Inject(method = "processUpdateSign", cancellable = true,at = @At("HEAD"))
    public void _processUpdateSign(C12PacketUpdateSign packetIn, CallbackInfo ci) {
        WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        ((TaskSubmitter) worldserver).submit(() -> processUpdateSign(packetIn), ci);
    }

}
