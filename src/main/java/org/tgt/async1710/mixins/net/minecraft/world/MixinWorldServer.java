package org.tgt.async1710.mixins.net.minecraft.world;

import cpw.mods.fml.common.FMLCommonHandler;
import io.micrometer.core.instrument.Timer;
import net.minecraft.crash.CrashReport;
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
import org.tgt.async1710.MonitorRegistry;
import org.tgt.async1710.TaskSubmitter;

import java.util.Arrays;
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
    private boolean running = false;

    public void allTick() {
        long i = System.nanoTime();
        ((TaskSubmitter)this).runTasks();
        if (tickCounter % 20 == 0) {
            ((DedicatedServer) mcServer).getConfigurationManager().
                    sendPacketToAllPlayersInDimension(
                            new S03PacketTimeUpdate(getTotalWorldTime(),
                                    getWorldTime(),
                                    getGameRules().getGameRuleBooleanValue("doDaylightCycle")),
                            provider.dimensionId);
        }
        FMLCommonHandler.instance().onPreWorldTick((WorldServer) (Object) this);
        ((TaskSubmitter)this).runTasks();
        try {
            tick();
        } catch (Throwable throwable1) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable1, "Exception ticking world");
            addWorldInfoToCrashReport(crashreport);
            throw new ReportedException(crashreport);
        }
        ((TaskSubmitter)this).runTasks();
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
        ((TaskSubmitter)this).runTasks();


        trackEntityTimer.record(() -> {
            theEntityTracker.updateTrackedEntities();
        });
        ((TaskSubmitter)this).runTasks();

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
    public abstract void flush();

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
     * 卸载区块的事情留在最后干
     *
     * @return
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/IChunkProvider;unloadQueuedChunks()Z"))
    public boolean noopUnloadChunks(IChunkProvider iChunkProvider) {
        return false;
    }
}
