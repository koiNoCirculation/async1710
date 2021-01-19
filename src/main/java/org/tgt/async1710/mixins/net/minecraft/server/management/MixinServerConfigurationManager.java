package org.tgt.async1710.mixins.net.minecraft.server.management;

import net.minecraft.server.management.ServerConfigurationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerConfigurationManager.class)
public abstract class MixinServerConfigurationManager {
    private Logger log = LogManager.getLogger(MixinServerConfigurationManager.class);

}
