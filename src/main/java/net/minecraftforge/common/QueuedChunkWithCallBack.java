package net.minecraftforge.common;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tgt.async1710.GlobalExecutor;
import org.tgt.async1710.GetLoadedChunks;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class QueuedChunkWithCallBack implements Supplier<Chunk> {

    final int x;
    final int z;
    final net.minecraft.world.chunk.storage.AnvilChunkLoader loader;
    final net.minecraft.world.World world;
    final net.minecraft.world.gen.ChunkProviderServer provider;
    private final Runnable callback;
    private static Logger logger = LogManager.getLogger(QueuedChunkWithCallBack.class);
    net.minecraft.nbt.NBTTagCompound compound;
    private boolean canceled = false;
    private static ConcurrentHashMap<TaskPointer, QueuedChunkWithCallBack> pendings = new ConcurrentHashMap<>();
    private final TaskPointer pointer;
    public static class TaskPointer {
        final int x;
        final int z;
        final int worldid;
        public TaskPointer(int x, int z, int worldid) {
            this.x = x;
            this.z = z;
            this.worldid = worldid;
        }
        @Override
        public boolean equals(Object object) {
            if (object != null && object instanceof TaskPointer) {
                TaskPointer other = (TaskPointer) object;
                return x == other.x && z == other.z && worldid == other.worldid;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (x * 31 + z) * 31 + worldid;
        }
    }
    public QueuedChunkWithCallBack(int x, int z, net.minecraft.world.chunk.storage.AnvilChunkLoader loader, net.minecraft.world.World world, net.minecraft.world.gen.ChunkProviderServer provider, Runnable callback) {
        this.x = x;
        this.z = z;
        this.loader = loader;
        this.world = world;
        this.provider = provider;
        this.callback = callback;
        this.pointer = new TaskPointer(x, z, provider.worldObj.provider.dimensionId);
    }

    public CompletableFuture<Chunk> submit() {
        pendings.put(pointer, this);
        return GlobalExecutor.submitTask(this);
    }

    public static void cancel(int x, int z, World world) {
        QueuedChunkWithCallBack queuedChunkWithCallBack = pendings.get(new TaskPointer(x, z, world.provider.dimensionId));
        if(queuedChunkWithCallBack != null) {
            queuedChunkWithCallBack.canceled = true;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");

        result.append(this.getClass().getName() + " {" + NEW_LINE);
        result.append(" x: " + x + NEW_LINE);
        result.append(" z: " + z + NEW_LINE);
        result.append(" loader: " + loader + NEW_LINE );
        result.append(" world: " + world.getWorldInfo().getWorldName() + NEW_LINE);
        result.append(" dimension: " + world.provider.dimensionId + NEW_LINE);
        result.append(" provider: " + world.provider.getClass().getName() + NEW_LINE);
        result.append("}");

        return result.toString();
    }

    @Override
    public Chunk get() {
        //stage1
        try {
            if(!canceled) {
                Object[] data = null;
                try {
                    data = loader.loadChunk__Async(world, x, z);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (data != null) {
                    compound = (net.minecraft.nbt.NBTTagCompound) data[1];
                }
                //stage2
                Chunk chunk = (Chunk) data[0];
                if (chunk == null) {
                    // If the chunk loading failed just do it synchronously (may generate)
                    provider.originalLoadChunk(x, z);
                } else {
                    loader.loadEntities(world, compound.getCompoundTag("Level"), chunk);
                    MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(chunk, compound)); // Don't call ChunkDataEvent.Load async
                    chunk.lastSaveTime = provider.worldObj.getTotalWorldTime();
                    provider.id2ChunkMap.add(ChunkCoordIntPair.chunkXZ2Int(x, z), chunk);
                    ((GetLoadedChunks)provider).getLoadedChunksSet().add(chunk);
                    chunk.onChunkLoad();

                    if (provider.serverChunkGenerator != null) {
                        provider.serverChunkGenerator.recreateStructures(x, z);
                    }

                    chunk.populateChunk(provider, provider, x, z);
                }
                if(callback != null) {
                    callback.run();
                }
                //logger.info("chunk at chunkx = {}, chunkz = {} finished, block at x = {}, y = 50, z = {} is {}", x, z,x << 4, z << 4, world.getBlock(x << 4, 50, z << 4).getClass());
                return chunk;
            }
        } catch (Exception e) {
            logger.error("Error loading chunk at x = {}, z = {}, in world DIM-{}",x,z,provider.worldObj.provider.dimensionId, e);
            provider.worldObj.theChunkProviderServer.dropChunk(x, z);
        } finally {
            pendings.remove(pointer, this);
        }
        return null;
    }
}