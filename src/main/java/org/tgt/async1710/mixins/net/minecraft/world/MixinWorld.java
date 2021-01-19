package org.tgt.async1710.mixins.net.minecraft.world;

import cpw.mods.fml.common.FMLLog;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ReportedException;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tgt.async1710.TaskSubmitter;
import org.tgt.async1710.WorldUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Mixin(World.class)
public abstract class MixinWorld implements WorldUtils, TaskSubmitter {

    protected String threadName;

    protected LinkedBlockingQueue<FutureTask<?>> tasks = new LinkedBlockingQueue<>();

    protected Logger logger = LogManager.getLogger(MixinWorld.class);


    @Override
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public void submit(FutureTask<?> task) {
        if (getRunning()) {
            tasks.add(task);
        }
    }

    @Override
    public void cancelTasks() {
        while (!tasks.isEmpty()) {
            tasks.poll().cancel(true);
        }
    }

    @Override
    public void runTasks() {
        while (!tasks.isEmpty()) {
            tasks.poll().run();
        }
    }

    @Shadow
    public abstract void destroyBlockInWorldPartially(int p_147443_1_, int x, int y, int z, int blockDamage);

    @Inject(method = "destroyBlockInWorldPartially", at = @At("HEAD"), cancellable = true)
    public void _destroyBlockInWorldPartially(int p_147443_1_, int x, int y, int z, int blockDamage, CallbackInfo ci) throws ExecutionException, InterruptedException, TimeoutException {
        /**
         * 没问题，我就是扔了两个同样的object
         */
        if (Thread.currentThread().getName() != ((WorldUtils) this).getThreadName() && getRunning()) {
            FutureTask<Integer> ft = new FutureTask<>(() -> {
                destroyBlockInWorldPartially(p_147443_1_, x, y, z, blockDamage);
                return 0;
            });
            submit(ft);
            ft.get(1000, TimeUnit.SECONDS);
            ci.cancel();
        }
    }

    /**
     * NetHandlerPlayserver did patch it, but we need to patch it again
     *
     * @param p_147471_1_
     * @param p_147471_2_
     * @param p_147471_3_
     */
    @Shadow
    public abstract void markBlockForUpdate(int p_147471_1_, int p_147471_2_, int p_147471_3_);

    @Shadow
    public List weatherEffects;

    @Shadow
    public abstract void removeEntity(Entity p_72900_1_);


    protected ConcurrentSet<Entity> loadedEntitySet = new ConcurrentSet<>();

    protected ConcurrentSet<Entity> toBeUnloadedEntitySet = new ConcurrentSet<>();

    @Shadow
    protected abstract boolean chunkExists(int p_72916_1_, int p_72916_2_);

    @Shadow
    public abstract Chunk getChunkFromChunkCoords(int p_72964_1_, int p_72964_2_);

    @Shadow
    public abstract void onEntityRemoved(Entity p_72847_1_);

    @Shadow
    public abstract void updateEntity(Entity p_72870_1_);

    @Shadow
    private boolean processingLoadedTiles;

    protected ConcurrentSet<TileEntity> loadedTileEntitySet = new ConcurrentSet<>();

    protected ConcurrentSet<TileEntity> toBeUnloadedTileEntitySet = new ConcurrentSet<>();

    protected ConcurrentSet<TileEntity> addedTileEntitySet = new ConcurrentSet<>();

    @Shadow
    public abstract boolean blockExists(int p_72899_1_, int p_72899_2_, int p_72899_3_);

    @Shadow
    public abstract boolean setBlockToAir(int x, int y, int z);


    @Shadow
    public List playerEntities;

    @Shadow
    protected List worldAccesses;

    @Shadow
    private List tileEntitiesToBeRemoved;

    @Shadow
    public abstract void onEntityAdded(Entity p_72923_1_);

