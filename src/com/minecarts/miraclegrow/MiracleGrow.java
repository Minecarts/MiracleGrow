package com.minecarts.miraclegrow;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;

import org.bukkit.plugin.PluginManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import com.minecarts.dbquery.DBQuery;
import com.minecarts.dbconnector.providers.Provider;

import com.minecarts.miraclegrow.BlockStateRestore.Cause;
import com.minecarts.miraclegrow.listener.*;
import org.bukkit.event.Listener;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import org.bukkit.event.Event.Type;
import org.bukkit.event.Event.Priority;
import static org.bukkit.event.Event.Type.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;


public class MiracleGrow extends org.bukkit.plugin.java.JavaPlugin {
    private static final Logger logger = Logger.getLogger("com.minecarts.miraclegrow"); 
    
    protected boolean debug;
    protected FileConfiguration config;
    protected ConfigurationSection worlds;
    
    protected DBQuery dbq;
    protected Provider provider;
    
    protected int flushInterval;
    protected int restoreInterval;
    
    protected HashMap<World, HashSet<BlockStateRestore>> queue = new HashMap<World, HashSet<BlockStateRestore>>();
    protected ArrayList<World> flushing = new ArrayList<World>();
    protected ArrayList<World> restoring = new ArrayList<World>();
    
    
    public void onEnable() {
        dbq = (DBQuery) getServer().getPluginManager().getPlugin("DBQuery");
        reloadConfig();
        
        PluginManager pluginManager = getServer().getPluginManager();
        HashMap<Listener, Type[]> listeners = new HashMap<Listener, Type[]>() {{
            put(new ServerListener(MiracleGrow.this), new Type[]{ PLUGIN_DISABLE });
            put(new WorldListener(MiracleGrow.this), new Type[]{ PORTAL_CREATE });
            put(new BlockListener(MiracleGrow.this), new Type[]{ BLOCK_PLACE, BLOCK_BREAK, BLOCK_FADE, BLOCK_FORM, BLOCK_SPREAD, BLOCK_FROMTO, LEAVES_DECAY, BLOCK_IGNITE, BLOCK_BURN, BLOCK_PISTON_EXTEND, BLOCK_PISTON_RETRACT });
            put(new EntityListener(MiracleGrow.this), new Type[]{ ENTITY_EXPLODE, ENDERMAN_PICKUP, ENDERMAN_PLACE });
        }};
        
        for(Entry<Listener, Type[]> entry : listeners.entrySet()) {
            for(Type type : entry.getValue()) {
                pluginManager.registerEvent(type, entry.getKey(), Priority.Monitor, this);
            }
        }
        
        
        
        /*
            CREATE TABLE IF NOT EXISTS `MiracleGrow_world_blocks` (
              `x` smallint(6) NOT NULL DEFAULT '0',
              `y` tinyint(4) NOT NULL DEFAULT '0',
              `z` smallint(6) NOT NULL DEFAULT '0',
              `type` smallint(6) DEFAULT NULL,
              `data` tinyint(4) DEFAULT NULL,
              PRIMARY KEY (`x`,`y`,`z`)
            ) ENGINE=MyISAM DEFAULT CHARSET=utf8;
            
            CREATE TABLE IF NOT EXISTS `MiracleGrow_world_jobs` (
              `x` smallint(6) NOT NULL DEFAULT '0',
              `y` tinyint(4) NOT NULL DEFAULT '0',
              `z` smallint(6) NOT NULL DEFAULT '0',
              `when` timestamp NULL DEFAULT NULL,
              `job` int(11) DEFAULT NULL,
              PRIMARY KEY (`x`,`y`,`z`),
              INDEX `when` (`when`),
              INDEX `job` (`job`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
         */
        
        
        
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                debug("Flushing block restore queue");
                flushQueue();
                
                // reschedule task with potentially changed flush interval
                getServer().getScheduler().scheduleSyncDelayedTask(MiracleGrow.this, this, flushInterval);
            }
        }, flushInterval);
        
        
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                if(!restoring.isEmpty() || !flushing.isEmpty()) {
                    debug("Flush or restore in progress, rescheduling restore task for 5 seconds from now");
                    getServer().getScheduler().scheduleSyncDelayedTask(MiracleGrow.this, this, 20 * 5);
                    return;
                }
                
                debug("Restoring blocks");
                restoreBlocks();
                
                // reschedule task with potentially changed restore interval
                getServer().getScheduler().scheduleSyncDelayedTask(MiracleGrow.this, this, restoreInterval);
            }
        }, restoreInterval);
        
        

        log("Version {0} enabled.", getDescription().getVersion());
    }
    
    public void onDisable() {
        flushQueue(false);
    }
    
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        
        if(config == null) config = getConfig();
        
        debug = config.getBoolean("debug");
        worlds = config.getConfigurationSection("worlds");
        provider = dbq.getProvider(config.getString("DBQuery.provider"));
        
        flushInterval = Math.max(20, 20 * config.getInt("flushInterval"));
        debug("Flushing block restore queue to database at a {0} tick interval", flushInterval);
        
        restoreInterval = Math.max(20, 20 * config.getInt("restoreInterval"));
        debug("Restoring blocks from database at a {0} tick interval", restoreInterval);
    }
    
    
    public void log(String message) {
        log(Level.INFO, message);
    }
    public void log(Level level, String message) {
        logger.log(level, MessageFormat.format("{0}> {1}", getDescription().getName(), message));
    }
    public void log(String message, Object... args) {
        log(MessageFormat.format(message, args));
    }
    public void log(Level level, String message, Object... args) {
        log(level, MessageFormat.format(message, args));
    }
    
    public void debug(String message) {
        if(debug) log(message);
    }
    public void debug(String message, Object... args) {
        if(debug) log(message, args);
    }
    
    
    
    
    
    public void scheduleRestore(Block block) {
        scheduleRestore(block.getState());
    }
    public void scheduleRestore(Block block, Cause cause) {
        scheduleRestore(block.getState(), cause);
    }
    public void scheduleRestore(Block block, int seconds) {
        scheduleRestore(block.getState(), seconds);
    }
    public void scheduleRestore(BlockState state) {
        scheduleRestore(state, BlockStateRestore.getBlockRestoreTime(state));
    }
    public void scheduleRestore(BlockState state, Cause cause) {
        scheduleRestore(state, BlockStateRestore.getBlockRestoreTime(state, cause));
    }
    public void scheduleRestore(BlockState state, int seconds) {
        BlockStateRestore restore = new BlockStateRestore(state, seconds);
        
        HashSet<BlockStateRestore> set = queue.get(state.getWorld());
        if(set == null) {
            set = new HashSet<BlockStateRestore>();
            queue.put(state.getWorld(), set);
        }
        
        if(set.contains(restore)) set.remove(restore);
        set.add(restore);
    }
    
    
    private void flushQueue() {
        flushQueue(true);
    }
    private void flushQueue(boolean async) {
        if(worlds == null) {
            debug("No table names found in worlds section of configuration file");
            return;
        }
        
        for(Entry<World, HashSet<BlockStateRestore>> entry : queue.entrySet()) {
            
            final World world = entry.getKey();
            final String table = worlds.getString(world.getName(), null);
            HashSet<BlockStateRestore> set = entry.getValue();
            
            if(restoring.contains(world)) {
                debug("Restore in progress for world {0}...", world.getName());
                continue;
            }
            
            if(flushing.contains(world)) {
                debug("Flush already in progress for world {0}...", world.getName());
                continue;
            }
            
            if(table == null) {
                debug("No table name found for worlds.{0}, clearing world's block queue", world.getName());
                set.clear();
                continue;
            }
            
            if(set.isEmpty()) {
                debug("No blocks in queue for world {0}, skipping", world.getName());
                continue;
            }
            
            debug("Flushing block restore queue for world {0}", world.getName());
            flushing.add(world);
            
            
            StringBuilder sql = new StringBuilder("INSERT INTO `").append(table).append("` (`x`, `y`, `z`, `type`, `data`, `when`) VALUES ");
            ArrayList<Object> params = new ArrayList();

            for(BlockStateRestore restore : set) {
                sql.append("(?, ?, ?, ?, ?, TIMESTAMPADD(SECOND, ?, NOW())), ");
                params.add(restore.state.getBlock().getX());
                params.add(restore.state.getBlock().getY());
                params.add(restore.state.getBlock().getZ());
                params.add(restore.state.getTypeId());
                params.add(restore.state.getData().getData());
                params.add(restore.seconds);
            }
            set.clear();

            sql.replace(sql.length() - 2, sql.length(), " ON DUPLICATE KEY UPDATE `when`=VALUES(`when`)");


            new Query(sql.toString(), async) {
                private int tries = 0;

                @Override
                public void onAffected(Integer affected) {
                    debug("{0} rows affected", affected);
                    flushing.remove(world);
                }

                @Override
                public void onException(Exception x, FinalQuery query) {
                    try {
                        throw x;
                    }
                    catch(java.sql.SQLException e) {
                        if(++tries < 5) {
                            log("SQLException on Query, retrying...");
                            e.printStackTrace();
                            query.run();
                        }
                        else {
                            log("FAILED! SQLException on Query: {0}", query);
                            e.printStackTrace();
                            flushing.remove(world);
                        }
                    }
                    catch(com.minecarts.dbquery.NoConnectionException e) {
                        if(++tries < 5) {
                            log("NoConnectionException on Query, retrying...");
                            e.printStackTrace();
                            query.run();
                        }
                        else {
                            log("FAILED! NoConnectionException on Query: {0}", query);
                            e.printStackTrace();
                            flushing.remove(world);
                        }
                    }
                    catch(Exception e) {
                        log("FAILED! Exception on Query: {0}", query);
                        e.printStackTrace();
                        flushing.remove(world);
                    }
                }
            }.affected(params.toArray());
            
        }
    }
    
    
    private void restoreBlocks() {
        if(!restoring.isEmpty() || !flushing.isEmpty()) {
            debug("Flush or restore in progress...");
            return;
        }
        
        if(worlds == null) {
            debug("No table names found in worlds section of configuration file");
            return;
        }
        
        for(final World world : getServer().getWorlds()) {
            
            final String table = worlds.getString(world.getName(), null);
            if(table == null) {
                debug("No table name found for worlds.{0}, clearing world's block queue", world.getName());
                continue;
            }
            
            debug("Restoring blocks for world {0}", world);
            restoring.add(world);
            
            
            final int restoreId = (int) (System.currentTimeMillis() / 1000L);
            StringBuilder sql = new StringBuilder("UPDATE `").append(table).append("` SET `restore_id`=? WHERE `when` <= NOW() ORDER BY `restore_id`, `when` LIMIT 1000");
            
            new Query(sql.toString()) {
                @Override
                public void onAffected(Integer affected) {
                    if(affected > 0) {
                        debug("Updated {0} rows for restore job #{1,number,#}", affected, restoreId);
                    }
                    else {
                        debug("No rows updated for restore job #{0,number,#}", restoreId);
                        restoring.remove(world);
                        return;
                    }
                    
                    StringBuilder sql = new StringBuilder("SELECT `x`, `y`, `z`, `type`, `data` FROM `").append(table).append("` WHERE `restore_id`=? ORDER BY `y`, `x`, `z`");
                    new Query(sql.toString()) {
                        private int tries = 0;

                        @Override
                        public void onFetch(ArrayList<HashMap> rows) {
                            if(rows.size() > 0) {
                                debug("Got {0} rows for restore job #{1,number,#}", rows.size(), restoreId);
                            }
                            else {
                                debug("No rows found for restore job #{0,number,#}", restoreId);
                                restoring.remove(world);
                                return;
                            }
                            
                            
                            // check chunks for player entities
                            // TODO: skip jobs with unloaded chunks?
                            for(HashMap<String, Integer> row : rows) {
                                int x = row.get("x").intValue() >> 4;
                                int z = row.get("z").intValue() >> 4;
                                
                                if(!world.isChunkLoaded(x, z)) continue;
                                
                                for(Entity entity : world.getChunkAt(x, z).getEntities()) {
                                    if(entity instanceof Player) {
                                        debug("Player entity found in chunk [{0} {1}], skipping restore job #{2,number,#}", x, z, restoreId);
                                        restoring.remove(world);
                                        return;
                                    }
                                }
                            }
                            
                            
                            int successes = 0;
                            int failures = 0;
                            
                            for(HashMap<String, Integer> row : rows) {
                                int x = row.get("x").intValue();
                                int y = row.get("y").intValue();
                                int z = row.get("z").intValue();
                                int type = row.get("type").intValue();
                                byte data = row.get("data").byteValue();
                                
                                Block block = world.getBlockAt(x, y, z);
                                if(type == block.getTypeId() && data == block.getData()) {
                                    // block is correct, no restore necessary
                                    continue;
                                }
                                
                                if(block.setTypeIdAndData(type, data, false)) {
                                    successes++;
                                }
                                else {
                                    // TODO: retry or re-add to block restore queue?
                                    debug("Failed to restore block at [{0} {1} {2}] to {3}:{4}", x, y, z, type, data);
                                    failures++;
                                }
                            }
                            
                            debug("{0}/{1} blocks didn''t need restoring", rows.size() - successes - failures, rows.size());
                            debug("{0}/{1} blocks successfully restored", successes, rows.size());
                            if(failures > 0) debug("{0}/{1} blocks failed", failures, rows.size());
                            
                            
                            
                            StringBuilder sql = new StringBuilder("DELETE FROM `").append(table).append("` WHERE `restore_id`=?");
                            
                            new Query(sql.toString()) {
                                @Override
                                public void onAffected(Integer affected) {
                                    debug("Deleted {0} rows from table {1} for block restore job #{2,number,#}", affected, table, restoreId);
                                    restoring.remove(world);
                                }
                                @Override
                                public void onException(Exception e, FinalQuery query) {
                                    e.printStackTrace();
                                    restoring.remove(world);
                                }
                            }.affected(restoreId);
                        }

                        @Override
                        public void onException(Exception x, FinalQuery query) {
                            try {
                                throw x;
                            }
                            catch(java.sql.SQLException e) {
                                if(++tries < 5) {
                                    log("SQLException on Query, retrying...");
                                    e.printStackTrace();
                                    query.run();
                                }
                                else {
                                    log("FAILED! SQLException on Query: {0}", query);
                                    e.printStackTrace();
                                    restoring.remove(world);
                                }
                            }
                            catch(com.minecarts.dbquery.NoConnectionException e) {
                                if(++tries < 5) {
                                    log("NoConnectionException on Query, retrying...");
                                    e.printStackTrace();
                                    query.run();
                                }
                                else {
                                    log("FAILED! NoConnectionException on Query: {0}", query);
                                    e.printStackTrace();
                                    restoring.remove(world);
                                }
                            }
                            catch(Exception e) {
                                log("FAILED! Exception on Query: {0}", query);
                                e.printStackTrace();
                                restoring.remove(world);
                            }
                        }
                    }.fetch(restoreId);
                }
                
                @Override
                public void onException(Exception e, FinalQuery query) {
                    e.printStackTrace();
                    restoring.remove(world);
                }
            }.affected(restoreId);
            
        }
            
            
    }
    
    
    
    class Query extends com.minecarts.dbquery.Query {
        public Query(String sql, boolean async) {
            this(sql);
            this.async = async;
        }
        public Query(String sql) {
            // TODO: configurable provider name
            super(MiracleGrow.this, MiracleGrow.this.provider, sql);
        }
        
        @Override
        public void onException(Exception x, FinalQuery query) {
            try {
                throw x;
            }
            catch(java.sql.SQLException e) {
                log("SQLException on Query: {0}", query);
                e.printStackTrace();
            }
            catch(com.minecarts.dbquery.NoConnectionException e) {
                log("NoConnectionException on Query: {0}", query);
                e.printStackTrace();
            }
            catch(Exception e) {
                log("Exception on Query: {0}", query);
                e.printStackTrace();
            }
        }
    }
    
}