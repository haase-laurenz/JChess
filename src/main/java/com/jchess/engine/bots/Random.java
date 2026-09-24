package com.jchess.engine.bots;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.engine.ChessEngine;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.rules.MoveGenerator;

import java.util.List;

/**
 * Ein spielbarer Zufalls-Bot für JChess.
 * 
 * Dieser Bot dient als unkomplizierter Spielpartner und Vorlage für deine
 * eigenen Schach-Engines (z.B. Minimax mit Alpha-Beta-Pruning, Eröffnungsbibliotheken
 * oder Bewertungsfunktionen).
 *
 * Ort: /engine/bots/Random.java
 */
public class Random implements ChessEngine {

    private final String name;
    private final java.util.Random rng;
    private boolean preferCaptures = true;

    public Random() {
        this("RandomBot v1.0");
    }

    public Random(String name) {
        this(name, new java.util.Random());
    }

    public Random(String name, java.util.Random rng) {
        this.name = name;
        this.rng = rng;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Steuert, ob der Bot Schlagzüge leicht bevorzugen soll (macht Partien lebendiger).
     */
    public void setPreferCaptures(boolean preferCaptures) {
        this.preferCaptures = preferCaptures;
    }

    public boolean isPreferCaptures() {
        return preferCaptures;
    }

    @Override
    public Move findBestMove(Board board, PieceColor color) {
        // 1. Alle aktuell legalen Züge berechnen
        List<Move> legalMoves = MoveGenerator.generateLegalMovesForColor(board, color);
        if (legalMoves.isEmpty()) {
            return null; // Matt oder Patt
        }

        // 2. Falls aktiviert: Schlagzüge mit 65% Wahrscheinlichkeit bevorzugen
        if (preferCaptures) {
            List<Move> captures = legalMoves.stream().filter(Move::isCapture).toList();
            if (!captures.isEmpty() && rng.nextDouble() < 0.65) {
                return captures.get(rng.nextInt(captures.size()));
            }
        }

        // 3. Einen zufälligen legalen Zug wählen
        return legalMoves.get(rng.nextInt(legalMoves.size()));
    }
}
