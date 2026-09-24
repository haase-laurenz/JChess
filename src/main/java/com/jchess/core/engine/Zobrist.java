package com.jchess.core.engine;

import com.jchess.core.board.Board;
import com.jchess.core.board.Position;
import com.jchess.core.piece.Piece;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;

import java.util.Random;

/**
 * Zobrist-Hashing für das JChess-Schachbrett:
 * Erzeugt 64-Bit Prüfsummen für Schachstellungen mittels XOR-Verknüpfung
 * von deterministischen 64-Bit Pseudozufallszahlen.
 *
 * Beinhaltet:
 * - Figuren auf Feldern (12 Typen x 64 Felder)
 * - Aktives Zugrecht (Side to Move)
 * - Rochaderechte (16 Kombinationen)
 * - En-Passant Zielfeld (8 Spalten)
 */
public final class Zobrist {

    // 12 Figurenarten: Weiß (0..5: P, N, B, R, Q, K), Schwarz (6..11: P, N, B, R, Q, K) auf 64 Feldern
    public static final long[][] PIECE_SQUARE = new long[12][64];

    // XOR-Wert wenn Schwarz am Zug ist
    public static final long SIDE_TO_MOVE;

    // 16 Kombinationen für Rochaderechte (0..15)
    public static final long[] CASTLING_RIGHTS = new long[16];

    // En-Passant Zielfeld-Spalte (0..7)
    public static final long[] EN_PASSANT_FILE = new long[8];

    static {
        // Fester Seed für reproduzierbare, deterministische Hashes über alle JVM-Läufe hinweg
        Random prng = new Random(0x1B86F280A4B38C2EL);

        for (int p = 0; p < 12; p++) {
            for (int sq = 0; sq < 64; sq++) {
                PIECE_SQUARE[p][sq] = nextRandom64(prng);
            }
        }

        SIDE_TO_MOVE = nextRandom64(prng);

        for (int c = 0; c < 16; c++) {
            CASTLING_RIGHTS[c] = nextRandom64(prng);
        }

        for (int f = 0; f < 8; f++) {
            EN_PASSANT_FILE[f] = nextRandom64(prng);
        }
    }

    private static long nextRandom64(Random prng) {
        long high = ((long) prng.nextInt()) << 32;
        long low = ((long) prng.nextInt()) & 0xFFFFFFFFL;
        return high | low;
    }

    /**
     * Ermittelt den Index im PIECE_SQUARE Array (0..5 für Weiß, 6..11 für Schwarz).
     */
    public static int getPieceIndex(PieceType type, PieceColor color) {
        return (color == PieceColor.WHITE ? 0 : 6) + type.ordinal();
    }

    /**
     * Berechnet den 64-Bit Zobrist-Hash für einen beliebigen Brettzustand von Grund auf neu.
     */
    public static long computeHash(Board board) {
        long hash = 0L;

        // Figuren auf dem Brett
        for (int sq = 0; sq < 64; sq++) {
            Piece piece = board.getPieceAt(Position.fromIndex(sq));
            if (piece != null) {
                int pieceIdx = getPieceIndex(piece.getType(), piece.getColor());
                hash ^= PIECE_SQUARE[pieceIdx][sq];
            }
        }

        // Am Zug (Schwarz)
        if (board.getActivePlayer() == PieceColor.BLACK) {
            hash ^= SIDE_TO_MOVE;
        }

        // Rochaderechte
        hash ^= CASTLING_RIGHTS[board.getCastlingRights() & 15];

        // En Passant Ziel
        Position ep = board.getEnPassantTarget();
        if (ep != null) {
            hash ^= EN_PASSANT_FILE[ep.getFile()];
        }

        return hash;
    }

    private Zobrist() {}
}
