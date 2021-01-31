package org.tgt.async1710.mixins.net.minecraft.entity;

import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityPlayerMP.class)
public class MixinEntityPlayerMP {
    /**
     * 跨世界没有重新初始化EntityPlayerMP实例，
     */
}
