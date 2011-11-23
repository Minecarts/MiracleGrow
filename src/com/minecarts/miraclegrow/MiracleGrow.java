package com.minecarts.miraclegrow;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;

import com.minecarts.dbquery.DBQuery;

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
    
    protected DBQuery dbq;
    protected HashSet<BlockStateRestore> queue = new HashSet<BlockStateRestore>();
    
    public void onEnable() {
        pdf = getDescription();
        dbq = (DBQuery) getServer().getPluginManager().getPlugin("DBQuery");
        
        PluginManager pluginManager = getServer().getPluginManager();
        HashMap<Listener, Type[]> listeners = new HashMap<Listener, Type[]>() {{
            put(new BlockListener(MiracleGrow.this), new Type[]{ BLOCK_PLACE, BLOCK_BREAK });
            put(new EntityListener(MiracleGrow.this), new Type[]{ ENTITY_EXPLODE });
            put(new ServerListener(MiracleGrow.this), new Type[]{ PLUGIN_DISABLE });
        }};
        
        for(java.util.Map.Entry<Listener, Type[]> entry : listeners.entrySet()) {
            for(Type type : entry.getValue()) {
                pluginManager.registerEvent(type, entry.getKey(), Priority.Monitor, this);
            }
        }
        
        
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                processQueue();
            }
        }, 20*30, 20*30); // TODO: configurable flush interval
        

        logf("Enabled {0}", pdf.getVersion());
    }
    
    public void onDisable() {
        processQueue(false);
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
    
    
    
    public class BlockStateRestore {
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
        BlockStateRestore restore = new BlockStateRestore(state, seconds);
        
        if(queue.contains(restore)) queue.remove(restore);
        queue.add(restore);
    }
    
    
    public void processQueue() {
        processQueue(true);
    }
    public void processQueue(boolean async) {
        if(queue.isEmpty()) return;
        
        StringBuilder sql = new StringBuilder("INSERT INTO `block_restore_queue` (`world`, `x`, `y`, `z`, `type`, `data`, `when`) VALUES ");
        ArrayList<Object> params = new ArrayList();
        
        for(BlockStateRestore restore : queue) {
            sql.append("(?, ?, ?, ?, ?, ?, TIMESTAMPADD(SECOND, ?, NOW())), ");
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
        
        sql.replace(sql.length() - 2, sql.length(), " ON DUPLICATE KEY UPDATE `when`=VALUES(`when`)");
        
        
        new Query(sql.toString(), async) {
            private int tries = 0;
            
            @Override
            public void onAffected(Integer affected) {
                logf("{0} rows affected", affected);
            }
            
            @Override
            public void onException(Exception x, FinalQuery query) {
                try {
                    throw x;
                }
                catch(java.sql.SQLException e) {
                    if(++tries < 5) {
                        logf("SQLException on Query, retrying...");
                        e.printStackTrace();
                        query.run();
                    }
                    else {
                        logf("FAILED! SQLException on Query: {0}", query);
                        e.printStackTrace();
                    }
                }
                catch(com.minecarts.dbquery.NoConnectionException e) {
                    if(++tries < 5) {
                        logf("NoConnectionException on Query, retrying...");
                        e.printStackTrace();
                        query.run();
                    }
                    else {
                        logf("FAILED! NoConnectionException on Query: {0}", query);
                        e.printStackTrace();
                    }
                }
                catch(Exception e) {
                    logf("FAILED! Exception on Query: {0}", query);
                    e.printStackTrace();
                }
            }
        }.affected(params.toArray());
    }
    
    
    
    class Query extends com.minecarts.dbquery.Query {
        public Query(String sql, boolean async) {
            this(sql);
            this.async = async;
        }
        public Query(String sql) {
            // TODO: configurable provider name
            super(MiracleGrow.this, ((DBQuery) getServer().getPluginManager().getPlugin("DBQuery")).getProvider("minecarts"), sql);
        }
        
        @Override
        public void onException(Exception x, FinalQuery query) {
            try {
                throw x;
            }
            catch(java.sql.SQLException e) {
                logf("SQLException on Query: {0}", query);
                e.printStackTrace();
            }
            catch(com.minecarts.dbquery.NoConnectionException e) {
                logf("NoConnectionException on Query: {0}", query);
                e.printStackTrace();
            }
            catch(Exception e) {
                logf("Exception on Query: {0}", query);
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