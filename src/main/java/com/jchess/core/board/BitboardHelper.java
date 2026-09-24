package com.jchess.core.board;

import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;

/**
 * Hochoptimierte Bitboard-Hilfsklasse für JChess.
 * Bietet vorberechnete Angriffstabellen (Springer, König, Bauern) und Magic Bitboards
 * für Schieberfiguren (Turm, Läufer, Dame).
 * Ermöglicht O(1) Schacherkennung und blitzschnelle Zuggenerierung.
 */
public final class BitboardHelper {

    public static final long[] SQUARE_BB = new long[64];
    public static final long[] KNIGHT_ATTACKS = new long[64];
    public static final long[] KING_ATTACKS = new long[64];
    // [0=White, 1=Black][square]
    public static final long[][] PAWN_ATTACKS = new long[2][64];

    public static final long[] BISHOP_MASKS = new long[64];
    public static final long[] ROOK_MASKS = new long[64];
    public static final int[] BISHOP_SHIFTS = new int[64];
    public static final int[] ROOK_SHIFTS = new int[64];

    public static final long[][] BISHOP_ATTACKS = new long[64][];
    public static final long[][] ROOK_ATTACKS = new long[64][];

    public static final long[] BISHOP_MAGICS = {
        0x0102500908108181L, 0x028208082100512aL, 0x040888004080000bL, 0x0084041080000040L,
        0x0202021000180912L, 0x0402023084a18104L, 0x1040440220100000L, 0x1c00410850022004L,
        0x8001040444582200L, 0x00004401480a0280L, 0x0000100100410801L, 0x000108160744a408L,
        0x0043211040021184L, 0x0e06620184200010L, 0x0880004410880804L, 0x4286130058028802L,
        0x0209001030010830L, 0x1450080910212240L, 0x0044108200220201L, 0x400400084010a000L,
        0x0325048820080408L, 0x200100d68180c000L, 0x2002000082100300L, 0x200908284b081100L,
        0x20384a0040920820L, 0x04023002a00420b2L, 0x0061090290094a00L, 0x0408080060202020L,
        0x2020840028802000L, 0x2008081008804400L, 0x200504480c040424L, 0x206c048010444411L,
        0x0814200880041040L, 0x0002025000221080L, 0x001c020120480044L, 0x4040110800040040L,
        0x002800240004c100L, 0x0030004a00004100L, 0x4001021080820800L, 0x0004040096004040L,
        0x120911082030c002L, 0x4401011010210200L, 0x8006004048009040L, 0x2018004208001c80L,
        0x009824010c020200L, 0x0040810400202100L, 0x110401940c000100L, 0x147c009201404202L,
        0x4142014303c10008L, 0x0126020619044080L, 0x4000088400888040L, 0x0000041084040308L,
        0x0012000821010000L, 0x2404c00821210211L, 0x0010200841005000L, 0x040210010a008241L,
        0x0004290110100200L, 0x0800024108088a42L, 0x000100c848441000L, 0x0220820017040900L,
        0x4800110208210108L, 0x0080084970014204L, 0x1002400224040084L, 0x2020083005003021L
    };

    public static final long[] ROOK_MAGICS = {
        0x0a80104002800025L, 0x4940001000402008L, 0x01800b9000822000L, 0x4100042100100008L,
        0x2080080080040002L, 0x0280040001620080L, 0x0400081044111082L, 0x020000844202210cL,
        0xc002002201008040L, 0x0000400020005000L, 0x2010801000802000L, 0x4000801000080080L,
        0x0401000801001004L, 0x0010800400020081L, 0x8009000200042100L, 0x1082000400410082L,
        0x8000410021008000L, 0x0110044000402008L, 0x1220008010082080L, 0x1010008010800800L,
        0x0008818004000802L, 0x4012008002040080L, 0x0284140010280211L, 0xe800020000840041L,
        0x0320228180004008L, 0x4000200040100040L, 0x0120001010020400L, 0x4400080080801000L,
        0x0441002d00103800L, 0x2182008080040002L, 0x4000010080800200L, 0x04800c8200090444L,
        0x1040004080800024L, 0x8050882000804000L, 0x8091004011002000L, 0x0000100021000900L,
        0x1018020400800880L, 0x4002000400800280L, 0x0004100864002221L, 0x0802040082000041L,
        0x0000208040108000L, 0x0010002002424000L, 0x0000410020030010L, 0x0002028840920020L,
        0x0000050008010010L, 0x4110040002008080L, 0x3043028110040008L, 0x0020e08100420004L,
        0x25a0284100801100L, 0x9809002080420200L, 0x4090420010208600L, 0x0001021000200900L,
        0x0148000880040080L, 0x0000020004008080L, 0x00161002c1080400L, 0x0000104108840a00L,
        0x0000148100204202L, 0x0001004000281081L, 0x2081021208200041L, 0x0011020810000421L,
        0x0002000408902082L, 0x0042000410018802L, 0x2110008201100804L, 0x0200022c00410086L
    };