    /*
    @Inject(method = "markBlockForUpdate", at = @At("HEAD"), cancellable = true)
    public void _markBlockForUpdate(int p_147471_1_, int p_147471_2_, int p_147471_3_, CallbackInfo ci) throws ExecutionException, InterruptedException, TimeoutException {
        if (Thread.currentThread().getName() != ((WorldUtils) this).getThreadName() && getRunning()) {
            FutureTask<Integer> ft = new FutureTask<>(() -> {
                markBlockForUpdate(p_147471_1_, p_147471_2_, p_147471_3_);
                return 0;
            });
            submit(ft);
            ft.get(1000, TimeUnit.SECONDS);
            ci.cancel();
        }
    }
    */


    /**
     * 由于tiles和block的list过大，因此不能直接替换成copyonwrite，仅仅在遍历的时候提供copy
     */
    private void updateWeatherEffects() {
        for (int i = 0; i < weatherEffects.size(); ++i) {
            Entity entity = (Entity) weatherEffects.get(i);

            try {
                ++entity.ticksExisted;
                entity.onUpdate();
            } catch (Throwable t) {
                CrashReport crashreport = CrashReport.makeCrashReport(t, "Ticking entity");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being ticked");

                if (entity == null) {
                    crashreportcategory.addCrashSection("Entity", "~~NULL~~");
                } else {
                    entity.addEntityCrashInfo(crashreportcategory);
                }

                if (ForgeModContainer.removeErroringEntities) {
                    FMLLog.getLogger().log(org.apache.logging.log4j.Level.ERROR, crashreport.getCompleteReport());
                    removeEntity(entity);
                } else {
                    throw new ReportedException(crashreport);
                }
            }

            if (entity.isDead) {
                weatherEffects.remove(i--);
            }
        }
    }

