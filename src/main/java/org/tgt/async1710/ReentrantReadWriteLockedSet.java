package org.tgt.async1710;

import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReentrantReadWriteLockedSet<E> implements Set<E> {
    private final Set<E> set;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();


    public ReentrantReadWriteLockedSet(Set<E> set) {
        this.set = set;
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return set.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return set.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        readLock.lock();
        try {
            return set.contains(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        readLock.lock();
        try {
            return set.iterator();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        readLock.lock();
        try {
            return set.toArray();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        readLock.lock();
        try {
            return set.toArray(a);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean add(E e) {
        writeLock.lock();
        try {
            return set.add(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        writeLock.lock();
        try {
            return set.remove(o);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        readLock.lock();
        try {
            return set.containsAll(c);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        writeLock.lock();
        try {
            return set.addAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        writeLock.lock();
        try {
            return set.removeAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        writeLock.lock();
        try {
            return set.retainAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            set.clear();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 遍历操作全部加写锁，保证一致性
     *
     * @param action
     */
    @Override
    public void forEach(Consumer<? super E> action) {
        writeLock.lock();
        try {
            set.forEach(action);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 遍历操作全部加写锁，保证一致性
     *
     * @param action
     */
    public void forEachConcurrent(Consumer<? super E> action, ExecutorService executor) {
        writeLock.lock();
        try {
            CompletableFuture.allOf(set.stream().map(e -> CompletableFuture.runAsync(() -> action.accept(e), executor)).collect(Collectors.toList()).toArray(new CompletableFuture[0])).join();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 遍历操作全部加写锁，保证一致性
     * 遍历操作本身需要复制集合，因为里面可能有递归修改，不复制会cme
     *
     * @param action
     */
    public void foreachWithRemove(Consumer<? super E> action, Predicate<? super E> removeCondition) {

        writeLock.lock();
        try {
            List<E> removes = new ArrayList<>();
            for(E e : ImmutableSet.copyOf(set)) {
                action.accept(e);
                if (removeCondition.test(e)) {
                    removes.add(e);
                }
            }
            set.removeAll(removes);

        } finally {
            writeLock.unlock();
        }
    }

    public void foreachWithRemoveConcurrent(Consumer<? super E> action, Predicate<? super E> removeCondition, Executor executor) {

        writeLock.lock();
        try {
            List<E> removes = new ArrayList<>();
            CompletableFuture.allOf(ImmutableSet.copyOf(set).stream().map(e -> CompletableFuture.runAsync(() -> {
                action.accept(e);
                if (removeCondition.test(e)) {
                    removes.add(e);
                }
            }, executor)).collect(Collectors.toList()).toArray(new CompletableFuture[0])).join();
            set.removeAll(removes);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 遍历操作全部加写锁，保证一致性
     */
    public void foreachWithBreak(Predicate<? super E> breakCondition) {

        writeLock.lock();
        try {
            Iterator<E> iterator = ImmutableSet.copyOf(set).iterator();
            while (iterator.hasNext()) {
                E next = iterator.next();
                if (breakCondition.test(next)) {
                    return;
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
}
