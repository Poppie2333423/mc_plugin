package com.pvpsurvival.listeners;

import com.pvpsurvival.game.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.entity.Player;

public class PlayerListener implements Listener {
    
    private final GameManager gameManager;
    
    public PlayerListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (gameManager.isGameActive()) {
            gameManager.handlePlayerDeath(event.getPlayer());
            
            // Benutzerdefinierte Todesnachricht
            event.deathMessage(Component.text(
                event.getPlayer().getName() + " wurde eliminiert!", 
                NamedTextColor.RED
            ));
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (gameManager.isGameActive()) {
            event.getPlayer().sendMessage(Component.text(
                "Eine Arena War Runde läuft gerade. Du kannst zuschauen!", 
                NamedTextColor.YELLOW
            ));
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (gameManager.isGameActive()) {
            gameManager.handlePlayerDeath(event.getPlayer());
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            if (gameManager.isGameActive() && !gameManager.canPvP()) {
                event.setCancelled(true);
                event.getDamager().sendMessage(Component.text(
                    "PvP ist derzeit deaktiviert! Vorbereitungsphase läuft.", 
                    NamedTextColor.YELLOW
                ));
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (gameManager.isGameActive() && !gameManager.canBreakBlocks()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text(
                "Du kannst noch keine Blöcke abbauen! Warte bis das Spiel startet.", 
                NamedTextColor.YELLOW
            ));
        }
    }
    
    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        // 5x schnelleres Leveln - Erfahrung multiplizieren
        if (gameManager.isGameActive()) {
            int originalExp = event.getAmount();
            event.setAmount(originalExp * 5);
        }
    }
}