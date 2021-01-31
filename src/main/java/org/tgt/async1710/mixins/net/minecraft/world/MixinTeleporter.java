/*
package org.tgt.async1710.mixins.net.minecraft.world;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

*/
/**
 * to read code
 *//*

@Mixin(Teleporter.class)
public abstract class MixinTeleporter {
    @Shadow @Final private WorldServer worldServerInstance;

    @Shadow @Final private LongHashMap destinationCoordinateCache;

    @Shadow @Final private List destinationCoordinateKeys;

    @Shadow public abstract boolean makePortal(Entity p_85188_1_);

    public void placeInPortal(Entity entity, double x, double y, double z, float yaw)
    {
        if (this.worldServerInstance.provider.dimensionId != 1)
        {
            if (!this.placeInExistingPortal(entity, x, y, z, yaw))
            {
                this.makePortal(entity);
                this.placeInExistingPortal(entity, x, y, z, yaw);
            }
        }
        else
        {
            */
/**
             * end world nether portal
             * regenerate portal
             *//*

            int flooredX = MathHelper.floor_double(entity.posX);
            int flooredY = MathHelper.floor_double(entity.posY) - 1;
            int flooredZ = MathHelper.floor_double(entity.posZ);
            byte b0 = 1;
            byte b1 = 0;

            */
/**
             * w = 4 and h = 5
             *//*

            for (int l = -2; l <= 2; ++l)
            {
                for (int i1 = -2; i1 <= 2; ++i1)
                {
                    for (int indexH = -1; indexH < 3; ++indexH)
                    {
                        int portalBlockX = flooredX + i1 * b0 + l * b1;
                        int portalBlockY = flooredY + indexH;
                        int portalBlockZ = flooredZ + i1 * b1 - l * b0;
                        boolean flag = indexH < 0;
                        this.worldServerInstance.setBlock(portalBlockX, portalBlockY, portalBlockZ, flag ? Blocks.obsidian : Blocks.air);
                    }
                }
            }

            entity.setLocationAndAngles((double)flooredX, (double)flooredY, (double)flooredZ, entity.rotationYaw, 0.0F);
            entity.motionX = entity.motionY = entity.motionZ = 0.0D;
        }
    }

    */
/**
     * Place an entity in a nearby portal which already exists.
     *//*

    public boolean placeInExistingPortal(Entity entity, double p_77184_2_, double p_77184_4_, double p_77184_6_, float p_77184_8_)
    {
        short distanceBetweenPortal = 128;
        double distanceToPortal = -1.0D;
        int existingPortalX = 0;
        int existingPortalY = 0;
        int existingPortalZ = 0;
        int flooredPosX = MathHelper.floor_double(entity.posX);
        int flooredPosZ = MathHelper.floor_double(entity.posZ);
        long chunkPosOfEntity = ChunkCoordIntPair.chunkXZ2Int(flooredPosX, flooredPosZ);
        boolean foundExistingPortal = true;

        if (this.destinationCoordinateCache.containsItem(chunkPosOfEntity))
        {
            Teleporter.PortalPosition portalposition = (Teleporter.PortalPosition)this.destinationCoordinateCache.getValueByKey(chunkPosOfEntity);
            distanceToPortal = 0.0D;
            existingPortalX = portalposition.posX;
            existingPortalY = portalposition.posY;
            existingPortalZ = portalposition.posZ;
            portalposition.lastUpdateTime = this.worldServerInstance.getTotalWorldTime();
            foundExistingPortal = false;
        }
        else
        {
            */
