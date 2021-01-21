package org.tgt.async1710;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public interface TaskSubmitter {
    <T> FutureTask<T> submit(Callable<T> task);

    FutureTask<?> submit(Runnable task);


    void runTasks();

    void cancelTasks();

}
