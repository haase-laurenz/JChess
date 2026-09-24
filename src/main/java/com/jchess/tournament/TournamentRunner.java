package com.jchess.tournament;

import engine.bots.KingLBot1;
import engine.bots.JChessV1;
import engine.bots.Random;

/**
 * Konsolen-Runner zum Ausführen von Turnieren zwischen verschiedenen KI-Engines.
 */
public class TournamentRunner {

    public static void main(String[] args) {
        System.out.println("====================================================================");
        System.out.println("               JChess Engine Tournament Framework");
        System.out.println("====================================================================");

        // 1. Turnierkonfiguration: 10 Partien pro Paarung (Hin- & Rückspiel mit Farbentausch -> 20 Partien gesamt)
        TournamentConfig config = new TournamentConfig();
        config.setGamesPerPairing(10);
        config.setTimeControlEnabled(true);
        config.setInitialTimeMs(1_000); // 1s Startzeit
        config.setIncrementMs(50);      // 50ms Inkrement pro Zug
        config.setMaxMovesPerGame(120); // Max. 120 Züge pro Partie

        Tournament tournament = new Tournament("JChess Herbstmeisterschaft 2026", config);

        // 2. Teilnehmer hinzufügen
        tournament.addParticipant(new engine.bots.JChessV2("JChessV2 (Advanced Eval)", 64, false, 1000, 0.05));
        tournament.addParticipant(new JChessV1("JChessV1 (5%)", 64, false, 1000, 0.05));
        tournament.addParticipant(new KingLBot1("KingLBot", 64, false, 1000, 0.05));

        // 3. Turnier starten
        tournament.run();

        // 4. Rangliste und Kreuztabelle ausgeben
        System.out.print(tournament.getLeaderboardString());
        System.out.print(tournament.getCrossTableString());
    }
}
