package org.tgt.async1710;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

public class GlobalExecutor {
    public static Executor fjp = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    public static Executor stp = Executors.newSingleThreadExecutor();
    public static <T> CompletableFuture<T> submitTask(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, fjp);
    }
}
