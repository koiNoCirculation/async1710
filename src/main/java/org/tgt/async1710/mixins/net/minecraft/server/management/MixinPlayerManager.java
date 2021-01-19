package org.tgt.async1710.mixins.net.minecraft.server.management;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tgt.async1710.TaskSubmitter;
import org.tgt.async1710.WorldUtils;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

@Mixin(PlayerManager.class)
public abstract class MixinPlayerManager {
    private Logger log = LogManager.getLogger(MixinPlayerManager.class);

    @Shadow @Mutable
    private List players;

    @Shadow @Mutable private List playerInstanceList;

    @Shadow @Mutable private List playerInstancesToUpdate;
    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(WorldServer p_i1176_1_, CallbackInfo ci) {
        players = new CopyOnWriteArrayList();
        playerInstanceList = new CopyOnWriteArrayList();
        playerInstancesToUpdate = new CopyOnWriteArrayList();
    }
}
