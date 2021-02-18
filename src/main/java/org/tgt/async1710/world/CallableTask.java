package org.tgt.async1710.world;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class CallableTask<T> implements Task {
    private CompletableFuture<T> ftr;
    private Callable<T> callable;
    public CallableTask(Callable<T> callable) {
        this.callable = callable;
        this.ftr = new CompletableFuture<>();
    }
    public CompletableFuture<T> getFuture() {
        return ftr;
    }
    public void call() {
        try {
            ftr.complete(callable.call());
        } catch (Exception e) {
            ftr.completeExceptionally(e);
        }
    }
    public void cancel() {
        ftr.cancel(true);
    }
}
