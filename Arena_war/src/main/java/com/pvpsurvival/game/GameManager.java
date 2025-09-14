package com.pvpsurvival.game;

import com.pvpsurvival.PvPSurvivalPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    
    private final ArenaWarPlugin plugin;
    private GameState gameState;
    private final Set<UUID> alivePlayers;
    private final Map<UUID, Integer> playerKills;
    private Location centerLocation;
    private WorldBorder worldBorder;
    private BukkitTask countdownTask;
    private BukkitTask gameTask;
    private BukkitTask enchantingTableTask;
    private Scoreboard gameScoreboard;
    private Objective mainObjective;
    private int pvpCountdown = 300; // 5 Minuten in Sekunden
    private Location enchantingTableLocation;
    
    public GameManager(ArenaWarPlugin plugin) {
        this.plugin = plugin;
        this.gameState = GameState.WAITING;
        this.alivePlayers = new HashSet<>();
        this.playerKills = new HashMap<>();
    }
    
    public boolean startGame() {
        if (gameState != GameState.WAITING) {
            return false;
        }
        
        // Nur Spieler im Survival-Modus für das Spiel berücksichtigen
        List<Player> eligiblePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                eligiblePlayers.add(player);
            }
        }
        
        if (eligiblePlayers.size() < 2 || eligiblePlayers.size() > 8) {
            return false;
        }
        
        gameState = GameState.STARTING;
        prepareGame(eligiblePlayers);
        return true;
    }
    
    private void prepareGame(List<Player> players) {
        // Vorherige Spieldaten löschen
        alivePlayers.clear();
        playerKills.clear();
        pvpCountdown = 300;
        
        // Scoreboard einrichten
        setupScoreboard();
        
        // Zufälligen Mittelpunkt generieren (weit weg von vorherigen Spielen)
        World world = Bukkit.getWorlds().get(0);
        Location newCenter = generateRandomSafeCenter(world);
        centerLocation = newCenter;
        
        // Zeit auf Tag setzen
        world.setTime(1000);
        
        // Weltgrenze einrichten
        setupWorldBorder(world);
        
        // Spieler zu Spawn-Positionen teleportieren
        teleportPlayersToSpawnPositions(players);
        
        // Spielern Startausrüstung und Effekte geben
        for (Player player : players) {
            // Spieler zurücksetzen
            player.setHealth(20.0); // Volle Leben
            player.setFoodLevel(20); // Voller Hunger
            player.setSaturation(20.0f); // Volle Sättigung
            player.setLevel(0); // 0 Level
            player.setExp(0.0f); // 0 Erfahrung
            
            // Alle Achievements zurücksetzen
            resetPlayerAchievements(player);
            
            giveStarterGear(player);
            giveStarterEffects(player);
            alivePlayers.add(player.getUniqueId());
            playerKills.put(player.getUniqueId(), 0);
            player.setGameMode(GameMode.SURVIVAL);
            player.setScoreboard(gameScoreboard);
        }
        
        // Alle anderen Spieler in Zuschauermodus setzen
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!players.contains(player)) {
                player.setGameMode(GameMode.SPECTATOR);
                player.setScoreboard(gameScoreboard);
                player.sendMessage(Component.text(
                    "Du bist Zuschauer für diese Runde!", 
                    NamedTextColor.YELLOW
                ));
            }
        }
        
        // Countdown starten
        startCountdown(players);
    }
    
    private void resetPlayerAchievements(Player player) {
        // Alle Achievements des Spielers zurücksetzen
        Iterator<Advancement> advancements = Bukkit.getServer().advancementIterator();
        while (advancements.hasNext()) {
            Advancement advancement = advancements.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            if (progress != null) {
                for (String criteria : progress.getAwardedCriteria()) {
                    progress.revokeCriteria(criteria);
                }
            }
        }
    }
    
    private Location generateRandomSafeCenter(World world) {
        Location safeLocation = null;
        int attempts = 0;
        int maxAttempts = 50;
        
        while (safeLocation == null && attempts < maxAttempts) {
            // Sehr große Bereiche für echte Zufälligkeit
            int centerX = ThreadLocalRandom.current().nextInt(-10000, 10001);
            int centerZ = ThreadLocalRandom.current().nextInt(-10000, 10001);
            
            // Prüfe, ob der Bereich hauptsächlich über Wasser ist
            if (isAreaSafe(world, centerX, centerZ)) {
                int centerY = world.getHighestBlockYAt(centerX, centerZ);
                safeLocation = new Location(world, centerX, centerY, centerZ);
            }
            attempts++;
        }
        
        // Fallback falls kein sicherer Bereich gefunden wurde
        if (safeLocation == null) {
            int centerX = ThreadLocalRandom.current().nextInt(-5000, 5001);
            int centerZ = ThreadLocalRandom.current().nextInt(-5000, 5001);
            int centerY = world.getHighestBlockYAt(centerX, centerZ);
            safeLocation = new Location(world, centerX, centerY, centerZ);
        }
        
        return safeLocation;
    }
    
    private boolean isAreaSafe(World world, int centerX, int centerZ) {
        int waterBlocks = 0;
        int totalChecked = 0;
        
        // Prüfe einen 200x200 Bereich um den Mittelpunkt
        for (int x = centerX - 100; x <= centerX + 100; x += 20) {
            for (int z = centerZ - 100; z <= centerZ + 100; z += 20) {
                Material blockType = world.getHighestBlockAt(x, z).getType();
                if (blockType == Material.WATER || blockType == Material.LAVA) {
                    waterBlocks++;
                }
                totalChecked++;
            }
        }
        
        // Weniger als 30% Wasser/Lava ist akzeptabel
        return (double) waterBlocks / totalChecked < 0.3;
    }
    
    private void setupScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        gameScoreboard = manager.getNewScoreboard();
        
        mainObjective = gameScoreboard.registerNewObjective("pvpsurvival", Criteria.DUMMY, 
            Component.text("ARENA WAR", NamedTextColor.GOLD, TextDecoration.BOLD));
        mainObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
    
    private void setupWorldBorder(World world) {
        worldBorder = world.getWorldBorder();
        worldBorder.setCenter(centerLocation);
        worldBorder.setSize(200);
        worldBorder.setWarningDistance(10);
        worldBorder.setWarningTime(30);
    }
    
    private void teleportPlayersToSpawnPositions(List<Player> players) {
        List<Location> spawnPositions = generateSpawnPositions();
        Collections.shuffle(spawnPositions);
        
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Location spawnLocation = spawnPositions.get(i);
            player.teleport(spawnLocation);
        }
    }
    
    private List<Location> generateSpawnPositions() {
        List<Location> positions = new ArrayList<>();
        World world = centerLocation.getWorld();
        
        // 4 Ecken (mit 3 Blöcken Abstand zur Grenze)
        positions.add(getSafeLocation(world, centerLocation.getBlockX() - 97, centerLocation.getBlockZ() - 97));
        positions.add(getSafeLocation(world, centerLocation.getBlockX() + 97, centerLocation.getBlockZ() - 97));
        positions.add(getSafeLocation(world, centerLocation.getBlockX() - 97, centerLocation.getBlockZ() + 97));
        positions.add(getSafeLocation(world, centerLocation.getBlockX() + 97, centerLocation.getBlockZ() + 97));
        
        // 4 Mittelseiten (mit 3 Blöcken Abstand zur Grenze)
        positions.add(getSafeLocation(world, centerLocation.getBlockX(), centerLocation.getBlockZ() - 97));
        positions.add(getSafeLocation(world, centerLocation.getBlockX() + 97, centerLocation.getBlockZ()));
        positions.add(getSafeLocation(world, centerLocation.getBlockX(), centerLocation.getBlockZ() + 97));
        positions.add(getSafeLocation(world, centerLocation.getBlockX() - 97, centerLocation.getBlockZ()));
        
        return positions;
    }
    
    private Location getSafeLocation(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z) + 1;
        return new Location(world, x + 0.5, y, z + 0.5);
    }
    
    private void giveStarterGear(Player player) {
        player.getInventory().clear();
        
        // Werkzeuge
        ItemStack pickaxe = createUnbreakableTool(Material.GOLDEN_PICKAXE, Enchantment.EFFICIENCY, 7);
        ItemStack axe = createUnbreakableTool(Material.GOLDEN_AXE, Enchantment.EFFICIENCY, 3);
        ItemStack shovel = createUnbreakableTool(Material.GOLDEN_SHOVEL, Enchantment.EFFICIENCY, 3);
        
        // Rüstung - Lederrüstung statt Kettenrüstung
        ItemStack helmet = createUnbreakableArmor(Material.LEATHER_HELMET);
        ItemStack chestplate = createUnbreakableArmor(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = createUnbreakableArmor(Material.LEATHER_LEGGINGS);
        ItemStack boots = createUnbreakableArmor(Material.LEATHER_BOOTS);
        
        // Essen
        ItemStack steak = new ItemStack(Material.COOKED_BEEF, 64);
        
        // Items setzen
        player.getInventory().addItem(pickaxe, axe, shovel, steak);
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }
    
    private ItemStack createUnbreakableTool(Material material, Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        if (enchantment != null) {
            meta.addEnchant(enchantment, level, true);
        }
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createUnbreakableArmor(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.PROTECTION, 1, true);
        item.setItemMeta(meta);
        return item;
    }
    
    private void giveStarterEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 18000, 1)); // 15 Minuten Speed II
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 18000, 0)); // 15 Minuten Regeneration I
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 12000, 0)); // 10 Minuten Nachtsicht
    }
    
    private void startCountdown(List<Player> players) {
        countdownTask = new BukkitRunnable() {
            int countdown = 15;
            
            @Override
            public void run() {
                if (countdown > 0) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        Title title = Title.title(
                            Component.text(String.valueOf(countdown), NamedTextColor.YELLOW, TextDecoration.BOLD),
                            Component.text("Spiel startet...", NamedTextColor.GRAY),
                            Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(1), Duration.ofMillis(250))
                        );
                        player.showTitle(title);
                        
                        // Countdown Sound
                        if (countdown <= 3) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                        } else {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                    countdown--;
                } else {
                    // Das eigentliche Spiel starten
                    startActualGame(players);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    
    private void startActualGame(List<Player> players) {
        gameState = GameState.PREPARATION;
        
        // Spieler benachrichtigen
        Component message = Component.text("Arena War gestartet! 5 Minuten Vorbereitungszeit (PvP deaktiviert)", NamedTextColor.GREEN);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            Title title = Title.title(
                Component.text("ARENA WAR GESTARTET!", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text("5 Minuten Vorbereitung", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
            );
            player.showTitle(title);
            
            // Spiel Start Sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
        updateScoreboard();
        
        // Spielphasen planen
        scheduleGamePhases(players);
        
        // Enchanting Table nach 10 Minuten spawnen
        scheduleEnchantingTable();
    }
    
    private void scheduleEnchantingTable() {
        enchantingTableTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnEnchantingTable();
            }
        }.runTaskLater(plugin, 12000); // 10 Minuten = 12000 Ticks
    }
    
    private void spawnEnchantingTable() {
        if (centerLocation == null) return;
        
        World world = centerLocation.getWorld();
        int centerX = centerLocation.getBlockX();
        int centerZ = centerLocation.getBlockZ();
        int centerY = world.getHighestBlockYAt(centerX, centerZ);
        
        // Enchanting Table in der Mitte platzieren
        enchantingTableLocation = new Location(world, centerX, centerY, centerZ);
        world.getBlockAt(enchantingTableLocation).setType(Material.ENCHANTING_TABLE);
        
        // Bücherregale um den Enchanting Table platzieren
        placeBookshelvesAround(world, centerX, centerY, centerZ);
        
        // Alle Spieler benachrichtigen
        Component message = Component.text("Ein Zaubertisch ist in der Mitte erschienen!", NamedTextColor.LIGHT_PURPLE);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        }
    }
    
    private void placeBookshelvesAround(World world, int centerX, int centerY, int centerZ) {
        // 5x5 Bereich um den Enchanting Table mit Bücherregalen
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                // Nicht den Mittelpunkt (Enchanting Table) überschreiben
                if (x == centerX && z == centerZ) continue;
                
                // Nur die äußeren Blöcke als Bücherregale setzen
                if (x == centerX - 2 || x == centerX + 2 || z == centerZ - 2 || z == centerZ + 2) {
                    Location bookshelfLoc = new Location(world, x, centerY, z);
                    world.getBlockAt(bookshelfLoc).setType(Material.BOOKSHELF);
                }
            }
        }
    }
    
    private void scheduleGamePhases(List<Player> players) {
        gameTask = new BukkitRunnable() {
            int timeElapsed = 0;
            
            @Override
            public void run() {
                timeElapsed++;
                pvpCountdown--;
                
                if (timeElapsed == 300) { // 5 Minuten - PvP aktivieren
                    gameState = GameState.PVP_ENABLED;
                    Component message = Component.text("PvP ist jetzt aktiviert! Kämpfe ums Überleben!", NamedTextColor.RED);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message);
                        Title title = Title.title(
                            Component.text("PVP AKTIVIERT!", NamedTextColor.RED, TextDecoration.BOLD),
                            Component.text("Kämpfe ums Überleben!", NamedTextColor.DARK_RED),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
                        );
                        player.showTitle(title);
                        
                        // PvP Aktiviert Sound
                        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.0f);
                    }
                }
                
                if (timeElapsed == 390) { // 6:30 Minuten - Grenze schrumpfen
                    Component message = Component.text("Die Boarder verkleinert sich jetzt!", NamedTextColor.DARK_RED);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message);
                        
                        // Border Shrink Sound
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.8f);
                    }
                    worldBorder.setSize(5, 1320); // Auf 5x5 über 22 Minuten schrumpfen (1320 Sekunden)
                }
                
                // Gewinnbedingung jede Sekunde prüfen
                checkWinCondition();
                
                // Scoreboard alle 2 Sekunden aktualisieren
                if (timeElapsed % 2 == 0) {
                    updateScoreboard();
                }
            }
        }.runTaskTimer(plugin, 20, 20); // Jede Sekunde ausführen
    }
    
    public void handlePlayerDeath(Player player) {
        if (!isGameActive() || !alivePlayers.contains(player.getUniqueId())) {
            return;
        }
        
        alivePlayers.remove(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        
        // Death Sound für alle Spieler
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 0.5f);
        }
        
        // Nach Killer suchen
        Player killer = player.getKiller();
        if (killer != null && alivePlayers.contains(killer.getUniqueId())) {
            int kills = playerKills.get(killer.getUniqueId()) + 1;
            playerKills.put(killer.getUniqueId(), kills);
            
            Component message = Component.text()
                .append(Component.text(killer.getName(), NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" hat ", NamedTextColor.RED))
                .append(Component.text(player.getName(), NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" getötet!", NamedTextColor.RED))
                .build();
            Bukkit.broadcast(message);
            
            // Kill Sound für den Killer
            killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        } else {
            Component message = Component.text()
                .append(Component.text(player.getName(), NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" wurde eliminiert!", NamedTextColor.RED))
                .build();
            Bukkit.broadcast(message);
        }
        
        updateScoreboard();
        checkWinCondition();
    }
    
    private void checkWinCondition() {
        if (alivePlayers.size() <= 1) {
            endGame();
        }
    }
    
    private void endGame() {
        gameState = GameState.WAITING;
        
        if (gameTask != null) {
            gameTask.cancel();
        }
        
        if (enchantingTableTask != null) {
            enchantingTableTask.cancel();
        }
        
        // Enchanting Table und Bücherregale entfernen
        if (enchantingTableLocation != null) {
            World world = enchantingTableLocation.getWorld();
            int centerX = enchantingTableLocation.getBlockX();
            int centerZ = enchantingTableLocation.getBlockZ();
            int centerY = enchantingTableLocation.getBlockY();
            
            // Enchanting Table entfernen
            world.getBlockAt(enchantingTableLocation).setType(Material.AIR);
            
            // Bücherregale entfernen
            for (int x = centerX - 2; x <= centerX + 2; x++) {
                for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                    if (x == centerX && z == centerZ) continue;
                    if (x == centerX - 2 || x == centerX + 2 || z == centerZ - 2 || z == centerZ + 2) {
                        Location bookshelfLoc = new Location(world, x, centerY, z);
                        world.getBlockAt(bookshelfLoc).setType(Material.AIR);
                    }
                }
            }
        }
        
        // Gewinner verkünden
        if (alivePlayers.size() == 1) {
            UUID winnerId = alivePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                Component winMessage = Component.text()
                    .append(Component.text(winner.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" gewinnt Arena War!", NamedTextColor.GOLD))
                    .build();
                Bukkit.broadcast(winMessage);
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Title title = Title.title(
                        Component.text("ARENA WAR BEENDET!", NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.text(winner.getName() + " gewinnt!", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
                    );
                    player.showTitle(title);
                    
                    // Victory Sound
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }
        } else {
            Component drawMessage = Component.text("Arena War endete unentschieden!", NamedTextColor.YELLOW);
            Bukkit.broadcast(drawMessage);
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
        
        // Alle Spieler zurücksetzen
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.clearActivePotionEffects();
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        
        // Weltgrenze zurücksetzen
        if (worldBorder != null) {
            worldBorder.reset();
        }
        
        alivePlayers.clear();
        playerKills.clear();
    }
    
    private void updateScoreboard() {
        if (mainObjective == null) return;
        
        // Alte Scores löschen
        for (String entry : mainObjective.getScoreboard().getEntries()) {
            mainObjective.getScoreboard().resetScores(entry);
        }
        
        int score = 15;
        
        // Leerzeile
        mainObjective.getScore(" ").setScore(score--);
        
        // Am Leben Anzeige
        Score aliveScore = mainObjective.getScore("§fAm Leben: §a" + alivePlayers.size());
        aliveScore.setScore(score--);
        
        // Leerzeile
        mainObjective.getScore("  ").setScore(score--);
        
        // PvP Timer (nur während Vorbereitung)
        if (gameState == GameState.PREPARATION && pvpCountdown > 0) {
            int minutes = pvpCountdown / 60;
            int seconds = pvpCountdown % 60;
            String timeString = String.format("%d:%02d", minutes, seconds);
            Score timerScore = mainObjective.getScore("§fPvP in: §c" + timeString);
            timerScore.setScore(score--);
            
            // Leerzeile
            mainObjective.getScore("   ").setScore(score--);
        }
        
        // Spieler Kills
        List<Map.Entry<UUID, Integer>> sortedKills = new ArrayList<>(playerKills.entrySet());
        sortedKills.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        for (Map.Entry<UUID, Integer> entry : sortedKills) {
            UUID playerId = entry.getKey();
            if (alivePlayers.contains(playerId)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    int kills = entry.getValue();
                    Score playerScore = mainObjective.getScore("§f" + player.getName() + ": §a" + kills);
                    playerScore.setScore(score--);
                }
            }
        }
    }
    
    public boolean canPvP() {
        return gameState == GameState.PVP_ENABLED;
    }
    
    public boolean canBreakBlocks() {
        return gameState == GameState.PREPARATION || gameState == GameState.PVP_ENABLED;
    }
    
    public boolean isGameActive() {
        return gameState != GameState.WAITING;
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    public void cleanup() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        if (gameTask != null) {
            gameTask.cancel();
        }
        if (enchantingTableTask != null) {
            enchantingTableTask.cancel();
        }
        endGame();
    }
}