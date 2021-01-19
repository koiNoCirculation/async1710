package org.tgt.async1710.mixins.net.minecraft.world.chunk;

import com.google.common.collect.Lists;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.tgt.async1710.GetLoadedChunks;

import java.util.*;

@Mixin(ChunkProviderServer.class)
public class MixinChunkProviderServer implements GetLoadedChunks {
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
}
