package com.jchess.core.evaluation;

import com.jchess.core.board.Board;
import com.jchess.core.board.Position;
import com.jchess.core.piece.Piece;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;
import com.jchess.core.rules.GameStatus;
import com.jchess.core.rules.MoveGenerator;

import java.util.EnumMap;
import java.util.Map;

/**
 * Stellungsbewertung für JChess:
 * - Ermittelt den Stellungswert aus Sicht von Weiß in Bauerneinheiten (z.B. +1.20 = Weiß führt mit 1,2 Bauern).
 * - Berücksichtigt Materialwerte und positionelle Figuren-Feld-Tabellen (Piece-Square Tables).
 * - Erkennt Matt und Patt.
 * - Berechnet Gewinnwahrscheinlichkeiten für die visuelle Eval-Bar.
 */
public final class PositionEvaluator {

    public static final double MATE_SCORE = 1000.0;

    // Piece-Square Tables (PST) in Zentibauern (aus Sicht von Weiß von Rang 1 bis Rang 8)
    private static final int[] PAWN_PST = {
             0,  0,  0,  0,  0,  0,  0,  0,
             5, 10, 10,-20,-20, 10, 10,  5,
             5, -5,-10,  0,  0,-10, -5,  5,
             0,  0,  0, 20, 20,  0,  0,  0,
             5,  5, 10, 25, 25, 10,  5,  5,
            10, 10, 20, 30, 30, 20, 10, 10,
            50, 50, 50, 50, 50, 50, 50, 50,
             0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_PST = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] BISHOP_PST = {
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] ROOK_PST = {
              0,  0,  0,  5,  5,  0,  0,  0,
             -5,  0,  0,  0,  0,  0,  0, -5,
             -5,  0,  0,  0,  0,  0,  0, -5,
             -5,  0,  0,  0,  0,  0,  0, -5,
             -5,  0,  0,  0,  0,  0,  0, -5,
             -5,  0,  0,  0,  0,  0,  0, -5,
              5, 10, 10, 10, 10, 10, 10,  5,
              0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] QUEEN_PST = {
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -10,  5,  5,  5,  5,  5,  0,-10,
              0,  0,  5,  5,  5,  5,  0, -5,
             -5,  0,  5,  5,  5,  5,  0, -5,
            -10,  0,  5,  5,  5,  5,  0,-10,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
    };

    private static final int[] KING_MIDDLE_PST = {
             20, 30, 10,  0,  0, 10, 30, 20,
             20, 20,  0,  0,  0,  0, 20, 20,
            -10,-20,-20,-20,-20,-20,-20,-10,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30
    };

    private PositionEvaluator() {}

    private static final engine.bots.JChessV1 staticJChess = new engine.bots.JChessV1(1);

    /**
     * Berechnet die Stellungsbewertung in Bauerneinheiten aus Sicht von Weiß.
     * Positiver Wert: Weiß im Vorteil.
     * Negativer Wert: Schwarz im Vorteil.
     */
    public static double evaluate(Board board) {
        GameStatus status = MoveGenerator.evaluateGameStatus(board);

        if (status == GameStatus.CHECKMATE) {
            return board.getActivePlayer() == PieceColor.WHITE ? -MATE_SCORE : MATE_SCORE;
        }

        if (status.isGameOver()) {
            return 0.0; // Patt oder Remis
        }

        // Nutze das starke JChessV1 Modell für die Evaluierung
        // Die evaluate() Methode gibt Centipawns aus Sicht des übergebenen Spielers zurück.
        double centipawns = staticJChess.evaluate(board, PieceColor.WHITE);
        
        // Umrechnen in Bauerneinheiten (z.B. +150 cp -> +1.50)
        return centipawns / 100.0;
    }

    private static int getPstBonus(PieceType type, PieceColor color, int file, int rank) {
        int index;
        if (color == PieceColor.WHITE) {
            index = rank * 8 + file;
        } else {
            // Für Schwarz das Brett spiegeln
            index = (7 - rank) * 8 + file;
        }

        return switch (type) {
            case PAWN -> PAWN_PST[index];
            case KNIGHT -> KNIGHT_PST[index];
            case BISHOP -> BISHOP_PST[index];
            case ROOK -> ROOK_PST[index];
            case QUEEN -> QUEEN_PST[index];
            case KING -> KING_MIDDLE_PST[index];
        };
    }

