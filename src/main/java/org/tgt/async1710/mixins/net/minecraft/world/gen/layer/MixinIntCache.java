package org.tgt.async1710.mixins.net.minecraft.world.gen.layer;

import net.minecraft.world.gen.layer.IntCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IntCache.class)
public class MixinIntCache {
    /**
     * @author lyt
     * @reason 直接分配内存
     * @param p_76445_0_
     * @return
     */
    @Overwrite
    public static int[] getIntCache(int p_76445_0_)
    {
        return new int[p_76445_0_];
    }

    /**
     * @author lyt
     * @reason 禁用
     */
    @Overwrite
    public static void resetIntCache()
    {

    }

    /**
     * @author
     * @reason 禁用
     */
    @Overwrite
    public static String getCacheSizes()
    {
       return "Intcache has been disabled";
    }
}
