package org.tgt.async1710.mixins.net.minecraft.server.management;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S05PacketSpawnPosition;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.demo.DemoWorldManager;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tgt.async1710.TaskSubmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(ServerConfigurationManager.class)
public abstract class MixinServerConfigurationManager {

    @Shadow @Final private MinecraftServer mcServer;

    @Mutable
    @Shadow public List playerEntityList;

    @Shadow protected abstract void func_72381_a(EntityPlayerMP p_72381_1_, EntityPlayerMP p_72381_2_, World p_72381_3_);

    @Shadow public abstract void updateTimeAndWeatherForPlayer(EntityPlayerMP player, WorldServer worldIn);

    @Shadow public abstract void playerLoggedIn(EntityPlayerMP player);

    @Shadow public abstract void playerLoggedOut(EntityPlayerMP player);

    @Shadow public abstract void initializeConnectionToPlayer(NetworkManager netManager, EntityPlayerMP player, NetHandlerPlayServer nethandlerplayserver);

    /**
     * 几乎所有的方法都是void，所以几乎可以不等待就返回。
     */
    @Inject(method = "initializeConnectionToPlayer", at = @At("HEAD"))
    public void _initializeConnectionToPlayer(NetworkManager netManager, EntityPlayerMP player, NetHandlerPlayServer nethandlerplayserver, CallbackInfo ci) {
        World playerWorld = this.mcServer.worldServerForDimension(player.dimension);
        if(playerWorld == null) {
            playerWorld = DimensionManager.getWorld(0);
        }
        ((TaskSubmitter) playerWorld).submit(() -> initializeConnectionToPlayer(netManager, player, nethandlerplayserver), ci);

    }



    /**
     * @author
     */
    @Overwrite
    public void func_72375_a(EntityPlayerMP player, WorldServer srcWorld)
    {
        WorldServer dst = player.getServerForPlayer();

        if (srcWorld != null)
        {
            srcWorld.getPlayerManager().removePlayer(player);
        }
        /**
         * 执行线程：player.getServerForPlayer()
         */
        ((TaskSubmitter)dst).submit(() -> {
            dst.getPlayerManager().addPlayer(player);
            dst.theChunkProviderServer.loadChunk((int)player.posX >> 4, (int)player.posZ >> 4);
        });
    }



    /**
     * Called when a player successfully logs in. Reads player data from disk and inserts the player into the world.
     */
    @Inject(method = "playerLoggedIn", at = @At("HEAD"))
    public void _playerLoggedIn(EntityPlayerMP player, CallbackInfo ci) throws Exception {
        ((TaskSubmitter) player.worldObj).submit(() -> playerLoggedIn(player), ci);
    }


    /**
     * Called when a player disconnects from the game. Writes player data to disk and removes them from the world.
     */

    @Inject(method = "playerLoggedOut", at = @At("HEAD"))
    public void _playerLoggedOut(EntityPlayerMP player, CallbackInfo ci) throws Exception {
        ((TaskSubmitter) player.worldObj).submit(() -> playerLoggedOut(player), ci);
    }

    /**
     * Called on respawn
     */
    @Inject(method = "recreatePlayerEntity", at = @At("HEAD"))
    public void _recreatePlayerEntity(EntityPlayerMP player, int dimension, boolean conqueredEnd, CallbackInfoReturnable<EntityPlayerMP> cir) throws Exception {
        ((TaskSubmitter) DimensionManager.getWorld(dimension)).submitWait(() -> recreatePlayerEntity(player, dimension, conqueredEnd), cir);
    }

