package com.minecarts.miraclegrow.listener;

import com.minecarts.miraclegrow.MiracleGrow;
import org.bukkit.event.entity.*;
import org.bukkit.block.Block;

public class EntityListener extends org.bukkit.event.entity.EntityListener {
    
    private MiracleGrow plugin;
    
    public EntityListener(MiracleGrow plugin) {
        this.plugin = plugin;
    }
    
    
    @Override
    public void onEntityExplode(EntityExplodeEvent event) {
        for(Block block : event.blockList()) {
            plugin.scheduleRestore(block, 30);
        }
        plugin.processQueue();
    }
    
}
