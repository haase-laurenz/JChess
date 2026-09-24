package com.jchess.tournament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Konfigurationseinstellungen für ein Schach-Turnier.
 */
public class TournamentConfig {

    private int gamesPerPairing = 10; // Immer gerade Zahl (z.B. 2, 4, 10) für ausgeglichene Weiß/Schwarz-Verteilung
    private boolean timeControlEnabled = true;
    private long initialTimeMs = 5_000; // 5 Sekunden Startzeit
    private long incrementMs = 100; // 100 ms Inkrement pro Zug
    private int maxMovesPerGame = 200; // Zugbegrenzung zur Vermeidung endloser Remis-Partien
    private List<String> openingFens = new ArrayList<>(); // Optionale Liste an Start-FENs

    public TournamentConfig() {
    }

    public static TournamentConfig defaultBlitz() {
        TournamentConfig config = new TournamentConfig();
        config.setGamesPerPairing(2);
        config.setTimeControlEnabled(true);
        config.setInitialTimeMs(3_000);
        config.setIncrementMs(100);
        return config;
    }

    public static TournamentConfig fastTest() {
        TournamentConfig config = new TournamentConfig();
        config.setGamesPerPairing(2);
        config.setTimeControlEnabled(true);
        config.setInitialTimeMs(1_000);
        config.setIncrementMs(50);
        config.setMaxMovesPerGame(100);
        return config;
    }

    public int getGamesPerPairing() {
        return gamesPerPairing;
    }

    public void setGamesPerPairing(int gamesPerPairing) {
        if (gamesPerPairing < 2 || gamesPerPairing % 2 != 0) {
            throw new IllegalArgumentException("gamesPerPairing muss eine gerade Zahl >= 2 sein für Farbausgleich.");
        }
        this.gamesPerPairing = gamesPerPairing;
    }

    public boolean isTimeControlEnabled() {
        return timeControlEnabled;
    }

    public void setTimeControlEnabled(boolean timeControlEnabled) {
        this.timeControlEnabled = timeControlEnabled;
    }

    public long getInitialTimeMs() {
        return initialTimeMs;
    }

    public void setInitialTimeMs(long initialTimeMs) {
        this.initialTimeMs = Math.max(500, initialTimeMs);
    }

    public long getIncrementMs() {
        return incrementMs;
    }

    public void setIncrementMs(long incrementMs) {
        this.incrementMs = Math.max(0, incrementMs);
    }

    public int getMaxMovesPerGame() {
        return maxMovesPerGame;
    }

    public void setMaxMovesPerGame(int maxMovesPerGame) {
        this.maxMovesPerGame = Math.max(20, maxMovesPerGame);
    }

    public List<String> getOpeningFens() {
        return Collections.unmodifiableList(openingFens);
    }

    public void addOpeningFen(String fen) {
        this.openingFens.add(fen);
    }

    public void setOpeningFens(List<String> fens) {
        this.openingFens = new ArrayList<>(fens);
    }
}
