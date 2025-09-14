package com.pvpsurvival.commands;

import com.pvpsurvival.game.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCommand implements CommandExecutor {
    
    private final GameManager gameManager;
    
    public StartCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arenawar.start")) {
            sender.sendMessage(Component.text("Du hast keine Berechtigung, Spiele zu starten!", NamedTextColor.RED));
            return true;
        }
        
        if (gameManager.isGameActive()) {
            sender.sendMessage(Component.text("Ein Spiel läuft bereits!", NamedTextColor.RED));
            return true;
        }
        
        int playerCount = Bukkit.getOnlinePlayers().size();
        if (playerCount < 2) {
            sender.sendMessage(Component.text("Es werden mindestens 2 Spieler benötigt, um ein Spiel zu starten!", NamedTextColor.RED));
            return true;
        }
        
        if (playerCount > 8) {
            sender.sendMessage(Component.text("Zu viele Spieler online! Maximal 8 Spieler erlaubt.", NamedTextColor.RED));
            return true;
        }
        
        if (gameManager.startGame()) {
            Component message = Component.text("Starte Arena War mit " + playerCount + " Spielern!", NamedTextColor.GREEN);
            Bukkit.broadcast(message);
        } else {
            sender.sendMessage(Component.text("Fehler beim Starten des Spiels. Bitte versuche es erneut.", NamedTextColor.RED));
        }
        
        return true;
    }
}