package org.tgt.async1710.mixins.net.minecraft.world.biome;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(BiomeDecorator.class)
public abstract class MixinBiomeDecorator {
    @Shadow public World currentWorld;

    @Shadow public Random randomGenerator;

    @Shadow public int chunk_X;

    @Shadow public int chunk_Z;

    @Shadow protected abstract void genDecorations(BiomeGenBase p_150513_1_);

    @Overwrite
    public void decorateChunk(World p_150512_1_, Random p_150512_2_, BiomeGenBase p_150512_3_, int p_150512_4_, int p_150512_5_)
    {
        synchronized (this) {
            if (this.currentWorld != null) {
                throw new RuntimeException("Already decorating!!");
            } else {
                this.currentWorld = p_150512_1_;
                this.randomGenerator = p_150512_2_;
                this.chunk_X = p_150512_4_;
                this.chunk_Z = p_150512_5_;
                this.genDecorations(p_150512_3_);
                this.currentWorld = null;
                this.randomGenerator = null;
            }
        }
    }
}
