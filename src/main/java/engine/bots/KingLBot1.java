package engine.bots;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.board.Position;
import com.jchess.core.clock.ChessClock;
import com.jchess.core.engine.ChessEngine;
import com.jchess.core.piece.Piece;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;
import com.jchess.core.rules.MoveGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * KingLBot1 - Intelligente Schach-Engine für JChess, basierend auf der JS_ONE Engine-Architektur.
 *
 * Enthaltene Schlüsselkonzepte aus JS_ONE:
 * 1. Iterative Deepening: Beginnend bei Tiefe 1 schrittweise Vertiefung bis maxDepth oder Zeitablauf.
 * 2. Quiescence Search (qsearch): Taktische Ruhesuche für Schlagzüge und Umwandlungen bei depth <= 0.
 * 3. Check Extensions: Verlängerung der Suchtiefe bei Schachgebot um 1 Ply (bis zu 16 Extensions).
 * 4. Stellungsbewertung (PST & Endgame Interpolation):
 *    - JS_ONE Materialwerte (P=100, N=300, B=320, R=500, Q=900, K=10000).
 *    - Exakte Piece-Square-Tabellen für alle Figurenarten.
 *    - Dynamische Endspiel-Interpolation für Bauern (pawns_end) und König (king_end)
 *      basierend auf der Anzahl der Schwer- und Leichtfiguren (factor = (16 - totalPieces) / 16).
 * 5. Move Ordering:
 *    - Bester Zug aus vorheriger Tiefe (Root-PV) an Position 1.
 *    - Schlagzüge sortiert nach MVV-LVA (Most Valuable Victim - Least Valuable Attacker).
 *    - Umwandlungen und Rochaden bevorzugt vor stillen Zügen.
 * 6. Dynamisches Zeitmanagement: Unterstützt Schachuhr (ChessClock) und feste Suchtiefen.
 *
 * Ort: /engine/bots/KingLBot1.java
 */
public class KingLBot1 implements ChessEngine {

    // --- Piece-Square Tables aus JS_ONE (aus Sicht von Weiß von Rang 1 bis 8, Index = rank * 8 + file) ---

    // PAWNS (Mittelspiel)
    private static final int[] PAWNS = {
         0,   0,   0,   0,   0,   0,   0,   0,
         5,  10,  10, -20, -20,  10,  10,   5,
         5,  -5, -10,   0,   0, -10,  -5,   5,
         0,   0,   0,  20,  20,   0,   0,   0,
         5,   5,  10,  25,  25,  10,   5,   5,
        10,  10,  20,  30,  30,  20,  10,  10,
        50,  50,  50,  50,  50,  50,  50,  50,
         0,   0,   0,   0,   0,   0,   0,   0
    };

    // PAWNS (Endspiel)
    private static final int[] PAWNS_END = {
         0,   0,   0,   0,   0,   0,   0,   0,
        10,  10,  10,  10,  10,  10,  10,  10,
        10,  10,  10,  10,  10,  10,  10,  10,
        20,  20,  20,  20,  20,  20,  20,  20,
        30,  30,  30,  30,  30,  30,  30,  30,
        50,  50,  50,  50,  50,  50,  50,  50,
        80,  80,  80,  80,  80,  80,  80,  80,
         0,   0,   0,   0,   0,   0,   0,   0
    };

    // KNIGHTS
    private static final int[] KNIGHTS = {
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20,   0,   5,   5,   0, -20, -40,
        -30,   5,  10,  15,  15,  10,   5, -30,
        -30,   0,  15,  20,  20,  15,   0, -30,
        -30,   5,  15,  20,  20,  15,   5, -30,
        -30,   0,  10,  15,  15,  10,   0, -30,
        -40, -20,   0,   0,   0,   0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50
    };

    // BISHOPS
    private static final int[] BISHOPS = {
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10,   5,   0,   0,   0,   0,   5, -10,
        -10,  10,  10,  10,  10,  10,  10, -10,
        -10,   0,  10,  10,  10,  10,   0, -10,
        -10,   5,   5,  10,  10,   5,   5, -10,
        -10,   0,   5,  10,  10,   5,   0, -10,
        -10,   0,   0,   0,   0,   0,   0, -10,
        -20, -10, -10, -10, -10, -10, -10, -20
    };

