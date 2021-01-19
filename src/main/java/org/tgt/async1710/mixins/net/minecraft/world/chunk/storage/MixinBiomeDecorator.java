package org.tgt.async1710.mixins.net.minecraft.world.chunk.storage;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(BiomeDecorator.class)
public abstract class MixinBiomeDecorator {
    @Shadow public int chunk_X;
    @Shadow public int chunk_Z;

    @Shadow protected abstract void genDecorations(BiomeGenBase p_150513_1_);

    private ThreadLocal<World> worldThreadLocal;

    private ThreadLocal<Random> randomThreadLocal;
    /**
     *
     */
    @Overwrite
    public void decorateChunk(World p_150512_1_, Random p_150512_2_, BiomeGenBase p_150512_3_, int p_150512_4_, int p_150512_5_) {
        worldThreadLocal.set(p_150512_1_);
        randomThreadLocal.set(p_150512_2_);
        chunk_X = p_150512_4_;
        chunk_Z = p_150512_5_;
        genDecorations(p_150512_3_);
        randomThreadLocal.remove();
        worldThreadLocal.remove();
    }

    @Redirect(method = "genDecorations", at = @At(value = "FIELD", target = "Lnet/minecraft/world/biome/BiomeDecorator;randomGenerator:Ljava/util/Random;"))
    public Random redirectRNGAccess(BiomeDecorator decorator) {
        return randomThreadLocal.get();
    }

    @Redirect(method = "genDecorations", at = @At(value = "FIELD", target = "Lnet/minecraft/world/biome/BiomeDecorator;currentWorld:Lnet/minecraft/world/World;"))
    public World redirectWorldAccess(BiomeDecorator decorator) {
        return worldThreadLocal.get();
    }
}
