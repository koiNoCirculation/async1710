package org.tgt.async1710.mixins.net.minecraft.block;

import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Block.class)
public abstract class MixinBlock {
    @Shadow public abstract Block setUnlocalizedName(String name);

    /**
     * EIO endercore
     */
    public Block setBlockName(String name) {
        return setUnlocalizedName(name);
    }
}
