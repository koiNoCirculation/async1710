package org.tgt.async1710.mixins.net.minecraft.world.chunk;

import com.google.common.collect.Lists;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.ChunkEvent;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tgt.async1710.ChunkGetLoadedEntities;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Chunk.class)
public class MixinChunk implements ChunkGetLoadedEntities {
    @Shadow public Map chunkTileEntityMap;

    @Shadow public World worldObj;

    @Shadow @Final public int xPosition;

    @Shadow @Final public int zPosition;

    @Shadow public boolean isChunkLoaded;
    @Shadow @Final private static Logger logger;
    private Set[] entitySets;

    //redirect all list iterators
    //replace map to concurrent map
    //set to concurrent set
    //do not use copyonwrite, just copy on iterate
    @Inject(method = "<init>(Lnet/minecraft/world/World;II)V", cancellable = true, at = @At("RETURN"))
    private void init1(World p_i1995_1_, int p_i1995_2_, int p_i1995_3_, CallbackInfo ci) {
        chunkTileEntityMap = new ConcurrentHashMap();
        entitySets = new Set[16];
        for (int k = 0; k < this.entitySets.length; ++k)
        {
            this.entitySets[k] = new ConcurrentSet<>();
        }
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }
        if (o != null && Chunk.class.isAssignableFrom(o.getClass())) {
            Chunk o1 = (Chunk) o;
            if(worldObj.provider.dimensionId == o1.worldObj.provider.dimensionId && o1.xPosition == xPosition && o1.zPosition == zPosition) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (worldObj.provider.dimensionId * 31 + xPosition) * 31 + zPosition;
    }

    @Inject(method = "addEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MathHelper;floor_double(D)I", ordinal = 2), cancellable = true)
    public void _addEntity(Entity p_76612_1_, CallbackInfo ci) {
        int layer = MathHelper.floor_double(p_76612_1_.posY / 16.0D);

        if (layer < 0)
        {
            layer = 0;
        }

        if (layer >= 16)
        {
            layer = 15;
        }

        MinecraftForge.EVENT_BUS.post(new EntityEvent.EnteringChunk(p_76612_1_, this.xPosition, this.zPosition, p_76612_1_.chunkCoordX, p_76612_1_.chunkCoordZ));
        p_76612_1_.addedToChunk = true;
        p_76612_1_.chunkCoordX = this.xPosition;
        p_76612_1_.chunkCoordY = layer;
        p_76612_1_.chunkCoordZ = this.zPosition;
        this.entitySets[layer].add(p_76612_1_);
        ci.cancel();
    }

    @Inject(method = "removeEntityAtIndex", at = @At(value = "HEAD"), cancellable = true)
    public void _removeEntityAt(Entity p_76608_1_, int index, CallbackInfo ci) {
        if (index < 0)
        {
            index = 0;
        }

        if (index >= 16)
        {
            index = 15;
        }
        this.entitySets[index].remove(p_76608_1_);
        ci.cancel();
    }

    @Inject(method = "onChunkLoad", at = @At(value = "HEAD"), cancellable = true)
    public void _onChunkLoad(CallbackInfo ci) {
        this.isChunkLoaded = true;
        this.worldObj.func_147448_a(this.chunkTileEntityMap.values());
        for (int i = 0; i < 16; ++i)
        {
            List<Entity> list = Lists.newArrayList(entitySets[i]);
            for (Entity entity : list) {
                entity.onChunkLoad();
            }
            this.worldObj.addLoadedEntities(list);
        }
        logger.info("{} entities and {} tiles", Arrays.stream(entitySets).map(e -> e.size()).reduce((a, b) -> a+b).get(), chunkTileEntityMap.size());
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load((Chunk)(Object) this));
        ci.cancel();
    }

    @Inject(method = "onChunkUnload", at = @At("HEAD"), cancellable = true)
    public void _onChunkUnload(CallbackInfo ci) {
        this.isChunkLoaded = false;
        for (Object value : chunkTileEntityMap.values()) {
            TileEntity tileEntity = (TileEntity) value;
            this.worldObj.markTileEntityForRemoval(tileEntity);
        }
        for (int i = 0; i < 16; ++i)
        {
            this.worldObj.unloadEntities(Lists.newArrayList(this.entitySets[i]));
        }
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload((Chunk) (Object)this));
        ci.cancel();
    }

    /**
     * On方法
     * 是不是可以用四叉树？？？？？
     * @param p_76588_1_
     * @param p_76588_2_
     * @param p_76588_3_
     * @param p_76588_4_
     * @param ci
     */
    @Inject(method = "getEntitiesWithinAABBForEntity", at = @At("HEAD"), cancellable = true)
    public void _getEntitiesWithinAABBForEntity(Entity p_76588_1_, AxisAlignedBB p_76588_2_, List p_76588_3_, IEntitySelector p_76588_4_, CallbackInfo ci) {
        int i = MathHelper.floor_double((p_76588_2_.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
        int j = MathHelper.floor_double((p_76588_2_.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);
        i = MathHelper.clamp_int(i, 0, 15);
        j = MathHelper.clamp_int(j, 0, 15);

        for (int k = i; k <= j; ++k)
        {
            for (Object o : entitySets[k]) {
                Entity entity1 = (Entity)o;

                if (entity1 != p_76588_1_ && entity1.boundingBox.intersectsWith(p_76588_2_) && (p_76588_4_ == null || p_76588_4_.isEntityApplicable(entity1)))
                {
                    p_76588_3_.add(entity1);
                    Entity[] aentity = entity1.getParts();

                    if (aentity != null)
                    {
                        for (int i1 = 0; i1 < aentity.length; ++i1)
                        {
                            entity1 = aentity[i1];

                            if (entity1 != p_76588_1_ && entity1.boundingBox.intersectsWith(p_76588_2_) && (p_76588_4_ == null || p_76588_4_.isEntityApplicable(entity1)))
                            {
                                p_76588_3_.add(entity1);
                            }
                        }
                    }
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "getEntitiesOfTypeWithinAAAB", at = @At("HEAD"), cancellable = true)
    public void getEntitiesOfTypeWithinAAAB(Class p_76618_1_, AxisAlignedBB p_76618_2_, List p_76618_3_, IEntitySelector p_76618_4_, CallbackInfo ci)
    {
        int i = MathHelper.floor_double((p_76618_2_.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
        int j = MathHelper.floor_double((p_76618_2_.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);
        i = MathHelper.clamp_int(i, 0, 15);
        j = MathHelper.clamp_int(j, 0, 15);

        for (int k = i; k <= j; ++k)
        {
            for (Object o : entitySets[k]) {
                Entity entity = (Entity)o;
                if (p_76618_1_.isAssignableFrom(entity.getClass()) && entity.boundingBox.intersectsWith(p_76618_2_) && (p_76618_4_ == null || p_76618_4_.isEntityApplicable(entity)))
                {
                    p_76618_3_.add(entity);
                }
            }
        }
        ci.cancel();
    }

    @Override
    public Set[] getLoadedEntitySet() {
        return entitySets;
    }
}
