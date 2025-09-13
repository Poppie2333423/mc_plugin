# PvP Survival Plugin

Ein benutzerdefiniertes Minecraft Paper Plugin für Version 1.21.6, das einen intensiven Multiplayer-PvP-Überlebensmodus einführt.

## Features

### Spielmodus-Übersicht
- Maximal 8 Spieler pro Runde
- Zufällige Weltgenerierung mit strategischer Positionierung
- 200x200 Anfangsgrenze, die über die Zeit auf 5x5 schrumpft
- Strategische Spawn-Positionierung (Ecken und Mittelseiten)

### Spieler-Setup
- **Startausrüstung**: Unzerstörbare goldene Werkzeuge, Kettenrüstung mit Schutz I, 64 Steaks
- **Starteffekte**: Geschwindigkeit I und Regeneration I für 10 Minuten
- **Spielphasen**: 5 Minuten Vorbereitung (kein PvP), dann vollständiger Kampf aktiviert

### Spielmechaniken
- **Grenzschrumpfung**: Beginnt nach 3 Minuten, schrumpft über 17 Minuten
- **Zuschauermodus**: Tote Spieler wechseln automatisch in den Zuschauermodus
- **Echtzeit-Scoreboard**: Zeigt lebende Spieler und Kill-Anzahl
- **Chat-Benachrichtigungen**: Wichtige Spiel-Updates und Kill-Ankündigungen

## Befehle

- `/start` - Starte eine neue PvP-Überlebensrunde mit allen Online-Spielern
  - Berechtigung: `pvpsurvival.start` (Standard: op)

## Installation

1. Lade die kompilierte Plugin-JAR-Datei herunter
2. Platziere sie im `plugins`-Ordner deines Servers
3. Starte deinen Paper-Server neu (1.21.6)
4. Verwende `/start`, um dein erstes Spiel zu beginnen!

## Spielablauf

1. **Vorbereitung** (10 Sekunden): Countdown-Anzeige, Spieler-Setup
2. **Vorbereitungsphase** (5 Minuten): PvP deaktiviert, ausrüsten und strategisieren
3. **Kampfphase** (Variabel): PvP aktiviert, kämpfe ums Überleben
4. **Grenzschrumpfung** (17 Minuten): Bereich wird kleiner, erzwingt Konfrontation
5. **Sieg**: Letzter überlebender Spieler gewinnt!

## Technische Details

- Entwickelt für Minecraft Paper 1.21.6
- Verwendet moderne Paper-API-Features
- Saubere, modulare Code-Architektur
- Umfassende Event-Behandlung
- Professionelle Plugin-Struktur

## Berechtigungen

- `pvpsurvival.start` - Erlaubt das Starten von Spielen (Standard: nur op)

Viel Spaß bei deinen benutzerdefinierten PvP-Überlebenskämpfen!