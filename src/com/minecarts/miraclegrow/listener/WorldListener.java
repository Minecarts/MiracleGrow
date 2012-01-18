package com.minecarts.miraclegrow.listener;

import com.minecarts.miraclegrow.MiracleGrow;
import com.minecarts.miraclegrow.BlockStateRestore.Cause;
import org.bukkit.event.world.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class WorldListener extends org.bukkit.event.world.WorldListener {
    
    private MiracleGrow plugin;
    
    public WorldListener(MiracleGrow plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onPortalCreate(PortalCreateEvent event) {
        if(event.isCancelled()) return;
        
        for(Block block : event.getBlocks()) {
            plugin.scheduleRestore(block, Cause.PLAYER);
        }
    }
    
    @Override
    public void onStructureGrow(StructureGrowEvent event) {
        if(event.isCancelled()) return;
        
        for(BlockState state : event.getBlocks()) {
            plugin.scheduleRestore(state.getBlock(), Cause.PLAYER);
        }
    }
}
