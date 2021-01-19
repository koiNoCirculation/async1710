package org.tgt.async1710.mixins.net.minecraftforge.common.chunkio;


import net.minecraftforge.common.QueuedChunkWithCallBack;
import net.minecraftforge.common.chunkio.ChunkIOExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.ExecutionException;

@Mixin(ChunkIOExecutor.class)
public class MixinChunkIOExecutor {
    private static Logger logger = LogManager.getLogger(MixinChunkIOExecutor.class);
    /**
     * @author lyt
     * @reason 重写比较方便，不会有人动这个吧，不会吧不会ｂａ
     */
    @Overwrite
    public static net.minecraft.world.chunk.Chunk syncChunkLoad(net.minecraft.world.World world, net.minecraft.world.chunk.storage.AnvilChunkLoader loader, net.minecraft.world.gen.ChunkProviderServer provider, int x, int z) throws ExecutionException, InterruptedException {
        return new QueuedChunkWithCallBack(x, z, loader, world, provider, null).submit().get();
    }

    /**
     * @author lyt
     * @reason 重写比较方便，不会有人动这个吧，不会吧不会ｂａ
     */
    @Overwrite
    public static void queueChunkLoad(net.minecraft.world.World world, net.minecraft.world.chunk.storage.AnvilChunkLoader loader, net.minecraft.world.gen.ChunkProviderServer provider, int x, int z, Runnable runnable) {
        new QueuedChunkWithCallBack(x, z, loader, world, provider, runnable).submit();
    }

    /**
     * @author lyt
     * @reason 重写比较方便，不会有人动这个吧，不会吧不会ｂａ
     */
    @Overwrite
    public static void dropQueuedChunkLoad(net.minecraft.world.World world, int x, int z, Runnable runnable) {
        QueuedChunkWithCallBack.cancel(x, z, world);
    }

    /**
     * @author lyt
     * @reason 重写比较方便，不会有人动这个吧，不会吧不会ｂａ
     */
    @Overwrite
    public static void adjustPoolSize(int players) {
        logger.info("这个调节没有用了，bye");
    }
}
