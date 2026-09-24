package com.jchess.core.board;

import java.util.Objects;

/**
 * Repräsentiert ein Feld auf dem Schachbrett.
 * file: 0..7 (a..h)
 * rank: 0..7 (1..8)
 * Verwendet Flyweight-Caching für alle 64 Felder, um Garbage-Collection bei
 * Engine-Suchen zu minimieren.
 */
public final class Position {
    private static final Position[][] CACHE = new Position[8][8];
    /** Flat Cache für O(1) Index→Position ohne Division/Modulo. */
    private static final Position[] INDEX_CACHE = new Position[64];

    static {
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                CACHE[f][r] = new Position(f, r);
            }
        }
        for (int i = 0; i < 64; i++) {
            INDEX_CACHE[i] = CACHE[i % 8][i / 8];
        }
    }

    private final int file;
    private final int rank;
    /** Gecachter 1D-Index (rank * 8 + file) für O(1) getIndex(). */
    private final int index;

    private Position(int file, int rank) {
        this.file = file;
        this.rank = rank;
        this.index = rank * 8 + file;
    }

    /**
     * Liefert die gecachte Position für file (0..7) und rank (0..7).
     * Gibt null zurück, wenn außerhalb des Brettes.
     */
    public static Position of(int file, int rank) {
        if (!isValid(file, rank)) {
            return null;
        }
        return CACHE[file][rank];
    }

    /**
     * Prüft, ob die Koordinaten auf dem Brett liegen.
     */
    public static boolean isValid(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    /**
     * Parst eine algebraische Notation (z.B. "e4", "a1").
     */
    public static Position fromAlgebraic(String notation) {
        if (notation == null || notation.length() != 2) {
            throw new IllegalArgumentException("Ungültige Notation: " + notation);
        }
        char fileChar = Character.toLowerCase(notation.charAt(0));
        char rankChar = notation.charAt(1);

        int file = fileChar - 'a';
        int rank = rankChar - '1';

        if (!isValid(file, rank)) {
            throw new IllegalArgumentException("Position außerhalb des Brettes: " + notation);
        }
        return of(file, rank);
    }

    /**
     * 1D-Index zwischen 0 und 63 (rank * 8 + file).
     */
    public static Position fromIndex(int index) {
        if (index < 0 || index >= 64) {
            throw new IllegalArgumentException("Index muss zwischen 0 und 63 liegen: " + index);
        }
        return INDEX_CACHE[index];
    }

    /**
     * Schneller Index→Position ohne Bounds-Check.
     * Nur verwenden wenn 0 ≤ index < 64 garantiert ist (MoveGenerator, Engine).
     */
    public static Position fromIndexFast(int index) {
        return INDEX_CACHE[index];
    }

    public int getFile() {
        return file;
    }

    public int getRank() {
        return rank;
    }

    public int getIndex() {
        return index;
    }

    /**
     * Erzeugt eine verschobene Position oder null, falls sie außerhalb des Bretts liegt.
     */
    public Position offset(int deltaFile, int deltaRank) {
        return of(file + deltaFile, rank + deltaRank);
    }

    /**
     * Gibt true zurück, wenn das Feld ein helles Feld ist.
     */
    public boolean isLightSquare() {
        return (file + rank) % 2 != 0;
    }

    /**
     * Liefert die Standardnotation (z.B. "e4", "c1").
     */
    public String toAlgebraic() {
        char fileChar = (char) ('a' + file);
        char rankChar = (char) ('1' + rank);
        return "" + fileChar + rankChar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position position)) return false;
        return file == position.file && rank == position.rank;
    }

    @Override
    public int hashCode() {
        return getIndex();
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
