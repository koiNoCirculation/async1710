package org.tgt.async1710;

import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

public interface WorldInfoGetter {
    public ISaveHandler getSaveHandler();
    public WorldInfo getWorldInfo();
}
