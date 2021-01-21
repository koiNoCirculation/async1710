package org.tgt.async1710.mixins.net.minecraftforge.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.tgt.async1710.WorldInfoGetter;
import org.tgt.async1710.WorldUtils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * public static void setWorld(int id, WorldServer world)
 *     {
 *         if (world != null)
 *         {
 *             worlds.put(id, world);
 *             <---startWorld
 *             weakWorldMap.put(world, world);
 *             MinecraftServer.getServer().worldTickTimes.put(id, new long[100]);
 *             FMLLog.info("Loading dimension %d (%s) (%s)", id, world.getWorldInfo().getWorldName(), world.func_73046_m());
 *         }
 *         else
 *         {
 *              <---stopWorld
 *             worlds.remove(id);
 *             MinecraftServer.getServer().worldTickTimes.remove(id);
 *             FMLLog.info("Unloading dimension %d", id);
 *         }
 *
 *         ArrayList<WorldServer> tmp = new ArrayList<WorldServer>();
 *         if (worlds.get( 0) != null)
 *             tmp.add(worlds.get( 0));
 *         if (worlds.get(-1) != null)
 *             tmp.add(worlds.get(-1));
 *         if (worlds.get( 1) != null)
 *             tmp.add(worlds.get( 1));
 *
 *         for (Entry<Integer, WorldServer> entry : worlds.entrySet())
 *         {
 *             int dim = entry.getKey();
 *             if (dim >= -1 && dim <= 1)
 *             {
 *                 continue;
 *             }
 *             tmp.add(entry.getValue());
 *         }
 *
 *         MinecraftServer.getServer().worldServers = tmp.toArray(new WorldServer[tmp.size()]);
 *     }
 */
@Mixin(DimensionManager.class)
public abstract class MixinDimensionManager {
    private static Map<Integer, Thread> worldThreads = new ConcurrentHashMap<>();

    @Shadow
    private static ArrayList<Integer> unloadQueue;

    @Shadow
    private static Hashtable<Integer, WorldServer> worlds;

    @Inject(method = "setWorld", remap = false, locals = LocalCapture.CAPTURE_FAILEXCEPTION, at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0, target = "Ljava/util/Hashtable;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", remap = false))
    private static void startWorld(int id, WorldServer world, CallbackInfo ci) {
        Thread thread = new Thread((Runnable)world);
        String threadName = "World-Thread-DIMID=" + world.provider.dimensionId;
        ((WorldUtils)world).setThreadName(threadName);
        thread.setName(threadName);
        worldThreads.put(id, thread);
        thread.start();
    }

    private static void stopWorld(int id) {
        WorldUtils worldUtils = (WorldUtils) worlds.get(id);
        if(worldUtils != null) {
            worldUtils.stop();
        }
    }

    /**
     * @author lyt
     * @reason sync
     * @param id
     */
    @Overwrite
    public static void unloadWorld(int id) {
        synchronized (unloadQueue) {
            unloadQueue.add(id);
        }
    }

    /**
     * @author lyt
     * @reason sync
     */
    @Overwrite
    public static void unloadWorlds(Hashtable<Integer, long[]> worldTickTimes) {
        synchronized (unloadQueue) {
            for (int id : unloadQueue) {
                stopWorld(id);
            }
            unloadQueue.clear();
        }
    }

    /**
     * @author
     */
    @Overwrite
    public static void initDimension(int dim) {
        WorldServer overworld = DimensionManager.getWorld(0);
        WorldServer newWorld;
        MinecraftServer server = MinecraftServer.getServer();
        if (overworld == null)
        {
            /**
             * recreate overworld on crash
             */
            WorldInfo worldInfo = ((WorldInfoGetter) server).getWorldInfo();
            ISaveHandler saveHandler = ((WorldInfoGetter) server).getSaveHandler();
            WorldSettings worldsettings = new WorldSettings(worldInfo);
            newWorld = new WorldServer(server, saveHandler, worldInfo.getWorldName(), 0, worldsettings, server.theProfiler);
        } else {
            try
            {
                DimensionManager.getProviderType(dim);
            }
            catch (Exception e)
            {
                System.err.println("Cannot Hotload Dim: " + e.getMessage());
                return; // If a provider hasn't been registered then we can't hotload the dim
            }
            MinecraftServer mcServer = overworld.func_73046_m();
            ISaveHandler savehandler = overworld.getSaveHandler();
            WorldSettings worldSettings = new WorldSettings(overworld.getWorldInfo());
            newWorld = new WorldServerMulti(mcServer, savehandler, overworld.getWorldInfo().getWorldName(), dim, worldSettings, overworld, mcServer.theProfiler);
        }


        newWorld.addWorldAccess(new WorldManager(server, newWorld));
        if (!server.isSinglePlayer())
        {
            newWorld.getWorldInfo().setGameType(server.getGameType());
        }

        server.setDifficultyForAllWorlds(server.getDifficulty());
    }
}

