package org.tgt.async1710.mixins.net.minecraft.entity.ai;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.tgt.async1710.ReadWriteLockedSet;

import java.util.*;

@Mixin(EntityAITasks.class)
public abstract class MixinEntityAITasks {
    @Shadow private int tickCount;
    @Shadow private int tickRate;

    @Shadow protected abstract boolean canContinue(EntityAITasks.EntityAITaskEntry p_75773_1_);

    @Shadow protected abstract boolean areTasksCompatible(EntityAITasks.EntityAITaskEntry p_75777_1_, EntityAITasks.EntityAITaskEntry p_75777_2_);

    public ReadWriteLockedSet<EntityAITasks.EntityAITaskEntry> taskEntriesSet = new ReadWriteLockedSet<>(new HashSet<>());
    /** A list of EntityAITaskEntrys that are currently being executed. */
    private ReadWriteLockedSet<EntityAITasks.EntityAITaskEntry> executingTaskEntriesSet = new ReadWriteLockedSet<>(new HashSet<>());

    @Redirect(method = "addTask", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false))
    public <E> boolean taskEntries_addTask(List<E> list, E e) {
        return taskEntriesSet.add((EntityAITasks.EntityAITaskEntry) e);
    }

    /**
     * @author
     */
    @Overwrite
    public void onUpdateTasks()
    {
        ArrayList arraylist = new ArrayList();
        if (this.tickCount++ % this.tickRate == 0)
        {
            taskEntriesSet.forEach(
                    (entityaitaskentry) -> {
                        boolean flag = executingTaskEntriesSet.contains(entityaitaskentry);

                        if (flag)
                        {
                            if (canUse(entityaitaskentry) && canContinue(entityaitaskentry))
                            {
                                return;
                            }
                            entityaitaskentry.action.resetTask();
                            executingTaskEntriesSet.remove(entityaitaskentry);
                        }

                        if (this.canUse(entityaitaskentry) && entityaitaskentry.action.shouldExecute())
                        {
                            arraylist.add(entityaitaskentry);
                            executingTaskEntriesSet.add(entityaitaskentry);
                        }
                    }
            );
        }
        else
        {
            executingTaskEntriesSet.foreachWithRemove(
                    entityAITaskEntry -> {}, entityAITaskEntry -> entityAITaskEntry.action.continueExecuting()
            );
        }

        for (Object o : arraylist) {
            EntityAITasks.EntityAITaskEntry entityaitaskentry = (EntityAITasks.EntityAITaskEntry)o;
            entityaitaskentry.action.startExecuting();
        }

        executingTaskEntriesSet.forEach((entityaitaskentry) -> {
            entityaitaskentry.action.updateTask();});
    }

    /**
     * @author
     */
    @Overwrite
    private boolean canUse(EntityAITasks.EntityAITaskEntry p_75775_1_)
    {
        final boolean[] result = {true};
        taskEntriesSet.foreachWithBreak(entityaitaskentry -> {
            if (entityaitaskentry != p_75775_1_)
            {
                if (p_75775_1_.priority >= entityaitaskentry.priority)
                {
                    if (executingTaskEntriesSet.contains(entityaitaskentry) && !this.areTasksCompatible(p_75775_1_, entityaitaskentry))
                    {
                        result[0] = true;
                        return false;
                    }
                }
                else if (executingTaskEntriesSet.contains(entityaitaskentry) && !entityaitaskentry.action.isInterruptible())
                {
                    result[0] = false;
                    return false;
                }
            }
            return true;
        });
        return true;
    }

    /**
     * @author
     */
    @Overwrite
    public void removeTask(EntityAIBase p_85156_1_)
    {
        taskEntriesSet.foreachWithRemove(entityaitaskentry -> {
            if (entityaitaskentry.action == p_85156_1_)
            {
                if (executingTaskEntriesSet.contains(entityaitaskentry))
                {
                    entityaitaskentry.action.resetTask();
                    executingTaskEntriesSet.remove(entityaitaskentry);
                }
            }
        }, (entityaitaskentry) -> entityaitaskentry.action == p_85156_1_);
    }

}
