package org.tgt.async1710.mixins.net.minecraft.world.chunk.storage;

import cpw.mods.fml.common.FMLLog;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tgt.async1710.ChunkGetLoadedEntities;
import org.tgt.async1710.WorldUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(AnvilChunkLoader.class)
public class MixinAnvilChunkloader {
    @Shadow private List chunksToRemove;

    @Shadow private Set pendingAnvilChunksCoordinates;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(File p_i2003_1_, CallbackInfo ci) {
        chunksToRemove = new CopyOnWriteArrayList();
        pendingAnvilChunksCoordinates = new ConcurrentSet();
    }

    @Inject(method = "writeChunkToNBT", at = @At("HEAD"), cancellable = true)
    public void _writeChunkToNBT(Chunk p_75820_1_, World p_75820_2_, NBTTagCompound p_75820_3_, CallbackInfo ci) {
        p_75820_3_.setByte("V", (byte)1);
        p_75820_3_.setInteger("xPos", p_75820_1_.xPosition);
        p_75820_3_.setInteger("zPos", p_75820_1_.zPosition);
        p_75820_3_.setLong("LastUpdate", p_75820_2_.getTotalWorldTime());
        p_75820_3_.setIntArray("HeightMap", p_75820_1_.heightMap);
        p_75820_3_.setBoolean("TerrainPopulated", p_75820_1_.isTerrainPopulated);
        p_75820_3_.setBoolean("LightPopulated", p_75820_1_.isLightPopulated);
        p_75820_3_.setLong("InhabitedTime", p_75820_1_.inhabitedTime);
        ExtendedBlockStorage[] aextendedblockstorage = p_75820_1_.getBlockStorageArray();
        NBTTagList nbttaglist = new NBTTagList();
        boolean flag = !p_75820_2_.provider.hasNoSky;
        ExtendedBlockStorage[] aextendedblockstorage1 = aextendedblockstorage;
        int i = aextendedblockstorage.length;
        NBTTagCompound nbttagcompound1;

        for (int j = 0; j < i; ++j)
        {
            ExtendedBlockStorage extendedblockstorage = aextendedblockstorage1[j];

            if (extendedblockstorage != null)
            {
                nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Y", (byte)(extendedblockstorage.getYLocation() >> 4 & 255));
                nbttagcompound1.setByteArray("Blocks", extendedblockstorage.getBlockLSBArray());

                if (extendedblockstorage.getBlockMSBArray() != null)
                {
                    nbttagcompound1.setByteArray("Add", extendedblockstorage.getBlockMSBArray().data);
                }

                nbttagcompound1.setByteArray("Data", extendedblockstorage.getMetadataArray().data);
                nbttagcompound1.setByteArray("BlockLight", extendedblockstorage.getBlocklightArray().data);

                if (flag)
                {
                    nbttagcompound1.setByteArray("SkyLight", extendedblockstorage.getSkylightArray().data);
                }
                else
                {
                    nbttagcompound1.setByteArray("SkyLight", new byte[extendedblockstorage.getBlocklightArray().data.length]);
                }

                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        p_75820_3_.setTag("Sections", nbttaglist);
        p_75820_3_.setByteArray("Biomes", p_75820_1_.getBiomeArray());
        p_75820_1_.hasEntities = false;
        NBTTagList nbttaglist2 = new NBTTagList();
        for (Set set : ((ChunkGetLoadedEntities) p_75820_1_).getLoadedEntitySet()) {
            for (Object o : set) {
                Entity entity = (Entity)o;
                nbttagcompound1 = new NBTTagCompound();

                try
                {
                    if (entity.writeToNBTOptional(nbttagcompound1))
                    {
                        p_75820_1_.hasEntities = true;
                        nbttaglist2.appendTag(nbttagcompound1);
                    }
                }
                catch (Exception e)
                {
                    FMLLog.log(Level.ERROR, e,
                            "An Entity type %s has thrown an exception trying to write state. It will not persist. Report this to the mod author",
                            entity.getClass().getName());
                }
            }
        }

        p_75820_3_.setTag("Entities", nbttaglist2);
        NBTTagList nbttaglist3 = new NBTTagList();
        for (Object value : p_75820_1_.chunkTileEntityMap.values()) {
            TileEntity tileentity = (TileEntity)value;
            nbttagcompound1 = new NBTTagCompound();
            try {
                tileentity.writeToNBT(nbttagcompound1);
                nbttaglist3.appendTag(nbttagcompound1);
            }
            catch (Exception e)
            {
                FMLLog.log(Level.ERROR, e,
                        "A TileEntity type %s has throw an exception trying to write state. It will not persist. Report this to the mod author",
                        tileentity.getClass().getName());
            }
        }
        p_75820_3_.setTag("TileEntities", nbttaglist3);
        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(p_75820_1_.xPosition, p_75820_1_.zPosition);
        Map<ChunkCoordIntPair, Queue<NextTickListEntry>> pendingTicks = ((WorldUtils) p_75820_2_).getPendingTicks();
        Queue<NextTickListEntry> nextTickListEntries = pendingTicks.get(chunkCoordIntPair);
        pendingTicks.remove(chunkCoordIntPair);
        if (nextTickListEntries != null)
        {
            long k = p_75820_2_.getTotalWorldTime();
            NBTTagList nbttaglist1 = new NBTTagList();
            for (Object o : nextTickListEntries) {
                NextTickListEntry nextticklistentry = (NextTickListEntry)o;
                NBTTagCompound nbttagcompound2 = new NBTTagCompound();
                nbttagcompound2.setInteger("i", Block.getIdFromBlock(nextticklistentry.func_151351_a()));
                nbttagcompound2.setInteger("x", nextticklistentry.xCoord);
                nbttagcompound2.setInteger("y", nextticklistentry.yCoord);
                nbttagcompound2.setInteger("z", nextticklistentry.zCoord);
                nbttagcompound2.setInteger("t", (int)(nextticklistentry.scheduledTime - k));
                nbttagcompound2.setInteger("p", nextticklistentry.priority);
                nbttaglist1.appendTag(nbttagcompound2);
            }
            p_75820_3_.setTag("TileTicks", nbttaglist1);
        }
        ci.cancel();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public void loadEntities(World p_75823_1_, NBTTagCompound p_75823_2_, Chunk chunk)
    {
        NBTTagList nbttaglist1 = p_75823_2_.getTagList("Entities", 10);

        if (nbttaglist1 != null)
        {
            for (int l = 0; l < nbttaglist1.tagCount(); ++l)
            {
                NBTTagCompound nbttagcompound3 = nbttaglist1.getCompoundTagAt(l);
                Entity entity2 = EntityList.createEntityFromNBT(nbttagcompound3, p_75823_1_);
                chunk.hasEntities = true;

                if (entity2 != null)
                {
                    chunk.addEntity(entity2);
                    Entity entity = entity2;

                    for (NBTTagCompound nbttagcompound2 = nbttagcompound3; nbttagcompound2.hasKey("Riding", 10); nbttagcompound2 = nbttagcompound2.getCompoundTag("Riding"))
                    {
                        Entity entity1 = EntityList.createEntityFromNBT(nbttagcompound2.getCompoundTag("Riding"), p_75823_1_);

                        if (entity1 != null)
                        {
                            chunk.addEntity(entity1);
                            entity.mountEntity(entity1);
                        }

                        entity = entity1;
                    }
                }
            }
        }

        NBTTagList nbttaglist2 = p_75823_2_.getTagList("TileEntities", 10);

        if (nbttaglist2 != null)
        {
            for (int i1 = 0; i1 < nbttaglist2.tagCount(); ++i1)
            {
                NBTTagCompound nbttagcompound4 = nbttaglist2.getCompoundTagAt(i1);
                TileEntity tileentity = TileEntity.createAndLoadEntity(nbttagcompound4);

                if (tileentity != null)
                {
                    chunk.addTileEntity(tileentity);
                }
            }
        }

        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(chunk.xPosition, chunk.zPosition);
        ConcurrentLinkedQueue<NextTickListEntry> nextTickQueue = new ConcurrentLinkedQueue<>();
        if (p_75823_2_.hasKey("TileTicks", 9))
        {

            NBTTagList nbttaglist3 = p_75823_2_.getTagList("TileTicks", 10);

            if (nbttaglist3 != null)
            {
                for (int j1 = 0; j1 < nbttaglist3.tagCount(); ++j1)
                {
                    //原有的worldserver.func_147446_b
                    NBTTagCompound nbttagcompound5 = nbttaglist3.getCompoundTagAt(j1);
                    int x = nbttagcompound5.getInteger("x");
                    int y = nbttagcompound5.getInteger("y");
                    int z = nbttagcompound5.getInteger("z");
                    Block blockid = Block.getBlockById(nbttagcompound5.getInteger("i"));
                    int time = nbttagcompound5.getInteger("t");
                    int priority = nbttagcompound5.getInteger("p");
                    NextTickListEntry nextticklistentry = new NextTickListEntry(x, y, z, blockid);
                    nextticklistentry.setPriority(priority);
                    if (blockid.getMaterial() != Material.air)
                    {
                        nextticklistentry.setScheduledTime((long)time + p_75823_1_.getTotalWorldTime());
                    }
                    nextTickQueue.offer(nextticklistentry);
                }
            }

        }
        ((WorldUtils)p_75823_1_).getPendingTicks().put(chunkCoordIntPair, nextTickQueue);
        // return chunk;
    }
}
