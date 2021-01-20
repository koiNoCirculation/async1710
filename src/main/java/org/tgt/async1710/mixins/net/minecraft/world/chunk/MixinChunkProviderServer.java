package org.tgt.async1710.mixins.net.minecraft.world.chunk;

import com.google.common.collect.Lists;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tgt.async1710.GetLoadedChunks;
import org.tgt.async1710.GlobalExecutor;
import org.tgt.async1710.ReentrantReadWriteLockedList;
import org.tgt.async1710.ReentrantReadWriteLockedSet;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServer implements GetLoadedChunks {
    @Shadow private Set droppedChunksSet;
    @Shadow private Set<Long> loadingChunks;
    @Shadow public List loadedChunks;

    @Shadow public abstract Chunk originalLoadChunk(int p_73158_1_, int p_73158_2_);

    private Set<Chunk> loadedChunksSet = new ConcurrentSet<>();

    @Override
    public Set<Chunk> getLoadedChunksSet() {
        return loadedChunksSet;
    }

    /**
     * @author lyt
     * @reason zhi jie chong xie
     * @return
     */
    @Overwrite
    public List func_152380_a()
    {
        return Arrays.asList(this.loadedChunksSet.toArray());
    }

    @Redirect(method = "unloadAllChunks", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false))
    public Iterator unloadAllChunks(List list) {
        return loadedChunksSet.iterator();
    }

    @Redirect(method = "originalLoadChunk", remap = false, at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false))
    public <E> boolean originalLoadChunks(List list, E e) {
        return loadedChunksSet.add((Chunk) e);
    }

    @Redirect(method = "saveChunks", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList(Ljava/lang/Iterable;)Ljava/util/ArrayList;", remap = false))
    public ArrayList saveChunks(Iterable elements) {
        return Lists.newArrayList(loadedChunksSet);
    }

    @Redirect(method = "unloadQueuedChunks", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(Ljava/lang/Object;)Z", remap = false))
    public boolean unloadQueuedChunks1(List list, Object o) {
        return loadedChunksSet.remove(o);
    }

    @Redirect(method = "unloadQueuedChunks", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", remap = false))
    public int unloadQueuedChunks2(List list) {
        return loadedChunksSet.size();
    }

    @Redirect(method = "loadChunk(IILjava/lang/Runnable;)Lnet/minecraft/world/chunk/Chunk;",remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/ChunkProviderServer;originalLoadChunk(II)Lnet/minecraft/world/chunk/Chunk;"))
    public Chunk originalLoad(ChunkProviderServer providerServer,int chunkX, int chunkZ) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> originalLoadChunk(chunkX, chunkZ), GlobalExecutor.stp).get();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public  void init(WorldServer p_i1520_1_, IChunkLoader p_i1520_2_, IChunkProvider p_i1520_3_, CallbackInfo callbackInfo) {
        droppedChunksSet = new ReentrantReadWriteLockedSet(new HashSet());
        loadingChunks = new ReentrantReadWriteLockedSet<>(new HashSet<>());
        loadedChunks = new ReentrantReadWriteLockedList(new ArrayList());
    }
}
