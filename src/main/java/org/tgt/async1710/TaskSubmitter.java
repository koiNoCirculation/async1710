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
            return Optional.empty();
        }
    }

    default <T> void submitWait(Callable<T> task, CallbackInfoReturnable<T> ci, T defaultValue) {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            /**
             * tps为0，5
             */
            try {
                ci.setReturnValue(submit0(task).get(500, TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                ci.setReturnValue(defaultValue);
            }
        }
    }


    default void submitWait(Runnable task, CallbackInfoReturnable<?> ci){
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            /**
             * tps为0，5
             */
            ci.cancel();
            try {
                submit0(task).get(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    default <T> T submitWait(Callable<T> task, T defaultValue) {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            /**
             * tps为0，5
             */
            try {
                return submit0(task).get(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                //e.printStackTrace();
                return defaultValue;
            }
        } else {
            try {
                return task.call();
            } catch (Exception e) {
                e.printStackTrace();
                return defaultValue;
            }
        }
    }


    default void submitWait(Runnable task) {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            /**
             * tps为0，5
             */
            try {
                submit0(task).get(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        } else {
            task.run();
        }
    }


    default void submitWait(Runnable task, CallbackInfo ci) {
        String threadName = ((WorldUtils) this).getThreadName();
        boolean running = ((WorldUtils) this).getRunning();

        if(threadName != Thread.currentThread().getName() && running) {
            /**
             * tps为0，5
             */
            ci.cancel();
            try {
                submit0(task).get(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    <T> FutureTask<T> submit0(Callable<T> task);

    FutureTask<?> submit0(Runnable task);


    void runTasks();

    void cancelTasks();

}