    private void updateNormalEntities() {
        /**
         *  this.loadedEntityList.removeAll(this.unloadedEntityList);
         *         int i;
         *         Entity entity;
         *         int j;
         *         int k;
         *
         *         for (i = 0; i < this.unloadedEntityList.size(); ++i)
         *         {
         *             entity = (Entity)this.unloadedEntityList.get(i);
         *             j = entity.chunkCoordX;
         *             k = entity.chunkCoordZ;
         *
         *             if (entity.addedToChunk && this.chunkExists(j, k))
         *             {
         *                 this.getChunkFromChunkCoords(j, k).removeEntity(entity);
         *             }
         *         }
         *
         *         for (i = 0; i < this.unloadedEntityList.size(); ++i)
         *         {
         *             this.onEntityRemoved((Entity)this.unloadedEntityList.get(i));
         *         }
         */
        Iterator<Entity> iteratorToBeUnloadedEntitySet = toBeUnloadedEntitySet.iterator();
        while (iteratorToBeUnloadedEntitySet.hasNext()) {
            Entity entity = iteratorToBeUnloadedEntitySet.next();
            int x = entity.chunkCoordX;
            int z = entity.chunkCoordZ;

            if (entity.addedToChunk && this.chunkExists(x, z)) {
                getChunkFromChunkCoords(x, z).removeEntity(entity);
            }
            onEntityRemoved(entity);
            iteratorToBeUnloadedEntitySet.remove();
        }

        /**
         *  for(i = 0; i < this.loadedEntityList.size(); ++i) {
         *             entity = (Entity)this.loadedEntityList.get(i);
         *             if (entity.ridingEntity != null) {
         *                 if (!entity.ridingEntity.isDead && entity.ridingEntity.riddenByEntity == entity) {
         *                     continue;
         *                 }
         *
         *                 entity.ridingEntity.riddenByEntity = null;
         *                 entity.ridingEntity = null;
         *             }
         *
         *             this.theProfiler.startSection("tick");
         *             if (!entity.isDead) {
         *                 try {
         *                     this.updateEntity(entity);
         *                 } catch (Throwable var12) {
         *                     crashreport = CrashReport.makeCrashReport(var12, "Ticking entity");
         *                     crashreportcategory = crashreport.makeCategory("Entity being ticked");
         *                     entity.addEntityCrashInfo(crashreportcategory);
         *                     if (!ForgeModContainer.removeErroringEntities) {
         *                         throw new ReportedException(crashreport);
         *                     }
         *
         *                     FMLLog.getLogger().log(Level.ERROR, crashreport.getCompleteReport());
         *                     this.removeEntity(entity);
         *                 }
         *             }
         *
         *             this.theProfiler.endSection();
         *             this.theProfiler.startSection("remove");
         *             if (entity.isDead) {
         *                 j = entity.chunkCoordX;
         *                 l = entity.chunkCoordZ;
         *                 if (entity.addedToChunk && this.chunkExists(j, l)) {
         *                     this.getChunkFromChunkCoords(j, l).removeEntity(entity);
         *                 }
         *
         *                 this.loadedEntityList.remove(i--);
         *                 this.onEntityRemoved(entity);
         *             }
         *
         *             this.theProfiler.endSection();
         *         }
         */
        Iterator<Entity> iteratorLoadedEntites = loadedEntitySet.iterator();
        while (iteratorLoadedEntites.hasNext()) {
            Entity entity = iteratorLoadedEntites.next();

            if (entity.ridingEntity != null) {
                if (!entity.ridingEntity.isDead && entity.ridingEntity.riddenByEntity == entity) {
                    continue;
                }

                entity.ridingEntity.riddenByEntity = null;
                entity.ridingEntity = null;
            }


            if (!entity.isDead) {
                try {
                    updateEntity(entity);
                } catch (Throwable throwable1) {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being ticked");
                    entity.addEntityCrashInfo(crashreportcategory);

                    if (ForgeModContainer.removeErroringEntities) {
                        FMLLog.getLogger().log(Level.ERROR, crashreport.getCompleteReport());
                        removeEntity(entity);
                    } else {
                        throw new ReportedException(crashreport);
                    }
                }
            }


            if (entity.isDead) {
                int x = entity.chunkCoordX;
                int z = entity.chunkCoordZ;

                if (entity.addedToChunk && this.chunkExists(x, z)) {
                    this.getChunkFromChunkCoords(x, z).removeEntity(entity);
                }
                this.onEntityRemoved(entity);
                iteratorLoadedEntites.remove();
            }
        }
    }

    public void tickTileEnitites() {
        this.processingLoadedTiles = true;
        Iterator<TileEntity> loadedTileEntitySetIterator = loadedTileEntitySet.iterator();
        while (loadedTileEntitySetIterator.hasNext()) {
            TileEntity tileentity = loadedTileEntitySetIterator.next();
            if (!tileentity.isInvalid() && tileentity.hasWorldObj() && this.blockExists(tileentity.xCoord, tileentity.yCoord, tileentity.zCoord)) {
                try {
                    tileentity.updateEntity();
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Ticking block entity");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Block entity being ticked");
                    tileentity.addInfoToCrashReport(crashreportcategory);
                    if (ForgeModContainer.removeErroringTileEntities) {
                        FMLLog.getLogger().log(org.apache.logging.log4j.Level.ERROR, crashreport.getCompleteReport());
                        tileentity.invalidate();
                        setBlockToAir(tileentity.xCoord, tileentity.yCoord, tileentity.zCoord);
                    } else {
                        throw new ReportedException(crashreport);
                    }
                }
            }

            if (tileentity.isInvalid()) {
                toBeUnloadedTileEntitySet.add(tileentity);
                loadedTileEntitySetIterator.remove();
                if (this.chunkExists(tileentity.xCoord >> 4, tileentity.zCoord >> 4)) {
                    Chunk chunk = this.getChunkFromChunkCoords(tileentity.xCoord >> 4, tileentity.zCoord >> 4);

                    if (chunk != null) {
                        chunk.removeInvalidTileEntity(tileentity.xCoord & 15, tileentity.yCoord, tileentity.zCoord & 15);
                    }
                }
            }
        }

