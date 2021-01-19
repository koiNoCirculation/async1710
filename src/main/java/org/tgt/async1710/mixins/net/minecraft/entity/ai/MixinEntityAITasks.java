package org.tgt.async1710.mixins.net.minecraft.entity.ai;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.entity.ai.EntityAITasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mixin(EntityAITasks.class)
public class MixinEntityAITasks {
    public Set<EntityAITasks.EntityAITaskEntry> taskEntriesSet = new ConcurrentSet<>();
    /** A list of EntityAITaskEntrys that are currently being executed. */
    private Set<EntityAITasks.EntityAITaskEntry> executingTaskEntriesSet = new ConcurrentSet<>();

    @Redirect(method = "addTask", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false))
    public <E> boolean taskEntries_addTask(List<E> list, E e) {
        return taskEntriesSet.add((EntityAITasks.EntityAITaskEntry) e);
    }

    @Redirect(method = "removeTask", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false))
    public Iterator taskEntries_removeTask(List list) {
        return taskEntriesSet.iterator();
    }


    @Redirect(method = "onUpdateTasks", at = @At(value = "INVOKE", ordinal = 0, target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false))
    public Iterator taskEntries_onUpdateTasks(List list) {
        return taskEntriesSet.iterator();
    }

    @Redirect(method = "canUse", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false))
    private Iterator taskEntries_canUse(List list) {
        return taskEntriesSet.iterator();
    }

    @Redirect(method = "removeTask", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z", remap = false))
    public boolean executingTaskEntries_contains_removeTask(List list, Object o) {
        return executingTaskEntriesSet.contains(o);
    }

    @Redirect(method = "removeTask", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(Ljava/lang/Object;)Z", remap = false))
    public boolean executingTaskEntries_remove_removeTask(List list, Object o) {
        return executingTaskEntriesSet.remove( o);
    }




    @Redirect(method = "onUpdateTasks", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z", remap = false))
    public boolean executingTaskEntries_contains_onUpdateTasks(List list, Object o) {

        return executingTaskEntriesSet.contains(o);
    }
    @Redirect(method = "onUpdateTasks", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(Ljava/lang/Object;)Z", remap = false))
    public boolean executingTaskEntries_remove_onUpdateTasks(List list, Object o) {

        return executingTaskEntriesSet.remove(o);
    }

    @Redirect(method = "onUpdateTasks", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false))
    public <E> boolean executingTaskEntries_add_onUpdateTasks(List list, E e) {

        return executingTaskEntriesSet.add((EntityAITasks.EntityAITaskEntry) e);
    }

    @Redirect(method = "onUpdateTasks", at = @At(value = "INVOKE", ordinal = 0, target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false))
    public Iterator executingTaskEntries_iterator_onUpdateTasks1(List list) {
        return executingTaskEntriesSet.iterator();
    }

    @Redirect(method = "onUpdateTasks", at = @At(value = "INVOKE", ordinal = 1, target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false))
    public Iterator executingTaskEntries_iterator_onUpdateTasks2(List list) {
        return executingTaskEntriesSet.iterator();
    }


    @Redirect(method = "canUse", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z", remap = false))
    private boolean executingTaskEntries_contains_canUse(List list, Object o) {
        return executingTaskEntriesSet.contains(o);
    }
}
