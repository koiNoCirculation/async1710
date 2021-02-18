package org.tgt.async1710.world;

import java.util.concurrent.CompletableFuture;

public class RunnableTask implements Task{
    private CompletableFuture<Void> ftr;
    private Runnable runnable;
    public RunnableTask(Runnable callable) {
        this.runnable = callable;
        this.ftr = new CompletableFuture<>();
    }
    public CompletableFuture<Void> getFuture() {
        return ftr;
    }
    public void call() {
        try {
            runnable.run();
            ftr.complete(null);
        } catch (Exception e) {
            ftr.completeExceptionally(e);
        }
    }
    public void cancel() {
        ftr.cancel(true);
    }
}
