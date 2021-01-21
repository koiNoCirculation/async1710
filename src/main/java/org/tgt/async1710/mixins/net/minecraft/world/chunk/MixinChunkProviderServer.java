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

import java.util.concurrent.ExecutionException;

@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServer {
    @Shadow public abstract Chunk originalLoadChunk(int p_73158_1_, int p_73158_2_);

    @Shadow public WorldServer worldObj;

    @Shadow public abstract void dropChunk(int p_73241_1_, int p_73241_2_);

    @Shadow public abstract void unloadAllChunks();

    @Shadow public abstract Chunk provideChunk(int p_73154_1_, int p_73154_2_);

    @Shadow public abstract Chunk loadChunk(int par1, int par2, Runnable runnable);

    @Inject(method = "dropChunk", at = @At("HEAD"), cancellable = true)
    public void _dropChunk(int p_73241_1_, int p_73241_2_, CallbackInfo ci) throws ExecutionException, InterruptedException {
        if(((WorldUtils)worldObj).getThreadName() != Thread.currentThread().getName() && ((WorldUtils)worldObj).getRunning()) {
            ((TaskSubmitter)worldObj).submit(() -> dropChunk(p_73241_1_, p_73241_2_)).get();
            ci.cancel();
        }
    }
    @Inject(method = "unloadAllChunks", at = @At("HEAD"), cancellable = true)
    public void _unloadAllChunks(CallbackInfo ci) throws ExecutionException, InterruptedException {
        if(((WorldUtils)worldObj).getThreadName() != Thread.currentThread().getName() && ((WorldUtils)worldObj).getRunning()) {
            ((TaskSubmitter)worldObj).submit(this::unloadAllChunks).get();
            ci.cancel();
        }
    }

    @Inject(method = "provideChunk", at = @At("HEAD"), cancellable = true)
    public void _provideChunk(int p_73154_1_, int p_73154_2_, CallbackInfoReturnable<Chunk> cir) throws ExecutionException, InterruptedException {
        String threadName = ((WorldUtils) worldObj).getThreadName();
        if(threadName != Thread.currentThread().getName() && !threadName.startsWith("Chunk I/O") && ((WorldUtils)worldObj).getRunning()) {
            cir.setReturnValue((Chunk) ((TaskSubmitter)worldObj).submit(() -> provideChunk(p_73154_1_, p_73154_2_)).get());
        }
    }
    @Inject(method = "loadChunk(IILjava/lang/Runnable;)Lnet/minecraft/world/chunk/Chunk;", at = @At("HEAD"), cancellable = true)
    public void _loadChunk(int par1, int par2, Runnable runnable, CallbackInfoReturnable<Chunk> cir) throws ExecutionException, InterruptedException {
        if(((WorldUtils)worldObj).getThreadName() != Thread.currentThread().getName() && ((WorldUtils)worldObj).getRunning()) {
            cir.setReturnValue(((TaskSubmitter)worldObj).submit(() -> loadChunk(par1, par2, runnable)).get());
        }
    }
}
