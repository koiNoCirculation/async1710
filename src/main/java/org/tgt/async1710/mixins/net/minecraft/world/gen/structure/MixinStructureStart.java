package org.tgt.async1710.mixins.net.minecraft.world.gen.structure;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tgt.async1710.ReadWriteLockedLinkedList;

import java.util.LinkedList;
import java.util.Random;

@Mixin(StructureStart.class)
public class MixinStructureStart {
    protected LinkedList components;

    @Inject(method = "<init>()V", at = @At("RETURN"))
    public void init(CallbackInfo ci) {
        components = new ReadWriteLockedLinkedList<StructureComponent>();
    }

    /**
     * 你iterator.remove()你妈呢都
     * @param p_75068_1_
     * @param p_75068_2_
     * @param p_75068_3_
     */
    @Overwrite
    public void generateStructure(World p_75068_1_, Random p_75068_2_, StructureBoundingBox p_75068_3_)
    {
        ((ReadWriteLockedLinkedList<StructureComponent>)components).foreachWithRemove((structurecomponent) ->{},
                (structurecomponent) -> structurecomponent.getBoundingBox().intersectsWith(p_75068_3_) && !structurecomponent.addComponentParts(p_75068_1_, p_75068_2_, p_75068_3_));
    }
}
