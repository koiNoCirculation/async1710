package org.tgt.async1710;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.tileentity.TileEntity;

import java.util.Set;

public class ChunkTileGroup {
    private boolean processingLoadedTiles = false;
    private Set<TileEntity> loadedTiles = new ConcurrentSet<>();
    private Set<TileEntity> loadingTiles = new ConcurrentSet<>();
    private Set<TileEntity> removingTiles = new ConcurrentSet<>();

    public Set<TileEntity> getLoadedTiles() {
        return loadedTiles;
    }

    public Set<TileEntity> getLoadingTiles() {
        return loadingTiles;
    }

    public Set<TileEntity> getRemovingTiles() {
        return removingTiles;
    }

    public void setProcessingLoadedTiles(boolean status) {
        this.processingLoadedTiles = status;
    }
    public boolean getProcessingLoadedTiles() {
        return processingLoadedTiles;
    }
}
