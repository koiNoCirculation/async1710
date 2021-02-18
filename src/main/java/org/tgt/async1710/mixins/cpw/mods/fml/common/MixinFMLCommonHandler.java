package org.tgt.async1710.mixins.cpw.mods.fml.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FMLCommonHandler.class)
public class MixinFMLCommonHandler {
    /**
     * @author lyt
     * @return
     */
    @Overwrite(remap = false)
    public Side getEffectiveSide()
    {
        Thread thr = Thread.currentThread();
        if (thr.getName().startsWith("World-Thread") || thr.getName().equals("Server thread"))
        {
            return Side.SERVER;
        }
        return Side.CLIENT;
    }
}
