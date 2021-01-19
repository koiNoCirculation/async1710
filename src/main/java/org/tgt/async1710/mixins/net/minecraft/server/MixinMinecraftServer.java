package org.tgt.async1710.mixins.net.minecraft.server;

import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraftforge.common.DimensionManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Shadow
    private int tickCounter;

    @Shadow
    public abstract int getMaxPlayers();

    @Shadow
    public abstract int getCurrentPlayerCount();

    @Shadow
    public abstract NetworkSystem getNetworkSystem();

    @Shadow
    private ServerConfigurationManager serverConfigManager;

    @Shadow
    private ServerStatusResponse statusResponse;

    @Shadow
    public Hashtable<Integer, long[]> worldTickTimes;

    @Shadow
    @Final
    private List playersOnline;



    @Shadow
    @Final
    private static Logger logger;


    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lcpw/mods/fml/common/FMLCommonHandler;onPreServerTick()V", remap = false))
    public void tick(CallbackInfo ci) {
        long i = System.nanoTime();
        FMLCommonHandler.instance().onPreServerTick();
        ++this.tickCounter;
        statusResponse.setPlayerCountData(new ServerStatusResponse.PlayerCountData(this.getMaxPlayers(), this.getCurrentPlayerCount()));
        GameProfile[] profiles = new GameProfile[this.getCurrentPlayerCount()];

        for (int k = 0; k < profiles.length; ++k)
        {
            profiles[k] = ((EntityPlayerMP)this.serverConfigManager.playerEntityList.get(k)).getGameProfile();
        }
        statusResponse.getPlayerCountData().setPlayers(profiles);
        ((DedicatedServer)(Object)this).executePendingCommands();
        this.getNetworkSystem().networkTick();
        this.serverConfigManager.onTick();
        DimensionManager.unloadWorlds(worldTickTimes);


        for (int k = 0; k < playersOnline.size(); ++k)
        {
            ((IUpdatePlayerListBox)playersOnline.get(k)).update();
        }
        FMLCommonHandler.instance().onPostServerTick();

        ci.cancel();
    }

    /**
     * @author lyt
     * @reason 不需要了
     */
    @Overwrite
    public void initialWorldChunkLoad() {
        logger.info("original initialWorldChunkLoad is abandoned");
    }

    @Inject(method = "stopServer", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;)V", ordinal = 2, remap = false))
    public void stopServer(CallbackInfo ci) throws InterruptedException {
        while (DimensionManager.getWorlds().length > 0) {
            Thread.sleep(50);
        }
    }

    @Redirect(method = "loadAllWorlds", at = @At(value = "INVOKE",
            target = "Lcpw/mods/fml/common/eventhandler/EventBus;post(Lcpw/mods/fml/common/eventhandler/Event;)Z",
            remap = false))
    private boolean cancelInitDimensionPostEvent(EventBus eventBus, Event event) {
        logger.info("Post of world load event is canceled, the event will occur in world thread");
        return true;
    }

}
