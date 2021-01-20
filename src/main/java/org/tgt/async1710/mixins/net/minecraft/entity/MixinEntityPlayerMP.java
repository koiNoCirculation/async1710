package org.tgt.async1710.mixins.net.minecraft.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tgt.async1710.GlobalExecutor;
import org.tgt.async1710.ReadWriteLockedList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO:
 * A detailed walkthrough of the error, its code path and all known details is as follows:
 * ---------------------------------------------------------------------------------------
 *
 * -- Head --
 * Stacktrace:
 * 	at java.util.LinkedList$ListItr.checkForComodification(LinkedList.java:966)
 * 	at java.util.LinkedList$ListItr.next(LinkedList.java:888)
 * 	at net.minecraft.entity.player.EntityPlayerMP.handler$onUpdate$zzk000(EntityPlayerMP.java:1338)
 * 	at net.minecraft.entity.player.EntityPlayerMP.onUpdate(EntityPlayerMP.java)
 * 	at net.minecraft.world.World.updateEntityWithOptionalForce(World.java:2315)
 * 	at net.minecraft.world.WorldServer.updateEntityWithOptionalForce(WorldServer.java:684)
 * 	at net.minecraft.world.World.updateEntity(World.java:2275)
 *
 * -- Entity being ticked --
 * Details:
 * 	Entity Type: null (net.minecraft.entity.player.EntityPlayerMP)
 * 	Entity ID: 229
 * 	Entity Name: Player468
 * 	Entity's Exact location: -163.76, 71.93, 262.36
 * 	Entity's Block location: World: (-164,71,262), Chunk: (at 12,4,6 in -11,16; contains blocks -176,0,256 to -161,255,271), Region: (-1,0; contains chunks -32,0 to -1,31, blocks -512,0,0 to -1,255,511)
 * 	Entity's Momentum: -0.64, 0.90, 0.10
 * Stacktrace:
 * 	at net.minecraft.world.World.lambda$null$2(World.java:4859)
 * 	at java.util.concurrent.CompletableFuture$AsyncSupply.run$$$capture(CompletableFuture.java:1604)
 * 	at java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java)
 * 	at java.util.concurrent.CompletableFuture$AsyncSupply.exec(CompletableFuture.java:1596)
 * 	at java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:289)
 * 	at java.util.concurrent.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:1056)
 * 	at java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1692)
 * 	at java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:175)
 */
