package com.minecarts.miraclegrow;

import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;

import org.bukkit.block.Block;
import org.bukkit.Material;

public class BlockRestoreEvent extends Event implements Cancellable {
    protected boolean cancelled = false;
    protected Block block;
    protected int type;
    protected byte data;
    
    public BlockRestoreEvent(Block block, int type, byte data) {
        super("BlockRestoreEvent");
        this.block = block;
        this.type = type;
        this.data = data;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    public Block getBlock() {
        return block;
    }
    public int getNewBlockTypeId() {
        return type;
    }
    public Material getNewBlockType() {
        return Material.getMaterial(type);
    }
    public byte getNewBlockData() {
        return data;
    }
}
