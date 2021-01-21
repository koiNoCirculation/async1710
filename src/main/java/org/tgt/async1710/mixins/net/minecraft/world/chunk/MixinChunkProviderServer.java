package org.tgt.async1710.mixins.net.minecraft.world.chunk;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tgt.async1710.TaskSubmitter;
import org.tgt.async1710.WorldUtils;

import java.sql.Time;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServer {

    @Shadow public WorldServer worldObj;

    @Shadow public abstract void dropChunk(int p_73241_1_, int p_73241_2_);

    @Shadow public abstract void unloadAllChunks();

    @Shadow public abstract Chunk loadChunk(int par1, int par2, Runnable runnable);

    @Inject(method = "dropChunk", at = @At("HEAD"), cancellable = true)
    public void _dropChunk(int p_73241_1_, int p_73241_2_, CallbackInfo ci) throws ExecutionException, InterruptedException {
        if(((WorldUtils)worldObj).getThreadName() != Thread.currentThread().getName() && ((WorldUtils)worldObj).getRunning()) {
            try {
                ((TaskSubmitter)worldObj).submit(() -> dropChunk(p_73241_1_, p_73241_2_)).get(50, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            ci.cancel();
        }
    }
    @Inject(method = "unloadAllChunks", at = @At("HEAD"), cancellable = true)
    public void _unloadAllChunks(CallbackInfo ci) throws ExecutionException, InterruptedException {
        if(((WorldUtils)worldObj).getThreadName() != Thread.currentThread().getName() && ((WorldUtils)worldObj).getRunning()) {
            try {
                ((TaskSubmitter)worldObj).submit(this::unloadAllChunks).get(50, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
            }
            ci.cancel();
        }
    }
    @Inject(method = "loadChunk(IILjava/lang/Runnable;)Lnet/minecraft/world/chunk/Chunk;", at = @At("HEAD"), cancellable = true, remap = false)
    public void _loadChunk(int par1, int par2, Runnable runnable, CallbackInfoReturnable<Chunk> cir) throws ExecutionException, InterruptedException {
        if(((WorldUtils)worldObj).getThreadName() != Thread.currentThread().getName() && ((WorldUtils)worldObj).getRunning()) {
            cir.setReturnValue(((TaskSubmitter)worldObj).submit(() -> loadChunk(par1, par2, runnable)).get());
        }
    }
}