/**
             * search for nearest existing portal
             *//*

            for (int x = flooredPosX - distanceBetweenPortal; x <= flooredPosX + distanceBetweenPortal; ++x)
            {
                double dx = (double)x + 0.5D - entity.posX;

                for (int z = flooredPosZ - distanceBetweenPortal; z <= flooredPosZ + distanceBetweenPortal; ++z)
                {
                    double dz = (double)z + 0.5D - entity.posZ;

                    for (int y = this.worldServerInstance.getActualHeight() - 1; y >= 0; --y)
                    {
                        if (this.worldServerInstance.getBlock(x, y, z) == Blocks.portal)
                        {
                            while (this.worldServerInstance.getBlock(x, y - 1, z) == Blocks.portal)
                            {
                                --y;
                            }

                            double dy = (double)y + 0.5D - entity.posY;
                            double distance = dx * dx + dy * dy + dz * dz;

                            if (distanceToPortal < 0.0D || distance < distanceToPortal)
                            {
                                distanceToPortal = distance;
                                existingPortalX = x;
                                existingPortalY = y;
                                existingPortalZ = z;
                            }
                        }
                    }
                }
            }
        }

        if (distanceToPortal >= 0.0D)
        {
            if (foundExistingPortal)
            {
                this.destinationCoordinateCache.add(chunkPosOfEntity, new Teleporter.PortalPosition(existingPortalX, existingPortalY, existingPortalZ, this.worldServerInstance.getTotalWorldTime()));
                this.destinationCoordinateKeys.add(Long.valueOf(chunkPosOfEntity));
            }

            double portalX = (double)existingPortalX + 0.5D;
            double portalY = (double)existingPortalY + 0.5D;
            double portalZ = (double)existingPortalZ + 0.5D;
            int portalIndex = -1;

            if (this.worldServerInstance.getBlock(existingPortalX - 1, existingPortalY, existingPortalZ) == Blocks.portal)
            {
                portalIndex = 2;
            }

            if (this.worldServerInstance.getBlock(existingPortalX + 1, existingPortalY, existingPortalZ) == Blocks.portal)
            {
                portalIndex = 0;
            }

            if (this.worldServerInstance.getBlock(existingPortalX, existingPortalY, existingPortalZ - 1) == Blocks.portal)
            {
                portalIndex = 3;
            }

            if (this.worldServerInstance.getBlock(existingPortalX, existingPortalY, existingPortalZ + 1) == Blocks.portal)
            {
                portalIndex = 1;
            }

            int teleportDirection = entity.getTeleportDirection();

            if (portalIndex > -1)
            {
                int k2 = Direction.rotateLeft[portalIndex];
                int l2 = Direction.offsetX[portalIndex];
                int i3 = Direction.offsetZ[portalIndex];
                int j3 = Direction.offsetX[k2];
                int k3 = Direction.offsetZ[k2];
                boolean flag1 = !this.worldServerInstance.isAirBlock(existingPortalX + l2 + j3, existingPortalY, existingPortalZ + i3 + k3) || !this.worldServerInstance.isAirBlock(existingPortalX + l2 + j3, existingPortalY + 1, existingPortalZ + i3 + k3);
                boolean flag2 = !this.worldServerInstance.isAirBlock(existingPortalX + l2, existingPortalY, existingPortalZ + i3) || !this.worldServerInstance.isAirBlock(existingPortalX + l2, existingPortalY + 1, existingPortalZ + i3);

                if (flag1 && flag2)
                {
                    portalIndex = Direction.rotateOpposite[portalIndex];
                    k2 = Direction.rotateOpposite[k2];
                    l2 = Direction.offsetX[portalIndex];
                    i3 = Direction.offsetZ[portalIndex];
                    j3 = Direction.offsetX[k2];
                    k3 = Direction.offsetZ[k2];
                    int x = existingPortalX - j3;
                    portalX -= (double)j3;
                    int k1 = existingPortalZ - k3;
                    portalZ -= (double)k3;
                    flag1 = !this.worldServerInstance.isAirBlock(x + l2 + j3, existingPortalY, k1 + i3 + k3) || !this.worldServerInstance.isAirBlock(x + l2 + j3, existingPortalY + 1, k1 + i3 + k3);
                    flag2 = !this.worldServerInstance.isAirBlock(x + l2, existingPortalY, k1 + i3) || !this.worldServerInstance.isAirBlock(x + l2, existingPortalY + 1, k1 + i3);
                }

                float f1 = 0.5F;
                float f2 = 0.5F;

                if (!flag1 && flag2)
                {
                    f1 = 1.0F;
                }
                else if (flag1 && !flag2)
                {
                    f1 = 0.0F;
                }
                else if (flag1 && flag2)
                {
                    f2 = 0.0F;
                }

                portalX += (double)((float)j3 * f1 + f2 * (float)l2);
                portalZ += (double)((float)k3 * f1 + f2 * (float)i3);
                float f3 = 0.0F;
                float f4 = 0.0F;
                float f5 = 0.0F;
                float f6 = 0.0F;

                if (portalIndex == teleportDirection)
                {
                    f3 = 1.0F;
                    f4 = 1.0F;
                }
                else if (portalIndex == Direction.rotateOpposite[teleportDirection])
                {
                    f3 = -1.0F;
                    f4 = -1.0F;
                }
                else if (portalIndex == Direction.enderEyeMetaToDirection[teleportDirection])
                {
                    f5 = 1.0F;
                    f6 = -1.0F;
                }
                else
                {
                    f5 = -1.0F;
                    f6 = 1.0F;
                }

                double d9 = entity.motionX;
                double d10 = entity.motionZ;
                entity.motionX = d9 * (double)f3 + d10 * (double)f6;
                entity.motionZ = d9 * (double)f5 + d10 * (double)f4;
                entity.rotationYaw = p_77184_8_ - (float)(teleportDirection * 90) + (float)(portalIndex * 90);
            }
            else
            {
                entity.motionX = entity.motionY = entity.motionZ = 0.0D;
            }

            entity.setLocationAndAngles(portalX, portalY, portalZ, entity.rotationYaw, entity.rotationPitch);
            return true;
        }
        else
        {
            return false;
        }
    }
}
*/
