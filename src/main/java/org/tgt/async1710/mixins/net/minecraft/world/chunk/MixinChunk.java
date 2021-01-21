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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区块需要在服务器刻的最后卸载来避免并发修改
 */
@Mixin(Chunk.class)
public class MixinChunk {

    @Shadow public World worldObj;

    @Shadow @Final public int xPosition;

    @Shadow @Final public int zPosition;

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
}
