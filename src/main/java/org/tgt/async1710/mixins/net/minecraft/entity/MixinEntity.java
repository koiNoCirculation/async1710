package org.tgt.async1710.mixins.net.minecraft.entity;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.tgt.async1710.UUIDGetter;

import java.util.UUID;

@Mixin(Entity.class)
public class MixinEntity implements UUIDGetter {

    @Shadow protected UUID entityUniqueID;

    @Override
    public boolean equals(Object o){
        if(o == this) {
            return true;
        }
        if(o != null) {
            if(getClass().isAssignableFrom(o.getClass())) {
                return entityUniqueID.equals(((UUIDGetter)o).getUUID());

            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getUUID().hashCode();
    }

    @Override
    public UUID getUUID() {
        return entityUniqueID;
    }
}
