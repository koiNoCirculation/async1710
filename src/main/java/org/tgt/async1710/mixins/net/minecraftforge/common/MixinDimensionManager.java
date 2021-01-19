package org.tgt.async1710.mixins.net.minecraftforge.common;

import com.google.common.collect.Multiset;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.tgt.async1710.WorldUtils;

import java.util.*;
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


    private static Logger mixinLogger = LogManager.getLogger(MixinDimensionManager.class);
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

    @Redirect(method = "initDimension",
            remap = false,
            at = @At(value = "INVOKE",
            target = "Lcpw/mods/fml/common/eventhandler/EventBus;post(Lcpw/mods/fml/common/eventhandler/Event;)Z",
            remap = false))
    private static boolean cancelInitDimensionPostEvent(EventBus eventBus, Event event) {
        mixinLogger.info("Post of world load event is canceled, the event will occur in world thread");
        return true;
    }

}