@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP extends EntityPlayer {
    @Shadow @Mutable
    public List<ChunkCoordIntPair> loadedChunks;

    @Shadow @Final public ItemInWorldManager theItemInWorldManager;

    @Shadow private int field_147101_bU;

    @Shadow @Mutable private List destroyedItemsNetCache;

    @Shadow public NetHandlerPlayServer playerNetServerHandler;

    @Shadow protected abstract void func_147097_b(TileEntity p_147097_1_);

    @Shadow public abstract WorldServer getServerForPlayer();


    public MixinEntityPlayerMP(World p_i45324_1_, GameProfile p_i45324_2_) {
        super(p_i45324_1_, p_i45324_2_);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(MinecraftServer p_i45285_1_, WorldServer p_i45285_2_, GameProfile p_i45285_3_, ItemInWorldManager p_i45285_4_, CallbackInfo ci) {
        loadedChunks = new ReadWriteLockedList(new LinkedList());
        destroyedItemsNetCache = new ReadWriteLockedList<Integer>(new LinkedList<>());
    }


    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true)
    public void onUpdate(CallbackInfo ci)
    {
        this.theItemInWorldManager.updateBlockRemoving();
        --this.field_147101_bU;

        if (this.hurtResistantTime > 0)
        {
            --this.hurtResistantTime;
        }

        this.openContainer.detectAndSendChanges();

        if (!this.worldObj.isRemote && !ForgeHooks.canInteractWith(this, this.openContainer))
        {
            this.closeScreen();
            this.openContainer = this.inventoryContainer;
        }

        ((ReadWriteLockedList<Integer>)destroyedItemsNetCache).foreachWithRemoveConcurrent(Int ->
            playerNetServerHandler.sendPacket(new S13PacketDestroyEntities(Int))
        , (Int) ->
            true
        , GlobalExecutor.fjp);
        if (!this.loadedChunks.isEmpty())
        {
            ArrayList<Chunk> chunksToSend = new ArrayList<>();
            ArrayList<TileEntity> chunkContainedTiles = new ArrayList<>();

            ((ReadWriteLockedList<ChunkCoordIntPair>)loadedChunks).foreachWithRemove((chunkcoordintpair) -> {
                if (chunkcoordintpair != null)
                {
                    if (this.worldObj.blockExists(chunkcoordintpair.chunkXPos << 4, 0, chunkcoordintpair.chunkZPos << 4))
                    {
                        Chunk chunk;
                        chunk = this.worldObj.getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);
                        if (chunk.func_150802_k())
                        {
                            chunksToSend.add(chunk);
                            chunkContainedTiles.addAll(
                                    ((WorldServer)this.worldObj)
                                            .func_147486_a(chunkcoordintpair.chunkXPos * 16, 0, chunkcoordintpair.chunkZPos * 16, chunkcoordintpair.chunkXPos * 16 + 15, 256, chunkcoordintpair.chunkZPos * 16 + 15));
                            //BugFix: 16 makes it load an extra chunk, which isn't associated with a player, which makes it not unload unless a player walks near it.
                        }
                    }
                }
            }, (chunkcoordintpair) -> {
                if (chunkcoordintpair != null)
                {
                    if (this.worldObj.blockExists(chunkcoordintpair.chunkXPos << 4, 0, chunkcoordintpair.chunkZPos << 4))
                    {
                        Chunk chunk;
                        chunk = this.worldObj.getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);
                        if (chunk.func_150802_k())
                        {
                            return true;
                        }
                    }
                }
                return false;
            });
            List<ChunkCoordIntPair> toRemoves = new ArrayList<>();

            ((ReadWriteLockedList<ChunkCoordIntPair>)loadedChunks).foreachWithBreak(chunkcoordintpair -> {
                if(chunkcoordintpair != null) {
                    if (this.worldObj.blockExists(chunkcoordintpair.chunkXPos << 4, 0, chunkcoordintpair.chunkZPos << 4))
                    {
                        Chunk chunk = this.worldObj.getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);

                        if (chunk.func_150802_k())
                        {
                            chunksToSend.add(chunk);
                            chunkContainedTiles.addAll(((WorldServer)this.worldObj).func_147486_a(chunkcoordintpair.chunkXPos * 16, 0, chunkcoordintpair.chunkZPos * 16, chunkcoordintpair.chunkXPos * 16 + 15, 256, chunkcoordintpair.chunkZPos * 16 + 15));
                            //BugFix: 16 makes it load an extra chunk, which isn't associated with a player, which makes it not unload unless a player walks near it.
                            toRemoves.add(chunkcoordintpair);
                        }
                    }
                } else {
                    toRemoves.add(chunkcoordintpair);
                }
                return chunksToSend.size() < S26PacketMapChunkBulk.func_149258_c();
            });
            loadedChunks.removeAll(toRemoves);


            if (!chunksToSend.isEmpty())
            {
                this.playerNetServerHandler.sendPacket(new S26PacketMapChunkBulk(chunksToSend));
                for (TileEntity tileentity : chunkContainedTiles) {
                    this.func_147097_b(tileentity);
                }

                for (Chunk chunk : chunksToSend) {
                    this.getServerForPlayer().getEntityTracker().func_85172_a((EntityPlayerMP) (Object)this, chunk);
                    MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(chunk.getChunkCoordIntPair(), (EntityPlayerMP) (Object)this));
                }
            }
        }
        ci.cancel();
    }


}
