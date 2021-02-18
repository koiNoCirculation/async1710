package org.tgt.async1710.world;

import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

public interface WorldInfoGetter {
    ISaveHandler getSaveHandler();

    WorldInfo getWorldInfo();
}
