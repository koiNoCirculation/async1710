package org.tgt.async1710.mixins.net.minecraft.world;

import io.micrometer.core.instrument.Timer;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tgt.async1710.MonitorRegistry;
import org.tgt.async1710.TaskSubmitter;
import org.tgt.async1710.WorldUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Mixin(World.class)
public abstract class MixinWorld implements WorldUtils, TaskSubmitter {

    protected String threadName;

    protected LinkedBlockingQueue<FutureTask<?>> tasks = new LinkedBlockingQueue<>();

    @Shadow
    public abstract void removeEntity(Entity p_72900_1_);

    @Shadow
    public abstract boolean blockExists(int p_72899_1_, int p_72899_2_, int p_72899_3_);

    @Shadow public abstract Block getBlock(int p_147439_1_, int p_147439_2_, int p_147439_3_);

    @Shadow public abstract List getEntitiesWithinAABBExcludingEntity(Entity p_72839_1_, AxisAlignedBB p_72839_2_);

    @Shadow public abstract boolean spawnEntityInWorld(Entity p_72838_1_);

    @Shadow public abstract void removePlayerEntityDangerously(Entity p_72973_1_);

    @Shadow public abstract void updateEntityWithOptionalForce(Entity p_72866_1_, boolean p_72866_2_);

    private Timer weatherTimer;

    private Timer entityTimer;

    private Timer entityInnerTimer;

    private Timer tilesTimer;


    @Override
    public void setThreadName(String threadName) {
        weatherTimer = MonitorRegistry.getInstance().timer("weather", "thread", threadName);

        entityTimer = MonitorRegistry.getInstance().timer("entities", "thread", threadName);

        tilesTimer = MonitorRegistry.getInstance().timer("tileentities", "thread", threadName);

        entityInnerTimer = MonitorRegistry.getInstance().timer("innerticks", "thread", threadName);

        this.threadName = threadName;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public <T> FutureTask<T> submit0(Callable<T> task) {
        FutureTask<T> tFutureTask = new FutureTask<>(task);
        if (getRunning()) {
            tasks.add(tFutureTask);
        }
        return tFutureTask;
    }

    @Override
    public FutureTask<?> submit0(Runnable task) {
        FutureTask<?> tFutureTask = new FutureTask(task, null);
        if (getRunning()) {
            tasks.add(tFutureTask);
        }
        return tFutureTask;
    }

    @Override
    public void cancelTasks() {
        while (!tasks.isEmpty()) {
            tasks.poll().cancel(true);
        }
    }

    @Override
    public void runTasks() {
        while (!tasks.isEmpty()) {
            tasks.poll().run();
        }
    }



    /**
     * @author lyt
     * @reason 局部变量axisAlignedBBS
     * @param p_72945_1_
     * @param p_72945_2_
     * @return
     */
    @Overwrite
    public List getCollidingBoundingBoxes(Entity p_72945_1_, AxisAlignedBB p_72945_2_) {
        ArrayList<AxisAlignedBB> axisAlignedBBS = new ArrayList<>();
        int i = MathHelper.floor_double(p_72945_2_.minX);
        int j = MathHelper.floor_double(p_72945_2_.maxX + 1.0D);
        int k = MathHelper.floor_double(p_72945_2_.minY);
        int l = MathHelper.floor_double(p_72945_2_.maxY + 1.0D);
        int i1 = MathHelper.floor_double(p_72945_2_.minZ);
        int j1 = MathHelper.floor_double(p_72945_2_.maxZ + 1.0D);

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = i1; l1 < j1; ++l1)
            {
                if (this.blockExists(k1, 64, l1))
                {
                    for (int i2 = k - 1; i2 < l; ++i2)
                    {
                        Block block;

                        if (k1 >= -30000000 && k1 < 30000000 && l1 >= -30000000 && l1 < 30000000)
                        {
                            block = getBlock(k1, i2, l1);
                        }
                        else
                        {
                            block = Blocks.stone;
                        }

                        block.addCollisionBoxesToList((World)(Object)this, k1, i2, l1, p_72945_2_, axisAlignedBBS, p_72945_1_);
                    }
                }
            }
        }

