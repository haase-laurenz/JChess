package com.jchess.core.piece;

/**
 * Repräsentiert die beiden Parteien im Schach: Weiß und Schwarz.
 */
public enum PieceColor {
    WHITE,
    BLACK;

    /**
     * Liefert die gegnerische Farbe.
     */
    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /**
     * Vorwärtsrichtung für Bauern auf dem Brett (Rang-Index).
     * Weiß zieht von Rang 1 (Index 1) nach oben (Index 7), also +1.
     * Schwarz zieht von Rang 6 nach unten (Index 0), also -1.
     */
    public int getPawnDirection() {
        return this == WHITE ? 1 : -1;
    }

    /**
     * Startrang für Bauern (Index 1 für Weiß, 6 für Schwarz).
     */
    public int getPawnStartRank() {
        return this == WHITE ? 1 : 6;
    }

    /**
     * Rang für Bauernumwandlung (Index 7 für Weiß, 0 für Schwarz).
     */
    public int getPromotionRank() {
        return this == WHITE ? 7 : 0;
    }
}