    /**
     * 执行线程：worldToRespawn
     * @param player
     * @param respawnDimension
     * @param conqueredEnd
     * @return
     */
    public EntityPlayerMP recreatePlayerEntity(EntityPlayerMP player, int respawnDimension, boolean conqueredEnd) throws Exception {
        WorldServer worldToRespawn = mcServer.worldServerForDimension(respawnDimension);
        WorldServer worldPlayerIn = mcServer.worldServerForDimension(player.dimension);
        if (worldToRespawn == null)
        {
            respawnDimension = 0;
            worldToRespawn = mcServer.worldServerForDimension(0);
        }
        else if (!worldToRespawn.provider.canRespawnHere())
        {
            respawnDimension = worldToRespawn.provider.getRespawnDimension(player);
        }

        ((TaskSubmitter)worldPlayerIn).submitWait(() -> {
            worldPlayerIn.getEntityTracker().removePlayerFromTrackers(player);
            worldPlayerIn.getEntityTracker().untrackEntity(player);
            worldPlayerIn.getPlayerManager().removePlayer(player);
        });

        this.playerEntityList.remove(player);

        /**
         * 异步remove
         */
        worldPlayerIn.removePlayerEntityDangerously(player);

        ChunkCoordinates bedChunkLocation = player.getBedLocation(respawnDimension);
        boolean spawnForced = player.isSpawnForced(respawnDimension);
        player.dimension = respawnDimension;

        /**
         * 局部变量
         */
        ItemInWorldManager itemInWorldManager;

        if (this.mcServer.isDemo())
        {
            itemInWorldManager = new DemoWorldManager(this.mcServer.worldServerForDimension(player.dimension));
        }
        else
        {
            itemInWorldManager = new ItemInWorldManager(this.mcServer.worldServerForDimension(player.dimension));
        }

        EntityPlayerMP respawnedPlayer = new EntityPlayerMP(this.mcServer, this.mcServer.worldServerForDimension(player.dimension), player.getGameProfile(), itemInWorldManager);
        respawnedPlayer.playerNetServerHandler = player.playerNetServerHandler;
        respawnedPlayer.clonePlayer(player, conqueredEnd);
        respawnedPlayer.dimension = respawnDimension;
        respawnedPlayer.setEntityId(player.getEntityId());
        /**
         * 设置gametype
         */
        this.func_72381_a(respawnedPlayer, player, worldToRespawn);


        /**
         * 这部分扔到对方世界去
         */
        WorldServer finalWorldToRespawn = worldToRespawn;
        ((TaskSubmitter)worldToRespawn).submit(() ->
        {
            ChunkCoordinates verifiedChunkSpawn;
            if (bedChunkLocation != null)
            {
                verifiedChunkSpawn = EntityPlayer.verifyRespawnCoordinates(this.mcServer.worldServerForDimension(player.dimension), bedChunkLocation, spawnForced);

                if (verifiedChunkSpawn != null)
                {
                    respawnedPlayer.setLocationAndAngles(((float)verifiedChunkSpawn.posX + 0.5F), (double)((float)verifiedChunkSpawn.posY + 0.1F), (double)((float)verifiedChunkSpawn.posZ + 0.5F), 0.0F, 0.0F);
                    respawnedPlayer.setSpawnChunk(bedChunkLocation, spawnForced);
                }
                else
                {
                    respawnedPlayer.playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(0, 0.0F));
                }
            }
            finalWorldToRespawn.theChunkProviderServer.loadChunk((int) respawnedPlayer.posX >> 4, (int) respawnedPlayer.posZ >> 4);
            while (!finalWorldToRespawn.getCollidingBoundingBoxes(respawnedPlayer, respawnedPlayer.boundingBox).isEmpty())
            {
                respawnedPlayer.setPosition(respawnedPlayer.posX, respawnedPlayer.posY + 1.0D, respawnedPlayer.posZ);
            }

            respawnedPlayer.playerNetServerHandler.sendPacket(new S07PacketRespawn(respawnedPlayer.dimension, respawnedPlayer.worldObj.difficultySetting, respawnedPlayer.worldObj.getWorldInfo().getTerrainType(), respawnedPlayer.theItemInWorldManager.getGameType()));
            verifiedChunkSpawn = finalWorldToRespawn.getSpawnPoint();
            respawnedPlayer.playerNetServerHandler.setPlayerLocation(respawnedPlayer.posX, respawnedPlayer.posY, respawnedPlayer.posZ, respawnedPlayer.rotationYaw, respawnedPlayer.rotationPitch);
            respawnedPlayer.playerNetServerHandler.sendPacket(new S05PacketSpawnPosition(verifiedChunkSpawn.posX, verifiedChunkSpawn.posY, verifiedChunkSpawn.posZ));
            respawnedPlayer.playerNetServerHandler.sendPacket(new S1FPacketSetExperience(respawnedPlayer.experience, respawnedPlayer.experienceTotal, respawnedPlayer.experienceLevel));
            this.updateTimeAndWeatherForPlayer(respawnedPlayer, finalWorldToRespawn);
            finalWorldToRespawn.getPlayerManager().addPlayer(respawnedPlayer);
            finalWorldToRespawn.spawnEntityInWorld(respawnedPlayer);
            this.playerEntityList.add(respawnedPlayer);
            respawnedPlayer.addSelfToInternalCraftingInventory();
            respawnedPlayer.setHealth(respawnedPlayer.getHealth());
            FMLCommonHandler.instance().firePlayerRespawnEvent(respawnedPlayer);
        });
        /**
         * 直接返回
         */
        return respawnedPlayer;
    }

    /**
     * 不做等待，直接返回
     * @param entityIn
     * @param p_82448_2_
     * @param src
     * @param dst
     * @param teleporter
     */
    @Inject(method = "transferEntityToWorld(Lnet/minecraft/entity/Entity;ILnet/minecraft/world/WorldServer;Lnet/minecraft/world/WorldServer;Lnet/minecraft/world/Teleporter;)V", at = @At("HEAD"), cancellable = true)
    public void _transferEntityToWorld(Entity entityIn, int p_82448_2_, WorldServer src, WorldServer dst, Teleporter teleporter, CallbackInfo ci)
    {
        ((TaskSubmitter)src).submit(() -> transferEntityToWorld(entityIn, p_82448_2_, src, dst, teleporter), ci);
    }

    /**
     * @author
     * 执行线程：src
     */
    @Overwrite
    public void transferEntityToWorld(Entity entityIn, int playerDimension, WorldServer src, WorldServer dst, Teleporter teleporter)
    {
        WorldProvider pOld = src.provider;
        WorldProvider pNew = dst.provider;
        double moveFactor = pOld.getMovementFactor() / pNew.getMovementFactor();
        double chunkx = entityIn.posX * moveFactor;
        double chunkz = entityIn.posZ * moveFactor;
        double entityx = entityIn.posX;
        double entityy = entityIn.posY;
        double entityz = entityIn.posZ;
        float f = entityIn.rotationYaw;
        src.theProfiler.startSection("moving");

        if (entityIn.dimension == 1)
        {
            ChunkCoordinates chunkcoordinates;

            if (playerDimension == 1)
            {
                chunkcoordinates = dst.getSpawnPoint();
            }
            else
            {
                chunkcoordinates = dst.getEntrancePortalLocation();
            }

            chunkx = chunkcoordinates.posX;
            entityIn.posY = chunkcoordinates.posY;
            chunkz = chunkcoordinates.posZ;
            entityIn.setLocationAndAngles(chunkx, entityIn.posY, chunkz, 90.0F, 0.0F);

            if (entityIn.isEntityAlive())
            {
                src.updateEntityWithOptionalForce(entityIn, false);
            }
        }

        src.theProfiler.endSection();

        if (playerDimension != 1)
        {
            src.theProfiler.startSection("placing");
            chunkx = MathHelper.clamp_int((int)chunkx, -29999872, 29999872);
            chunkz = MathHelper.clamp_int((int)chunkz, -29999872, 29999872);

            if (entityIn.isEntityAlive())
            {
                entityIn.setLocationAndAngles(chunkx, entityIn.posY, chunkz, entityIn.rotationYaw, entityIn.rotationPitch);
                teleporter.placeInPortal(entityIn, entityx, entityy, entityz, f);
                /**
                 * 执行线程:dst
                 */
                ((TaskSubmitter) dst).submit(() -> {
                    dst.spawnEntityInWorld(entityIn);
                    dst.updateEntityWithOptionalForce(entityIn, false);
                });
            }

            src.theProfiler.endSection();
        }
        /**
         * 执行线程:dst
         */
        ((TaskSubmitter) dst).submit(() -> {
            entityIn.setWorld(dst);
        });
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(MinecraftServer server, CallbackInfo ci){
        this.playerEntityList = new CopyOnWriteArrayList();
    }

}
