package com.minecarts.miraclegrow.listener;

import com.minecarts.miraclegrow.MiracleGrow;
import org.bukkit.event.block.*;

public class BlockListener extends org.bukkit.event.block.BlockListener {
    
    private MiracleGrow plugin;
    
    public BlockListener(MiracleGrow plugin) {
        this.plugin = plugin;
    }
    
    
    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        plugin.scheduleRestore(event.getBlockReplacedState());
    }
    
    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.scheduleRestore(event.getBlock());
    }
    
}
