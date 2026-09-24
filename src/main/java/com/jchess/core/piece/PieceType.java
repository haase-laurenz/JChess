package com.jchess.core.piece;

/**
 * Typen von Schachfiguren mit symbolischen Bezeichnungen, Standard-Punktwerten
 * und Zuordnungen für Notation und Rendering.
 */
public enum PieceType {
    PAWN('P', 100, 5),
    KNIGHT('N', 320, 3),
    BISHOP('B', 330, 2),
    ROOK('R', 500, 4),
    QUEEN('Q', 900, 1),
    KING('K', 20000, 0);

    private final char symbol;
    private final int baseValue;
    private final int spriteColumn;

    PieceType(char symbol, int baseValue, int spriteColumn) {
        this.symbol = symbol;
        this.baseValue = baseValue;
        this.spriteColumn = spriteColumn;
    }

    /**
     * Standard-Buchstabenkürzel (z.B. 'N' für Springer/Knight, 'K' für König).
     */
    public char getSymbol() {
        return symbol;
    }

    /**
     * Standard-Evaluierungswert in Zentibauern (z.B. Bauer=100, Springer=320).
     */
    public int getBaseValue() {
        return baseValue;
    }

    /**
     * Spaltenindex im Standard 2x6 Spritesheet (King=0, Queen=1, Bishop=2, Knight=3, Rook=4, Pawn=5).
     */
    public int getSpriteColumn() {
        return spriteColumn;
    }

    /**
     * Gibt an, ob sich die Figur über mehrere Felder geradlinig bewegt (Läufer, Turm, Dame).
     */
    public boolean isSliding() {
        return this == BISHOP || this == ROOK || this == QUEEN;
    }

    /**
     * Ermittelt den PieceType aus dem Standard-Buchstabensymbol (P, N, B, R, Q, K).
     */
    public static PieceType fromSymbol(char symbol) {
        char upper = Character.toUpperCase(symbol);
        return switch (upper) {
            case 'P' -> PAWN;
            case 'N' -> KNIGHT;
            case 'B' -> BISHOP;
            case 'R' -> ROOK;
            case 'Q' -> QUEEN;
            case 'K' -> KING;
            default -> throw new IllegalArgumentException("Unbekanntes Figurensymbol: " + symbol);
        };
    }
}
