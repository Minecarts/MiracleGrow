package com.minecarts.miraclegrow;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import static org.bukkit.Material.*;

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
        float seconds = cause.seconds;
        
        switch(state.getType()) {
            
            case MOB_SPAWNER:
                seconds *= 4;
                seconds += 60 * 60 * 24;
                break;
                
            case DIAMOND_ORE:
            case LAPIS_ORE:
            case GOLD_ORE:
            case IRON_ORE:
            case REDSTONE_ORE:
            case GLOWSTONE:
                seconds *= 3;
                seconds += 60 * 60 * 24;
                break;
                
            case COAL_ORE:
            case CLAY:
            case MOSSY_COBBLESTONE:
            case SUGAR_CANE:
            case RED_MUSHROOM:
            case BROWN_MUSHROOM:
            case RED_ROSE:
            case YELLOW_FLOWER:
            case PUMPKIN:
            case JACK_O_LANTERN:
            case MELON:
            case WATER_LILY:
            case NETHER_BRICK:
            case NETHER_FENCE:
            case NETHER_BRICK_STAIRS:
            case NETHER_WARTS:
                seconds *= 2;
                seconds += 60 * 60 * 24;
                break;
                
            case SMOOTH_BRICK:
                // mossy and cracked smooth brick
                if(state.getData().getData() > 0) {
                    seconds *= 2;
                    seconds += 60 * 60 * 24;
                    break;
                }
            
        }
        
        // +5% to -15% based on block depth
        seconds *= (state.getY() / 128) * -0.2 + 0.05;
        
        // +-5% random variability
        seconds *= Math.random() * 0.1 + 0.95;
        
        return (int) seconds;
    }
}