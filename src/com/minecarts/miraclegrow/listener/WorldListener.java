package com.minecarts.miraclegrow.listener;

import com.minecarts.miraclegrow.MiracleGrow;
import org.bukkit.event.world.*;
import org.bukkit.block.Block;

public class WorldListener extends org.bukkit.event.world.WorldListener {
    
    private MiracleGrow plugin;
    
    public WorldListener(MiracleGrow plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onPortalCreate(PortalCreateEvent event) {
        if(event.isCancelled()) return;
        
        for(Block block : event.getBlocks()) {
            plugin.scheduleRestore(block, 30);
        }
    }
}
