package org.tgt.async1710.mixins.net.minecraft.world.gen.layer;

import net.minecraft.world.gen.layer.IntCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IntCache.class)
public class MixinIntCache {
    /**
     * @author
     */
    @Overwrite
    public static synchronized int[] getIntCache(int p_76445_0_)
    {
        return new int[p_76445_0_];
    }
}
