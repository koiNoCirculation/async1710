package org.tgt.async1710;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

public class GlobalExecutor {
    public static ForkJoinPool fjp = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    public static <T> CompletableFuture<T> submitTask(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, fjp);
    }
}
