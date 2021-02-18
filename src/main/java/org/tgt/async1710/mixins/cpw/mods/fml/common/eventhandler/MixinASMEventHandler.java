package org.tgt.async1710.mixins.cpw.mods.fml.common.eventhandler;

import cpw.mods.fml.common.eventhandler.ASMEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.reflect.Method;

@Mixin(ASMEventHandler.class)
public class MixinASMEventHandler {
    @Shadow private static int IDs;

    @Overwrite(remap = false)
    private String getUniqueName(Method callback)
    {
        return String.format("%s_%s_%d_%s_%s_%s", getClass().getName(),
                Thread.currentThread().getName(),
                IDs++,
                callback.getDeclaringClass().getSimpleName(),
                callback.getName(),
                callback.getParameterTypes()[0].getSimpleName());
    }

}
