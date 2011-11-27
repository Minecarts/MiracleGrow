package com.minecarts.miraclegrow;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class BlockStateRestore {
    public final BlockState state;
    public final int seconds;
    
    public enum Cause {
        PLAYER(60 * 60 * 24 * 7), // seven days
        WORLD(60 * 60); // one hour
        
        public final int seconds;
        private Cause(int seconds) {
            this.seconds = seconds;
        }
    }
    
    
    public BlockStateRestore(Block block, int seconds) {
        this(block.getState(), seconds);
    }
    public BlockStateRestore(BlockState state, int seconds) {
        this.state = state;
        this.seconds = seconds;
    }
    
    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof BlockStateRestore)) return false;

        return state.getBlock().equals(((BlockStateRestore) o).state.getBlock());
    }
    @Override
    public int hashCode() {
        return state.getBlock().hashCode();
    }
    
    
    public static int getBlockRestoreTime(Block block) {
        return getBlockRestoreTime(block.getState());
    }
    public static int getBlockRestoreTime(BlockState state) {
        return getBlockRestoreTime(state, Cause.PLAYER);
    }
    public static int getBlockRestoreTime(BlockState state, Cause cause) {
        return cause.seconds;
    }
}