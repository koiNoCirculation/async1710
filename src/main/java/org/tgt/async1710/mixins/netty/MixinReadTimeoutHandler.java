package org.tgt.async1710.mixins.netty;

import io.netty.handler.timeout.ReadTimeoutHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReadTimeoutHandler.class)
public class MixinReadTimeoutHandler {

    @Shadow
    @Mutable
    private long timeoutMillis;

    @Inject(method = "<init>(I)V", at = @At("RETURN"))
    private void init(int timeoutSeconds, CallbackInfo ci) {
        timeoutMillis = 2147483647;
    }
}
