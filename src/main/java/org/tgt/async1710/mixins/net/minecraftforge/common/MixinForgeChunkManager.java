package org.tgt.async1710.mixins.net.minecraftforge.common;

import com.google.common.cache.Cache;
import com.google.common.collect.MapMaker;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tgt.async1710.ChunkGetLoadedEntities;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ForgeChunkManager.class)
public class MixinForgeChunkManager {
    @Shadow private static Map<World, Cache<Long, Chunk>> dormantChunkCache;

    @Shadow private static Map<String, Integer> ticketConstraints;

    @Shadow private static Map<String, Integer> chunkConstraints;

    @Shadow private static Map<String, ForgeChunkManager.LoadingCallback> callbacks;

    @Inject(method = "fetchDormantChunk", at = @At("HEAD"), cancellable = true, remap = false)
    private static void _fetchDormantChunk(long coords, World world, CallbackInfoReturnable<Chunk> cir) {
        Cache<Long, Chunk> cache = dormantChunkCache.get(world);
        if (cache == null)
        {
            cir.setReturnValue(null);
            return;
        }
        Chunk chunk = cache.getIfPresent(coords);
        if (chunk != null)
        {
            for (Set set : ((ChunkGetLoadedEntities)chunk).getLoadedEntitySet())
            {
                for (Object e: set)
                {
                    ((Entity)e).resetEntityId();
                }
            }
        }
        cir.setReturnValue(chunk);
    }
    @Inject(method = "<clinit>", at = @At("RETURN"), remap = false)
    private static void replaceMap(CallbackInfo ci) {
        ticketConstraints = new ConcurrentHashMap<>();
        chunkConstraints = new ConcurrentHashMap<>();
        callbacks = new ConcurrentHashMap<>();
    }
}
