package com.jchess.core.rules;

import com.jchess.core.board.BitboardHelper;
import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.board.Position;
import com.jchess.core.piece.Piece;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Erzeugt pseudo-legale und voll legale Züge für das Schachspiel.
 * Hochoptimiert mit 64-Bit-Bitboards und Magic Bitboards für maximale Perft- und Engine-Performance.
 * Beinhaltet vollständige Schachregeln:
 * - Normale Züge & Schlagzüge aller Figuren
 * - Bauern-Doppelschritt, Bauernumwandlung, En Passant
 * - Rochade (kurz & lang mit Schach-Durchzugs-Prüfung)
 * - Schach-, Matt- und Patt-Erkennung
 */
public final class MoveGenerator {

    private static final PieceType[] PROMOTION_PIECES = {
            PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN
    };

    private MoveGenerator() {}

    /**
     * Erzeugt alle voll legalen Züge für die aktuelle Partei.
     * Ein Zug ist legal, wenn der eigene König danach nicht im Schach steht.
     */
    public static List<Move> generateLegalMoves(Board board) {
        return generateLegalMovesForColor(board, board.getActivePlayer());
    }

    /**
     * Erzeugt alle legalen Züge für eine bestimmte Farbe.
     */
    public static List<Move> generateLegalMovesForColor(Board board, PieceColor color) {
        List<Move> pseudoLegalMoves = generatePseudoLegalMoves(board, color);
        List<Move> legalMoves = new ArrayList<>(pseudoLegalMoves.size());

        for (Move move : pseudoLegalMoves) {
            board.makeMove(move);
            boolean inCheck = isKingInCheck(board, color);
            board.undoMove();

            if (!inCheck) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    /**
     * Prüft effizient, ob mindestens ein legaler Zug existiert.
     * Bricht sofort beim ersten gefundenen legalen Zug ab — deutlich schneller
     * als generateLegalMoves() für Matt-/Patt-Erkennung in Perft und Engine.
     */
    public static boolean hasAnyLegalMove(Board board) {
        PieceColor color = board.getActivePlayer();
        List<Move> pseudoLegalMoves = generatePseudoLegalMoves(board, color);

        for (Move move : pseudoLegalMoves) {
            board.makeMove(move);
            boolean inCheck = isKingInCheck(board, color);
            board.undoMove();

            if (!inCheck) {
                return true;
            }
        }
        return false;
    }


    /**
     * Liefert alle legalen Züge, die von einem bestimmten Feld ausgehen.
     */
    public static List<Move> generateLegalMovesFromSquare(Board board, Position from) {
        Piece piece = board.getPieceAt(from);
        if (piece == null || piece.getColor() != board.getActivePlayer()) {
            return List.of();
        }

        List<Move> allLegal = generateLegalMoves(board);
        List<Move> fromMoves = new ArrayList<>();
        for (Move move : allLegal) {
            if (move.getFrom().equals(from)) {
                fromMoves.add(move);
            }
        }
        return fromMoves;
    }

    /**
     * Erzeugt alle pseudo-legalen Züge (ohne Rücksicht auf eigenes Schach) mittels Bitboards.
     */
    public static List<Move> generatePseudoLegalMoves(Board board, PieceColor color) {
        List<Move> moves = new ArrayList<>(64);
        long friendly = board.getColorBitboard(color);
        long enemy = board.getColorBitboard(color.opposite());
        long occupied = board.getOccupiedBitboard();

        // 1. Bauernzüge
        generatePawnMoves(board, color, enemy, occupied, moves);

        // 2. Springerzüge
        long knights = board.getBitboard(PieceType.KNIGHT, color);
        while (knights != 0) {
            int fromSq = Long.numberOfTrailingZeros(knights);
            Position from = Position.fromIndexFast(fromSq);
            Piece piece = board.getPieceAtIndex(fromSq);
            long dests = BitboardHelper.getKnightAttacks(fromSq) & ~friendly;
            while (dests != 0) {
                int toSq = Long.numberOfTrailingZeros(dests);
                Position to = Position.fromIndexFast(toSq);
                Piece target = board.getPieceAtIndex(toSq);
                moves.add(Move.normal(from, to, piece, target));
                dests &= dests - 1;
            }
            knights &= knights - 1;
        }

        // 3. Läuferzüge
        long bishops = board.getBitboard(PieceType.BISHOP, color);
        while (bishops != 0) {
            int fromSq = Long.numberOfTrailingZeros(bishops);
            Position from = Position.fromIndexFast(fromSq);
            Piece piece = board.getPieceAtIndex(fromSq);
            long dests = BitboardHelper.getBishopAttacks(fromSq, occupied) & ~friendly;
            while (dests != 0) {
                int toSq = Long.numberOfTrailingZeros(dests);
                Position to = Position.fromIndexFast(toSq);
                Piece target = board.getPieceAtIndex(toSq);
                moves.add(Move.normal(from, to, piece, target));
                dests &= dests - 1;
            }
            bishops &= bishops - 1;
        }

        // 4. Turmzüge
        long rooks = board.getBitboard(PieceType.ROOK, color);
        while (rooks != 0) {
            int fromSq = Long.numberOfTrailingZeros(rooks);
            Position from = Position.fromIndexFast(fromSq);
            Piece piece = board.getPieceAtIndex(fromSq);
            long dests = BitboardHelper.getRookAttacks(fromSq, occupied) & ~friendly;
            while (dests != 0) {
                int toSq = Long.numberOfTrailingZeros(dests);
                Position to = Position.fromIndexFast(toSq);
                Piece target = board.getPieceAtIndex(toSq);
                moves.add(Move.normal(from, to, piece, target));
                dests &= dests - 1;
            }
            rooks &= rooks - 1;
        }

        // 5. Damenzüge
        long queens = board.getBitboard(PieceType.QUEEN, color);
        while (queens != 0) {
            int fromSq = Long.numberOfTrailingZeros(queens);
            Position from = Position.fromIndexFast(fromSq);
            Piece piece = board.getPieceAtIndex(fromSq);
            long dests = BitboardHelper.getQueenAttacks(fromSq, occupied) & ~friendly;
            while (dests != 0) {
                int toSq = Long.numberOfTrailingZeros(dests);
                Position to = Position.fromIndexFast(toSq);
                Piece target = board.getPieceAtIndex(toSq);
                moves.add(Move.normal(from, to, piece, target));
                dests &= dests - 1;
            }
            queens &= queens - 1;
        }

        // 6. Königszüge & Rochade
        int kingSq = board.getKingSquare(color);
        if (kingSq >= 0) {
            Position from = Position.fromIndexFast(kingSq);
            Piece king = board.getPieceAtIndex(kingSq);
            long dests = BitboardHelper.getKingAttacks(kingSq) & ~friendly;
            while (dests != 0) {
                int toSq = Long.numberOfTrailingZeros(dests);
                Position to = Position.fromIndexFast(toSq);
                Piece target = board.getPieceAtIndex(toSq);
                moves.add(Move.normal(from, to, king, target));
                dests &= dests - 1;
            }
            generateCastlingMoves(board, kingSq, king, moves);
        }

        return moves;
    }

    private static void generatePawnMoves(Board board, PieceColor color, long enemy, long occupied, List<Move> moves) {
        long pawns = board.getBitboard(PieceType.PAWN, color);
        int dir = color.getPawnDirection();
        int promoRank = color.getPromotionRank();
        int startRank = color.getPawnStartRank();
        Position epTarget = board.getEnPassantTarget();
        int epSq = (epTarget != null) ? epTarget.getIndex() : -1;

        while (pawns != 0) {
            int fromSq = Long.numberOfTrailingZeros(pawns);
            Position from = Position.fromIndexFast(fromSq);
            Piece pawn = board.getPieceAtIndex(fromSq);
            int rank = fromSq / 8;

            // 1. Einfacher Vorwärtsschritt
            int oneStepSq = fromSq + dir * 8;
            if (oneStepSq >= 0 && oneStepSq < 64 && (occupied & (1L << oneStepSq)) == 0) {
                Position oneStep = Position.fromIndexFast(oneStepSq);
                if (oneStep.getRank() == promoRank) {
                    for (PieceType promo : PROMOTION_PIECES) {
                        moves.add(Move.promotion(from, oneStep, pawn, null, promo));
                    }
                } else {
                    moves.add(Move.normal(from, oneStep, pawn, null));

                    // 2. Doppelschritt
                    if (rank == startRank) {
                        int twoStepSq = fromSq + 2 * dir * 8;
                        if ((occupied & (1L << twoStepSq)) == 0) {
                            moves.add(Move.doublePawnPush(from, Position.fromIndexFast(twoStepSq), pawn));
                        }
                    }
                }
            }

            // 3. Diagonale Schlagzüge
            long attackDests = BitboardHelper.getPawnAttacks(fromSq, color) & enemy;
            while (attackDests != 0) {
                int toSq = Long.numberOfTrailingZeros(attackDests);
                Position to = Position.fromIndexFast(toSq);
                Piece targetPiece = board.getPieceAtIndex(toSq);
                if (to.getRank() == promoRank) {
                    for (PieceType promo : PROMOTION_PIECES) {
                        moves.add(Move.promotion(from, to, pawn, targetPiece, promo));
                    }
                } else {
                    moves.add(Move.normal(from, to, pawn, targetPiece));
                }
                attackDests &= attackDests - 1;
            }

            // 4. En Passant
            if (epSq >= 0 && (BitboardHelper.getPawnAttacks(fromSq, color) & (1L << epSq)) != 0) {
                Position to = epTarget;
                Position capturedPawnPos = Position.of(to.getFile(), from.getRank());
                Piece capturedPawn = board.getPieceAt(capturedPawnPos);
                if (capturedPawn != null && capturedPawn.getColor() != color) {
                    moves.add(Move.enPassant(from, to, pawn, capturedPawn));
                }
            }

            pawns &= pawns - 1;
        }
    }

    private static void generateCastlingMoves(Board board, int kingSq, Piece king, List<Move> moves) {
        PieceColor color = king.getColor();
        PieceColor enemy = color.opposite();
        int rank = (color == PieceColor.WHITE) ? 0 : 7;
        int expectedKingSq = rank * 8 + 4;

        // König muss auf e1 (4) bzw. e8 (60) stehen
        if (kingSq != expectedKingSq) {
            return;
        }

        // König darf nicht aktuell im Schach stehen
        if (BitboardHelper.isSquareAttacked(board, kingSq, enemy)) {
            return;
        }

        int rights = board.getCastlingRights();
        long occupied = board.getOccupiedBitboard();

        // Kurze Rochade (Kingside: O-O nach g1/g8)
        int ksMask = (color == PieceColor.WHITE) ? Board.CASTLE_WHITE_KINGSIDE : Board.CASTLE_BLACK_KINGSIDE;
        if ((rights & ksMask) != 0) {
            int fSq = rank * 8 + 5;
            int gSq = rank * 8 + 6;
            long betweenMask = (1L << fSq) | (1L << gSq);
            if ((occupied & betweenMask) == 0) {
                if (!BitboardHelper.isSquareAttacked(board, fSq, enemy) &&
                    !BitboardHelper.isSquareAttacked(board, gSq, enemy)) {
                    moves.add(Move.castle(Position.fromIndexFast(kingSq), Position.fromIndexFast(gSq), king));
                }
            }
        }

        // Lange Rochade (Queenside: O-O-O nach c1/c8)
        int qsMask = (color == PieceColor.WHITE) ? Board.CASTLE_WHITE_QUEENSIDE : Board.CASTLE_BLACK_QUEENSIDE;
        if ((rights & qsMask) != 0) {
            int bSq = rank * 8 + 1;
            int cSq = rank * 8 + 2;
            int dSq = rank * 8 + 3;
            long betweenMask = (1L << bSq) | (1L << cSq) | (1L << dSq);
            if ((occupied & betweenMask) == 0) {
                if (!BitboardHelper.isSquareAttacked(board, dSq, enemy) &&
                    !BitboardHelper.isSquareAttacked(board, cSq, enemy)) {
                    moves.add(Move.castle(Position.fromIndexFast(kingSq), Position.fromIndexFast(cSq), king));
                }
            }
        }
    }

    /**
     * Prüft in O(1) mit Bitboards, ob ein Feld von einer gegnerischen Farbe angegriffen wird.
     */
    public static boolean isSquareAttacked(Board board, Position target, PieceColor attackerColor) {
        if (target == null) return false;
        return BitboardHelper.isSquareAttacked(board, target.getIndex(), attackerColor);
    }

    /**
     * Prüft in O(1) mit Bitboards, ob der König der angegebenen Farbe im Schach steht.
     */
    public static boolean isKingInCheck(Board board, PieceColor kingColor) {
        return BitboardHelper.isKingInCheck(board, kingColor);
    }

    /**
     * Prüft, ob ungenügendes Material für ein Schachmatt vorliegt.
     */
    public static boolean hasInsufficientMaterial(Board board) {
        return board != null && board.hasInsufficientMaterial();
    }

    /**
     * Ermittelt den aktuellen Status des Spiels (Matt, Patt, ungenügend Material, 3-fache Wiederholung, 50-Züge-Regel, Schach, Aktiv).
     */
    public static GameStatus evaluateGameStatus(Board board) {
        List<Move> legalMoves = generateLegalMoves(board);
        boolean inCheck = isKingInCheck(board, board.getActivePlayer());

        if (legalMoves.isEmpty()) {
            return inCheck ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        }

        // 1. Ungenügendes Material (K vs. K, K+N vs. K, K+B vs. K, K+N vs. K+N, etc.)
        if (board.hasInsufficientMaterial()) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL;
        }

        // 2. Dreifache Stellungswiederholung (3-Peat Repetition)
        if (board.isThreefoldRepetition()) {
            return GameStatus.DRAW_THREEFOLD_REPETITION;
        }

        // 3. 50-Züge-Regel (100 Halbzüge ohne Bauernzug oder Schlagfall)
        if (board.getHalfmoveClock() >= 100) {
            return GameStatus.DRAW_FIFTY_MOVES;
        }

        if (inCheck) {
            return GameStatus.CHECK;
        }

        return GameStatus.ACTIVE;
    }
}
