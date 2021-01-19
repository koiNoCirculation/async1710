package org.tgt.async1710.mixins.net.minecraft.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import net.minecraft.util.IntHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Mixin(IntHashMap.class)
public class MixinIntHashMap {
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Int2ObjectMap map = new Int2ObjectRBTreeMap();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();


    /**
     * @author lyt
     * @reason sb
     * @return
     */
    @Overwrite
    public Object removeObject(int p_76049_1_) {
        try {
            readLock.lock();
            try {
                Object remove = map.remove(p_76049_1_);
                return remove;
            } finally {
                readLock.unlock();

            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * @author lyt
     * @reason sb
     * @return
     */
    @Overwrite
    public void clearMap() {
        try {
            writeLock.lock();
            try {
                map.clear();
            } finally {
                writeLock.unlock();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * @author lyt
     * @reason sb
     * @return
     */
    @Overwrite
    public void addKey(int p_76038_1_, Object p_76038_2_) {
        try {
            writeLock.lock();
            try {
                map.put(p_76038_1_, p_76038_2_);
            } finally {
                writeLock.unlock();
            }
        }catch (Exception e) {
            throw e;
        }
    }
}