    // ROOKS
    private static final int[] ROOKS = {
          0,   0,   0,  10,  10,  10,   0,   0,
          5,  10,  10,  10,  10,  10,  10,   5,
         -5,   0,   0,   0,   0,   0,   0,  -5,
         -5,   0,   0,   0,   0,   0,   0,  -5,
         -5,   0,   0,   0,   0,   0,   0,  -5,
         -5,   0,   0,   0,   0,   0,   0,  -5,
          5,  15,  15,  15,  15,  15,  15,   5,
          0,   0,   0,   5,   5,   0,   0,   0
    };

    // QUEENS
    private static final int[] QUEENS = {
        -20, -10, -10,  -5,  -5, -10, -10, -20,
        -10,   0,   5,   0,   0,   0,   0, -10,
        -10,   5,   5,   5,   5,   5,   0, -10,
          0,   0,   5,   5,   5,   5,   0,  -5,
         -5,   0,   5,   5,   5,   5,   0,  -5,
        -10,   0,   5,   5,   5,   5,   0, -10,
        -10,   0,   0,   0,   0,   0,   0, -10,
        -20, -10, -10,  -5,  -5, -10, -10, -20
    };

    // KING (Mittelspiel)
    private static final int[] KING = {
         20,  30,  10,   0,   0,  10,  30,  20,
         20,  20,  -5,  -5,  -5,  -5,  20,  20,
        -10, -20, -20, -20, -20, -20, -20, -10,
        -20, -30, -30, -40, -40, -30, -30, -20,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -40, -50, -50, -60, -60, -50, -50, -40,
        -60, -60, -60, -60, -60, -60, -60, -60,
        -80, -70, -70, -70, -70, -70, -70, -80
    };

    // KING (Endspiel)
    private static final int[] KING_END = {
        -50, -30, -30, -30, -30, -30, -30, -50,
        -30, -25,   0,   0,   0,   0, -25, -30,
        -25, -20,  20,  25,  25,  20, -20, -25,
        -20, -15,  30,  40,  40,  30, -15, -20,
        -15, -10,  35,  45,  45,  35, -10, -15,
        -10,  -5,  20,  30,  30,  20,  -5, -10,
         -5,   0,   5,   5,   5,   5,   0,  -5,
        -20, -10, -10, -10, -10, -10, -10, -20
    };

    private static final double MATE_SCORE = 30000.0;

    private final String name;
    private int searchDepth;
    private boolean useFixedDepth;
    private long defaultTimeLimitMs = 1000; // Standardmäßig 5 Sekunden Denkfenster

    // Diagnose- & Performancemetriken
    private long lastNodesEvaluated = 0;
    private int lastDepthReached = 0;
    private long lastSearchTimeMs = 0;
    private long lastNps = 0;

    private double remainingTimePercentage = 0.05; // Standard: 5% der verbleibenden Restzeit

    // Steuerungszustand für Iterative Deepening
    private volatile boolean timeExceeded = false;
    private volatile boolean stopRequested = false;
    private Move iterationBestMove = null;

    // Transposition Table mit Zobrist-Hashing
    private final TranspositionTable transpositionTable = new TranspositionTable();

    // Killer Moves (2 pro Ply, bis max 100 Plies)
    private final Move[][] killerMoves = new Move[100][2];

    @FunctionalInterface
    public interface DepthListener {
        void onDepthCompleted(int depth, double score, Move bestMove, long nodes, long elapsedMs);
    }

