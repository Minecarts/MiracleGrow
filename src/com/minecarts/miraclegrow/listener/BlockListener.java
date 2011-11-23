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
        if(event.isCancelled()) return;
        
        plugin.scheduleRestore(event.getBlockReplacedState());
    }
    
    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if(event.isCancelled()) return;
        
        plugin.scheduleRestore(event.getBlock());
    }
    
    @Override
    public void onBlockFade(BlockFadeEvent event) {
        if(event.isCancelled()) return;
        
        plugin.scheduleRestore(event.getBlock(), 30);
    }
    
    @Override
    public void onBlockForm(BlockFormEvent event) {
        if(event.isCancelled()) return;
        
        plugin.scheduleRestore(event.getBlock(), 30);
    }
    
    @Override
    public void onBlockSpread(BlockSpreadEvent event) {
        if(event.isCancelled()) return;
        
        plugin.scheduleRestore(event.getBlock(), 30);
    }
    
    @Override
    public void onBlockFromTo(BlockFromToEvent event) {
        if(event.isCancelled()) return;
        
        plugin.scheduleRestore(event.getToBlock(), 30);
    }
    
    @Override
    public void onLeavesDecay(LeavesDecayEvent event) {
        if(event.isCancelled()) return;
        
        plugin.scheduleRestore(event.getBlock(), 30);
    }
    
    @Override
    public void onBlockIgnite(BlockIgniteEvent event) {
        if(event.isCancelled()) return;
        
        plugin.scheduleRestore(event.getBlock(), 30);
    }
    
    @Override
    public void onBlockBurn(BlockBurnEvent event) {
        if(event.isCancelled()) return;
        
        plugin.scheduleRestore(event.getBlock(), 30);
    }
    
    @Override
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if(event.isCancelled()) return;
        if(!event.isSticky()) return;
        
        // pulled block
        plugin.scheduleRestore(event.getBlock().getRelative(event.getDirection(), 2));
        // pulled block's destination
        plugin.scheduleRestore(event.getBlock().getRelative(event.getDirection(), 1));
    }
    
    @Override
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if(event.isCancelled()) return;
        
        for(int i = 1, length = event.getLength(); i <= length; i++) {
            plugin.scheduleRestore(event.getBlock().getRelative(event.getDirection(), i));
        }
    }
    
}
