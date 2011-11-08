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
    
    
    public void scheduleRestore(Block block, int seconds) {
        logf("Scheduling restore for block {0} in {1} seconds", block, seconds);
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
    
}