package com.jchess.core.rules;

import com.jchess.core.board.Board;
import com.jchess.core.piece.PieceColor;
import engine.bots.CompactMove;

/**
 * Performance Testing (Perft) für die Schach-Engine.
 * Traversiert den vollständigen Spielbaum bis zu einer bestimmten Tiefe,
 * um die Korrektheit des MoveGenerators (Rochade, En Passant, Promotions, Schach, Matt)
 * sowie die Zuggüte und Performance (Nodes pro Sekunde) exakt zu verifizieren.
 *
 * Optimiert mit Zero-Allocation CompactMoves (16-bit) und Pseudo-Legal-Traversierung.
 */
public final class Perft {

    public static class Result {
        public int depth;
        public long nodes;
        public long captures;
        public long enpassants;
        public long castles;
        public long promotions;
        public long checks;
        public long mates;
        public long elapsedMillis;

        public void add(Result other) {
            this.nodes += other.nodes;
            this.captures += other.captures;
            this.enpassants += other.enpassants;
            this.castles += other.castles;
            this.promotions += other.promotions;
            this.checks += other.checks;
            this.mates += other.mates;
        }

        public double getNodesPerSecond() {
            if (elapsedMillis <= 0) return 0.0;
            return (nodes * 1000.0) / elapsedMillis;
        }
    }

    private Perft() {}

    /**
     * Führt eine vollständige Perft-Berechnung mit Metrik-Zählung für die angegebene Tiefe durch.
     */
    public static Result perft(Board board, int depth) {
        long startTime = System.currentTimeMillis();
        Result result = perftRecursive(board, depth);
        result.depth = depth;
        result.elapsedMillis = Math.max(1, System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Kernalgorithmus: Traversiert mit Zero-Allocation CompactMoves (short-Arrays).
     */
    private static Result perftRecursive(Board board, int depth) {
        if (depth == 0) {
            Result res = new Result();
            res.nodes = 1;
            return res;
        }

        Result total = new Result();
        PieceColor sideToMove = board.getActivePlayer();
        
        short[] pseudoMoves = new short[256];
        int numMoves = MoveGenerator.generatePseudoLegalCompactMoves(board, sideToMove, pseudoMoves, 0);

        if (depth == 1) {
            // Depth-1 Bulk-Counting mit integrierter Legalitätsprüfung
            for (int i = 0; i < numMoves; i++) {
                short move = pseudoMoves[i];
                int flag = CompactMove.getMoveFlag(move);
                int targetSq = CompactMove.getTargetSquare(move);
                
                boolean isEnPassant = (flag == CompactMove.EnPassantCaptureFlag);
                boolean isCastle = (flag == CompactMove.CastleFlag);
                boolean isPromotion = (flag >= CompactMove.PromoteToQueenFlag);
                boolean isCapture = isEnPassant || (board.getPieceAtIndex(targetSq) != null);

                board.makeMove(move);

                // Legalitätsprüfung: Steht unser eigener König im Schach?
                if (MoveGenerator.isKingInCheck(board, sideToMove)) {
                    board.undoMove();
                    continue; // Illegaler Zug — überspringen
                }

                // Legaler Zug — Metriken zählen
                total.nodes++;

                if (isCapture) {
                    total.captures++;
                }
                if (isEnPassant) {
                    total.enpassants++;
                }
                if (isCastle) {
                    total.castles++;
                }
                if (isPromotion) {
                    total.promotions++;
                }

                // Check/Mate-Erkennung: Steht der GEGNER jetzt im Schach?
                PieceColor opponent = board.getActivePlayer();
                boolean opponentInCheck = MoveGenerator.isKingInCheck(board, opponent);
                if (opponentInCheck) {
                    short[] oppMoves = new short[256];
                    int oppCount = MoveGenerator.generatePseudoLegalCompactMoves(board, opponent, oppMoves, 0);
                    boolean hasLegalMove = false;
                    for (int j = 0; j < oppCount; j++) {
                        board.makeMove(oppMoves[j]);
                        if (!MoveGenerator.isKingInCheck(board, opponent)) {
                            hasLegalMove = true;
                            board.undoMove();
                            break;
                        }
                        board.undoMove();
                    }

                    if (!hasLegalMove) {
                        total.mates++;
                    } else {
                        total.checks++;
                    }
                }

                board.undoMove();
            }
            return total;
        }

        // Tiefe > 1: Pseudo-legal traversieren, Legalität inline prüfen
        for (int i = 0; i < numMoves; i++) {
            short move = pseudoMoves[i];
            board.makeMove(move);

            // Legalitätsprüfung: Steht unser eigener König im Schach?
            if (MoveGenerator.isKingInCheck(board, sideToMove)) {
                board.undoMove();
                continue; // Illegaler Zug — überspringen
            }

            Result sub = perftRecursive(board, depth - 1);
            total.add(sub);
            board.undoMove();
        }

        return total;
    }

    /**
     * Schneller Perft (nur Leaf-Node Count) für maximale Geschwindigkeit.
     * Zero-Allocation mit CompactMoves.
     */
    public static long perftNodesOnly(Board board, int depth) {
        if (depth == 0) return 1;

        PieceColor sideToMove = board.getActivePlayer();
        short[] pseudoMoves = new short[256];
        int numMoves = MoveGenerator.generatePseudoLegalCompactMoves(board, sideToMove, pseudoMoves, 0);

        if (depth == 1) {
            // Bulk-Count: Nur legale Züge zählen
            int count = 0;
            for (int i = 0; i < numMoves; i++) {
                short move = pseudoMoves[i];
                board.makeMove(move);
                if (!MoveGenerator.isKingInCheck(board, sideToMove)) {
                    count++;
                }
                board.undoMove();
            }
            return count;
        }

        long nodes = 0;
        for (int i = 0; i < numMoves; i++) {
            short move = pseudoMoves[i];
            board.makeMove(move);
            if (!MoveGenerator.isKingInCheck(board, sideToMove)) {
                nodes += perftNodesOnly(board, depth - 1);
            }
            board.undoMove();
        }
        return nodes;
    }
}
