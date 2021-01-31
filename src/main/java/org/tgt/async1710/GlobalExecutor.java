package org.tgt.async1710;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import sun.nio.ch.ThreadPool;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class GlobalExecutor {
    /**
     * Network IO Threads
     */
    public static Executor worker = new ThreadPoolExecutor(4,
            Runtime.getRuntime().availableProcessors(),
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024),
            new ThreadFactoryBuilder().setNameFormat("Async worker #%d").build());
    public static <T> CompletableFuture<T> submitTask(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, worker);
    }
    public static CompletableFuture<?> submitTask(Runnable task) {
        return CompletableFuture.runAsync(task, worker);
    }
}
