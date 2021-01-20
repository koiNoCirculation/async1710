package org.tgt.async1710;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;

import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 因为你他妈的声明了linkedlist而不是list
 * @param <E>
 */
public class ReadWriteLockedLinkedList<E> extends LinkedList<E> {

    private final StampedLock lock = new StampedLock();
    @Override
    public int size() {
        long l = lock.tryOptimisticRead();
        int size = super.size();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                size = super.size();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        long l = lock.tryOptimisticRead();
        boolean empty = super.isEmpty();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                empty = super.isEmpty();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return empty;
    }

    @Override
    public boolean contains(Object o) {
        long l = lock.tryOptimisticRead();
        boolean contains = super.contains(o);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                contains = super.contains(o);
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return contains;
    }

    @Override
    public Iterator<E> iterator() {
        long l = lock.tryOptimisticRead();
        UnmodifiableIterator<E> iterator = ImmutableList.copyOf(this).iterator();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                iterator = ImmutableList.copyOf(this).iterator();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return iterator;
    }

    @Override
    public Object[] toArray() {
        long l = lock.tryOptimisticRead();
        Object[] objects = super.toArray();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                objects = super.toArray();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return objects;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        long l = lock.tryOptimisticRead();
        T[] objects = super.toArray(a);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                objects = super.toArray(a);
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
            return super.add(e);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean remove(Object o) {
        long l = lock.writeLock();
        try {
            return super.remove(o);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        long l = lock.tryOptimisticRead();
        boolean b = super.containsAll(c);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                b = super.containsAll(c);
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
            return super.addAll(c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        long l = lock.writeLock();
        try {
            return super.addAll(index, c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        long l = lock.writeLock();
        try {
            return super.removeAll( c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        long l = lock.writeLock();
        try {
            return super.retainAll(c);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public void clear() {
        long l = lock.writeLock();
        try {
            super.clear();
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public E get(int index) {
        long l = lock.tryOptimisticRead();
        E e = super.get(index);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                e = super.get(index);;
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
            return super.set(index, element);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public void add(int index, E element) {
        long l = lock.writeLock();
        try {
            super.add(index, element);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public E remove(int index) {
        long l = lock.writeLock();
        try {
            return super.remove(index);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public int indexOf(Object o) {
        long l = lock.writeLock();
        try {
            return super.indexOf(o);
        } finally {
            lock.unlockWrite(l);
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        long l = lock.tryOptimisticRead();
        int i = super.lastIndexOf(o);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                i = super.lastIndexOf(o);;
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return i;
    }

    @Override
    public ListIterator<E> listIterator() {
        long l = lock.tryOptimisticRead();
        ListIterator<E> eListIterator = super.listIterator();
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                eListIterator = super.listIterator();
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return eListIterator;
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        long l = lock.tryOptimisticRead();
        ListIterator<E> eListIterator = super.listIterator(index);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                eListIterator = super.listIterator(index);
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
            super.sort(c);
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
            super.forEach(action);
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
            Iterator<E> iterator = super.iterator();
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
}
