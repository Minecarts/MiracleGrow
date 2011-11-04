package com.minecarts.miraclegrow;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;

import org.bukkit.util.config.Configuration;
import org.bukkit.plugin.PluginDescriptionFile;

import com.minecarts.dbquery.DBQuery;
import com.minecarts.dbquery.DBQuery.AsyncQueryHelper;


public class MiracleGrow extends org.bukkit.plugin.java.JavaPlugin {
    private static final Logger logger = Logger.getLogger("com.minecarts.miraclegrow"); 
    
    private PluginDescriptionFile pdf;
    private AsyncQueryHelper db;

    public void onEnable() {
        pdf = getDescription();
        dbq = (DBQuery) getServer().getPluginManager().getPlugin("DBQuery");

        this.db = dbq.getAsyncConnection("minecarts");

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
    
}