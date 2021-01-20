package org.tgt.async1710.mixins.net.minecraft.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import net.minecraft.util.IntHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.locks.StampedLock;

@Mixin(IntHashMap.class)
public class MixinIntHashMap {
    private StampedLock lock = new StampedLock();
    private Int2ObjectMap map = new Int2ObjectRBTreeMap();


    /**
     * @author lyt
     * @reason sb
     * @return
     */
    @Overwrite
    public Object removeObject(int p_76049_1_) {
        long l = lock.writeLock();
        try {
            Object remove = map.remove(p_76049_1_);
            return remove;
        } finally {
            lock.unlockWrite(l);
        }
    }

    /**
     * @author lyt
     * @reason sb
     * @return
     */
    @Overwrite
    public void clearMap() {
        long l = lock.writeLock();
        try {
            map.clear();
        } finally {
            lock.unlockWrite(l);
        }
    }

    /**
     * @author lyt
     * @reason sb
     * @return
     */
    @Overwrite
    public void addKey(int p_76038_1_, Object p_76038_2_) {
        long l = lock.writeLock();
        try {
            map.put(p_76038_1_, p_76038_2_);
        } finally {
            lock.unlockWrite(l);
        }
    }

    /**
     * @author lyt
     * @reason sb
     * @param p_76041_1_
     * @return
     */
    @Overwrite
    public Object lookup(int p_76041_1_)
    {
        long l = lock.tryOptimisticRead();
        Object o = map.get(p_76041_1_);
        if (!lock.validate(l)) { // 检查乐观读锁后是否有其他写锁发生
            long lp = lock.readLock(); // 获取一个悲观读锁
            try {
                o = map.get(p_76041_1_);
            } finally {
                lock.unlockRead(lp); // 释放悲观读锁
            }
        }
        return o;
    }
}
