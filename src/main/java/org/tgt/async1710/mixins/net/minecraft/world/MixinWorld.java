package org.tgt.async1710.mixins.net.minecraft.world;

import com.google.common.collect.ImmutableList;
import cpw.mods.fml.common.FMLLog;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.*;
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
import org.tgt.async1710.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Mixin(World.class)
public abstract class MixinWorld implements WorldUtils, TaskSubmitter {

    protected String threadName;

    protected LinkedBlockingQueue<FutureTask<?>> tasks = new LinkedBlockingQueue<>();

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
    public List weatherEffects;

    @Shadow
    public abstract void removeEntity(Entity p_72900_1_);


    protected Set<Entity> loadedEntitySet = new ReadWriteLockedSet<>(new HashSet<>());

    protected Set<Entity> toBeUnloadedEntitySet = new ReadWriteLockedSet<>(new HashSet<>());

    @Shadow
    protected abstract boolean chunkExists(int p_72916_1_, int p_72916_2_);

    @Shadow
    public abstract Chunk getChunkFromChunkCoords(int p_72964_1_, int p_72964_2_);

    @Shadow
    public abstract void onEntityRemoved(Entity p_72847_1_);

    @Shadow
    public abstract void updateEntity(Entity p_72870_1_);

    @Shadow
    public abstract boolean blockExists(int p_72899_1_, int p_72899_2_, int p_72899_3_);

    @Shadow
    public abstract boolean setBlockToAir(int x, int y, int z);


    @Shadow
    public List playerEntities;

    @Shadow
    protected List worldAccesses;

    @Shadow
    public abstract void onEntityAdded(Entity p_72923_1_);


    @Shadow public abstract Block getBlock(int p_147439_1_, int p_147439_2_, int p_147439_3_);

    @Shadow public abstract List getEntitiesWithinAABBExcludingEntity(Entity p_72839_1_, AxisAlignedBB p_72839_2_);

