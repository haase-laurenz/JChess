package com.jchess.core.rules;

/**
 * Status der Schachpartie.
 */
public enum GameStatus {
    ACTIVE("Partie läuft"),
    CHECK("Schach!"),
    CHECKMATE("Schachmatt!"),
    STALEMATE("Patt (Remis)"),
    DRAW_FIFTY_MOVES("Remis (50-Züge-Regel)"),
    DRAW_INSUFFICIENT_MATERIAL("Remis (Ungenügend Material)"),
    DRAW_THREEFOLD_REPETITION("Remis (Dreifache Stellungswiederholung)");

    private final String description;

    GameStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isGameOver() {
        return this == CHECKMATE || this == STALEMATE ||
               this == DRAW_FIFTY_MOVES || this == DRAW_INSUFFICIENT_MATERIAL ||
               this == DRAW_THREEFOLD_REPETITION;
    }
}
