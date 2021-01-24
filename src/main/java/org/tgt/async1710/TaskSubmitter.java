package org.tgt.async1710;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public interface TaskSubmitter {
    default <T> Optional<FutureTask<T>> submit(Callable<T> task) throws Exception {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            return Optional.of(submit0(task));
        } else {
            task.call();
            return Optional.empty();
        }
    }

    default Optional<FutureTask<?>> submit(Runnable task) {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            return Optional.of(submit0(task));
        } else {
            task.run();
            return Optional.empty();
        }
    }

    default <T> Optional<FutureTask<T>> submit(Callable<T> task, CallbackInfo ci) throws Exception {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            ci.cancel();
            return Optional.of(submit0(task));
        } else {
            task.call();
            return Optional.empty();
        }
    }

    default Optional<FutureTask<?>> submit(Runnable task, CallbackInfo ci) {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            ci.cancel();
            return Optional.of(submit0(task));
        } else {
            task.run();
            return Optional.empty();
        }
    }

    default <T> Optional<FutureTask<T>> submit(Callable<T> task, CallbackInfoReturnable<T> ci, T defaultValue) throws Exception {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            ci.setReturnValue(defaultValue);
            return Optional.of(submit0(task));
        } else {
            ci.setReturnValue(task.call());
            return Optional.empty();
        }
    }


    default Optional<FutureTask<?>> submit(Runnable task, CallbackInfoReturnable<?> ci) {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            ci.cancel();
            return Optional.of(submit0(task));
        } else {
            task.run();
            return Optional.empty();
        }
    }

    default <T> void submitWait(Callable<T> task, CallbackInfoReturnable<T> ci) throws Exception {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            /**
             * tps为0，5
             */
            ci.setReturnValue(submit0(task).get(2000, TimeUnit.MILLISECONDS));
        } else {
            ci.setReturnValue(task.call());
        }
    }


    default void submitWait(Runnable task, CallbackInfoReturnable<?> ci) throws Exception {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            /**
             * tps为0，5
             */
            submit0(task).get(2000, TimeUnit.MILLISECONDS);
        } else {
            task.run();
        }
        ci.cancel();
    }

    default <T> T submitWait(Callable<T> task) throws Exception {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            /**
             * tps为0，5
             */
            return submit0(task).get(2000, TimeUnit.MILLISECONDS);
        } else {
            return task.call();
        }
    }


    default void submitWait(Runnable task) throws Exception {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            /**
             * tps为0，5
             */
            submit0(task).get(2000, TimeUnit.MILLISECONDS);
        } else {
            task.run();
        }
    }



    <T> FutureTask<T> submit0(Callable<T> task);

    FutureTask<?> submit0(Runnable task);


    void runTasks();

    void cancelTasks();

}
