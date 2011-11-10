package com.minecarts.miraclegrow;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;

import com.minecarts.dbquery.DBQuery;
import com.minecarts.dbquery.DBQuery.AsyncQueryHelper;

import com.minecarts.miraclegrow.listener.*;
import org.bukkit.event.Listener;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.Location;
import org.bukkit.Material;

import org.bukkit.event.Event.Type;
import org.bukkit.event.Event.Priority;
import static org.bukkit.event.Event.Type.*;

import java.util.HashMap;

public class MiracleGrow extends org.bukkit.plugin.java.JavaPlugin {
    private static final Logger logger = Logger.getLogger("com.minecarts.miraclegrow"); 
    
    private PluginDescriptionFile pdf;
    
    protected AsyncQueryHelper db;
    
    public void onEnable() {
        pdf = getDescription();
        DBQuery dbq = (DBQuery) getServer().getPluginManager().getPlugin("DBQuery");
        
        // TODO: use config for pool name
        db = dbq.getAsyncConnection("minecarts");
        
        
        PluginManager pluginManager = getServer().getPluginManager();
        HashMap<Listener, Type[]> listeners = new HashMap<Listener, Type[]>() {{
            put(new BlockListener(MiracleGrow.this), new Type[]{ BLOCK_PLACE, BLOCK_BREAK });
            put(new EntityListener(MiracleGrow.this), new Type[]{ ENTITY_EXPLODE });
        }};
        
        for(java.util.Map.Entry<Listener, Type[]> entry : listeners.entrySet()) {
            for(Type type : entry.getValue()) {
                pluginManager.registerEvent(type, entry.getKey(), Priority.Monitor, this);
            }
        }
        

        logf("Enabled {0}", pdf.getVersion());
    }
    
    public void onDisable() {
    }
    
    
    public void log(String message) {
        log(Level.INFO, message);
    }
    public void log(Level level, String message) {
        logger.log(level, MessageFormat.format("{0}> {1}", pdf.getName(), message));
    }
    
    public void logf(String message, Object... args) {
        log(MessageFormat.format(message, args));
    }
    public void logf(Level level, String message, Object... args) {
        log(level, MessageFormat.format(message, args));
    }
    
    
    public void scheduleBlockRestore(Block block) {
        scheduleBlockRestore(block.getState());
    }
    public void scheduleBlockRestore(Block block, int seconds) {
        scheduleBlockRestore(block.getState(), seconds);
    }
    public void scheduleBlockRestore(BlockState state) {
        scheduleBlockRestore(state, getBlockRestoreTime(state));
    }
    public void scheduleBlockRestore(BlockState state, int seconds) {
        final Location loc = state.getBlock().getLocation();
        
        try {
            db.affected("INSERT INTO `block_restore_queue` (`world`, `x`, `y`, `z`, `type`, `data`, `when`) VALUES (?, ?, ?, ?, ?, ?, TIMESTAMPADD(SECOND, ?, NOW())) ON DUPLICATE KEY UPDATE `when`=VALUES(`when`)", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), state.getTypeId(), state.getData().getData(), seconds, new Callback() {
                @Override
                public void onComplete(Integer affected) {
                    logf("{0} rows affected", affected);
                }
            });
        }
        catch(Exception e) {
            // shouldn't get here with async
        }
        
        logf("Scheduling restore for block {1} at {0} in {2} seconds", loc, state.getType(), seconds);
    }
    
    class Callback extends com.minecarts.dbquery.Callback {
        
        @Override
        public void onError(Exception x) {
            try {
                throw x;
            }
            catch(java.sql.SQLException e) {
                log("SQLException:");
                e.printStackTrace();
            }
            catch(com.minecarts.dbquery.NoConnectionException e) {
                log("NoConnectionException:");
                e.printStackTrace();
            }
            catch(Exception e) {
                log("Exception:");
                e.printStackTrace();
            }
        }
    }
    
    
    public int getBlockRestoreTime(Block block) {
        return getBlockRestoreTime(block.getState());
    }
    public int getBlockRestoreTime(BlockState state) {
        logf("Getting block restore time for {1} at {0}", state.getBlock().getLocation(), state.getType());
        return 60*60*24;
    }
    
}