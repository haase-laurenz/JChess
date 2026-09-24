package com.jchess.core.engine;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.rules.MoveGenerator;

import java.util.List;
import java.util.Random;

/**
 * Ein einfacher Schach-Bot, der zufällige legale Züge wählt.
 * Bevorzugt Schlagzüge leicht gegenüber stillen Zügen, um lebendiger zu spielen.
 * Dient als sofort spielbare Referenz und Vorlage für deine eigenen Algorithmen.
 */
public class RandomBot implements ChessEngine {
    private final String name;
    private final Random random = new Random();

    public RandomBot() {
        this("RandomBot (Starter)");
    }

    public RandomBot(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Move findBestMove(Board board, PieceColor color) {
        List<Move> legalMoves = MoveGenerator.generateLegalMovesForColor(board, color);
        if (legalMoves.isEmpty()) {
            return null;
        }

        // Leichtes Bias für Schlagzüge (macht das Spiel interessanter als reiner Zufall)
        List<Move> captures = legalMoves.stream().filter(Move::isCapture).toList();
        if (!captures.isEmpty() && random.nextDouble() < 0.65) {
            return captures.get(random.nextInt(captures.size()));
        }

        return legalMoves.get(random.nextInt(legalMoves.size()));
    }
}
