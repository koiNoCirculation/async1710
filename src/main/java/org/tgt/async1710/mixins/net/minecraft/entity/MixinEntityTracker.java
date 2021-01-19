package org.tgt.async1710.mixins.net.minecraft.entity;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.entity.EntityTracker;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(EntityTracker.class)
public class MixinEntityTracker {

    @Shadow
    @Mutable
    private Set trackedEntities;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(WorldServer p_i1516_1_, CallbackInfo ci) {
        trackedEntities = new ConcurrentSet();
    }
}
