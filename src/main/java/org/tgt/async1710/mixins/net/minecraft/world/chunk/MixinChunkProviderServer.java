package org.tgt.async1710.mixins.net.minecraft.world.chunk;

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tgt.async1710.world.TaskSubmitter;

@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServer {

    @Shadow
    public WorldServer worldObj;

    @Shadow
    public abstract void unloadAllChunks();

    @Inject(method = "unloadAllChunks", at = @At("HEAD"), cancellable = true)
    public void _unloadAllChunks(CallbackInfo ci) {
        ((TaskSubmitter) worldObj).submit(() -> unloadAllChunks(), ci);
    }
}
