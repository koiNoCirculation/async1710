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
import org.tgt.async1710.ReadWriteLockedList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP extends EntityPlayer {
    @Shadow @Mutable
    public List<ChunkCoordIntPair> loadedChunks;

    @Shadow @Final public ItemInWorldManager theItemInWorldManager;

    @Shadow private int field_147101_bU;

    @Shadow @Final private List destroyedItemsNetCache;

    @Shadow public NetHandlerPlayServer playerNetServerHandler;

    @Shadow protected abstract void func_147097_b(TileEntity p_147097_1_);

    @Shadow public abstract WorldServer getServerForPlayer();

    public MixinEntityPlayerMP(World p_i45324_1_, GameProfile p_i45324_2_) {
        super(p_i45324_1_, p_i45324_2_);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(MinecraftServer p_i45285_1_, WorldServer p_i45285_2_, GameProfile p_i45285_3_, ItemInWorldManager p_i45285_4_, CallbackInfo ci) {
        loadedChunks = new ReadWriteLockedList(new LinkedList());
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

        while (!this.destroyedItemsNetCache.isEmpty())
        {
            int i = Math.min(this.destroyedItemsNetCache.size(), 127);
            int[] aint = new int[i];
            Iterator iterator = this.destroyedItemsNetCache.iterator();
            int j = 0;

            while (iterator.hasNext() && j < i)
            {
                aint[j++] = ((Integer)iterator.next()).intValue();
                iterator.remove();
            }

            this.playerNetServerHandler.sendPacket(new S13PacketDestroyEntities(aint));
        }

        if (!this.loadedChunks.isEmpty())
        {
            ArrayList arraylist = new ArrayList();
            Iterator iterator1 = this.loadedChunks.iterator();
            ArrayList arraylist1 = new ArrayList();
            Chunk chunk;

            List<ChunkCoordIntPair> toRemoves = new ArrayList<>();

            while (iterator1.hasNext() && arraylist.size() < S26PacketMapChunkBulk.func_149258_c())
            {
                ChunkCoordIntPair chunkcoordintpair = (ChunkCoordIntPair)iterator1.next();

                if (chunkcoordintpair != null)
                {
                    if (this.worldObj.blockExists(chunkcoordintpair.chunkXPos << 4, 0, chunkcoordintpair.chunkZPos << 4))
                    {
                        chunk = this.worldObj.getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);

                        if (chunk.func_150802_k())
                        {
                            arraylist.add(chunk);
                            arraylist1.addAll(((WorldServer)this.worldObj).func_147486_a(chunkcoordintpair.chunkXPos * 16, 0, chunkcoordintpair.chunkZPos * 16, chunkcoordintpair.chunkXPos * 16 + 15, 256, chunkcoordintpair.chunkZPos * 16 + 15));
                            //BugFix: 16 makes it load an extra chunk, which isn't associated with a player, which makes it not unload unless a player walks near it.
                            toRemoves.add(chunkcoordintpair);
                        }
                    }
                }
                else
                {
                    toRemoves.add(chunkcoordintpair);
                }
            }
            loadedChunks.removeAll(toRemoves);
            if (!arraylist.isEmpty())
            {
                this.playerNetServerHandler.sendPacket(new S26PacketMapChunkBulk(arraylist));
                for (Object o : arraylist1) {
                    TileEntity tileentity = (TileEntity)o;
                    this.func_147097_b(tileentity);
                }

                for (Object o : arraylist) {
                    chunk = (Chunk)o;
                    this.getServerForPlayer().getEntityTracker().func_85172_a((EntityPlayerMP) (Object)this, chunk);
                    MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(chunk.getChunkCoordIntPair(), (EntityPlayerMP) (Object)this));
                }
            }
        }
        ci.cancel();
    }


}
