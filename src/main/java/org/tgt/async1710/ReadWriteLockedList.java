package org.tgt.async1710;

import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockedList<E> implements List<E> {
    private List<E> list;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();

    public ReadWriteLockedList(List<E> list) {
        this.list = list;
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return list.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return list.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        readLock.lock();
        try {
            return list.contains(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        readLock.lock();
        try {
            return ImmutableList.copyOf(list).iterator();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        readLock.lock();
        try {
            return ImmutableList.copyOf(list).toArray();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        readLock.lock();
        try {
            return ImmutableList.copyOf(list).toArray(a);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean add(E e) {
        writeLock.lock();
        try {
            return list.add(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        writeLock.lock();
        try {
            return list.remove(o);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        readLock.lock();
        try {
            return list.containsAll(c);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        writeLock.lock();
        try {
            return list.addAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        writeLock.lock();
        try {
            return list.addAll(index, c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        writeLock.lock();
        try {
            return list.removeAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        writeLock.lock();
        try {
            return list.removeAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            list.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E get(int index) {
        readLock.lock();
        try {
            return list.get(index);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public E set(int index, E element) {
        writeLock.lock();
        try {
            return list.set(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void add(int index, E element) {
        writeLock.lock();
        try {
            list.add(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E remove(int index) {
        writeLock.lock();
        try {
            return list.remove(index);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int indexOf(Object o) {
        readLock.lock();
        try {
            return list.indexOf(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        readLock.lock();
        try {
            return list.lastIndexOf(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ListIterator<E> listIterator() {
        readLock.lock();
        try {
            return ImmutableList.copyOf(list).listIterator();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        readLock.lock();
        try {
            return ImmutableList.copyOf(list).listIterator(index);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        readLock.lock();
        try {
            return ImmutableList.copyOf(list).subList(fromIndex, toIndex);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void sort(Comparator<? super E> c) {
        writeLock.lock();
        try {
            list.sort(c);
        } finally {
            writeLock.unlock();
        }
    }
}
