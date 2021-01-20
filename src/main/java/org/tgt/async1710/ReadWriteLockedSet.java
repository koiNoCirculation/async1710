package org.tgt.async1710;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReadWriteLockedSet<E> implements Set<E> {
    private final Set<E> set;

    private final StampedLock lock = new StampedLock();

    public  ReadWriteLockedSet(Set<E> set) {
        this.set = set;
    }

    @Override
    public int size() {
        long l = lock.tryOptimisticRead();
        int size = set.size();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                size = set.size();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        long l = lock.tryOptimisticRead();
        boolean empty = set.isEmpty();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                empty = set.isEmpty();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return empty;
    }

    @Override
    public boolean contains(Object o) {
        long l = lock.tryOptimisticRead();
        boolean contains = set.contains(o);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                contains = set.contains(o);
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return contains;
    }

    @Override
    public Iterator<E> iterator() {
        long l = lock.tryOptimisticRead();
        UnmodifiableIterator<E> iterator = ImmutableList.copyOf(set).iterator();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                iterator = ImmutableList.copyOf(set).iterator();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return iterator;
    }

    @Override
    public Object[] toArray() {
        long l = lock.tryOptimisticRead();
        Object[] objects = set.toArray();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                objects = set.toArray();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return objects;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        long l = lock.tryOptimisticRead();
        T[] objects = set.toArray(a);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                objects = set.toArray(a);
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return objects;
    }

    @Override
    public boolean add(E e) {
        long l = lock.writeLock();
        try {
            return set.add(e);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean remove(Object o) {
        long l = lock.writeLock();
        try {
            return set.remove(o);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        long l = lock.tryOptimisticRead();
        boolean b = set.containsAll(c);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                b = set.containsAll(c);
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return b;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        long l = lock.writeLock();
        try {
            return set.addAll(c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        long l = lock.writeLock();
        try {
            return set.removeAll( c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        long l = lock.writeLock();
        try {
            return set.retainAll(c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public void clear() {
        long l = lock.writeLock();
        try {
            set.clear();
        } finally {
            lock.unlockWrite(l);
        }
    }

    /**
     * 遍历操作全部加写锁，保证一致性
     * @param action
     */
    @Override
    public void forEach(Consumer<? super E> action) {
        long l = lock.writeLock();
        try {
            set.forEach(action);
        } finally {
            lock.unlockWrite(l);
        }
    }

    /**
     * 遍历操作全部加写锁，保证一致性
     * @param action
     */
    public void forEachConcurrent(Consumer<? super E> action, ExecutorService executor) {
        long l = lock.writeLock();
        try {
            CompletableFuture.allOf(set.stream().map(e -> CompletableFuture.runAsync(() -> action.accept(e), executor)).toArray(CompletableFuture[]::new));
        } finally {
            lock.unlockWrite(l);
        }
    }

    /**
     * 遍历操作全部加写锁，保证一致性
     * @param action
     */
    public void foreachWithRemove(Consumer<? super E> action, Predicate<? super E> removeCondition) {
        long l = lock.writeLock();
        try {
            Iterator<E> iterator = set.iterator();
            while (iterator.hasNext()) {
                E next = iterator.next();
                action.accept(next);
                if(removeCondition.test(next)) {
                    iterator.remove();
                }
            }
        } finally {
            lock.unlockWrite(l);
        }
    }

    public void foreachWithRemoveConcurrent(Consumer<? super E> action, Predicate<? super E> removeCondition, Executor executor) {
        long l = lock.writeLock();
        try {
            List<E> removes = new ArrayList<>();
            CompletableFuture.allOf(set.stream().map(e -> CompletableFuture.runAsync(() -> {
                action.accept(e);
                if (removeCondition.test(e)) {
                    removes.add(e);
                }
            }, executor)).toArray(CompletableFuture[]::new));
           set.removeAll(removes);
        } finally {
            lock.unlockWrite(l);
        }
    }

    /**
     * 遍历操作全部加写锁，保证一致性
     */
    public void foreachWithBreak(Predicate<? super E> breakCondition) {
        long l = lock.writeLock();
        try {
            Iterator<E> iterator = set.iterator();
            while (iterator.hasNext()) {
                E next = iterator.next();
                if(breakCondition.test(next)) {
                    return;
                }
            }
        } finally {
            lock.unlockWrite(l);
        }
    }
}