        Iterator<TileEntity> iteratorToBeUnloadedTileEntitySet = toBeUnloadedTileEntitySet.iterator();
        while (iteratorToBeUnloadedTileEntitySet.hasNext()) {
            TileEntity next = iteratorToBeUnloadedTileEntitySet.next();
            next.onChunkUnload();
            iteratorToBeUnloadedTileEntitySet.remove();
        }

        this.processingLoadedTiles = false;
    }

    private void addNewTileEntities() {
        Iterator<TileEntity> iterator = addedTileEntitySet.iterator();
        while (iterator.hasNext()) {
            TileEntity tile = iterator.next();
            if (!tile.isInvalid()) {
                if (!this.loadedTileEntitySet.contains(tile)) {
                    this.loadedTileEntitySet.add(tile);
                }
            } else {
                if (this.chunkExists(tile.xCoord >> 4, tile.zCoord >> 4)) {
                    Chunk chunk1 = this.getChunkFromChunkCoords(tile.xCoord >> 4, tile.zCoord >> 4);

                    if (chunk1 != null) {
                        chunk1.removeInvalidTileEntity(tile.xCoord & 15, tile.yCoord, tile.zCoord & 15);
                    }
                }
            }
            iterator.remove();
        }
    }

    /**
     * @author lyt
     * @reason la ji dai ma cao ni ma
     */
    @Overwrite
    public void updateEntities() {

        updateWeatherEffects();
        addNewTileEntities();
        tickTileEnitites();
        updateNormalEntities();
    }

    /**
     * playercopy on write 开销小
     *
     * @param p_i45369_1_
     * @param p_i45369_2_
     * @param p_i45369_3_
     * @param p_i45369_4_
     * @param p_i45369_5_
     * @param ci
     */
    @Inject(method = "<init>(Lnet/minecraft/world/storage/ISaveHandler;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/world/WorldProvider;Lnet/minecraft/profiler/Profiler;)V", at = @At("RETURN"))
    public void init(ISaveHandler p_i45369_1_, String p_i45369_2_, WorldSettings p_i45369_3_, WorldProvider p_i45369_4_, Profiler p_i45369_5_, CallbackInfo ci) {
        playerEntities = new CopyOnWriteArrayList();
        worldAccesses = new CopyOnWriteArrayList();
        weatherEffects = new CopyOnWriteArrayList();
    }

    //重定向loadedEntityList
    //public boolean spawnEntityInWorld(Entity p_72838_1_)

    @Redirect(method = "spawnEntityInWorld", at = @At(value = "INVOKE", ordinal = 1, target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false))
    public <E> boolean _spawnEntityInWorld1(List<E> list, E e) {
        return loadedEntitySet.add((Entity) e);
    }

    //public void removePlayerEntityDangerously(Entity p_72973_1_)
    @Redirect(method = "removePlayerEntityDangerously", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(Ljava/lang/Object;)Z", remap = false))
    public <E> boolean _removePlayerEntityDangerously(List<E> list, E e) {
        return loadedEntitySet.remove((Entity) e);
    }

    //重定向unloadedEntityList
    //    public void unloadEntities(List p_72828_1_)

    @Redirect(method = "unloadEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;addAll(Ljava/util/Collection;)Z", remap = false))
    public <E> boolean _unloadEntities(List list, Collection<? extends E> c) {
        return toBeUnloadedEntitySet.addAll((Collection<? extends Entity>) c);
    }
    //重定向loadedTileEntityList
    //重定向addedTileEntityList

    /**
     * @param p_147448_1_
     * @author lyt
     * @reason 带if太恶心，直接换掉
     */
    @Overwrite
    public void func_147448_a(Collection p_147448_1_) {
        Set<TileEntity> dest = processingLoadedTiles ? addedTileEntitySet : loadedTileEntitySet;
        for (TileEntity entity : (Collection<TileEntity>) p_147448_1_) {
            if (entity.canUpdate()) dest.add(entity);
        }
    }

    /**
     * @author lyt
     * @reason 带if太恶心，直接换掉
     */
    @Overwrite(remap = false)
    public void addTileEntity(TileEntity entity) {
        Set<TileEntity> dest = processingLoadedTiles ? addedTileEntitySet : loadedTileEntitySet;
        if (entity.canUpdate()) {
            dest.add(entity);
        }
    }

    //    public void setTileEntity(int x, int y, int z, TileEntity tileEntityIn)
    @Redirect(method = "setTileEntity", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0, remap = false))
    public <E> boolean _setTileEntity0(List<E> list, E e) {
        return addedTileEntitySet.add((TileEntity) e);
    }

    @Redirect(method = "setTileEntity", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1, remap = false))
    public <E> boolean _setTileEntity1(List<E> list, E e) {
        return loadedTileEntitySet.add((TileEntity) e);
    }

    @Redirect(method = "setTileEntity", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false))
    public <E> Iterator<E> _setTileEntity2(List<E> list) {
        return (Iterator<E>) addedTileEntitySet.iterator();
    }


    /**
     * @param x
     * @param y
     * @param z
     * @return
     * @author lyt
     * @reason fori 改不来的
     */
    @Overwrite
    public TileEntity getTileEntity(int x, int y, int z) {
        if (y >= 0 && y < 256) {
            TileEntity rtn = null;
            if (this.processingLoadedTiles) {
                for (TileEntity tileEntity : addedTileEntitySet) {
                    if (!tileEntity.isInvalid() && tileEntity.xCoord == x && tileEntity.yCoord == y && tileEntity.zCoord == z) {
                        rtn = tileEntity;
                        break;
                    }
                }
            }

            if (rtn == null) {
                Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);

                if (chunk != null) {
                    rtn = chunk.getBlockTileEntityInChunk(x & 15, y, z & 15);
                }
            }
            for (TileEntity tileEntity : addedTileEntitySet) {
                if (!tileEntity.isInvalid() && tileEntity.xCoord == x && tileEntity.yCoord == y && tileEntity.zCoord == z) {
                    rtn = tileEntity;
                    break;
                }
            }
            return rtn;
        } else {
            return null;
        }
    }


    //重定向tileEntitiesToBeRemoved
    //public void markTileEntityForRemoval(TileEntity tileEntityIn)
    @Redirect(method = "markTileEntityForRemoval", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false))
    public <E> boolean _markTileEntityForRemoval(List list, E e) {
        return tileEntitiesToBeRemoved.add(e);
    }

    @Inject(method = "countEntities(Ljava/lang/Class;)I", at = @At("HEAD"), cancellable = true, remap = false)
    public void countEntities1(Class p_72907_1_, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) loadedEntitySet.stream().filter(e -> (!(e instanceof EntityLiving) || !((EntityLiving)e).isNoDespawnRequired()) && p_72907_1_.isAssignableFrom(e.getClass())).count());
    }
    @Inject(method = "countEntities(Lnet/minecraft/entity/EnumCreatureType;Z)I", at = @At("HEAD"), cancellable = true, remap = false)
    public void countEntities2(EnumCreatureType type, boolean forSpawnCount, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) loadedEntitySet.stream().filter(e -> e.isCreatureType(type, forSpawnCount)).count());
    }
    @Inject(method = "addLoadedEntities", at = @At("HEAD"), cancellable = true)
    public void addLoadedEntities(List p_72868_1_, CallbackInfo ci) {
        for (Object o : p_72868_1_) {
            Entity entity = (Entity)o;
            if (!MinecraftForge.EVENT_BUS.post(new EntityJoinWorldEvent(entity, (World) (Object)this))) {
                this.loadedEntitySet.add(entity);
                this.onEntityAdded(entity);
            }
        }
        ci.cancel();
    }
}
