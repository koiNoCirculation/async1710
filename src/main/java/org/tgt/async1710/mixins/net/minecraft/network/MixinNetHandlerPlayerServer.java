package org.tgt.async1710.mixins.net.minecraft.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.vecmath.Vector3d;

@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayerServer {
    @Shadow @Final private static Logger logger;

    @Shadow public EntityPlayerMP playerEntity;

    @Inject(method = "processPlayer", at = @At("HEAD"))
    public void logProcessPlayer(C03PacketPlayer packetIn, CallbackInfo ci) { /*
        double positionX = packetIn.getPositionX();
        double positionY = packetIn.getPositionY();
        double positionZ = packetIn.getPositionZ();
        Vector3d current = new Vector3d(positionX, positionY, positionZ);
        Vector3d current1 = new Vector3d(positionX, positionY, positionZ);
        Vector3d last = new Vector3d(playerEntity.posX, playerEntity.posY, playerEntity.posZ);
        current.sub(last);
        double length = current.length();
        logger.info("current pos = {}, last pos = {}, distance = {}", current1, last, length) ;
        */
    }

}