    /**
     * Ermittelt die Gewinnwahrscheinlichkeit für Weiß zwischen 0.0 (Schwarz gewinnt)
     * und 1.0 (Weiß gewinnt). 0.5 bedeutet völlig ausgeglichen.
     */
    public static double getWinProbabilityWhite(double evaluation) {
        if (evaluation >= MATE_SCORE - 10) return 1.0;
        if (evaluation <= -MATE_SCORE + 10) return 0.0;

        // Sigmoid-Kurve nach Lichess / Chess.com Standard
        return 1.0 / (1.0 + Math.pow(10.0, -evaluation / 4.0));
    }

    /**
     * Formatiert den Wert für die Anzeige (z.B. "+0.35", "-1.50", "M" bei Matt).
     */
    public static String formatScore(double score, GameStatus status) {
        if (status == GameStatus.CHECKMATE) {
            return score > 0 ? "+M" : "-M";
        }
        if (score >= MATE_SCORE - 50) {
            int m = Math.max(1, (int) Math.round(MATE_SCORE - score));
            return "+M" + m;
        }
        if (score <= -MATE_SCORE + 50) {
            int m = Math.max(1, (int) Math.round(MATE_SCORE + score));
            return "-M" + m;
        }
        if (Math.abs(score) < 0.05) {
            return "0.0";
        }
        return String.format(java.util.Locale.US, "%+.1f", score);
    }

    /**
     * Liefert eine kurze deutsche textuelle Einschätzung der Stellung.
     */
    public static String getAdvantageText(double score, GameStatus status) {
        if (status == GameStatus.CHECKMATE) {
            return score > 0 ? "Schachmatt! Weiß gewinnt" : "Schachmatt! Schwarz gewinnt";
        }
        if (status == GameStatus.STALEMATE) {
            return "Remis durch Patt";
        }
        if (status == GameStatus.DRAW_INSUFFICIENT_MATERIAL) {
            return "Remis (Ungenügendes Material)";
        }
        if (status == GameStatus.DRAW_THREEFOLD_REPETITION) {
            return "Remis (3-fache Stellungswiederholung)";
        }
        if (status == GameStatus.DRAW_FIFTY_MOVES) {
            return "Remis (50-Züge-Regel)";
        }

        if (Math.abs(score) < 0.3) {
            return "Ausgeglichen";
        } else if (score >= 3.0) {
            return "Entscheidender Vorteil Weiß";
        } else if (score >= 1.2) {
            return "Klarer Vorteil Weiß";
        } else if (score > 0) {
            return "Leichter Vorteil Weiß";
        } else if (score <= -3.0) {
            return "Entscheidender Vorteil Schwarz";
        } else if (score <= -1.2) {
            return "Klarer Vorteil Schwarz";
        } else {
            return "Leichter Vorteil Schwarz";
        }
    }

    /**
     * Ermittelt die Materialzählung beider Farben (ohne Positionswerte).
     */
    public static MaterialBalance getMaterialBalance(Board board) {
        Map<PieceType, Integer> whitePieces = new EnumMap<>(PieceType.class);
        Map<PieceType, Integer> blackPieces = new EnumMap<>(PieceType.class);
        int whiteMat = 0;
        int blackMat = 0;

        for (int i = 0; i < 64; i++) {
            Piece piece = board.getPieceAt(Position.fromIndex(i));
            if (piece == null || piece.getType() == PieceType.KING) continue;

            if (piece.getColor() == PieceColor.WHITE) {
                whitePieces.merge(piece.getType(), 1, Integer::sum);
                whiteMat += piece.getType().getBaseValue();
            } else {
                blackPieces.merge(piece.getType(), 1, Integer::sum);
                blackMat += piece.getType().getBaseValue();
            }
        }

        return new MaterialBalance(whitePieces, blackPieces, (whiteMat - blackMat) / 100);
    }

    public record MaterialBalance(
            Map<PieceType, Integer> whitePieces,
            Map<PieceType, Integer> blackPieces,
            int materialDifference
    ) {
        public String getDifferenceLabel() {
            if (materialDifference == 0) return "Material: Gleich";
            if (materialDifference > 0) return "Material: Weiß +" + materialDifference;
            return "Material: Schwarz +" + (-materialDifference);
        }
    }
}
