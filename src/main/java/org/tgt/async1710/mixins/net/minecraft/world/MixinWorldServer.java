package org.tgt.async1710.mixins.net.minecraft.world;

import com.google.common.collect.Lists;
import cpw.mods.fml.common.FMLCommonHandler;
import io.micrometer.core.instrument.Timer;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.ReportedException;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tgt.async1710.MonitorRegistry;
import org.tgt.async1710.TaskSubmitter;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer extends World implements Runnable {
    public MixinWorldServer(ISaveHandler p_i45368_1_, String p_i45368_2_, WorldProvider p_i45368_3_, WorldSettings p_i45368_4_, Profiler p_i45368_5_) {
        super(p_i45368_1_, p_i45368_2_, p_i45368_3_, p_i45368_4_, p_i45368_5_);
    }

    private long lastWarn = 0;

    @Shadow
    @Final
    private MinecraftServer mcServer;

    protected int tickCounter;

    @Shadow
    @Final
    private EntityTracker theEntityTracker;

    private final Logger mixinLogger = LogManager.getLogger(getClass());

    @Shadow
    public abstract void tick();


    @Shadow
    public abstract void updateEntities();

    @Shadow
    public abstract void saveAllChunks(boolean p_73044_1_, IProgressUpdate p_73044_2_) throws MinecraftException;

    private Timer blockTickTimer;

    private Timer saveChunksTimer;

    private Timer trackEntityTimer;

    private Timer tickTimer;


    /**
     * block tick用一个区块做key的集合比较好，这样可以按照区块去tick block，保证chunk的unload发生在tick的最后。
     */
    private ConcurrentHashMap<ChunkCoordIntPair, Queue<NextTickListEntry>> chunkTickListMap = new ConcurrentHashMap<>();

    private boolean running = false;

    public void allTick() {
        long i = System.nanoTime();
        if (tickCounter % 20 == 0) {
            ((DedicatedServer) mcServer).getConfigurationManager().
                    sendPacketToAllPlayersInDimension(
                            new S03PacketTimeUpdate(getTotalWorldTime(),
                                    getWorldTime(),
                                    getGameRules().getGameRuleBooleanValue("doDaylightCycle")),
                            provider.dimensionId);
        }
        FMLCommonHandler.instance().onPreWorldTick((WorldServer) (Object) this);

        try {
            tick();
        } catch (Throwable throwable1) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable1, "Exception ticking world");
            addWorldInfoToCrashReport(crashreport);
            throw new ReportedException(crashreport);
        }

        long timeMillis = System.currentTimeMillis();
        try {
            updateEntities();
        } catch (Throwable throwable) {
            CrashReport crashreport1 = CrashReport.makeCrashReport(throwable, "Exception ticking world entities");
            addWorldInfoToCrashReport(crashreport1);
            throw new ReportedException(crashreport1);
        }
        if (provider.dimensionId == 0) {
            long l = System.currentTimeMillis() - timeMillis;
            if (l > 300) {
                mixinLogger.info("world entity tick time = {}", l);
            }
        }


        trackEntityTimer.record(() -> {
            theEntityTracker.updateTrackedEntities();
        });

        FMLCommonHandler.instance().instance().onPostWorldTick((WorldServer) (Object) this);
        this.chunkProvider.unloadQueuedChunks();
        if (this.tickCounter % 900 == 0) {
            saveChunksTimer.record(() -> {
                try {
                    this.saveAllChunks(true, null);
                } catch (MinecraftException e) {
                    mixinLogger.error("error saving world!", e);
                }
            });
        }
        ((TaskSubmitter) this).runTasks();
        /**
         * for unload
         */
        long[] longs = mcServer.worldTickTimes.get(provider.dimensionId);
        if (longs != null) {
            longs[this.tickCounter % 100] = System.nanoTime() - i;
        }
    }

    private void initialChunkLoad() {
        ChunkCoordinates chunkcoordinates = getSpawnPoint();
        mixinLogger.info("Preparing start region for level " + provider.dimensionId);
        for (int k = -192; k <= 192; k += 16) {
            for (int l = -192; l <= 192; l += 16) {
                chunkProvider.loadChunk(chunkcoordinates.posX + k >> 4, chunkcoordinates.posZ + l >> 4);
            }
        }
    }


    @Override
    public void run() {
        blockTickTimer = MonitorRegistry.getInstance().timer("blocks", "thread", Thread.currentThread().getName());
        saveChunksTimer = MonitorRegistry.getInstance().timer("saveChunks", "thread", Thread.currentThread().getName());
        trackEntityTimer = MonitorRegistry.getInstance().timer("trackEntities", "thread", Thread.currentThread().getName());
        tickTimer = MonitorRegistry.getInstance().timer("worldServerTick", "thread", Thread.currentThread().getName());
        try {
            initialChunkLoad();
            running = true;
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(this));
            while (running) {
                tickTimer.record(() -> {
                    long timeMillis = System.currentTimeMillis();
                    ++tickCounter;
                    allTick();
                    long now = System.currentTimeMillis();
                    long elapsed = now - timeMillis;
                    long remains = 50 - elapsed;
                    if (remains > 0) {
                        try {
                            Thread.sleep(remains);
                        } catch (InterruptedException e) {
                        }
                    } else {
                        if (now - lastWarn > 15000 && elapsed > 2000) {
                            mixinLogger.warn("Can't keep up! Did the system time change, or is the server overloaded? Running {}ms behind, skipping {} tick(s)", elapsed, elapsed / 50L);
                            lastWarn = now;
                        }
                    }
                });
            }
        } catch (ReportedException e) {
            //reported Exception when ticking
            //do cleaning on error
            mixinLogger.error("fatal error.", e);
            ((TaskSubmitter) this).cancelTasks();
            for (Object playerEntity : playerEntities) {
                String collect = Arrays.stream(e.getStackTrace()).map(t -> t.toString()).collect(Collectors.joining("\n"));
                ((EntityPlayerMP) playerEntity).playerNetServerHandler.kickPlayerFromServer("Please reconnect, fatal world ticking exception: \n" + collect);
            }
        } finally {
            mixinLogger.info("Unloading world, exiting world thread");
            try {
                saveAllChunks(true, null);
            } catch (MinecraftException minecraftException) {
                mixinLogger.error("fatal error in exception handling! aborting.", minecraftException);
            } finally {
                flush();
                DimensionManager.setWorld(provider.dimensionId, null);
                MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(this));
            }
        }
    }

    @Shadow
    public abstract List func_147486_a(int p_147486_1_, int p_147486_2_, int p_147486_3_, int p_147486_4_, int p_147486_5_, int p_147486_6_);

    @Shadow
    public abstract void flush();

    @Shadow
    @Final
    private static Logger logger;

    @Shadow
    public List<Teleporter> customTeleporters;

    @Shadow
    protected Set<ChunkCoordIntPair> doneChunks;

    /**
     * 继承接口，但是接口在超类的mixin
     */
    public void stop() {
        running = false;
    }

    /**
     * 继承接口，但是接口在超类的mixin
     */
    public boolean getRunning() {
        return running;
    }

    /**
     * 替换集合
     *
     * @param p_i45284_1_
     * @param p_i45284_2_
     * @param p_i45284_3_
     * @param p_i45284_4_
     * @param p_i45284_5_
     * @param p_i45284_6_
     * @param ci
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(MinecraftServer p_i45284_1_, ISaveHandler p_i45284_2_, String p_i45284_3_, int p_i45284_4_, WorldSettings p_i45284_5_, Profiler p_i45284_6_, CallbackInfo ci) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        doneChunks = new ConcurrentSet<>();
        customTeleporters = new CopyOnWriteArrayList<>();
    }


    /**
     * block tick相关
     */
    @Inject(method = "isBlockTickScheduledThisTick", at = @At(value = "INVOKE"), cancellable = true)
    public void isBlockTickScheduledThisTick(int p_147477_1_, int p_147477_2_, int p_147477_3_, Block p_147477_4_, CallbackInfoReturnable<Boolean> cir) {
        NextTickListEntry nextticklistentry = new NextTickListEntry(p_147477_1_, p_147477_2_, p_147477_3_, p_147477_4_);
        Queue<NextTickListEntry> nextTickListEntries = chunkTickListMap.get(new ChunkCoordIntPair(p_147477_1_ >> 4, p_147477_3_ >> 4));
        cir.setReturnValue(nextTickListEntries != null && nextTickListEntries.contains(nextticklistentry));
    }

    /**
     * 这个是从区块反序列化nexttick
     *
     * @param p_147446_1_ x
     * @param p_147446_2_ y
     * @param p_147446_3_ z
     * @param p_147446_4_
     * @param p_147446_5_
     * @param p_147446_6_
     * @param ci
     */
    @Inject(method = "func_147446_b", at = @At(value = "INVOKE"), cancellable = true, remap = false)
    public void func_147446_b(int p_147446_1_, int p_147446_2_, int p_147446_3_, Block p_147446_4_, int p_147446_5_, int p_147446_6_, CallbackInfo ci) {
        NextTickListEntry nextticklistentry = new NextTickListEntry(p_147446_1_, p_147446_2_, p_147446_3_, p_147446_4_);
        nextticklistentry.setPriority(p_147446_6_);

        if (p_147446_4_.getMaterial() != Material.air) {
            nextticklistentry.setScheduledTime((long) p_147446_5_ + this.worldInfo.getWorldTotalTime());
        }
        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(p_147446_1_ >> 4, p_147446_3_ >> 4);
        Queue<NextTickListEntry> nextTickListEntries = chunkTickListMap.get(chunkCoordIntPair);
        if (nextTickListEntries == null) {
            nextTickListEntries = new ConcurrentLinkedQueue<>();
            chunkTickListMap.put(chunkCoordIntPair, nextTickListEntries);
        }
        nextTickListEntries.add(nextticklistentry); //区块刚加出来，判你个鬼
        //hashset大概也是这个意思吧，通过这个监测有没有重复block tick的存在
        /**
         if (!this.pendingTickListEntriesHashSet.contains(nextticklistentry))
         {
         this.pendingTickListEntriesHashSet.add(nextticklistentry);
         this.pendingTickListEntriesTreeSet.add(nextticklistentry);
         }**/
        ci.cancel();
    }


    /**
     * 当前block坐标 +-20范围内的block updates
     * 是拿来持久化当前区块的方块更新的，那就简单了.jpg
     */
    @Inject(method = "getPendingBlockUpdates", at = @At(value = "INVOKE"), cancellable = true)
    public void getPendingBlockUpdates(Chunk p_72920_1_, boolean p_72920_2_, CallbackInfoReturnable<List> cir) {
        ArrayList arraylist = null;
        ChunkCoordIntPair chunkcoordintpair = p_72920_1_.getChunkCoordIntPair();
        Queue<NextTickListEntry> nextTickListEntries = chunkTickListMap.get(chunkcoordintpair);
        if (nextTickListEntries != null) {
            arraylist = Lists.newArrayList(nextTickListEntries);
        }
        cir.setReturnValue(arraylist);
    }


    /**
     * 方块更新etc
     *
     * @param p_72955_1_
     */
    @Inject(method = "tickUpdates", at = @At("HEAD"), cancellable = true)
    public void tickUpdates(boolean p_72955_1_, CallbackInfoReturnable<Boolean> cir) {
        blockTickTimer.record(() -> {
            chunkTickListMap.forEach(this::tickAChunk);
            cir.setReturnValue(true);
        });
    }

    /**
     * 永远安全，chunk的卸载将全部被挤到最后在做，在那之前所有的方块都有机会被tick
     *
     * @param pair
     * @param queue
     */
    public void tickAChunk(ChunkCoordIntPair pair, Queue<NextTickListEntry> queue) {
        while (queue.peek() != null && queue.peek().scheduledTime <= worldInfo.getWorldTime()) {
            NextTickListEntry nextticklistentry = queue.poll();
            Block block = this.getBlock(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord);

            if (block.getMaterial() != Material.air && Block.isEqualTo(block, nextticklistentry.func_151351_a())) {
                try {
                    block.updateTick(this, nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord, this.rand);
                } catch (Throwable throwable1) {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable1, "Exception while ticking a block");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being ticked");
                    int k;

                    try {
                        k = this.getBlockMetadata(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord);
                    } catch (Throwable throwable) {
                        k = -1;
                    }

                    CrashReportCategory.addBlockInfo(crashreportcategory, nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord, block, k);
                    throw new ReportedException(crashreport);
                }
            }
        }
    }

    /**
     * 触发方块更新
     *
     * @param x
     * @param y
     * @param z
     * @param block
     * @param p_147454_5_
     * @param p_147454_6_
     * @param ci
     */
    @Inject(method = "scheduleBlockUpdateWithPriority", at = @At("HEAD"), cancellable = true)
    public void _scheduleBlockUpdateWithPriority(int x, int y, int z, Block block, int p_147454_5_, int p_147454_6_, CallbackInfo ci) {
        NextTickListEntry nextticklistentry = new NextTickListEntry(x, y, z, block);
        //Keeping here as a note for future when it may be restored.
        //boolean isForced = getPersistentChunks().containsKey(new ChunkCoordIntPair(nextticklistentry.xCoord >> 4, nextticklistentry.zCoord >> 4));
        //byte b0 = isForced ? 0 : 8;
        byte b0 = 0;

        if (this.scheduledUpdatesAreImmediate && block.getMaterial() != Material.air) {
            if (block.requiresUpdates()) {
                b0 = 8;

                if (this.checkChunksExist(nextticklistentry.xCoord - b0, nextticklistentry.yCoord - b0, nextticklistentry.zCoord - b0, nextticklistentry.xCoord + b0, nextticklistentry.yCoord + b0, nextticklistentry.zCoord + b0)) {
                    Block block1 = this.getBlock(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord);

                    if (block1.getMaterial() != Material.air && block1 == nextticklistentry.func_151351_a()) {
                        block1.updateTick(this, nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord, this.rand);
                    }
                }

                return;
            }

            p_147454_5_ = 1;
        }

        if (this.checkChunksExist(x - b0, y - b0, z - b0, x + b0, y + b0, z + b0)) {
            if (block.getMaterial() != Material.air) {
                nextticklistentry.setScheduledTime((long) p_147454_5_ + this.worldInfo.getWorldTotalTime());
                nextticklistentry.setPriority(p_147454_6_);
            }
            ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(x >> 4, z >> 4);
            Queue<NextTickListEntry> nextTickListEntries = chunkTickListMap.get(chunkCoordIntPair);
            if (nextTickListEntries == null) {
                nextTickListEntries = new ConcurrentLinkedQueue<>();
                chunkTickListMap.put(chunkCoordIntPair, nextTickListEntries);
            }
            nextTickListEntries.add(nextticklistentry);
        }
        ci.cancel();
    }

    /**
     * 卸载区块的事情留在最后干
     *
     * @return
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/IChunkProvider;unloadQueuedChunks()Z"))
    public boolean noopUnloadChunks(IChunkProvider iChunkProvider) {
        return false;
    }

    public Map<ChunkCoordIntPair, Queue<NextTickListEntry>> getPendingTicks() {
        return chunkTickListMap;
    }

}
