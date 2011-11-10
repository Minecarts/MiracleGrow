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

import org.bukkit.event.Event.Type;
import org.bukkit.event.Event.Priority;
import static org.bukkit.event.Event.Type.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;


public class MiracleGrow extends org.bukkit.plugin.java.JavaPlugin {
    private static final Logger logger = Logger.getLogger("com.minecarts.miraclegrow"); 
    
    private PluginDescriptionFile pdf;
    
    protected AsyncQueryHelper db;
    protected HashSet<BlockStateRestore> queue = new HashSet<BlockStateRestore>();
    
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
    
    
    
    class BlockStateRestore {
        public final BlockState state;
        public final int seconds;
        
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
    }
    
    public void scheduleRestore(Block block) {
        scheduleRestore(block.getState());
    }
    public void scheduleRestore(Block block, int seconds) {
        scheduleRestore(block.getState(), seconds);
    }
    public void scheduleRestore(BlockState state) {
        scheduleRestore(state, getBlockRestoreTime(state));
    }
    public void scheduleRestore(BlockState state, int seconds) {
        logf("restoring block state {0} in {1} seconds", state.getType(), seconds);
        
        BlockStateRestore restore = new BlockStateRestore(state, seconds);
        logf("{0}", restore);
        
        if(queue.contains(restore)) {
            log("queue already contains restore, removing old value...");
            queue.remove(restore);
        }
        
        log("restore added to queue");
        queue.add(restore);
        logf("queue size: {0}", queue.size());
    }
    
    
    public void processQueue() {
        if(queue.isEmpty()) return;
        
        StringBuilder query = new StringBuilder("INSERT INTO `block_restore_queue` (`world`, `x`, `y`, `z`, `type`, `data`, `when`) VALUES ");
        ArrayList<Object> params = new ArrayList();
        
        for(BlockStateRestore restore : queue) {
            query.append("(?, ?, ?, ?, ?, ?, TIMESTAMPADD(SECOND, ?, NOW())), ");
            params.add(restore.state.getBlock().getWorld().getName());
            params.add(restore.state.getBlock().getX());
            params.add(restore.state.getBlock().getY());
            params.add(restore.state.getBlock().getZ());
            params.add(restore.state.getTypeId());
            params.add(restore.state.getData().getData());
            params.add(restore.seconds);
        }
        // TODO: requeue in case of query failure
        queue.clear();
        
        query.replace(query.length() - 2, query.length(), " ON DUPLICATE KEY UPDATE `when`=VALUES(`when`)");
        log(query.toString());
        params.add(new Callback() {
            @Override
            public void onComplete(Integer affected) {
                logf("{0} rows affected", affected);
            }
        });
        
        try {
            db.affected(query.toString(), params.toArray());
        }
        catch(Exception e) {
            // shouldn't get here with async
        }
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