    public void stopSearch() {
        this.stopRequested = true;
        this.timeExceeded = true;
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    /**
     * Erstellt KingLBot1 mit Standardeinstellungen (5s Bedenkzeit / 5% Restzeit mit Iterative Deepening).
     */
    public KingLBot1() {
        this("KingLBot1", 64, false, 5000, 0.05);
    }

    /**
     * Erstellt KingLBot1 mit einem individuellen Denkfenster in Millisekunden (z.B. 5000 für 5s).
     */
    public KingLBot1(long defaultTimeLimitMs) {
        this("KingLBot1 (" + (defaultTimeLimitMs / 1000) + "s)", 64, false, defaultTimeLimitMs, 0.05);
    }

    public KingLBot1(double remainingTimePercentage) {
        this("KingLBot1 (" + Math.round(remainingTimePercentage * 100) + "%)", 64, false, 5000, remainingTimePercentage);
    }

    public KingLBot1(long defaultTimeLimitMs, double remainingTimePercentage) {
        this("KingLBot1", 64, false, defaultTimeLimitMs, remainingTimePercentage);
    }

    public KingLBot1(String name, long defaultTimeLimitMs, double remainingTimePercentage) {
        this(name, 64, false, defaultTimeLimitMs, remainingTimePercentage);
    }

    /**
     * Erstellt KingLBot1 mit einer festen maximalen Suchtiefe (z.B. 3 oder 4).
     */
    public KingLBot1(int searchDepth) {
        this("KingLBot1", searchDepth, true, 5000, 0.05);
    }

    /**
     * Erstellt KingLBot1 mit individuellem Namen und Suchtiefe.
     */
    public KingLBot1(String name, int searchDepth) {
        this(name, searchDepth, true, 5000, 0.05);
    }

    public KingLBot1(String name, int searchDepth, boolean useFixedDepth) {
        this(name, searchDepth, useFixedDepth, 5000, 0.05);
    }

    public KingLBot1(String name, int searchDepth, boolean useFixedDepth, long defaultTimeLimitMs) {
        this(name, searchDepth, useFixedDepth, defaultTimeLimitMs, 0.05);
    }

    public KingLBot1(String name, int searchDepth, boolean useFixedDepth, long defaultTimeLimitMs, double remainingTimePercentage) {
        this.name = name;
        this.searchDepth = Math.max(1, searchDepth);
        this.useFixedDepth = useFixedDepth;
        this.defaultTimeLimitMs = Math.max(50, defaultTimeLimitMs);
        this.remainingTimePercentage = Math.max(0.001, Math.min(0.50, remainingTimePercentage));
    }

    @Override
    public String getName() {
        return name;
    }

    public double getRemainingTimePercentage() {
        return remainingTimePercentage;
    }

    public void setRemainingTimePercentage(double remainingTimePercentage) {
        this.remainingTimePercentage = Math.max(0.001, Math.min(0.50, remainingTimePercentage));
    }

    public int getSearchDepth() {
        return searchDepth;
    }

    public void setSearchDepth(int searchDepth) {
        this.searchDepth = Math.max(1, searchDepth);
        this.useFixedDepth = true;
    }

    public boolean isUseFixedDepth() {
        return useFixedDepth;
    }

    public void setUseFixedDepth(boolean useFixedDepth) {
        this.useFixedDepth = useFixedDepth;
    }

    public long getDefaultTimeLimitMs() {
        return defaultTimeLimitMs;
    }

    public void setDefaultTimeLimitMs(long defaultTimeLimitMs) {
        this.defaultTimeLimitMs = Math.max(50, defaultTimeLimitMs);
    }

    public long getLastNodesEvaluated() {
        return lastNodesEvaluated;
    }

    public int getLastDepthReached() {
        return lastDepthReached;
    }

    public long getLastSearchTimeMs() {
        return lastSearchTimeMs;
    }

    public long getLastNps() {
        return lastNps;
    }

    @Override
    public Move findBestMove(Board board, PieceColor color) {
        return findBestMoveWithListener(board, color, null, null);
    }

    @Override
    public Move findBestMove(Board board, PieceColor color, ChessClock clock) {
        return findBestMoveWithListener(board, color, clock, null);
    }

    public Move findBestMoveWithListener(Board board, PieceColor color, DepthListener listener) {
        return findBestMoveWithListener(board, color, null, listener);
    }

    public Move findBestMoveWithListener(Board board, PieceColor color, ChessClock clock, DepthListener listener) {
        List<Move> legalMoves = MoveGenerator.generateLegalMovesForColor(board, color);
        if (legalMoves.isEmpty()) {
            return null; // Matt oder Patt
        }
        if (legalMoves.size() == 1) {
            Move singleMove = legalMoves.get(0);
            if (listener != null) {
                listener.onDepthCompleted(1, 0.0, singleMove, 1, 0);
            }
            return singleMove;
        }

        // Bedenkzeit berechnen:
        long allocatedTimeMs = defaultTimeLimitMs;
        if (clock != null && clock.isEnabled()) {
            long remainingMs = clock.getRemainingTimeMillis(color);
            long incMs = clock.getIncrementMillis();
            if (remainingMs > 0) {
                // Dynamische Bedenkzeit: Prozentsatz der Restzeit (z.B. 5%) plus 70% des Inkrements
                long safetyMargin = remainingMs > 1000 ? 100 : 20;
                long safetyTime = Math.max(15, remainingMs - safetyMargin);
                long clockBudget = Math.max(50, (long) (remainingMs * remainingTimePercentage) + (incMs * 7) / 10);
                allocatedTimeMs = Math.min(clockBudget, safetyTime);
            }
        }

        long startTime = System.currentTimeMillis();
        long deadline = useFixedDepth && (clock == null || !clock.isEnabled())
                ? Long.MAX_VALUE
                : startTime + allocatedTimeMs;

        lastNodesEvaluated = 0;
        lastDepthReached = 0;
        timeExceeded = false;
        stopRequested = false;
        for (int i = 0; i < killerMoves.length; i++) {
            killerMoves[i][0] = null;
            killerMoves[i][1] = null;
        }

        Move bestMoveOverall = legalMoves.get(0);
        double bestScoreOverall = -MATE_SCORE;

        int targetMaxDepth = useFixedDepth ? searchDepth : Math.min(searchDepth, 64);

        // --- Iterative Deepening Schleife ---
        for (int currentDepth = 1; currentDepth <= targetMaxDepth; currentDepth++) {
            if (stopRequested || Thread.currentThread().isInterrupted()) {
                break;
            }

            iterationBestMove = null;

            double score = searchRoot(board, currentDepth, color, deadline, bestMoveOverall);

            // Falls die Suche abgebrochen wurde, bevor dieser Zug vollständig war
            if ((timeExceeded || stopRequested) && iterationBestMove == null) {
                break;
            }

            if (iterationBestMove != null) {
                bestMoveOverall = iterationBestMove;
                bestScoreOverall = score;
                lastDepthReached = currentDepth;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            long nps = elapsed > 0 ? (lastNodesEvaluated * 1000L) / elapsed : 0;
            String evalStr = formatScore(bestScoreOverall);
            String moveStr = bestMoveOverall != null ? bestMoveOverall.toSimpleNotation() : "-";
            long ttHits = transpositionTable.getHits();
            // System.out.printf("%s: Tiefe %2d | Eval: %7s | Bester Zug: %-6s | Zeit: %5d ms | Knoten: %8d | NPS: %,d | TT-Treffer: %,d%n",
            //        name, currentDepth, evalStr, moveStr, elapsed, lastNodesEvaluated, nps, ttHits);

            // Listener über abgeschlossene Tiefe informieren
            if (listener != null && iterationBestMove != null) {
                listener.onDepthCompleted(currentDepth, bestScoreOverall, bestMoveOverall, lastNodesEvaluated, elapsed);
            }

            // Matt gefunden (Gewinn oder unvermeidlicher Verlust) -> sofortige Zugabgabe
            if (Math.abs(bestScoreOverall) > 15000.0) {
                break;
            }

            // Nächste Tiefe nur anstoßen, wenn noch mindestens 30% der Bedenkzeit verbleiben
            if (!useFixedDepth && elapsed >= (allocatedTimeMs * 7) / 10) {
                break;
            }

            if (timeExceeded || stopRequested || System.currentTimeMillis() >= deadline) {
                break;
            }
        }

        long totalTime = Math.max(1, System.currentTimeMillis() - startTime);
        lastSearchTimeMs = totalTime;
        lastNps = (lastNodesEvaluated * 1000L) / totalTime;

        return bestMoveOverall;
    }

    /**
     * Wurzelsuche für die aktuelle Iteration von Iterative Deepening.
     */
    private double searchRoot(Board board, int depth, PieceColor color, long deadline, Move pvMove) {
        List<Move> legalMoves = MoveGenerator.generateLegalMovesForColor(board, color);

        // TT-Lookup für Move Ordering an der Wurzel
        TranspositionTable.TTEntry ttEntry = transpositionTable.probe(board.getZobristKey(), 0);
        Move preferredMove = (pvMove != null) ? pvMove : (ttEntry != null ? ttEntry.bestMove : null);
        orderMoves(legalMoves, preferredMove, 0);

        double alpha = -MATE_SCORE;
        double beta = MATE_SCORE;
        double maxScore = -MATE_SCORE;

        for (Move move : legalMoves) {
            if (stopRequested || Thread.currentThread().isInterrupted() || System.currentTimeMillis() >= deadline) {
                timeExceeded = true;
                break;
            }

            board.makeMove(move);
            double score = -search(board, -beta, -alpha, depth - 1, 1, 0, color.opposite(), deadline, true);
            board.undoMove();

            if (timeExceeded || stopRequested) {
                break;
            }

            if (score > maxScore) {
                maxScore = score;
                iterationBestMove = move;
            }
            if (score > alpha) {
                alpha = score;
            }
            if (alpha >= beta) {
                break;
            }
        }

        if (!timeExceeded && !stopRequested && iterationBestMove != null) {
            transpositionTable.store(board.getZobristKey(), depth, TranspositionTable.FLAG_EXACT, maxScore, iterationBestMove, 0);
        }

        return maxScore;
    }

    /**
     * Rekursive Alpha-Beta Minimax-Suche (Fail-Soft Negamax).
     * Enthält Check Extensions, Quiescence Search und Timeout-Prüfungen.
     */
    private double search(Board board, double alpha, double beta, int depth, int ply, int numExtensions,
                          PieceColor color, long deadline, boolean allowNullMove) {
        lastNodesEvaluated++;

        // Regelmäßige Timeout- & Abbruchprüfung alle 2048 Knoten zur Vermeidung von Systemaufruf-Overhead
        if (stopRequested || Thread.currentThread().isInterrupted() ||
                ((lastNodesEvaluated & 2047) == 0 && System.currentTimeMillis() >= deadline)) {
            timeExceeded = true;
            return 0.0;
        }

        // Remisprüfung: 3-fache Stellungswiederholung, ungenügendes Material oder 50-Züge-Regel
        if (ply > 0) {
            if (board.isThreefoldRepetition() || board.getRepetitionCount(board.getZobristKey()) >= 2 ||
                board.hasInsufficientMaterial() || board.getHalfmoveClock() >= 100) {
                return 0.0; // Remis
            }
        }

        boolean inCheck = MoveGenerator.isKingInCheck(board, color);

        // Check Extensions: Wenn im Schach, Suchtiefe um 1 Ply verlängern (max 16 mal)
        int extension = (inCheck && numExtensions < 16) ? 1 : 0;
        int effectiveDepth = depth + extension;

        if (effectiveDepth <= 0) {
            return quiescence(board, alpha, beta, color, ply, deadline);
        }

        // Null Move Pruning (NMP)
        // Vermeidet Zugzwang, indem nur gesucht wird, wenn mehr als Bauern übrig sind.
        if (allowNullMove && !inCheck && effectiveDepth >= 3 && board.hasNonPawnMaterial(color)) {
            board.makeNullMove();
            int R = 2; // Reduction depth
            double nullScore = -search(board, -beta, -beta + 1, effectiveDepth - 1 - R, ply + 1, numExtensions, color.opposite(), deadline, false);
            board.undoNullMove();

            if (timeExceeded || stopRequested) {
                return 0.0;
            }

            if (nullScore >= beta) {
                return beta; // Cutoff
            }
        }

        // Transposition Table Probing
        long zobristKey = board.getZobristKey();
        TranspositionTable.TTEntry ttEntry = transpositionTable.probe(zobristKey, ply);
        if (ttEntry != null && ttEntry.depth >= effectiveDepth) {
            if (ttEntry.flag == TranspositionTable.FLAG_EXACT) {
                return ttEntry.score;
            }
            if (ttEntry.flag == TranspositionTable.FLAG_LOWERBOUND && ttEntry.score >= beta) {
                return ttEntry.score; // Beta-Cutoff aus TT
            }
            if (ttEntry.flag == TranspositionTable.FLAG_UPPERBOUND && ttEntry.score <= alpha) {
                return ttEntry.score; // Alpha-Cutoff aus TT
            }
        }

        List<Move> legalMoves = MoveGenerator.generateLegalMovesForColor(board, color);

        if (legalMoves.isEmpty()) {
            if (inCheck) {
                // Schachmatt: Je schneller das Matt, desto höher der Score (-MATE_SCORE + ply)
                return -MATE_SCORE + ply;
            } else {
                // Patt / Remis
                return 0.0;
            }
        }

        Move ttMove = (ttEntry != null) ? ttEntry.bestMove : null;
        orderMoves(legalMoves, ttMove, ply);

        double originalAlpha = alpha;
        Move bestMove = null;
        double maxScore = -MATE_SCORE;

        int moveCount = 0;
        for (Move move : legalMoves) {
            moveCount++;
            board.makeMove(move);
            
            boolean givesCheck = MoveGenerator.isKingInCheck(board, color.opposite());
            double score;
            
            // LMR (Late Move Reductions)
            // Reduziere Suchtiefe für stille Züge, die weiter hinten in der Liste stehen.
            if (depth >= 3 && moveCount >= 4 && !inCheck && !givesCheck && !move.isCapture() && !move.isPromotion()) {
                int reduction = (moveCount > 6) ? 2 : 1;
                
                // Reduzierte Suche (Principal Variation Search Prinzip)
                score = -search(board, -beta, -alpha, depth - 1 + extension - reduction, ply + 1,
                                       numExtensions + extension, color.opposite(), deadline, true);
                
                // Re-Search in voller Tiefe, falls die reduzierte Suche überraschend gut war
                if (score > alpha) {
                    score = -search(board, -beta, -alpha, depth - 1 + extension, ply + 1,
                                           numExtensions + extension, color.opposite(), deadline, true);
                }
            } else {
                // Reguläre Suche in voller Tiefe
                score = -search(board, -beta, -alpha, depth - 1 + extension, ply + 1,
                                       numExtensions + extension, color.opposite(), deadline, true);
            }
            
            board.undoMove();

            if (timeExceeded || stopRequested) {
                return 0.0;
            }

            if (score > maxScore) {
                maxScore = score;
                bestMove = move;
            }
            if (score > alpha) {
                alpha = score;
            }
            if (alpha >= beta) {
                // Killer Move speichern (nur für stille Züge)
                if (!move.isCapture() && !move.isPromotion() && ply < killerMoves.length) {
                    if (!move.equals(killerMoves[ply][0])) {
                        killerMoves[ply][1] = killerMoves[ply][0];
                        killerMoves[ply][0] = move;
                    }
                }
                break; // Beta-Cutoff (Fail-Soft)
            }
        }

        // In Transposition Table speichern
        if (!timeExceeded && !stopRequested) {
            byte flag;
            if (maxScore >= beta) {
                flag = TranspositionTable.FLAG_LOWERBOUND;
            } else if (maxScore > originalAlpha) {
                flag = TranspositionTable.FLAG_EXACT;
            } else {
                flag = TranspositionTable.FLAG_UPPERBOUND;
            }
            transpositionTable.store(zobristKey, effectiveDepth, flag, maxScore, bestMove, ply);
        }

        return maxScore;
    }

    /**
     * Quiescence Search (Ruhesuche): Sucht ausschließlich Schlagzüge und Umwandlungen,
     * um taktische Ungenauigkeiten durch den Horizon-Effekt zu verhindern.
     */
    private double quiescence(Board board, double alpha, double beta, PieceColor color, int ply, long deadline) {
        lastNodesEvaluated++;

        if (stopRequested || timeExceeded || Thread.currentThread().isInterrupted() ||
                ((lastNodesEvaluated & 2047) == 0 && System.currentTimeMillis() >= deadline)) {
            timeExceeded = true;
            return 0.0;
        }

        boolean inCheck = MoveGenerator.isKingInCheck(board, color);
        if (inCheck) {
            // Im Schach müssen alle Ausweichzüge geprüft werden
            List<Move> evasions = MoveGenerator.generateLegalMovesForColor(board, color);
            if (evasions.isEmpty()) {
                return -MATE_SCORE + ply;
            }
            orderMoves(evasions, null, ply);
            for (Move move : evasions) {
                board.makeMove(move);
                double score = -quiescence(board, -beta, -alpha, color.opposite(), ply + 1, deadline);
                board.undoMove();
                if (timeExceeded || stopRequested) return 0.0;
                if (score >= beta) return beta;
                if (score > alpha) alpha = score;
            }
            return alpha;
        }

        // Stand-Pat: Statische Bewertung der aktuellen Ruhestellung
        double standPat = evaluate(board, color);
        if (standPat >= beta) {
            return beta;
        }
        if (standPat > alpha) {
            alpha = standPat;
        }

        // Nur Schlagzüge und Umwandlungen berücksichtigen
        List<Move> legalMoves = MoveGenerator.generateLegalMovesForColor(board, color);
        List<Move> tacticalMoves = new ArrayList<>();
        for (Move m : legalMoves) {
            if (m.isCapture() || m.isPromotion()) {
                tacticalMoves.add(m);
            }
        }

        if (tacticalMoves.isEmpty()) {
            return standPat;
        }

        orderMoves(tacticalMoves, null, ply);

        for (Move move : tacticalMoves) {
            board.makeMove(move);
            double score = -quiescence(board, -beta, -alpha, color.opposite(), ply + 1, deadline);
            board.undoMove();

            if (timeExceeded || stopRequested) return 0.0;

            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        return alpha;
    }

    /**
     * Bewertet die Stellung vollständig mit den PST-Tabellen und der Endspiel-Interpolation aus JS_ONE.
     *
     * @param board Aktuelles Schachbrett.
     * @param activeColor Die Farbe, aus deren Sicht die Bewertung erfolgen soll.
     * @return Stellungsbewertung in Zentibauern (positiv = vorteilhaft für activeColor).
     */
    public double evaluate(Board board, PieceColor activeColor) {
        double score = 0.0;
        int totalPieces = getTotalPieceCount(board);
        double factor = Math.max(0.0, Math.min(1.0, (16.0 - totalPieces) / 16.0));

        for (int i = 0; i < 64; i++) {
            Piece piece = board.getPieceAt(Position.fromIndex(i));
            if (piece == null) continue;

            PieceType type = piece.getType();
            int baseVal = getPieceBaseValue(type);

            if (piece.getColor() == PieceColor.WHITE) {
                score += baseVal;
                score += getPieceSquareScore(type, i, factor);
            } else {
                // Rangspiegelung für Schwarz: (7 - rank) * 8 + file
                int flippedIndex = (i % 8) + (7 - (i / 8)) * 8;
                score -= baseVal;
                score -= getPieceSquareScore(type, flippedIndex, factor);
            }
        }

        return activeColor == PieceColor.WHITE ? score : -score;
    }

    /**
     * Zählt die Anzahl der aktiven Leicht- und Schwerfiguren (Springer, Läufer, Türme, Damen) beider Seiten.
     */
    private int getTotalPieceCount(Board board) {
        int count = 0;
        for (int i = 0; i < 64; i++) {
            Piece p = board.getPieceAt(Position.fromIndex(i));
            if (p != null) {
                PieceType t = p.getType();
                if (t == PieceType.KNIGHT || t == PieceType.BISHOP || t == PieceType.ROOK || t == PieceType.QUEEN) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Ermittelt den positionellen Bonus anhand des Figurentyps und des Feldes.
     */
    private double getPieceSquareScore(PieceType type, int index, double factor) {
        return switch (type) {
            case PAWN -> PAWNS[index] + (PAWNS_END[index] - PAWNS[index]) * factor;
            case KNIGHT -> KNIGHTS[index];
            case BISHOP -> BISHOPS[index];
            case ROOK -> ROOKS[index];
            case QUEEN -> QUEENS[index];
            case KING -> KING[index] + (KING_END[index] - KING[index]) * factor;
        };
    }

    /**
     * Materialbasiswerte aus JS_ONE (Bauer 100, Springer 300, Läufer 320, Turm 500, Dame 900, König 10000).
     */
    private static int getPieceBaseValue(PieceType type) {
        return switch (type) {
            case PAWN -> 100;
            case KNIGHT -> 300;
            case BISHOP -> 320;
            case ROOK -> 500;
            case QUEEN -> 900;
            case KING -> 10000;
        };
    }

    /**
     * Sortiert Züge vor:
     * 1. Vorheriger bester Zug an der Wurzel (PV-Move) für sofortige Cutoffs.
     * 2. Schlagzüge nach MVV-LVA (Most Valuable Victim - Least Valuable Attacker).
     * 3. Killer Moves (nur in regulärer Suche).
     * 4. Bauernumwandlungen.
     * 5. Rochaden.
     * 6. Stille Züge.
     */
    private void orderMoves(List<Move> moves, Move pvMove, int ply) {
        moves.sort((a, b) -> Integer.compare(scoreMove(b, pvMove, ply), scoreMove(a, pvMove, ply)));
    }

    private int scoreMove(Move move, Move pvMove, int ply) {
        if (pvMove != null && move.equals(pvMove)) {
            return 1_000_000; // Höchste Priorität für bisher besten Zug
        }
        int score = 0;
        if (move.isCapture()) {
            int victimValue = move.getCapturedPiece() != null
                    ? getPieceBaseValue(move.getCapturedPiece().getType())
                    : 100;
            int attackerValue = getPieceBaseValue(move.getMovedPiece().getType());
            // MVV-LVA: Wertvolles Opfer geschlagen von schwachem Angreifer
            score += 10_000 + (victimValue * 10 - attackerValue);
        }
        if (move.isPromotion()) {
            score += 8_000 + (move.getPromotionType() != null ? getPieceBaseValue(move.getPromotionType()) : 0);
        } else if (!move.isCapture()) {
            // Killer Moves (nur stille Züge) prüfen
            if (ply >= 0 && ply < killerMoves.length) {
                if (move.equals(killerMoves[ply][0])) {
                    score += 5_000;
                } else if (move.equals(killerMoves[ply][1])) {
                    score += 4_000;
                }
            }
        }
        if (move.isCastle()) {
            score += 500;
        }
        return score;
    }

    /**
     * Formatiert die Zentibauern-Bewertung für Logs (z.B. "+0.35", "-1.20", "+M2", "-M1").
     */
    public static String formatScore(double score) {
        if (score >= 25000.0) {
            int mateInPly = (int) Math.round(MATE_SCORE - score);
            int mateInMoves = Math.max(1, (mateInPly + 1) / 2);
            return "+M" + mateInMoves;
        } else if (score <= -25000.0) {
            int mateInPly = (int) Math.round(MATE_SCORE + score);
            int mateInMoves = Math.max(1, (mateInPly + 1) / 2);
            return "-M" + mateInMoves;
        } else {
            return String.format(java.util.Locale.US, "%+.2f", score / 100.0);
        }
    }

    public TranspositionTable getTranspositionTable() {
        return transpositionTable;
    }

    public void clearTranspositionTable() {
        transpositionTable.clear();
    }
}
