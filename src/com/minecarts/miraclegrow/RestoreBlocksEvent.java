package com.minecarts.miraclegrow;

import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.HashMap;
import java.util.Set;
import java.util.Collection;

public class RestoreBlocksEvent extends Event implements Cancellable {
    protected boolean cancelled = false;
    protected HashMap<Block, BlockState> blocks;
    
    public RestoreBlocksEvent(HashMap<Block, BlockState> blocks) {
        super("RestoreBlocksEvent");
        this.blocks = blocks;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
    
    public Set<Block> getBlocks() {
        return blocks.keySet();
    }
    public Collection<BlockState> getBlockChanges() {
        return blocks.values();
    }
}
