package com.jchess.tournament;

import com.jchess.core.engine.ChessEngine;
import com.jchess.core.piece.PieceColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repräsentiert das Ergebnis einer einzelnen Turnierpartie.
 */
public class GameResult {

    public enum Outcome {
        WHITE_WIN("1-0"),
        BLACK_WIN("0-1"),
        DRAW("1/2-1/2");

        private final String notation;

        Outcome(String notation) {
            this.notation = notation;
        }

        public String getNotation() {
            return notation;
        }
    }

    public enum TerminationReason {
        CHECKMATE("Schachmatt"),
        STALEMATE("Patt"),
        INSUFFICIENT_MATERIAL("Ungenügend Material"),
        THREEFOLD_REPETITION("Dreifache Stellungswiederholung"),
        FIFTY_MOVES("50-Züge-Regel"),
        TIMEOUT("Zeitüberschreitung"),
        ILLEGAL_MOVE("Regelwidriger Zug"),
        MAX_MOVES_DRAW("Maximale Zugzahl erreicht (Remis)");

        private final String description;

        TerminationReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final ChessEngine whiteEngine;
    private final ChessEngine blackEngine;
    private final Outcome outcome;
    private final TerminationReason reason;
    private final int totalMoves;
    private final long durationMs;
    private final List<String> moveHistory;

    public GameResult(ChessEngine whiteEngine, ChessEngine blackEngine, Outcome outcome,
                      TerminationReason reason, int totalMoves, long durationMs, List<String> moveHistory) {
        this.whiteEngine = whiteEngine;
        this.blackEngine = blackEngine;
        this.outcome = outcome;
        this.reason = reason;
        this.totalMoves = totalMoves;
        this.durationMs = durationMs;
        this.moveHistory = moveHistory != null ? new ArrayList<>(moveHistory) : Collections.emptyList();
    }

    public ChessEngine getWhiteEngine() {
        return whiteEngine;
    }

    public ChessEngine getBlackEngine() {
        return blackEngine;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public TerminationReason getReason() {
        return reason;
    }

    public int getTotalMoves() {
        return totalMoves;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public List<String> getMoveHistory() {
        return Collections.unmodifiableList(moveHistory);
    }

    public boolean isDraw() {
        return outcome == Outcome.DRAW;
    }

    public ChessEngine getWinnerEngine() {
        if (outcome == Outcome.WHITE_WIN) return whiteEngine;
        if (outcome == Outcome.BLACK_WIN) return blackEngine;
        return null;
    }

    public ChessEngine getLoserEngine() {
        if (outcome == Outcome.WHITE_WIN) return blackEngine;
        if (outcome == Outcome.BLACK_WIN) return whiteEngine;
        return null;
    }

    public double getScoreForEngine(ChessEngine engine) {
        if (outcome == Outcome.DRAW) return 0.5;
        if (outcome == Outcome.WHITE_WIN) return engine == whiteEngine ? 1.0 : 0.0;
        if (outcome == Outcome.BLACK_WIN) return engine == blackEngine ? 1.0 : 0.0;
        return 0.0;
    }

    @Override
    public String toString() {
        return String.format("%s (W) vs %s (B) -> %s (%s, %d Züge, %d ms)",
                whiteEngine.getName(), blackEngine.getName(), outcome.getNotation(), reason.getDescription(), totalMoves, durationMs);
    }
}
