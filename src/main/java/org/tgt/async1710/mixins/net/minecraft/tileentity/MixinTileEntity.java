package org.tgt.async1710.mixins.net.minecraft.tileentity;

import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TileEntity.class)
public class MixinTileEntity {
    @Shadow public int xCoord;

    @Shadow public int yCoord;

    @Shadow public int zCoord;

    @Override
    public boolean equals(Object o){
        if(o == this) {
            return true;
        }
        if(o != null && getClass().isAssignableFrom(o.getClass())) {
            TileEntity o1 = (TileEntity) o;
            if(xCoord == o1.xCoord && yCoord == o1.yCoord && zCoord == o1.zCoord) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((xCoord * 31 + yCoord) * 31 + zCoord) * 31;
    }
}
