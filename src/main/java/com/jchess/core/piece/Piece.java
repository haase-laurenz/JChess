package com.jchess.core.piece;

import java.util.Objects;

/**
 * Eine unmodifizierbare Schachfigur mit Typ und Farbe.
 */
public final class Piece {
    private final PieceType type;
    private final PieceColor color;

    private static final Piece[][] CACHE = new Piece[2][6];

    static {
        for (PieceColor c : PieceColor.values()) {
            for (PieceType t : PieceType.values()) {
                CACHE[c.ordinal()][t.ordinal()] = new Piece(t, c);
            }
        }
    }

    public static Piece of(PieceType type, PieceColor color) {
        if (type == null || color == null) return null;
        return CACHE[color.ordinal()][type.ordinal()];
    }

    public Piece(PieceType type, PieceColor color) {
        this.type = Objects.requireNonNull(type, "PieceType darf nicht null sein");
        this.color = Objects.requireNonNull(color, "PieceColor darf nicht null sein");
    }

    public PieceType getType() {
        return type;
    }

    public PieceColor getColor() {
        return color;
    }

    public boolean isWhite() {
        return color == PieceColor.WHITE;
    }

    public boolean isBlack() {
        return color == PieceColor.BLACK;
    }

    /**
     * Unicode-Schachsymbol für Text- und Fallback-Darstellung (z.B. ♔ oder ♟).
     */
    public String getUnicodeSymbol() {
        if (color == PieceColor.WHITE) {
            return switch (type) {
                case KING -> "♔";
                case QUEEN -> "♕";
                case ROOK -> "♖";
                case BISHOP -> "♗";
                case KNIGHT -> "♘";
                case PAWN -> "♙";
            };
        } else {
            return switch (type) {
                case KING -> "♚";
                case QUEEN -> "♛";
                case ROOK -> "♜";
                case BISHOP -> "♝";
                case KNIGHT -> "♞";
                case PAWN -> "♟";
            };
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Piece piece)) return false;
        return type == piece.type && color == piece.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, color);
    }

    @Override
    public String toString() {
        return (color == PieceColor.WHITE ? "W_" : "B_") + type.name();
    }
}
