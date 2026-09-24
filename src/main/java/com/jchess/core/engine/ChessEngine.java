package com.jchess.core.engine;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.piece.PieceColor;

/**
 * Universelle Schnittstelle für Schach-Engines / KI-Bots.
 * Hier kannst du später deine eigenen Algorithmen (z.B. Minimax,
 * Alpha-Beta-Pruning, Transposition Tables oder neuronale Netze) einhängen.
 */
public interface ChessEngine {

    /**
     * Name der Engine (z.B. "RandomBot v1.0", "MyMinimaxEngine").
     */
    String getName();

    /**
     * Berechnet den besten Zug für den angegebenen Brettzustand und die Spielerfarbe.
     *
     * @param board Aktueller Zustand des Schachbretts.
     * @param color Farbe, für die gerechnet werden soll.
     * @return Der berechnete Move oder null, falls keine Züge möglich sind (Matt / Patt).
     */
    Move findBestMove(Board board, PieceColor color);

    /**
     * Berechnet den besten Zug unter Berücksichtigung der Schachuhr / verbleibenden Bedenkzeit.
     * Ermöglicht intelligentes Zeitmanagement in deinen Bots (z.B. Suchtiefe anpassen bei wenig Zeit).
     * Standardmäßig wird findBestMove(board, color) aufgerufen, sodass Bots abwärtskompatibel bleiben.
     *
     * @param board Aktueller Zustand des Schachbretts.
     * @param color Farbe, für die gerechnet werden soll.
     * @param clock Aktuelle Schachuhr mit Restzeiten für beide Spieler (oder null).
     * @return Der berechnete Move oder null.
     */
    default Move findBestMove(Board board, PieceColor color, com.jchess.core.clock.ChessClock clock) {
        return findBestMove(board, color);
    }
}