        double d0 = 0.25D;
        List list = getEntitiesWithinAABBExcludingEntity(p_72945_1_, p_72945_2_.expand(d0, d0, d0));

        for (int j2 = 0; j2 < list.size(); ++j2)
        {
            AxisAlignedBB axisalignedbb1 = ((Entity)list.get(j2)).getBoundingBox();

            if (axisalignedbb1 != null && axisalignedbb1.intersectsWith(p_72945_2_))
            {
                axisAlignedBBS.add(axisalignedbb1);
            }

            axisalignedbb1 = p_72945_1_.getCollisionBox((Entity)list.get(j2));

            if (axisalignedbb1 != null && axisalignedbb1.intersectsWith(p_72945_2_))
            {
                axisAlignedBBS.add(axisalignedbb1);
            }
        }

        return axisAlignedBBS;
    }

    /**
     * @author lyt
     * @reason 局部变量
     * axisAlignedBBS
     * @param p_147461_1_
     */
    @Inject(method = "func_147461_a", at = @At("HEAD"), cancellable = true)
    public void func_147461_a(AxisAlignedBB p_147461_1_, CallbackInfoReturnable<List> cir)
    {
        ArrayList<AxisAlignedBB> axisAlignedBBS = new ArrayList<>();
        int i = MathHelper.floor_double(p_147461_1_.minX);
        int j = MathHelper.floor_double(p_147461_1_.maxX + 1.0D);
        int k = MathHelper.floor_double(p_147461_1_.minY);
        int l = MathHelper.floor_double(p_147461_1_.maxY + 1.0D);
        int i1 = MathHelper.floor_double(p_147461_1_.minZ);
        int j1 = MathHelper.floor_double(p_147461_1_.maxZ + 1.0D);

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = i1; l1 < j1; ++l1)
            {
                if (this.blockExists(k1, 64, l1))
                {
                    for (int i2 = k - 1; i2 < l; ++i2)
                    {
                        Block block;

                        if (k1 >= -30000000 && k1 < 30000000 && l1 >= -30000000 && l1 < 30000000)
                        {
                            block = this.getBlock(k1, i2, l1);
                        }
                        else
                        {
                            block = Blocks.bedrock;
                        }

                        block.addCollisionBoxesToList((World)(Object)this, k1, i2, l1, p_147461_1_, axisAlignedBBS, (Entity)null);
                    }
                }
            }
        }
        cir.setReturnValue(axisAlignedBBS);
    }

    @Inject(method = "spawnEntityInWorld", at = @At(value = "HEAD", remap = false), cancellable = true)
    public void _spawnEntityInWorld(Entity p_72838_1_, CallbackInfoReturnable<Boolean> cir) throws Exception {
        submit(() -> spawnEntityInWorld(p_72838_1_), cir, true);
    }

    //public void removePlayerEntityDangerously(Entity p_72973_1_)
    @Inject(method = "removePlayerEntityDangerously", at = @At(value = "HEAD", remap = false), cancellable = true)
    public void _removePlayerEntityDangerously(Entity p_72973_1_, CallbackInfo ci) {
        submit(() -> removePlayerEntityDangerously(p_72973_1_));
        if(threadName != Thread.currentThread().getName() && getRunning()) {
            try {
                submit(() -> removePlayerEntityDangerously(p_72973_1_));
            } catch (Exception e) {
            }
            ci.cancel();
        }
    }

    @Inject(method = "removeEntity", at = @At(value = "HEAD"), cancellable = true)
    private void _removeEntity(Entity p_72973_1_, CallbackInfo ci) {
        submit(() -> removeEntity(p_72973_1_));

    }

    @Inject(method = "updateEntityWithOptionalForce", at = @At(value = "HEAD"), cancellable = true)
    public void _updateEntityWithOptionalForce(Entity p_72866_1_, boolean p_72866_2_, CallbackInfo ci) {
        submit(() -> updateEntityWithOptionalForce(p_72866_1_, p_72866_2_));
    }
}
