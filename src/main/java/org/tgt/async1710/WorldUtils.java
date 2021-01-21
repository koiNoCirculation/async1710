package org.tgt.async1710;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.NextTickListEntry;

import java.util.Map;
import java.util.Queue;

public interface WorldUtils {
    void setThreadName(String threadName);
    String getThreadName();
    void stop();
    boolean getRunning();
    boolean getExit();
    Map<ChunkCoordIntPair, Queue<NextTickListEntry>> getPendingTicks();
}
