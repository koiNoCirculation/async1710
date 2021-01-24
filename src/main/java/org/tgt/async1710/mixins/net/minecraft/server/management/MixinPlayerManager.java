package org.tgt.async1710.mixins.net.minecraft.server.management;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tgt.async1710.TaskSubmitter;
import org.tgt.async1710.WorldUtils;

import java.util.concurrent.ExecutionException;

@Mixin(PlayerManager.class)
public abstract class MixinPlayerManager {
    private Logger logger = LogManager.getLogger();

    @Shadow public abstract void updateMountedMovingPlayer(EntityPlayerMP p_72685_1_);

    //调用链条：ServerConfigurationManager -> PlayerManager 唯一，所以直接patch上层
    /**
    @Inject(method = "updateMountedMovingPlayer", at = @At("HEAD"), cancellable = true)
    public void _updateMountedMovingPlayer(EntityPlayerMP p_72685_1_, CallbackInfo ci) throws ExecutionException, InterruptedException {
        logger.info("player managedXZ = ({},{}), xz = ({},{})", p_72685_1_.managedPosX, p_72685_1_.managedPosZ, p_72685_1_.managedPosZ, p_72685_1_.posX, p_72685_1_, p_72685_1_.posZ);
        World worldObj = p_72685_1_.worldObj;
        WorldUtils worldObj1 = (WorldUtils) worldObj;
        if(worldObj1.getThreadName() != Thread.currentThread().getName() && worldObj1.getRunning()) {
            ((TaskSubmitter)worldObj1).submit(() -> updateMountedMovingPlayer(p_72685_1_)).get();
            ci.cancel();
        }
    }
    **/

    public void sendToAllPlayersWatchingChunk(Packet p_151251_1_) {

    }

    /**
     * @author
     * 人类可以看懂的代码
     */
    /*
    @Overwrite
    public void updateMountedMovingPlayer(EntityPlayerMP player) throws ClassNotFoundException {
        int playerChunkX = (int)player.posX >> 4;
        int playerChunkZ = (int)player.posZ >> 4;
        double dx = player.managedPosX - player.posX;
        double dz = player.managedPosZ - player.posZ;
        double distanceSqr = dx * dx + dz * dz;

        if (distanceSqr >= 64.0D)
        {
            int managedChunkX = (int)player.managedPosX >> 4;
            int managedChunkZ = (int)player.managedPosZ >> 4;
            int viewRadius = this.playerViewRadius;
            int chunkdx = playerChunkX - managedChunkX;
            int chunkdz = playerChunkZ - managedChunkZ;
            boolean playerEnteredAnotherChunk = chunkdx != 0 || chunkdz != 0;
            List<ChunkCoordIntPair> chunksToLoad = new ArrayList<ChunkCoordIntPair>();

            if (playerEnteredAnotherChunk)
            {
                for (int i = playerChunkX - viewRadius; i <= playerChunkX + viewRadius; ++i)
                {
                    for (int j = playerChunkZ - viewRadius; j <= playerChunkZ + viewRadius; ++j)
                    {
                        if (!this.overlaps(i, j, managedChunkX, managedChunkZ, viewRadius))
                        {
                            chunksToLoad.add(new ChunkCoordIntPair(i, j));
                        }

                        if (!this.overlaps(i - chunkdx, j - chunkdz, playerChunkX, playerChunkZ, viewRadius))
                        {

                            Object playerInstance = ((PlayerManager)(Object) this).getPlayerInstance(i - chunkdx, j - chunkdz, false);

                            if (playerInstance != null)
                            {
                                Class<?> aClass = Class.forName("net.minecraft.server.management.PlayerManager.PlayerInstance");
                                Method removePlayer = aClass.getDeclaredMethod("removePlayer", EntityPlayerMP.class);
                                removePlayer.invoke(this, player);
                            }
                        }
                    }
                }

                this.filterChunkLoadQueue(player);
                player.managedPosX = player.posX;
                player.managedPosZ = player.posZ;
                // send nearest chunks first
                java.util.Collections.sort(chunksToLoad, new net.minecraftforge.common.util.ChunkCoordComparator(player));

                for (ChunkCoordIntPair pair : chunksToLoad)
                {
                    ((PlayerManager)(Object) this).getPlayerInstance(pair.chunkXPos, pair.chunkZPos, true).addPlayer(player);
                }

                if (viewRadius > 1 || viewRadius < -1 || chunkdx > 1 || chunkdz < -1)
                {
                    java.util.Collections.sort(player.loadedChunks, new net.minecraftforge.common.util.ChunkCoordComparator(player));
                }
            }
        }
    }
     */
}