    static {
        // 1. Grundlegende Bit-Masken
        for (int sq = 0; sq < 64; sq++) {
            SQUARE_BB[sq] = 1L << sq;
        }

        // 2. Springer-Angriffe
        int[][] knightDeltas = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };
        for (int sq = 0; sq < 64; sq++) {
            int tf = sq % 8, tr = sq / 8;
            long mask = 0L;
            for (int[] d : knightDeltas) {
                int f = tf + d[0], r = tr + d[1];
                if (f >= 0 && f < 8 && r >= 0 && r < 8) {
                    mask |= (1L << (r * 8 + f));
                }
            }
            KNIGHT_ATTACKS[sq] = mask;
        }

        // 3. Königs-Angriffe
        int[][] kingDeltas = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
        };
        for (int sq = 0; sq < 64; sq++) {
            int tf = sq % 8, tr = sq / 8;
            long mask = 0L;
            for (int[] d : kingDeltas) {
                int f = tf + d[0], r = tr + d[1];
                if (f >= 0 && f < 8 && r >= 0 && r < 8) {
                    mask |= (1L << (r * 8 + f));
                }
            }
            KING_ATTACKS[sq] = mask;
        }

        // 4. Bauern-Angriffe
        for (int sq = 0; sq < 64; sq++) {
            int tf = sq % 8, tr = sq / 8;
            long whiteAttacks = 0L;
            if (tr < 7) {
                if (tf > 0) whiteAttacks |= (1L << ((tr + 1) * 8 + (tf - 1)));
                if (tf < 7) whiteAttacks |= (1L << ((tr + 1) * 8 + (tf + 1)));
            }
            PAWN_ATTACKS[PieceColor.WHITE.ordinal()][sq] = whiteAttacks;

            long blackAttacks = 0L;
            if (tr > 0) {
                if (tf > 0) blackAttacks |= (1L << ((tr - 1) * 8 + (tf - 1)));
                if (tf < 7) blackAttacks |= (1L << ((tr - 1) * 8 + (tf + 1)));
            }
            PAWN_ATTACKS[PieceColor.BLACK.ordinal()][sq] = blackAttacks;
        }

        // 5. Magic Bitboards Initialisierung
        initMagicBitboards();
    }

    private static void initMagicBitboards() {
        for (int sq = 0; sq < 64; sq++) {
            // Bishop
            long bMask = maskBishop(sq);
            BISHOP_MASKS[sq] = bMask;
            int bBits = Long.bitCount(bMask);
            BISHOP_SHIFTS[sq] = 64 - bBits;
            BISHOP_ATTACKS[sq] = new long[1 << bBits];

            int bCombinations = 1 << bBits;
            for (int i = 0; i < bCombinations; i++) {
                long occ = occupancyFromIndex(i, bBits, bMask);
                int idx = (int) ((occ * BISHOP_MAGICS[sq]) >>> BISHOP_SHIFTS[sq]);
                BISHOP_ATTACKS[sq][idx] = attackBishop(sq, occ);
            }

            // Rook
            long rMask = maskRook(sq);
            ROOK_MASKS[sq] = rMask;
            int rBits = Long.bitCount(rMask);
            ROOK_SHIFTS[sq] = 64 - rBits;
            ROOK_ATTACKS[sq] = new long[1 << rBits];

            int rCombinations = 1 << rBits;
            for (int i = 0; i < rCombinations; i++) {
                long occ = occupancyFromIndex(i, rBits, rMask);
                int idx = (int) ((occ * ROOK_MAGICS[sq]) >>> ROOK_SHIFTS[sq]);
                ROOK_ATTACKS[sq][idx] = attackRook(sq, occ);
            }
        }
    }

    private static long maskBishop(int sq) {
        long result = 0L;
        int tr = sq / 8, tf = sq % 8;
        for (int r = tr + 1, f = tf + 1; r <= 6 && f <= 6; r++, f++) result |= (1L << (r * 8 + f));
        for (int r = tr + 1, f = tf - 1; r <= 6 && f >= 1; r++, f--) result |= (1L << (r * 8 + f));
        for (int r = tr - 1, f = tf + 1; r >= 1 && f <= 6; r--, f++) result |= (1L << (r * 8 + f));
        for (int r = tr - 1, f = tf - 1; r >= 1 && f >= 1; r--, f--) result |= (1L << (r * 8 + f));
        return result;
    }

    private static long maskRook(int sq) {
        long result = 0L;
        int tr = sq / 8, tf = sq % 8;
        for (int r = tr + 1; r <= 6; r++) result |= (1L << (r * 8 + tf));
        for (int r = tr - 1; r >= 1; r--) result |= (1L << (r * 8 + tf));
        for (int f = tf + 1; f <= 6; f++) result |= (1L << (tr * 8 + f));
        for (int f = tf - 1; f >= 1; f--) result |= (1L << (tr * 8 + f));
        return result;
    }

    private static long attackBishop(int sq, long block) {
        long result = 0L;
        int tr = sq / 8, tf = sq % 8;
        for (int r = tr + 1, f = tf + 1; r <= 7 && f <= 7; r++, f++) {
            result |= (1L << (r * 8 + f));
            if ((block & (1L << (r * 8 + f))) != 0) break;
        }
        for (int r = tr + 1, f = tf - 1; r <= 7 && f >= 0; r++, f--) {
            result |= (1L << (r * 8 + f));
            if ((block & (1L << (r * 8 + f))) != 0) break;
        }
        for (int r = tr - 1, f = tf + 1; r >= 0 && f <= 7; r--, f++) {
            result |= (1L << (r * 8 + f));
            if ((block & (1L << (r * 8 + f))) != 0) break;
        }
        for (int r = tr - 1, f = tf - 1; r >= 0 && f >= 0; r--, f--) {
            result |= (1L << (r * 8 + f));
            if ((block & (1L << (r * 8 + f))) != 0) break;
        }
        return result;
    }

    private static long attackRook(int sq, long block) {
        long result = 0L;
        int tr = sq / 8, tf = sq % 8;
        for (int r = tr + 1; r <= 7; r++) {
            result |= (1L << (r * 8 + tf));
            if ((block & (1L << (r * 8 + tf))) != 0) break;
        }
        for (int r = tr - 1; r >= 0; r--) {
            result |= (1L << (r * 8 + tf));
            if ((block & (1L << (r * 8 + tf))) != 0) break;
        }
        for (int f = tf + 1; f <= 7; f++) {
            result |= (1L << (tr * 8 + f));
            if ((block & (1L << (tr * 8 + f))) != 0) break;
        }
        for (int f = tf - 1; f >= 0; f--) {
            result |= (1L << (tr * 8 + f));
            if ((block & (1L << (tr * 8 + f))) != 0) break;
        }
        return result;
    }

    private static long occupancyFromIndex(int index, int bits, long mask) {
        long occ = 0L;
        long tempMask = mask;
        for (int i = 0; i < bits; i++) {
            int sq = Long.numberOfTrailingZeros(tempMask);
            tempMask &= tempMask - 1;
            if ((index & (1 << i)) != 0) {
                occ |= (1L << sq);
            }
        }
        return occ;
    }

    private BitboardHelper() {}

    /**
     * Liefert die Angriffe eines Läufers auf square unter Berücksichtigung der Figurenblockaden.
     */
    public static long getBishopAttacks(int square, long occupied) {
        long occ = occupied & BISHOP_MASKS[square];
        int idx = (int) ((occ * BISHOP_MAGICS[square]) >>> BISHOP_SHIFTS[square]);
        return BISHOP_ATTACKS[square][idx];
    }

    /**
     * Liefert die Angriffe eines Turms auf square unter Berücksichtigung der Figurenblockaden.
     */
    public static long getRookAttacks(int square, long occupied) {
        long occ = occupied & ROOK_MASKS[square];
        int idx = (int) ((occ * ROOK_MAGICS[square]) >>> ROOK_SHIFTS[square]);
        return ROOK_ATTACKS[square][idx];
    }

    /**
     * Liefert die Angriffe einer Dame auf square unter Berücksichtigung der Figurenblockaden.
     */
    public static long getQueenAttacks(int square, long occupied) {
        return getBishopAttacks(square, occupied) | getRookAttacks(square, occupied);
    }

    public static long getKnightAttacks(int square) {
        return KNIGHT_ATTACKS[square];
    }

    public static long getKingAttacks(int square) {
        return KING_ATTACKS[square];
    }

    public static long getPawnAttacks(int square, PieceColor color) {
        return PAWN_ATTACKS[color.ordinal()][square];
    }

    /**
     * Prüft in O(1) mit Bitboards, ob ein bestimmtes Feld von einer gegnerischen Farbe angegriffen wird.
     */
    public static boolean isSquareAttacked(Board board, int targetSq, PieceColor attackerColor) {
        // 1. Bauernangriffe (Rückwärts-Projektion: welches Feld müsste ein Bauer haben, um targetSq anzugreifen?)
        PieceColor defenderColor = attackerColor.opposite();
        long pawnAttacksFromTarget = PAWN_ATTACKS[defenderColor.ordinal()][targetSq];
        long enemyPawns = board.getBitboard(PieceType.PAWN, attackerColor);
        if ((pawnAttacksFromTarget & enemyPawns) != 0) {
            return true;
        }

        // 2. Springerangriffe
        long enemyKnights = board.getBitboard(PieceType.KNIGHT, attackerColor);
        if ((KNIGHT_ATTACKS[targetSq] & enemyKnights) != 0) {
            return true;
        }

        // 3. Königsangriffe
        long enemyKing = board.getBitboard(PieceType.KING, attackerColor);
        if ((KING_ATTACKS[targetSq] & enemyKing) != 0) {
            return true;
        }

        long occupied = board.getOccupiedBitboard();

        // 4. Diagonale Angreifer (Läufer & Dame)
        long diagAttackers = board.getBitboard(PieceType.BISHOP, attackerColor) | board.getBitboard(PieceType.QUEEN, attackerColor);
        if (diagAttackers != 0 && (getBishopAttacks(targetSq, occupied) & diagAttackers) != 0) {
            return true;
        }

        // 5. Orthogonale Angreifer (Turm & Dame)
        long orthoAttackers = board.getBitboard(PieceType.ROOK, attackerColor) | board.getBitboard(PieceType.QUEEN, attackerColor);
        if (orthoAttackers != 0 && (getRookAttacks(targetSq, occupied) & orthoAttackers) != 0) {
            return true;
        }

        return false;
    }

    /**
     * Prüft, ob der König der angegebenen Farbe im Schach steht.
     */
    public static boolean isKingInCheck(Board board, PieceColor kingColor) {
        int kingSq = board.getKingSquare(kingColor);
        if (kingSq < 0) return false;
        return isSquareAttacked(board, kingSq, kingColor.opposite());
    }

    /**
     * Ermittelt alle Figuren (Bitboard), die ein bestimmtes Feld angreifen.
     * Berücksichtigt auch das (künstliche) occupied-Bitboard für X-Ray Angriffe (SEE).
     */
    public static long getAttackers(Board board, int targetSq, long occupied) {
        long attackers = 0L;

        // Weiße Bauern (greifen von unten nach oben an)
        long whitePawns = board.getBitboard(PieceType.PAWN, PieceColor.WHITE);
        long bPawnAttacks = PAWN_ATTACKS[PieceColor.BLACK.ordinal()][targetSq];
        attackers |= (bPawnAttacks & whitePawns);

        // Schwarze Bauern (greifen von oben nach unten an)
        long blackPawns = board.getBitboard(PieceType.PAWN, PieceColor.BLACK);
        long wPawnAttacks = PAWN_ATTACKS[PieceColor.WHITE.ordinal()][targetSq];
        attackers |= (wPawnAttacks & blackPawns);

        // Springer
        long knights = board.getBitboard(PieceType.KNIGHT, PieceColor.WHITE) | board.getBitboard(PieceType.KNIGHT, PieceColor.BLACK);
        attackers |= (KNIGHT_ATTACKS[targetSq] & knights);

        // Könige
        long kings = board.getBitboard(PieceType.KING, PieceColor.WHITE) | board.getBitboard(PieceType.KING, PieceColor.BLACK);
        attackers |= (KING_ATTACKS[targetSq] & kings);

        // Läufer und Damen
        long diagAttackers = board.getBitboard(PieceType.BISHOP, PieceColor.WHITE) | board.getBitboard(PieceType.BISHOP, PieceColor.BLACK) |
                             board.getBitboard(PieceType.QUEEN, PieceColor.WHITE) | board.getBitboard(PieceType.QUEEN, PieceColor.BLACK);
        attackers |= (getBishopAttacks(targetSq, occupied) & diagAttackers);

        // Türme und Damen
        long orthoAttackers = board.getBitboard(PieceType.ROOK, PieceColor.WHITE) | board.getBitboard(PieceType.ROOK, PieceColor.BLACK) |
                              board.getBitboard(PieceType.QUEEN, PieceColor.WHITE) | board.getBitboard(PieceType.QUEEN, PieceColor.BLACK);
        attackers |= (getRookAttacks(targetSq, occupied) & orthoAttackers);

        return attackers;
    }
}
