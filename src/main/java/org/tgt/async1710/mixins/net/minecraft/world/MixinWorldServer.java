package org.tgt.async1710.mixins.net.minecraft.world;

import cpw.mods.fml.common.FMLCommonHandler;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
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
import org.tgt.async1710.TaskSubmitter;
import org.tgt.async1710.WorldUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
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

    private boolean running = false;

    public void allTick() {
        long i = System.nanoTime();
        ((TaskSubmitter)this).runTasks();
        if (tickCounter % 20 == 0) {
            ((DedicatedServer)mcServer).getConfigurationManager().
                    sendPacketToAllPlayersInDimension(
                            new S03PacketTimeUpdate(getTotalWorldTime(),
                                    getWorldTime(),
                                    getGameRules().getGameRuleBooleanValue("doDaylightCycle")),
                            provider.dimensionId);
        }
        FMLCommonHandler.instance().onPreWorldTick((WorldServer)(Object)this);


        try {
            tick();
        } catch (Throwable throwable1) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable1, "Exception ticking world");
            addWorldInfoToCrashReport(crashreport);
            throw new ReportedException(crashreport);
        }

        ((TaskSubmitter)this).runTasks();
        try {
            updateEntities();
        } catch (Throwable throwable) {
            CrashReport crashreport1 = CrashReport.makeCrashReport(throwable, "Exception ticking world entities");
            addWorldInfoToCrashReport(crashreport1);
            throw new ReportedException(crashreport1);
        }

        ((TaskSubmitter)this).runTasks();
        FMLCommonHandler.instance().instance().onPostWorldTick((WorldServer)(Object)this);

        theEntityTracker.updateTrackedEntities();

        ((TaskSubmitter)this).runTasks();
        if (this.tickCounter % 900 == 0) {
            try {
                this.saveAllChunks(true, null);
            } catch (MinecraftException e) {
                mixinLogger.error("error saving world!", e);
            }
        }
        ((TaskSubmitter)this).runTasks();
        /**
         * for unload
         */
        long[] longs = mcServer.worldTickTimes.get(provider.dimensionId);
        if(longs != null) {
            longs[this.tickCounter % 100] = System.nanoTime() - i;
        }
    }

    private void initialChunkLoad() {
        ChunkCoordinates chunkcoordinates = getSpawnPoint();
        long timelast = System.currentTimeMillis();
        int i = 0;
        mixinLogger.info("Preparing start region for level " + provider.dimensionId);
        for (int k = -192; k <= 192; k += 16)
        {
            for (int l = -192; l <= 192; l += 16)
            {
                long timeNow = System.currentTimeMillis();

                if (timeNow - timelast > 1000L)
                {
                    mixinLogger.info("Preparing spawn area {}", i * 100 / 625);
                    timelast = timeNow;
                }

                ++i;
                chunkProvider.loadChunk(chunkcoordinates.posX + k >> 4, chunkcoordinates.posZ + l >> 4);
            }
        }
    }



    @Override
    public void run() {
        try {
            initialChunkLoad();
            running = true;
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(this));
            while (running) {
                long timeMillis = System.currentTimeMillis();
                ++tickCounter;
                allTick();
                long now = System.currentTimeMillis();
                long elapsed = now - timeMillis;
                long remains = 50 - elapsed;
                if(remains > 0) {
                    Thread.sleep(remains);
                } else {
                    if(now - lastWarn > 15000 && elapsed > 2000) {
                        mixinLogger.warn("Can't keep up! Did the system time change, or is the server overloaded? Running {}ms behind, skipping {} tick(s)", elapsed, elapsed / 50L);
                        lastWarn = now;
                    }
                }
            }
        }
        catch (InterruptedException e) {

        }
        catch (ReportedException e) {
            //reported Exception when ticking
            //do cleaning on error
            mixinLogger.error("fatal error.", e);
            ((TaskSubmitter)this).cancelTasks();
            for (Object playerEntity : playerEntities) {
                String collect = Arrays.stream(e.getStackTrace()).map(t -> t.toString()).collect(Collectors.joining("\n"));
                ((EntityPlayerMP)playerEntity).playerNetServerHandler.kickPlayerFromServer("Please reconnect, fatal world ticking exception: \n" + collect);
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

    /*
    @Shadow
    public abstract void updateAllPlayersSleepingFlag();

    @Inject(method = "updateAllPlayersSleepingFlag", at = @At(value = "HEAD"), cancellable = true)
    private void _updateAllPlayersSleepingFlag(CallbackInfo ci) throws ExecutionException, InterruptedException, TimeoutException {
        if(Thread.currentThread().getName() != ((WorldUtils)this).getThreadName() && ((WorldUtils)this).getRunning()) {
            FutureTask<Integer> ft = new FutureTask<>(() -> {
                updateAllPlayersSleepingFlag();
                return 0;
            });
            ((TaskSubmitter)this).submit(ft);
            ft.get(1000, TimeUnit.SECONDS);
            ci.cancel();
        }
    }

    @Shadow
    public abstract void updateEntityWithOptionalForce(Entity p_72866_1_, boolean p_72866_2_);

    @Inject(method = "updateEntityWithOptionalForce", at = @At(value = "HEAD"), cancellable = true)
    private void _updateEntityWithOptionalForce(Entity p_72866_1_, boolean p_72866_2_, CallbackInfo ci) throws ExecutionException, InterruptedException, TimeoutException {
        if(Thread.currentThread().getName() != ((WorldUtils)this).getThreadName() && ((WorldUtils)this).getRunning()) {
            FutureTask<Integer> ft = new FutureTask<>(() -> {
                updateEntityWithOptionalForce(p_72866_1_, p_72866_2_);
                return 0;
            });
            ((TaskSubmitter)this).submit(ft);
            ft.get(1000, TimeUnit.SECONDS);
            ci.cancel();
        }
    }
    */

    @Shadow
    public abstract List func_147486_a(int p_147486_1_, int p_147486_2_, int p_147486_3_, int p_147486_4_, int p_147486_5_, int p_147486_6_);

    @Shadow public abstract void flush();

    @Shadow @Final private static Logger logger;

    @Shadow private Set pendingTickListEntriesHashSet;

    @Shadow private TreeSet pendingTickListEntriesTreeSet;

    @Shadow public List<Teleporter> customTeleporters;

    @Shadow protected Set<ChunkCoordIntPair> doneChunks;

    private Set<NextTickListEntry> pendingTickSetEntriesThisTick = new ConcurrentSet<>();

    @Inject(method = "func_147486_a", remap = false, at = @At(value = "HEAD"), cancellable = true)
    private void  _func_147486_a(int p_147486_1_, int p_147486_2_, int p_147486_3_, int p_147486_4_, int p_147486_5_, int p_147486_6_, CallbackInfoReturnable<List> cir) throws ExecutionException, InterruptedException, TimeoutException {
        if(Thread.currentThread().getName() != ((WorldUtils)this).getThreadName() && ((WorldUtils)this).getRunning()) {
            FutureTask<List> ft = new FutureTask<>(() -> func_147486_a(p_147486_1_,p_147486_2_,p_147486_3_, p_147486_4_,p_147486_5_,p_147486_6_));
            ((TaskSubmitter)this).submit(ft);
            cir.setReturnValue(ft.get(1000, TimeUnit.SECONDS));
        }
    }

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
     * replace pendingTickListEntriesThisTick to pendingTickSetEntriesThisTick
     */
    @Redirect(method = "tickUpdates", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false))
    public <E> boolean handlependingTickListEntriesThisTick1(List list, E e) {
        return pendingTickSetEntriesThisTick.add((NextTickListEntry) e);
    }

    @Redirect(method = "tickUpdates", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false))
    public Iterator<NextTickListEntry> handlependingTickListEntriesThisTick2(List list) {
        return pendingTickSetEntriesThisTick.iterator();
    }

    @Redirect(method = "tickUpdates", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", remap = false))
    public void handlependingTickListEntriesThisTick3(List list) {
        pendingTickSetEntriesThisTick.clear();
    }

    @Redirect(method = "getPendingBlockUpdates", at = @At(value = "INVOKE", ordinal = 1, target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false))
    public Iterator<NextTickListEntry> handleIteratorRemove1(List list)  {
        return pendingTickSetEntriesThisTick.iterator();
        /**
         * if (!this.pendingTickListEntriesThisTick.isEmpty())
         *                 {
         *                     logger.debug("toBeTicked = " + this.pendingTickListEntriesThisTick.size());
         *                 }
         * zhe kuai bu gaile ~
         */
    }
    /**
     * 替换集合
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
        pendingTickListEntriesHashSet = new ConcurrentSkipListSet();
        Constructor<TreeSet> declaredConstructor = TreeSet.class.getDeclaredConstructor(NavigableMap.class);
        declaredConstructor.setAccessible(true);
        pendingTickListEntriesTreeSet = declaredConstructor.newInstance(new ConcurrentSkipListMap());
        doneChunks = new ConcurrentSet<>();
        customTeleporters = new CopyOnWriteArrayList<>();
    }
}
