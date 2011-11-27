package com.minecarts.miraclegrow.listener;

import com.minecarts.miraclegrow.MiracleGrow;
import com.minecarts.miraclegrow.BlockStateRestore.Cause;
import org.bukkit.event.entity.*;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;

public class EntityListener extends org.bukkit.event.entity.EntityListener {
    
    private MiracleGrow plugin;
    
    public EntityListener(MiracleGrow plugin) {
        this.plugin = plugin;
    }
    
    
    @Override
    public void onEntityExplode(EntityExplodeEvent event) {
        if(event.isCancelled()) return;
        
        final Cause cause = (event.getEntity() instanceof TNTPrimed) ? Cause.PLAYER : Cause.WORLD;
        
        for(Block block : event.blockList()) {
            plugin.scheduleRestore(block, cause);
        }
    }
    
    @Override
    public void onEndermanPickup(EndermanPickupEvent event) {
        plugin.scheduleRestore(event.getBlock(), Cause.WORLD);
    }
    
    @Override
    public void onEndermanPlace(EndermanPlaceEvent event) {
        plugin.scheduleRestore(event.getLocation().getBlock(), Cause.WORLD);
    }
    
}
