package org.tgt.async1710;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ReentrantReadWriteLockedList<E> implements List<E> {
    private final List<E> collection;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();


    public ReentrantReadWriteLockedList(List<E> collection) {
        this.collection = collection;
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return collection.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return collection.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        readLock.lock();
        try {
            return collection.contains(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        readLock.lock();
        try {
            return collection.iterator();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        readLock.lock();
        try {
            return collection.toArray();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        readLock.lock();
        try {
            return collection.toArray(a);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean add(E e) {
        writeLock.lock();
        try {
            return collection.add(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        writeLock.lock();
        try {
            return collection.remove(o);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        readLock.lock();
        try {
            return collection.containsAll(c);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        writeLock.lock();
        try {
            return collection.addAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        writeLock.lock();
        try {
            return collection.addAll(index, c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        writeLock.lock();
        try {
            return collection.removeAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        writeLock.lock();
        try {
            return collection.retainAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        writeLock.lock();
        try {
            collection.replaceAll(operator);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void sort(Comparator<? super E> c) {
        writeLock.lock();
        try {
            collection.sort(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            collection.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E get(int index) {
        readLock.lock();
        try {
            return collection.get(index);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public E set(int index, E element) {
        writeLock.lock();
        try {
            return collection.set(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void add(int index, E element) {
        writeLock.lock();
        try {
            collection.add(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E remove(int index) {
        writeLock.lock();
        try {
            return collection.remove(index);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int indexOf(Object o) {
        readLock.lock();
        try {
            return collection.indexOf(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        readLock.lock();
        try {
            return collection.lastIndexOf(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ListIterator<E> listIterator() {
        return collection.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return collection.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        readLock.lock();
        try {
            return collection.subList(fromIndex, toIndex);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return collection.spliterator();
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
            collection.forEach(action);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 遍历操作全部加写锁，保证一致性
     * 由于进其他线程执行，必须复制集合，否则死锁，
     *
     * @param action
     */
    public void forEachConcurrent(Consumer<? super E> action, ExecutorService executor) {
        readLock.lock();
        ImmutableList<E> copy;
        try {
            copy = ImmutableList.copyOf(collection);
        } finally {
            readLock.unlock();
        }
        CompletableFuture.allOf(copy.stream().map(e -> CompletableFuture.runAsync(() -> action.accept(e), executor)).collect(Collectors.toList()).toArray(new CompletableFuture[0])).join();

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
            for (E e : ImmutableSet.copyOf(collection)) {
                action.accept(e);
                if (removeCondition.test(e)) {
                    removes.add(e);
                }
            }
            collection.removeAll(removes);

        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 由于进其他线程执行，必须复制集合，否则死锁，
     * @param action
     * @param removeCondition
     * @param executor
     */
    public void foreachWithRemoveConcurrent(Consumer<? super E> action, Predicate<? super E> removeCondition, Executor executor) {

        readLock.lock();
        ImmutableList<E> copy;
        try {
            copy = ImmutableList.copyOf(collection);
        } finally {
            readLock.unlock();
        }
        List<E> removes = new ArrayList<>();
        CompletableFuture.allOf(copy.stream().map(e -> CompletableFuture.runAsync(() -> {
            action.accept(e);
            if (removeCondition.test(e)) {
                removes.add(e);
            }
        }, executor)).collect(Collectors.toList()).toArray(new CompletableFuture[0])).join();
        writeLock.lock();
        try {
            collection.removeAll(removes);
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
            Iterator<E> iterator = ImmutableSet.copyOf(collection).iterator();
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
