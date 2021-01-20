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
import java.util.function.Supplier;

public class ReadWriteLockedList<E> implements List<E> {
    private final List<E> list;

    private final StampedLock lock = new StampedLock();

    public ReadWriteLockedList(List<E> list) {
        this.list = list;
    }

    @Override
    public int size() {
        long l = lock.tryOptimisticRead();
        int size = list.size();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                size = list.size();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        long l = lock.tryOptimisticRead();
        boolean empty = list.isEmpty();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                empty = list.isEmpty();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return empty;
    }

    @Override
    public boolean contains(Object o) {
        long l = lock.tryOptimisticRead();
        boolean contains = list.contains(o);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                contains = list.contains(o);
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return contains;
    }

    @Override
    public Iterator<E> iterator() {
        long l = lock.tryOptimisticRead();
        UnmodifiableIterator<E> iterator = ImmutableList.copyOf(list).iterator();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                iterator = ImmutableList.copyOf(list).iterator();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return iterator;
    }

    @Override
    public Object[] toArray() {
        long l = lock.tryOptimisticRead();
        Object[] objects = list.toArray();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                objects = list.toArray();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return objects;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        long l = lock.tryOptimisticRead();
        T[] objects = list.toArray(a);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                objects = list.toArray(a);
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
            return list.add(e);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean remove(Object o) {
        long l = lock.writeLock();
        try {
            return list.remove(o);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        long l = lock.tryOptimisticRead();
        boolean b = list.containsAll(c);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                b = list.containsAll(c);
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
            return list.addAll(c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        long l = lock.writeLock();
        try {
            return list.addAll(index, c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        long l = lock.writeLock();
        try {
            return list.removeAll( c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        long l = lock.writeLock();
        try {
            return list.retainAll(c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public void clear() {
        long l = lock.writeLock();
        try {
            list.clear();
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public E get(int index) {
        long l = lock.tryOptimisticRead();
        E e = list.get(index);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                e = list.get(index);;
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return e;
    }

    @Override
    public E set(int index, E element) {
        long l = lock.writeLock();
        try {
            return list.set(index, element);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public void add(int index, E element) {
        long l = lock.writeLock();
        try {
            list.add(index, element);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public E remove(int index) {
        long l = lock.writeLock();
        try {
            return list.remove(index);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public int indexOf(Object o) {
        long l = lock.writeLock();
        try {
            return list.indexOf(o);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        long l = lock.tryOptimisticRead();
        int i = list.lastIndexOf(o);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                i = list.lastIndexOf(o);;
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return i;
    }

    @Override
    public ListIterator<E> listIterator() {
        long l = lock.tryOptimisticRead();
        ListIterator<E> eListIterator = list.listIterator();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                eListIterator = list.listIterator();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return eListIterator;
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        long l = lock.tryOptimisticRead();
        ListIterator<E> eListIterator = list.listIterator(index);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                eListIterator = list.listIterator(index);
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return eListIterator;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        long l = lock.tryOptimisticRead();
        List<E> es = subList(fromIndex, toIndex);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                es = subList(fromIndex, toIndex);
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return es;
    }

    @Override
    public void sort(Comparator<? super E> c) {
        long l = lock.writeLock();
        try {
            list.sort(c);
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
            list.forEach(action);
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
            CompletableFuture.allOf(list.stream().map(e -> CompletableFuture.runAsync(() -> action.accept(e), executor)).toArray(CompletableFuture[]::new));
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
            Iterator<E> iterator = list.iterator();
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
            CompletableFuture.allOf(list.stream().map(e -> CompletableFuture.runAsync(() -> {
                action.accept(e);
                if (removeCondition.test(e)) {
                    removes.add(e);
                }
            }, executor)).toArray(CompletableFuture[]::new));
            list.removeAll(removes);
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
            Iterator<E> iterator = list.iterator();
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