    @Shadow public abstract void updateNeighborsAboutBlockChange(int x, int yPos, int z, Block blockIn);

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
         *                     entity.addEntityCrashInfo(crashreportcategory);NP
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
        ((ReadWriteLockedSet<Entity>)loadedEntitySet).foreachWithRemove(
                (e) -> {
                    if (e.ridingEntity != null) {
                        e.ridingEntity.riddenByEntity = null;
                        e.ridingEntity = null;
                    }
                    try {
                        updateEntity(e);
                    } catch (Throwable throwable1) {
                        CrashReport crashreport = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                        CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being ticked");
                        e.addEntityCrashInfo(crashreportcategory);
                        FMLLog.getLogger().log(Level.ERROR, crashreport.getCompleteReport());
                        if (ForgeModContainer.removeErroringEntities) {
                            this.removeEntity(e);
                        } else {
                            throw new ReportedException(crashreport);
                        }
                    }
                }
        , (entity) -> {
                    if(entity.isDead) {
                        int x = entity.chunkCoordX;
                        int z = entity.chunkCoordZ;

                        if (entity.addedToChunk && this.chunkExists(x, z)) {
                            this.getChunkFromChunkCoords(x, z).removeEntity(entity);
                        }
                        this.onEntityRemoved(entity);
                    }
                    return entity.isDead;
                });
    }

    public void tickTileEnitites() {
        chunkTileEntitiyListMap.forEach((chunkCoord, tileGroup) -> {
            tileGroup.setProcessingLoadedTiles(true);
            Iterator<TileEntity> loadedTileEntitySetIterator = tileGroup.getLoadedTiles().iterator();
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
                    tileGroup.getRemovingTiles().add(tileentity);
                    loadedTileEntitySetIterator.remove();
                    if (this.chunkExists(tileentity.xCoord >> 4, tileentity.zCoord >> 4)) {
                        Chunk chunk = this.getChunkFromChunkCoords(tileentity.xCoord >> 4, tileentity.zCoord >> 4);

                        if (chunk != null) {
                            chunk.removeInvalidTileEntity(tileentity.xCoord & 15, tileentity.yCoord, tileentity.zCoord & 15);
                        }
                    }
                }
            }

            Iterator<TileEntity> iteratorToBeUnloadedTileEntitySet = tileGroup.getRemovingTiles().iterator();
            while (iteratorToBeUnloadedTileEntitySet.hasNext()) {
                TileEntity next = iteratorToBeUnloadedTileEntitySet.next();
                next.onChunkUnload();
                iteratorToBeUnloadedTileEntitySet.remove();
            }
            tileGroup.setProcessingLoadedTiles(false);
        });

    }

    private void addNewTileEntities() {
        chunkTileEntitiyListMap.forEach((chunkCoordIntPair, chunkTileGroup) -> {
            ReadWriteLockedSet<TileEntity> loadingTiles = (ReadWriteLockedSet<TileEntity>) chunkTileGroup.getLoadingTiles();
            Set<TileEntity> loadedTiles =  chunkTileGroup.getLoadedTiles();
            loadingTiles.foreachWithRemove((tile) -> {
                if (!tile.isInvalid()) {
                    if (!loadedTiles.contains(tile)) {
                        loadedTiles.add(tile);
                    }
                } else {
                    if (this.chunkExists(tile.xCoord >> 4, tile.zCoord >> 4)) {
                        Chunk chunk1 = this.getChunkFromChunkCoords(tile.xCoord >> 4, tile.zCoord >> 4);

                        if (chunk1 != null) {
                            chunk1.removeInvalidTileEntity(tile.xCoord & 15, tile.yCoord, tile.zCoord & 15);
                        }
                    }
                }
            }, (tile) -> true);
        });

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
     * 这个似乎还是会慢点，但是还好，大部分都是其他瓶颈
     */
    @Overwrite
    public void func_147448_a(Collection<TileEntity> p_147448_1_) {
        for (TileEntity tileEntity : p_147448_1_) {
            ChunkTileGroup chunkTileGroup = chunkTileEntitiyListMap.get(new ChunkCoordIntPair(tileEntity.xCoord >> 4, tileEntity.zCoord >> 4));
            if(chunkTileGroup != null) {
                Set<TileEntity> dest = chunkTileGroup.getProcessingLoadedTiles() ? chunkTileGroup.getLoadingTiles() : chunkTileGroup.getLoadedTiles();
                if (tileEntity.canUpdate()) {
                    dest.add(tileEntity);
                }
            }
        }
    }

    /**
     * @author lyt
     * @reason 带if太恶心，直接换掉
     */
    @Overwrite(remap = false)
    public void addTileEntity(TileEntity tileEntity) {
        ChunkTileGroup chunkTileGroup = chunkTileEntitiyListMap.get(new ChunkCoordIntPair(tileEntity.xCoord >> 4, tileEntity.zCoord >> 4));
        if(chunkTileGroup != null) {
            Set<TileEntity> dest = chunkTileGroup.getProcessingLoadedTiles() ? chunkTileGroup.getLoadingTiles() : chunkTileGroup.getLoadedTiles();
            if (tileEntity.canUpdate()) {
                dest.add(tileEntity);
            }
        }
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

    /**
     * @author lyt
     * @reason 局部变量axisAlignedBBS
     * @param p_72945_1_
     * @param p_72945_2_
     * @return
     */
    @Overwrite
    public List getCollidingBoundingBoxes(Entity p_72945_1_, AxisAlignedBB p_72945_2_) {
        ArrayList<AxisAlignedBB> axisAlignedBBS = new ArrayList<>();
        int i = MathHelper.floor_double(p_72945_2_.minX);
        int j = MathHelper.floor_double(p_72945_2_.maxX + 1.0D);
        int k = MathHelper.floor_double(p_72945_2_.minY);
        int l = MathHelper.floor_double(p_72945_2_.maxY + 1.0D);
        int i1 = MathHelper.floor_double(p_72945_2_.minZ);
        int j1 = MathHelper.floor_double(p_72945_2_.maxZ + 1.0D);

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = i1; l1 < j1; ++l1)
            {
                if (this.blockExists(k1, 64, l1))
                {
                    for (int i2 = k - 1; i2 < l; ++i2)
                    {
                        Block block;

                        if (k1 >= -30000000 && k1 < 30000000 && l1 >= -30000000 && l1 < 30000000)
                        {
                            block = getBlock(k1, i2, l1);
                        }
                        else
                        {
                            block = Blocks.stone;
                        }

                        block.addCollisionBoxesToList((World)(Object)this, k1, i2, l1, p_72945_2_, axisAlignedBBS, p_72945_1_);
                    }
                }
            }
        }

        double d0 = 0.25D;
        List list = getEntitiesWithinAABBExcludingEntity(p_72945_1_, p_72945_2_.expand(d0, d0, d0));

        for (int j2 = 0; j2 < list.size(); ++j2)
        {
            AxisAlignedBB axisalignedbb1 = ((Entity)list.get(j2)).getBoundingBox();

            if (axisalignedbb1 != null && axisalignedbb1.intersectsWith(p_72945_2_))
            {
                axisAlignedBBS.add(axisalignedbb1);
            }

            axisalignedbb1 = p_72945_1_.getCollisionBox((Entity)list.get(j2));

            if (axisalignedbb1 != null && axisalignedbb1.intersectsWith(p_72945_2_))
            {
                axisAlignedBBS.add(axisalignedbb1);
            }
        }

        return axisAlignedBBS;
    }

    /**
     * @author lyt
     * @reason 局部变量
     * axisAlignedBBS
     * @param p_147461_1_
     */
    @Inject(method = "func_147461_a", at = @At("HEAD"), cancellable = true)
    public void func_147461_a(AxisAlignedBB p_147461_1_, CallbackInfoReturnable<List> cir)
    {
        ArrayList<AxisAlignedBB> axisAlignedBBS = new ArrayList<>();
        int i = MathHelper.floor_double(p_147461_1_.minX);
        int j = MathHelper.floor_double(p_147461_1_.maxX + 1.0D);
        int k = MathHelper.floor_double(p_147461_1_.minY);
        int l = MathHelper.floor_double(p_147461_1_.maxY + 1.0D);
        int i1 = MathHelper.floor_double(p_147461_1_.minZ);
        int j1 = MathHelper.floor_double(p_147461_1_.maxZ + 1.0D);

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = i1; l1 < j1; ++l1)
            {
                if (this.blockExists(k1, 64, l1))
                {
                    for (int i2 = k - 1; i2 < l; ++i2)
                    {
                        Block block;

                        if (k1 >= -30000000 && k1 < 30000000 && l1 >= -30000000 && l1 < 30000000)
                        {
                            block = this.getBlock(k1, i2, l1);
                        }
                        else
                        {
                            block = Blocks.bedrock;
                        }

                        block.addCollisionBoxesToList((World)(Object)this, k1, i2, l1, p_147461_1_, axisAlignedBBS, (Entity)null);
                    }
                }
            }
        }
        cir.setReturnValue(axisAlignedBBS);
    }


    /**
     * tile其实应该也类似,用一个区块做key的集合比较好，它们都不能动。
     *
     */
    private ConcurrentHashMap<ChunkCoordIntPair, ChunkTileGroup> chunkTileEntitiyListMap = new ConcurrentHashMap<>();

    /**
     * @param x
     * @param y
     * @param z
     * @author lyt
     * @reason fori 改不来的
     */
    @Inject(method = "getTileEntity", at = @At("HEAD"), cancellable = true)
    public void getTileEntity(int x, int y, int z, CallbackInfoReturnable<TileEntity> cir) {
        TileEntity rtn = null;
        if (y >= 0 && y < 256)
        {
            ChunkCoordIntPair coord = new ChunkCoordIntPair(x >> 4, z >> 4);
            ChunkTileGroup chunkTileGroup = chunkTileEntitiyListMap.get(coord);
            if(chunkTileGroup != null) {
                Set<TileEntity> loadedTiles = chunkTileGroup.getLoadedTiles();
                Set<TileEntity> loadingTiles = chunkTileGroup.getLoadingTiles();
                if (chunkTileGroup.getProcessingLoadedTiles()) {
                    for (TileEntity loadingTile : loadingTiles) {
                        if (!loadingTile.isInvalid() && loadingTile.xCoord == x && loadingTile.yCoord == y && loadingTile.zCoord == z) {
                            rtn = loadingTile;
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
                if (rtn == null) {
                    for (TileEntity loadedTile : loadedTiles) {
                        if (!loadedTile.isInvalid() && loadedTile.xCoord == x && loadedTile.yCoord == y && loadedTile.zCoord == z) {
                            rtn = loadedTile;
                            break;
                        }
                    }
                }
            }
        }
        cir.setReturnValue(rtn);
    }

    @Inject(method = "setTileEntity", at = @At("HEAD"), cancellable = true, remap = false)
    public void _setTileEntity(int x, int y, int z, TileEntity tileEntityIn, CallbackInfo ci) {
        if (tileEntityIn == null || tileEntityIn.isInvalid())
        {
            return;
        }
        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(x >> 4, z >> 4);
        ChunkTileGroup chunkTileGroup = chunkTileEntitiyListMap.get(chunkCoordIntPair);
        if(chunkTileGroup == null) {
            chunkTileGroup = new ChunkTileGroup();
            chunkTileEntitiyListMap.put(chunkCoordIntPair, chunkTileGroup);
        }
        if (tileEntityIn.canUpdate())
        {
            if (chunkTileGroup.getProcessingLoadedTiles())
            {
                Set<TileEntity> loadedTiles = chunkTileGroup.getLoadedTiles();
                if(loadedTiles.contains(tileEntityIn)) {
                    TileEntity another = null;
                    for (TileEntity loadedTile : loadedTiles) {
                        if(loadedTile.equals(tileEntityIn)) {
                            another = loadedTile;
                            break;
                        }
                    }
                    //no NPE
                    another.invalidate();
                    chunkTileGroup.getLoadingTiles().add(tileEntityIn);
                }
            }
            else
            {
                chunkTileGroup.getLoadedTiles().add(tileEntityIn);
            }
        }
        Chunk chunk = getChunkFromChunkCoords(x >> 4, z >> 4);
        if (chunk != null)
        {
            chunk.setBlockTileEntityInChunk(x & 15, y, z & 15, tileEntityIn);
        }
        //notify tile changes
        updateNeighborsAboutBlockChange(x, y, z, getBlock(x, y, z));
        ci.cancel();
    }

    //重定向tileEntitiesToBeRemoved
    //public void markTileEntityForRemoval(TileEntity tileEntityIn)
    @Redirect(method = "markTileEntityForRemoval", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false))
    public <E> boolean _markTileEntityForRemoval(List list, E e) {
        TileEntity tileEntity = (TileEntity) e;
        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(tileEntity.xCoord >> 4, tileEntity.zCoord >> 4);
        ChunkTileGroup chunkTileGroup = chunkTileEntitiyListMap.get(chunkCoordIntPair);
        if(chunkTileGroup != null) {
            return chunkTileGroup.getRemovingTiles().add((TileEntity) e);
        } else {
            return false;
        }
    }
}
