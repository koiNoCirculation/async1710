package org.tgt.async1710.mixins.net.minecraft.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import net.minecraft.util.LongHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Mixin(LongHashMap.class)
public class MixinLongHashMap {
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Long2ObjectMap map = new Long2ObjectRBTreeMap();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();
    /**
     * @author lyt
     * @reason sb
     * @return
     */
    @Overwrite
    public int getNumHashElements()
    {
        try {
            readLock.lock();
            try {
                return map.size();
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
    public Object remove(long p_76159_1_) {
        try {
            writeLock.lock();
            try {
                return map.remove(p_76159_1_);
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
    public void add(long p_76163_1_, Object p_76163_3_) {
        try {
            writeLock.lock();
            try {
                map.put(p_76163_1_, p_76163_3_);
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
    public boolean containsItem(long p_76161_1_) {
        try {
            readLock.lock();
            try {
                return map.containsKey(p_76161_1_);
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
    public Object getValueByKey(long p_76164_1_) {
        try {
            readLock.lock();
            try {
                return map.get(p_76164_1_);
            } finally {
                readLock.unlock();
            }
        } catch (Exception e) {
            throw e;
        }
    }

}
