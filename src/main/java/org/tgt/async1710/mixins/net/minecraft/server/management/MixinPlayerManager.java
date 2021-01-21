package org.tgt.async1710.mixins.net.minecraft.server.management;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tgt.async1710.TaskSubmitter;
import org.tgt.async1710.WorldUtils;

import java.util.concurrent.ExecutionException;

@Mixin(PlayerManager.class)
public abstract class MixinPlayerManager {

    @Shadow public abstract void updateMountedMovingPlayer(EntityPlayerMP p_72685_1_);

    @Inject(method = "updateMountedMovingPlayer", at = @At("HEAD"), cancellable = true)
    public void _updateMountedMovingPlayer(EntityPlayerMP p_72685_1_, CallbackInfo ci) throws ExecutionException, InterruptedException {
        World worldObj = p_72685_1_.worldObj;
        WorldUtils worldObj1 = (WorldUtils) worldObj;
        if(worldObj1.getThreadName() != Thread.currentThread().getName() && worldObj1.getRunning()) {
            ((TaskSubmitter)worldObj1).submit(() -> updateMountedMovingPlayer(p_72685_1_)).get();
            ci.cancel();
        }
    }
}
