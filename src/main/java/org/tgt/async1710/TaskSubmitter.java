package org.tgt.async1710;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public interface TaskSubmitter {
    <T> FutureTask<T> submit(Callable<T> task);

    FutureTask submit(Runnable task);


    void runTasks();

    void cancelTasks();


    /**
     * 挂靠一个接口再说，似乎非mixin类不能再mixin加载？？？？？
     * @param old
     * @param <T>
     * @return
     */
    public static <T> List<T> copyList(List<T> old) {
        ArrayList<T> ts = new ArrayList<>(old);
        Collections.copy(ts, old);
        return ts;
    }
}
