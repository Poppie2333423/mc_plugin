package com.pvpsurvival;

import com.pvpsurvival.commands.StartCommand;
import com.pvpsurvival.game.GameManager;
import com.pvpsurvival.listeners.CraftingListener;
import com.pvpsurvival.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ArenaWarPlugin extends JavaPlugin {
    
    private GameManager gameManager;
    
    @Override
    public void onEnable() {
        getLogger().info("Arena War Plugin enabled!");
        
        // Initialize game manager
        gameManager = new GameManager(this);
        
        // Register commands
        getCommand("start").setExecutor(new StartCommand(gameManager));
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(), this);
        
        getLogger().info("Arena War Plugin loaded successfully!");
    }
    
    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.cleanup();
        }
        getLogger().info("Arena War Plugin disabled!");
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
}