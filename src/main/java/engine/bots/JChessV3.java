package engine.bots;

import com.jchess.core.board.BitboardHelper;
import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.board.Position;
import com.jchess.core.clock.ChessClock;
import com.jchess.core.engine.ChessEngine;
import com.jchess.core.piece.Piece;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;
import com.jchess.core.rules.MoveGenerator;

public class JChessV3 implements ChessEngine {

    // --- Piece-Square Tables ---
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
    
    private static final int[] PIECE_VALUES = { 100, 320, 330, 500, 900, 20000 };

    private static final long[][] PASSED_PAWN_MASK = new long[2][64];
    private static final long[][] PAWN_SUPPORT_MASK = new long[2][64];
    
    static {
        for (int sq = 0; sq < 64; sq++) {
            int file = sq % 8;
            int rank = sq / 8;
            
            // White passed pawn mask
            long wMask = 0L;
            for (int r = rank + 1; r < 8; r++) {
                wMask |= (1L << (r * 8 + file));
                if (file > 0) wMask |= (1L << (r * 8 + file - 1));
                if (file < 7) wMask |= (1L << (r * 8 + file + 1));
            }
            PASSED_PAWN_MASK[PieceColor.WHITE.ordinal()][sq] = wMask;
            
            // White support mask (rank - 1, file - 1 and file + 1)
            long wSupport = 0L;
            if (rank > 0) {
                if (file > 0) wSupport |= (1L << ((rank - 1) * 8 + file - 1));
                if (file < 7) wSupport |= (1L << ((rank - 1) * 8 + file + 1));
            }
            PAWN_SUPPORT_MASK[PieceColor.WHITE.ordinal()][sq] = wSupport;
            
            // Black passed pawn mask
            long bMask = 0L;
            for (int r = rank - 1; r >= 0; r--) {
                bMask |= (1L << (r * 8 + file));
                if (file > 0) bMask |= (1L << (r * 8 + file - 1));
                if (file < 7) bMask |= (1L << (r * 8 + file + 1));
            }
            PASSED_PAWN_MASK[PieceColor.BLACK.ordinal()][sq] = bMask;
            
            // Black support mask (rank + 1, file - 1 and file + 1)
            long bSupport = 0L;
            if (rank < 7) {
                if (file > 0) bSupport |= (1L << ((rank + 1) * 8 + file - 1));
                if (file < 7) bSupport |= (1L << ((rank + 1) * 8 + file + 1));
            }
            PAWN_SUPPORT_MASK[PieceColor.BLACK.ordinal()][sq] = bSupport;
        }
    }

    private final String name;
    private int searchDepth;
    private boolean useFixedDepth;
    private long defaultTimeLimitMs = 1000;
    
    private long lastNodesEvaluated = 0;
    private int lastDepthReached = 0;
    private long lastSearchTimeMs = 0;
    private long lastNps = 0;
    private double remainingTimePercentage = 0.05;

    private volatile boolean timeExceeded = false;
    private volatile boolean stopRequested = false;
    private short iterationBestMove = 0;

    private final TT16 transpositionTable = new TT16();
    private final short[][] killerMoves = new short[100][2];
    private final int[][][] historyTable = new int[2][64][64];

    // Preallocated per-ply buffers to avoid GC pressure in the hot path
    private static final int MAX_PLY = 128;
    private final short[][] plyMoves      = new short[MAX_PLY][256];  // search/searchRoot: legal moves
    private final short[][] plyQuiets     = new short[MAX_PLY][256];  // search: searched quiets for history gravity
    private final short[][] plyEvasions   = new short[MAX_PLY][256];  // quiescence: check evasions
    private final short[][] plyQMoves     = new short[MAX_PLY][256];  // quiescence: pseudo-legal moves
    private final short[][] plyTacticals  = new short[MAX_PLY][256];  // quiescence: tactical moves
    private final short[][] plyPseudo     = new short[MAX_PLY][256];  // generateLegalMoves: pseudo-legal buffer
    private final int[][]   plyScores     = new int[MAX_PLY][256];    // orderMoves: scoring buffer
    private final int[]     seeGain       = new int[32];              // see: gain array (non-recursive)

    private final int[] whitePawnsOnFile = new int[8];
    private final int[] blackPawnsOnFile = new int[8];

    public void stopSearch() {
        this.stopRequested = true;
        this.timeExceeded = true;
    }
    public boolean isStopRequested() { return stopRequested; }
    public String getName() { return name; }
    public long getLastNodesEvaluated() { return lastNodesEvaluated; }
    public int getLastDepthReached() { return lastDepthReached; }
    public long getLastSearchTimeMs() { return lastSearchTimeMs; }
    public long getLastNps() { return lastNps; }

    public JChessV3() { this("JChessV3", 64, false, 5000, 0.05); }
    
    public JChessV3(long defaultTimeLimitMs) {
        this("JChessV3", 64, false, defaultTimeLimitMs, 0.05);
    }
    
    public JChessV3(int searchDepth) {
        this("JChessV3", searchDepth, true, 5000, 0.05);
    }
    
    public JChessV3(String name, long defaultTimeLimitMs, double remainingTimePercentage) {
        this(name, 64, false, defaultTimeLimitMs, remainingTimePercentage);
    }

    public JChessV3(String name, int searchDepth, boolean useFixedDepth, long defaultTimeLimitMs, double remainingTimePercentage) {
        this.name = name;
        this.searchDepth = Math.max(1, searchDepth);
        this.useFixedDepth = useFixedDepth;
        this.defaultTimeLimitMs = Math.max(50, defaultTimeLimitMs);
        this.remainingTimePercentage = Math.max(0.001, Math.min(0.50, remainingTimePercentage));
    }

    @FunctionalInterface
    public interface DepthListener {
        void onDepthCompleted(int depth, double score, Move bestMove, long nodes, long elapsedMs);
    }

    @Override
    public Move findBestMove(Board board, PieceColor color) {
        return findBestMoveWithListener(board, color, null, null);
    }

    @Override
    public Move findBestMove(Board board, PieceColor color, ChessClock clock) {
        return findBestMoveWithListener(board, color, clock, null);
    }

    public Move findBestMoveWithListener(Board board, PieceColor color, ChessClock clock, DepthListener listener) {
        short[] legalMoves = plyMoves[0];
        int legalCount = generateLegalMoves(board, color, legalMoves, 0);
        
        if (legalCount == 0) return null;
        if (legalCount == 1) {
            Move singleMove = CompactMove.toStandardMove(legalMoves[0], board);
            if (listener != null) listener.onDepthCompleted(1, 0.0, singleMove, 1, 0);
            return singleMove;
        }

        long allocatedTimeMs = defaultTimeLimitMs;
        if (clock != null && clock.isEnabled()) {
            long remainingMs = clock.getRemainingTimeMillis(color);
            long incMs = clock.getIncrementMillis();
            if (remainingMs > 0) {
                long safetyMargin = remainingMs > 1000 ? 100 : 20;
                long safetyTime = Math.max(15, remainingMs - safetyMargin);
                long clockBudget = Math.max(50, (long) (remainingMs * remainingTimePercentage) + (incMs * 7) / 10);
                allocatedTimeMs = Math.min(clockBudget, safetyTime);
            }
        }

        long startTime = System.currentTimeMillis();
        long deadline = useFixedDepth && (clock == null || !clock.isEnabled()) ? Long.MAX_VALUE : startTime + allocatedTimeMs;

        lastNodesEvaluated = 0;
        lastDepthReached = 0;
        timeExceeded = false;
        stopRequested = false;
        for (int i = 0; i < killerMoves.length; i++) { killerMoves[i][0] = 0; killerMoves[i][1] = 0; }
        for (int c = 0; c < 2; c++) for (int f = 0; f < 64; f++) for (int t = 0; t < 64; t++) historyTable[c][f][t] = 0;

        short bestMoveOverall = legalMoves[0];
        double alpha = -MATE_SCORE;
        double beta = MATE_SCORE;
        double bestScoreOverall = 0;

        int targetMaxDepth = useFixedDepth ? searchDepth : Math.min(searchDepth, 64);

        for (int currentDepth = 1; currentDepth <= targetMaxDepth; currentDepth++) {
            if (stopRequested || Thread.currentThread().isInterrupted()) break;
            iterationBestMove = 0;

            double score = searchRoot(board, currentDepth, color, deadline, bestMoveOverall, alpha, beta);

            if ((score <= alpha || score >= beta) && !timeExceeded && !stopRequested && currentDepth > 1) {
                alpha = -MATE_SCORE;
                beta = MATE_SCORE;
                score = searchRoot(board, currentDepth, color, deadline, bestMoveOverall, alpha, beta);
            }

            if ((timeExceeded || stopRequested) && iterationBestMove == 0) break;

            if (iterationBestMove != 0) {
                bestMoveOverall = iterationBestMove;
                bestScoreOverall = score;
                lastDepthReached = currentDepth;
                if (currentDepth >= 2) {
                    alpha = bestScoreOverall - 50;
                    beta = bestScoreOverall + 50;
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (listener != null && iterationBestMove != 0) {
                listener.onDepthCompleted(currentDepth, bestScoreOverall, CompactMove.toStandardMove(bestMoveOverall, board), lastNodesEvaluated, elapsed);
            }

            if (Math.abs(bestScoreOverall) > 15000.0) break;
            if (!useFixedDepth && elapsed >= (allocatedTimeMs * 7) / 10) break;
            if (timeExceeded || stopRequested || System.currentTimeMillis() >= deadline) break;
        }

        long totalTime = Math.max(1, System.currentTimeMillis() - startTime);
        lastSearchTimeMs = totalTime;
        lastNps = (lastNodesEvaluated * 1000L) / totalTime;
        return CompactMove.toStandardMove(bestMoveOverall, board);
    }

    private double searchRoot(Board board, int depth, PieceColor color, long deadline, short pvMove, double alpha, double beta) {
        short[] moves = plyMoves[0];
        int count = generateLegalMoves(board, color, moves, 0);

        TT16.TTEntry ttEntry = transpositionTable.probe(board.getZobristKey(), 0);
        short preferredMove = (pvMove != 0) ? pvMove : (ttEntry != null ? ttEntry.bestMove : 0);
        scoreMoves(moves, count, preferredMove, 0, color, board);

        double maxScore = -MATE_SCORE;
        int[] scores = plyScores[0];

        for (int i=0; i<count; i++) {
            if (stopRequested || Thread.currentThread().isInterrupted() || System.currentTimeMillis() >= deadline) {
                timeExceeded = true;
                break;
            }

            // Pick-best: select highest scored move from index i to count-1
            int bestIdx = i;
            int bestScore = scores[i];
            for (int j = i + 1; j < count; j++) {
                if (scores[j] > bestScore) {
                    bestScore = scores[j];
                    bestIdx = j;
                }
            }
            if (bestIdx != i) {
                scores[bestIdx] = scores[i];
                scores[i] = bestScore;
                short tempM = moves[i];
                moves[i] = moves[bestIdx];
                moves[bestIdx] = tempM;
            }

            short move = moves[i];
            board.makeMove(move);
            double score;
            if (i == 0) {
                score = -search(board, -beta, -alpha, depth - 1, 1, 0, color.opposite(), deadline, true);
            } else {
                score = -search(board, -alpha - 1, -alpha, depth - 1, 1, 0, color.opposite(), deadline, true);
                if (score > alpha && score < beta) {
                    score = -search(board, -beta, -alpha, depth - 1, 1, 0, color.opposite(), deadline, true);
                }
            }
            board.undoMove();

            if (timeExceeded || stopRequested) break;
            if (score > maxScore) {
                maxScore = score;
                iterationBestMove = move;
            }
            if (score > alpha) alpha = score;
            if (alpha >= beta) break;
        }

        if (!timeExceeded && !stopRequested && iterationBestMove != 0) {
            transpositionTable.store(board.getZobristKey(), depth, TT16.FLAG_EXACT, maxScore, iterationBestMove, 0);
        }
        return maxScore;
    }

    private double search(Board board, double alpha, double beta, int depth, int ply, int numExtensions, PieceColor color, long deadline, boolean allowNullMove) {
        lastNodesEvaluated++;
        if (stopRequested || Thread.currentThread().isInterrupted() || (java.util.concurrent.ThreadLocalRandom.current().nextInt(1024) == 0 && System.currentTimeMillis() >= deadline)) {
            timeExceeded = true; return 0.0;
        }
        if (ply > 0 && (board.isThreefoldRepetition() || board.getRepetitionCount(board.getZobristKey()) >= 2 || board.hasInsufficientMaterial() || board.getHalfmoveClock() >= 100)) {
            return 0.0;
        }

        boolean inCheck = MoveGenerator.isKingInCheck(board, color);
        int extension = (inCheck && numExtensions < 16) ? 1 : 0;
        int effectiveDepth = depth + extension;

        if (effectiveDepth <= 0) return quiescence(board, alpha, beta, color, ply, deadline);

        if (allowNullMove && !inCheck && effectiveDepth >= 3 && board.hasNonPawnMaterial(color)) {
            board.makeNullMove();
            double nullScore = -search(board, -beta, -beta + 1, effectiveDepth - 1 - 2, ply + 1, numExtensions, color.opposite(), deadline, false);
            board.undoNullMove();
            if (timeExceeded || stopRequested) return 0.0;
            if (nullScore >= beta) return beta;
        }

        long zobristKey = board.getZobristKey();
        TT16.TTEntry ttEntry = transpositionTable.probe(zobristKey, ply);
        if (ttEntry != null && ttEntry.depth >= effectiveDepth) {
            if (ttEntry.flag == TT16.FLAG_EXACT) return ttEntry.score;
            if (ttEntry.flag == TT16.FLAG_LOWERBOUND && ttEntry.score >= beta) return ttEntry.score;
            if (ttEntry.flag == TT16.FLAG_UPPERBOUND && ttEntry.score <= alpha) return ttEntry.score;
        }

        int safePly = Math.min(ply, MAX_PLY - 1);
        short[] moves = plyMoves[safePly];
        int count = generatePseudoLegalMoves(board, color, moves);

        if (count == 0) return inCheck ? -MATE_SCORE + ply : 0.0;

        short ttMove = (ttEntry != null) ? ttEntry.bestMove : 0;
        scoreMoves(moves, count, ttMove, ply, color, board);

        double originalAlpha = alpha;
        short bestMove = 0;
        double maxScore = -MATE_SCORE;

        boolean fPrune = false;
        if (effectiveDepth <= 2 && !inCheck && alpha > -MATE_SCORE + 100 && beta < MATE_SCORE - 100) {
            double staticEval = evaluate(board, color);
            if (staticEval + effectiveDepth * 200 <= alpha) fPrune = true;
        }

        short[] searchedQuiets = plyQuiets[safePly];
        int quietsSearched = 0;
        int legalMovesSearched = 0;
        int[] scores = plyScores[safePly];

        boolean isPvNode = (beta - alpha > 1.0);

        for (int i=0; i<count; i++) {
            // Pick-best: select highest scored move from index i to count-1
            int bestIdx = i;
            int bestScore = scores[i];
            for (int j = i + 1; j < count; j++) {
                if (scores[j] > bestScore) {
                    bestScore = scores[j];
                    bestIdx = j;
                }
            }
            if (bestIdx != i) {
                scores[bestIdx] = scores[i];
                scores[i] = bestScore;
                short tempM = moves[i];
                moves[i] = moves[bestIdx];
                moves[bestIdx] = tempM;
            }

            short move = moves[i];
            int fromSq = CompactMove.getStartSquare(move);
            int toSq = CompactMove.getTargetSquare(move);
            int moveFlag = CompactMove.getMoveFlag(move);
            boolean isCapture = (board.getPieceAtIndex(toSq) != null) || (moveFlag == CompactMove.EnPassantCaptureFlag);
            boolean isPromotion = (moveFlag >= CompactMove.PromoteToQueenFlag);

            board.makeMove(move);

            // Lazy legality check: if king is in check after the move, it was illegal
            if (MoveGenerator.isKingInCheck(board, color)) {
                board.undoMove();
                continue;
            }

            boolean givesCheck = MoveGenerator.isKingInCheck(board, color.opposite());
            boolean isQuiet = !isCapture && !isPromotion && !givesCheck;
            
            if (fPrune && legalMovesSearched > 0 && isQuiet) {
                board.undoMove();
                continue;
            }

            legalMovesSearched++;
            int newDepth = depth - 1 + extension;
            double score;

            if (legalMovesSearched == 1) {
                score = -search(board, -beta, -alpha, newDepth, ply + 1, numExtensions + extension, color.opposite(), deadline, true);
            } else {
                int reduction = 0;
                if (effectiveDepth >= 3 && legalMovesSearched >= 4 && !inCheck && isQuiet) {
                    reduction = 1;
                    if (effectiveDepth >= 6 && legalMovesSearched >= 8)
                        reduction++;
                    // History: moves that historically failed to cause cutoffs get reduced more
                    int histScore = historyTable[color.ordinal()][fromSq][toSq];
                    if (histScore < 0)
                        reduction++;
                    reduction = Math.min(reduction, effectiveDepth - 2);
                }

                if (reduction > 0) {
                    score = -search(board, -alpha - 1, -alpha, newDepth - reduction, ply + 1, numExtensions + extension, color.opposite(), deadline, true);
                    if (score > alpha) {
                        score = -search(board, -alpha - 1, -alpha, newDepth, ply + 1, numExtensions + extension, color.opposite(), deadline, true);
                    }
                } else {
                    score = -search(board, -alpha - 1, -alpha, newDepth, ply + 1, numExtensions + extension, color.opposite(), deadline, true);
                }

                if (isPvNode && score > alpha && score < beta) {
                    score = -search(board, -beta, -alpha, newDepth, ply + 1, numExtensions + extension, color.opposite(), deadline, true);
                }
            }
            board.undoMove();

            if (timeExceeded || stopRequested) return 0.0;
            if (score > maxScore) { maxScore = score; bestMove = move; }
            if (score > alpha) alpha = score;
            if (alpha >= beta) {
                if (isQuiet) {
                    if (ply < killerMoves.length && move != killerMoves[ply][0]) {
                        killerMoves[ply][1] = killerMoves[ply][0];
                        killerMoves[ply][0] = move;
                    }
                    historyTable[color.ordinal()][fromSq][toSq] += effectiveDepth * effectiveDepth;
                    // History gravity: penalize all quiet moves tried before the cutoff move
                    for (int q = 0; q < quietsSearched; q++) {
                        int qs = CompactMove.getStartSquare(searchedQuiets[q]);
                        int qt = CompactMove.getTargetSquare(searchedQuiets[q]);
                        historyTable[color.ordinal()][qs][qt] -= effectiveDepth * effectiveDepth;
                    }
                }
                break;
            }
            // Track searched quiet moves for history gravity
            if (isQuiet) {
                searchedQuiets[quietsSearched++] = move;
            }
        }

        if (legalMovesSearched == 0) return inCheck ? -MATE_SCORE + ply : 0.0;

        if (!timeExceeded && !stopRequested) {
            byte flag = maxScore >= beta ? TT16.FLAG_LOWERBOUND : (maxScore > originalAlpha ? TT16.FLAG_EXACT : TT16.FLAG_UPPERBOUND);
            transpositionTable.store(zobristKey, effectiveDepth, flag, maxScore, bestMove, ply);
        }
        return maxScore;
    }

    private double quiescence(Board board, double alpha, double beta, PieceColor color, int ply, long deadline) {
        lastNodesEvaluated++;
        if (stopRequested || timeExceeded || Thread.currentThread().isInterrupted() || (java.util.concurrent.ThreadLocalRandom.current().nextInt(1024) == 0 && System.currentTimeMillis() >= deadline)) {
            timeExceeded = true; return 0.0;
        }

        boolean inCheck = MoveGenerator.isKingInCheck(board, color);
        if (inCheck) {
            int safePly = Math.min(ply, MAX_PLY - 1);
            short[] evasions = plyEvasions[safePly];
            int count = generatePseudoLegalMoves(board, color, evasions);
            if (count == 0) return -MATE_SCORE + ply;
            scoreMoves(evasions, count, (short)0, ply, color, board);
            int[] scores = plyScores[safePly];
            int legalEvasions = 0;

            for (int i=0; i<count; i++) {
                int bestIdx = i;
                int bestScore = scores[i];
                for (int j = i + 1; j < count; j++) {
                    if (scores[j] > bestScore) {
                        bestScore = scores[j];
                        bestIdx = j;
                    }
                }
                if (bestIdx != i) {
                    scores[bestIdx] = scores[i];
                    scores[i] = bestScore;
                    short tempM = evasions[i];
                    evasions[i] = evasions[bestIdx];
                    evasions[bestIdx] = tempM;
                }

                short move = evasions[i];
                board.makeMove(move);
                if (MoveGenerator.isKingInCheck(board, color)) {
                    board.undoMove();
                    continue;
                }
                legalEvasions++;
                double score = -quiescence(board, -beta, -alpha, color.opposite(), ply + 1, deadline);
                board.undoMove();
                if (timeExceeded || stopRequested) return 0.0;
                if (score >= beta) return beta;
                if (score > alpha) alpha = score;
            }
            if (legalEvasions == 0) return -MATE_SCORE + ply;
            return alpha;
        }

        double standPat = evaluate(board, color);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;

        int safePly = Math.min(ply, MAX_PLY - 1);
        short[] moves = plyQMoves[safePly];
        int count = generatePseudoLegalMoves(board, color, moves);
        short[] tacticalMoves = plyTacticals[safePly];
        int tCount = 0;
        for (int i=0; i<count; i++) {
            short m = moves[i];
            int moveFlag = CompactMove.getMoveFlag(m);
            boolean isCapture = board.getPieceCodeAtIndex(CompactMove.getTargetSquare(m)) != Board.EMPTY_SQUARE || moveFlag == CompactMove.EnPassantCaptureFlag;
            boolean isPromotion = moveFlag >= CompactMove.PromoteToQueenFlag;
            
            if (isCapture || isPromotion) {
                // SEE pruning for captures
                if (isCapture && !isPromotion && see(board, m) < 0) {
                    continue;
                }

                board.makeMove(m);
                if (!MoveGenerator.isKingInCheck(board, color)) tacticalMoves[tCount++] = m;
                board.undoMove();
            }
        }
        
        if (tCount == 0) return standPat;
        scoreMoves(tacticalMoves, tCount, (short)0, safePly, color, board);
        int[] scores = plyScores[safePly];

        for (int i=0; i<tCount; i++) {
            int bestIdx = i;
            int bestScore = scores[i];
            for (int j = i + 1; j < tCount; j++) {
                if (scores[j] > bestScore) {
                    bestScore = scores[j];
                    bestIdx = j;
                }
            }
            if (bestIdx != i) {
                scores[bestIdx] = scores[i];
                scores[i] = bestScore;
                short tempM = tacticalMoves[i];
                tacticalMoves[i] = tacticalMoves[bestIdx];
                tacticalMoves[bestIdx] = tempM;
            }

            short move = tacticalMoves[i];
            board.makeMove(move);
            double score = -quiescence(board, -beta, -alpha, color.opposite(), ply + 1, deadline);
            board.undoMove();
            if (timeExceeded || stopRequested) return 0.0;
            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    private void scoreMoves(short[] moves, int count, short ttMove, int ply, PieceColor color, Board board) {
        int safePly = Math.min(ply, MAX_PLY - 1);
        int[] scores = plyScores[safePly];
        for (int i = 0; i < count; i++) {
            short m = moves[i];
            if (m == ttMove) {
                scores[i] = 10000000;
                continue;
            }
            int score = 0;
            int fromSq = CompactMove.getStartSquare(m);
            int toSq = CompactMove.getTargetSquare(m);
            byte target = board.getPieceCodeAtIndex(toSq);
            byte moved = board.getPieceCodeAtIndex(fromSq);
            int movedVal = PIECE_VALUES[moved % 6];
            
            if (target != Board.EMPTY_SQUARE) {
                int targetVal = PIECE_VALUES[target % 6];
                score += 1000000 + targetVal * 10 - movedVal;
            } else if (CompactMove.getMoveFlag(m) == CompactMove.EnPassantCaptureFlag) {
                score += 1000000 + 1000 - movedVal;
            }
            if (CompactMove.getMoveFlag(m) >= CompactMove.PromoteToQueenFlag) {
                PieceType promo = CompactMove.getPromotionPieceType(m);
                score += 900000 + (promo != null ? PIECE_VALUES[promo.ordinal()] : 0);
            }
            if (target == Board.EMPTY_SQUARE && !(CompactMove.getMoveFlag(m) >= CompactMove.PromoteToQueenFlag)) {
                if (ply < killerMoves.length) {
                    if (m == killerMoves[ply][0]) score += 90000;
                    else if (m == killerMoves[ply][1]) score += 80000;
                }
                score += historyTable[color.ordinal()][fromSq][toSq];
            }
            scores[i] = score;
        }
    }

    private void orderMoves(short[] moves, int count, short ttMove, int ply, PieceColor color, Board board) {
        scoreMoves(moves, count, ttMove, ply, color, board);
        int safePly = Math.min(ply, MAX_PLY - 1);
        int[] scores = plyScores[safePly];
        for (int i = 0; i < count - 1; i++) {
            for (int j = i + 1; j < count; j++) {
                if (scores[j] > scores[i]) {
                    int tempScore = scores[i]; scores[i] = scores[j]; scores[j] = tempScore;
                    short tempMove = moves[i]; moves[i] = moves[j]; moves[j] = tempMove;
                }
            }
        }
    }

    private int generateLegalMoves(Board board, PieceColor color, short[] moves, int safePly) {
        short[] pseudo = plyPseudo[safePly];
        int pCount = generatePseudoLegalMoves(board, color, pseudo);
        int lCount = 0;
        for (int i = 0; i < pCount; i++) {
            short m = pseudo[i];
            board.makeMove(m);
            if (!MoveGenerator.isKingInCheck(board, color)) {
                moves[lCount++] = m;
            }
            board.undoMove();
        }
        return lCount;
    }

    private int generatePseudoLegalMoves(Board board, PieceColor color, short[] moves) {
        long friendly = board.getColorBitboard(color);
        long enemy = board.getColorBitboard(color.opposite());
        long occupied = board.getOccupiedBitboard();
        int count = 0;
        
        long pawns = board.getBitboard(PieceType.PAWN, color);
        int dir = color.getPawnDirection();
        int promoRank = color.getPromotionRank();
        int startRank = color.getPawnStartRank();
        Position epTarget = board.getEnPassantTarget();
        int epSq = (epTarget != null) ? epTarget.getIndex() : -1;

        while (pawns != 0) {
            int fromSq = Long.numberOfTrailingZeros(pawns);
            int rank = fromSq / 8;
            int oneStepSq = fromSq + dir * 8;
            if (oneStepSq >= 0 && oneStepSq < 64 && (occupied & (1L << oneStepSq)) == 0) {
                if (oneStepSq / 8 == promoRank) {
                    moves[count++] = CompactMove.createMove(fromSq, oneStepSq, CompactMove.PromoteToQueenFlag);
                    moves[count++] = CompactMove.createMove(fromSq, oneStepSq, CompactMove.PromoteToKnightFlag);
                    moves[count++] = CompactMove.createMove(fromSq, oneStepSq, CompactMove.PromoteToRookFlag);
                    moves[count++] = CompactMove.createMove(fromSq, oneStepSq, CompactMove.PromoteToBishopFlag);
                } else {
                    moves[count++] = CompactMove.createMove(fromSq, oneStepSq, CompactMove.NoFlag);
                    if (rank == startRank) {
                        int twoStepSq = fromSq + 2 * dir * 8;
                        if ((occupied & (1L << twoStepSq)) == 0) {
                            moves[count++] = CompactMove.createMove(fromSq, twoStepSq, CompactMove.PawnTwoUpFlag);
                        }
                    }
                }
            }
            long attackDests = BitboardHelper.getPawnAttacks(fromSq, color) & enemy;
            while (attackDests != 0) {
                int toSq = Long.numberOfTrailingZeros(attackDests);
                if (toSq / 8 == promoRank) {
                    moves[count++] = CompactMove.createMove(fromSq, toSq, CompactMove.PromoteToQueenFlag);
                    moves[count++] = CompactMove.createMove(fromSq, toSq, CompactMove.PromoteToKnightFlag);
                    moves[count++] = CompactMove.createMove(fromSq, toSq, CompactMove.PromoteToRookFlag);
                    moves[count++] = CompactMove.createMove(fromSq, toSq, CompactMove.PromoteToBishopFlag);
                } else {
                    moves[count++] = CompactMove.createMove(fromSq, toSq, CompactMove.NoFlag);
                }
                attackDests &= attackDests - 1;
            }
            if (epSq >= 0 && (BitboardHelper.getPawnAttacks(fromSq, color) & (1L << epSq)) != 0) {
                moves[count++] = CompactMove.createMove(fromSq, epSq, CompactMove.EnPassantCaptureFlag);
            }
            pawns &= pawns - 1;
        }

        long knights = board.getBitboard(PieceType.KNIGHT, color);
        while (knights != 0) {
            int fromSq = Long.numberOfTrailingZeros(knights);
            long dests = BitboardHelper.getKnightAttacks(fromSq) & ~friendly;
            while (dests != 0) {
                moves[count++] = CompactMove.createMove(fromSq, Long.numberOfTrailingZeros(dests), CompactMove.NoFlag);
                dests &= dests - 1;
            }
            knights &= knights - 1;
        }

        long bishops = board.getBitboard(PieceType.BISHOP, color);
        while (bishops != 0) {
            int fromSq = Long.numberOfTrailingZeros(bishops);
            long dests = BitboardHelper.getBishopAttacks(fromSq, occupied) & ~friendly;
            while (dests != 0) {
                moves[count++] = CompactMove.createMove(fromSq, Long.numberOfTrailingZeros(dests), CompactMove.NoFlag);
                dests &= dests - 1;
            }
            bishops &= bishops - 1;
        }

        long rooks = board.getBitboard(PieceType.ROOK, color);
        while (rooks != 0) {
            int fromSq = Long.numberOfTrailingZeros(rooks);
            long dests = BitboardHelper.getRookAttacks(fromSq, occupied) & ~friendly;
            while (dests != 0) {
                moves[count++] = CompactMove.createMove(fromSq, Long.numberOfTrailingZeros(dests), CompactMove.NoFlag);
                dests &= dests - 1;
            }
            rooks &= rooks - 1;
        }

        long queens = board.getBitboard(PieceType.QUEEN, color);
        while (queens != 0) {
            int fromSq = Long.numberOfTrailingZeros(queens);
            long dests = BitboardHelper.getQueenAttacks(fromSq, occupied) & ~friendly;
            while (dests != 0) {
                moves[count++] = CompactMove.createMove(fromSq, Long.numberOfTrailingZeros(dests), CompactMove.NoFlag);
                dests &= dests - 1;
            }
            queens &= queens - 1;
        }

        int kingSq = board.getKingSquare(color);
        if (kingSq >= 0) {
            long dests = BitboardHelper.getKingAttacks(kingSq) & ~friendly;
            while (dests != 0) {
                moves[count++] = CompactMove.createMove(kingSq, Long.numberOfTrailingZeros(dests), CompactMove.NoFlag);
                dests &= dests - 1;
            }
            
            int rank = (color == PieceColor.WHITE) ? 0 : 7;
            if (kingSq == rank * 8 + 4 && !BitboardHelper.isSquareAttacked(board, kingSq, color.opposite())) {
                int rights = board.getCastlingRights();
                int ksMask = (color == PieceColor.WHITE) ? Board.CASTLE_WHITE_KINGSIDE : Board.CASTLE_BLACK_KINGSIDE;
                if ((rights & ksMask) != 0) {
                    int fSq = rank * 8 + 5, gSq = rank * 8 + 6;
                    if ((occupied & ((1L << fSq) | (1L << gSq))) == 0 &&
                        !BitboardHelper.isSquareAttacked(board, fSq, color.opposite()) &&
                        !BitboardHelper.isSquareAttacked(board, gSq, color.opposite())) {
                        moves[count++] = CompactMove.createMove(kingSq, gSq, CompactMove.CastleFlag);
                    }
                }
                int qsMask = (color == PieceColor.WHITE) ? Board.CASTLE_WHITE_QUEENSIDE : Board.CASTLE_BLACK_QUEENSIDE;
                if ((rights & qsMask) != 0) {
                    int bSq = rank * 8 + 1, cSq = rank * 8 + 2, dSq = rank * 8 + 3;
                    if ((occupied & ((1L << bSq) | (1L << cSq) | (1L << dSq))) == 0 &&
                        !BitboardHelper.isSquareAttacked(board, dSq, color.opposite()) &&
                        !BitboardHelper.isSquareAttacked(board, cSq, color.opposite())) {
                        moves[count++] = CompactMove.createMove(kingSq, cSq, CompactMove.CastleFlag);
                    }
                }
            }
        }
        return count;
    }

    public double evaluate(Board board, PieceColor activeColor) {
        double score = 0.0;
        int totalPieces = 0;
        
        long wKnights = board.getBitboard(PieceType.KNIGHT, PieceColor.WHITE);
        long bKnights = board.getBitboard(PieceType.KNIGHT, PieceColor.BLACK);
        long wBishops = board.getBitboard(PieceType.BISHOP, PieceColor.WHITE);
        long bBishops = board.getBitboard(PieceType.BISHOP, PieceColor.BLACK);
        long wRooks = board.getBitboard(PieceType.ROOK, PieceColor.WHITE);
        long bRooks = board.getBitboard(PieceType.ROOK, PieceColor.BLACK);
        long wQueens = board.getBitboard(PieceType.QUEEN, PieceColor.WHITE);
        long bQueens = board.getBitboard(PieceType.QUEEN, PieceColor.BLACK);
        long wPawns = board.getBitboard(PieceType.PAWN, PieceColor.WHITE);
        long bPawns = board.getBitboard(PieceType.PAWN, PieceColor.BLACK);
        int wKingSq = board.getKingSquare(PieceColor.WHITE);
        int bKingSq = board.getKingSquare(PieceColor.BLACK);

        long occupied = board.getOccupiedBitboard();
        long wPieces = board.getColorBitboard(PieceColor.WHITE);
        long bPieces = board.getColorBitboard(PieceColor.BLACK);

        totalPieces += Long.bitCount(wKnights) + Long.bitCount(bKnights) +
                       Long.bitCount(wBishops) + Long.bitCount(bBishops) +
                       Long.bitCount(wRooks)   + Long.bitCount(bRooks)   +
                       Long.bitCount(wQueens)  + Long.bitCount(bQueens);

        double factor = Math.max(0.0, Math.min(1.0, (16.0 - totalPieces) / 16.0));
        
        for (int i = 0; i < 8; i++) {
            whitePawnsOnFile[i] = 0;
            blackPawnsOnFile[i] = 0;
        }

        // --- PAWNS ---
        long tempWPawns = wPawns;
        while (tempWPawns != 0) {
            int sq = Long.numberOfTrailingZeros(tempWPawns);
            int file = sq % 8;
            int rank = sq / 8;
            whitePawnsOnFile[file]++;
            
            score += 100 + PAWNS[sq] * (1 - factor) + PAWNS_END[sq] * factor;
            
            if ((bPawns & PASSED_PAWN_MASK[PieceColor.WHITE.ordinal()][sq]) == 0L) score += 20 + (rank * 10);
            if ((wPawns & PAWN_SUPPORT_MASK[PieceColor.WHITE.ordinal()][sq]) != 0L) score += 10;
            if ((wPawns & PASSED_PAWN_MASK[PieceColor.BLACK.ordinal()][sq]) == 0L) score -= 5;
            
            tempWPawns &= tempWPawns - 1;
        }

        long tempBPawns = bPawns;
        while (tempBPawns != 0) {
            int sq = Long.numberOfTrailingZeros(tempBPawns);
            int file = sq % 8;
            int rank = sq / 8;
            int mirrored = (7 - rank) * 8 + file;
            blackPawnsOnFile[file]++;
            
            score -= 100 + PAWNS[mirrored] * (1 - factor) + PAWNS_END[mirrored] * factor;
            
            if ((wPawns & PASSED_PAWN_MASK[PieceColor.BLACK.ordinal()][sq]) == 0L) score -= 20 + ((7 - rank) * 10);
            if ((bPawns & PAWN_SUPPORT_MASK[PieceColor.BLACK.ordinal()][sq]) != 0L) score -= 10;
            if ((bPawns & PASSED_PAWN_MASK[PieceColor.WHITE.ordinal()][sq]) == 0L) score += 5;
            
            tempBPawns &= tempBPawns - 1;
        }
        
        // --- KNIGHTS ---
        long tempWKnights = wKnights;
        while (tempWKnights != 0) {
            int sq = Long.numberOfTrailingZeros(tempWKnights);
            int rank = sq / 8;
            score += 300 + KNIGHTS[sq];
            if (rank >= 3 && rank <= 5 && (wPawns & PAWN_SUPPORT_MASK[PieceColor.WHITE.ordinal()][sq]) != 0L && (bPawns & PASSED_PAWN_MASK[PieceColor.WHITE.ordinal()][sq]) == 0L) score += 15;
            tempWKnights &= tempWKnights - 1;
        }
        long tempBKnights = bKnights;
        while (tempBKnights != 0) {
            int sq = Long.numberOfTrailingZeros(tempBKnights);
            int rank = sq / 8;
            int mirrored = (7 - rank) * 8 + (sq % 8);
            score -= 300 + KNIGHTS[mirrored];
            if (rank >= 2 && rank <= 4 && (bPawns & PAWN_SUPPORT_MASK[PieceColor.BLACK.ordinal()][sq]) != 0L && (wPawns & PASSED_PAWN_MASK[PieceColor.BLACK.ordinal()][sq]) == 0L) score -= 15;
            tempBKnights &= tempBKnights - 1;
        }

        // --- BISHOPS (with mobility) ---
        int whiteBishops = 0;
        long tempWBishops = wBishops;
        while (tempWBishops != 0) {
            whiteBishops++;
            int sq = Long.numberOfTrailingZeros(tempWBishops);
            int rank = sq / 8;
            score += 320 + BISHOPS[sq];
            // Mobility: count squares not blocked by own pieces
            long attacks = BitboardHelper.getBishopAttacks(sq, occupied);
            int mobility = Long.bitCount(attacks & ~wPieces);
            score += (mobility - 5) * 4;  // baseline 5 squares, 4cp per extra square
            if (rank >= 3 && rank <= 5 && (wPawns & PAWN_SUPPORT_MASK[PieceColor.WHITE.ordinal()][sq]) != 0L && (bPawns & PASSED_PAWN_MASK[PieceColor.WHITE.ordinal()][sq]) == 0L) score += 15;
            tempWBishops &= tempWBishops - 1;
        }
        int blackBishops = 0;
        long tempBBishops = bBishops;
        while (tempBBishops != 0) {
            blackBishops++;
            int sq = Long.numberOfTrailingZeros(tempBBishops);
            int rank = sq / 8;
            int mirrored = (7 - rank) * 8 + (sq % 8);
            score -= 320 + BISHOPS[mirrored];
            long attacks = BitboardHelper.getBishopAttacks(sq, occupied);
            int mobility = Long.bitCount(attacks & ~bPieces);
            score -= (mobility - 5) * 4;
            if (rank >= 2 && rank <= 4 && (bPawns & PAWN_SUPPORT_MASK[PieceColor.BLACK.ordinal()][sq]) != 0L && (wPawns & PASSED_PAWN_MASK[PieceColor.BLACK.ordinal()][sq]) == 0L) score -= 15;
            tempBBishops &= tempBBishops - 1;
        }
        if (whiteBishops >= 2) score += 40;
        if (blackBishops >= 2) score -= 40;

        // --- ROOKS (with mobility) ---
        long tempWRooks = wRooks;
        while (tempWRooks != 0) {
            int sq = Long.numberOfTrailingZeros(tempWRooks);
            int rank = sq / 8;
            score += 500 + ROOKS[sq];
            // Mobility: count squares not blocked by own pieces
            long attacks = BitboardHelper.getRookAttacks(sq, occupied);
            int mobility = Long.bitCount(attacks & ~wPieces);
            score += (mobility - 7) * 3;  // baseline 7 squares, 3cp per extra square
            if (rank == 6) score += 20;
            tempWRooks &= tempWRooks - 1;
        }
        long tempBRooks = bRooks;
        while (tempBRooks != 0) {
            int sq = Long.numberOfTrailingZeros(tempBRooks);
            int rank = sq / 8;
            int mirrored = (7 - rank) * 8 + (sq % 8);
            score -= 500 + ROOKS[mirrored];
            long attacks = BitboardHelper.getRookAttacks(sq, occupied);
            int mobility = Long.bitCount(attacks & ~bPieces);
            score -= (mobility - 7) * 3;
            if (rank == 1) score -= 20;
            tempBRooks &= tempBRooks - 1;
        }

        // --- QUEENS ---
        long tempWQueens = wQueens;
        while (tempWQueens != 0) {
            int sq = Long.numberOfTrailingZeros(tempWQueens);
            score += 900 + QUEENS[sq];
            tempWQueens &= tempWQueens - 1;
        }
        long tempBQueens = bQueens;
        while (tempBQueens != 0) {
            int sq = Long.numberOfTrailingZeros(tempBQueens);
            int mirrored = (7 - (sq / 8)) * 8 + (sq % 8);
            score -= 900 + QUEENS[mirrored];
            tempBQueens &= tempBQueens - 1;
        }

        // --- KING (PST + King Safety) ---
        if (wKingSq >= 0) {
            score += KING[wKingSq] * (1 - factor) + KING_END[wKingSq] * factor;
            // King Safety (only in middlegame)
            if (factor < 0.7) {
                score += evaluateKingSafety(board, PieceColor.WHITE, wKingSq, occupied,
                        wPawns, bPawns, bKnights, bBishops, bRooks, bQueens, bPieces, factor);
            }
        }
        if (bKingSq >= 0) {
            int mirrored = (7 - (bKingSq / 8)) * 8 + (bKingSq % 8);
            score -= KING[mirrored] * (1 - factor) + KING_END[mirrored] * factor;
            if (factor < 0.7) {
                score -= evaluateKingSafety(board, PieceColor.BLACK, bKingSq, occupied,
                        bPawns, wPawns, wKnights, wBishops, wRooks, wQueens, wPieces, factor);
            }
        }

        // --- PAWN STRUCTURE & ROOK FILES ---
        for (int file = 0; file < 8; file++) {
            int wP = whitePawnsOnFile[file];
            int bP = blackPawnsOnFile[file];
            
            long fileMask = 0x0101010101010101L << file;
            if ((wRooks & fileMask) != 0L) {
                if (wP == 0 && bP == 0) score += 25;
                else if (wP == 0) score += 12;
            }
            if ((bRooks & fileMask) != 0L) {
                if (wP == 0 && bP == 0) score -= 25;
                else if (bP == 0) score -= 12;
            }
            
            if (wP > 1) score -= 8 * (wP - 1);
            if (bP > 1) score += 8 * (bP - 1);
            
            if (wP > 0) {
                boolean isolated = true;
                if (file > 0 && whitePawnsOnFile[file - 1] > 0) isolated = false;
                if (file < 7 && whitePawnsOnFile[file + 1] > 0) isolated = false;
                if (isolated) score -= 10 * wP;
            }
            if (bP > 0) {
                boolean isolated = true;
                if (file > 0 && blackPawnsOnFile[file - 1] > 0) isolated = false;
                if (file < 7 && blackPawnsOnFile[file + 1] > 0) isolated = false;
                if (isolated) score += 10 * bP;
            }
        }

        return activeColor == PieceColor.WHITE ? score : -score;
    }

    /**
     * King Safety evaluation from the perspective of the defending king.
     * Returns a POSITIVE score = safer king, NEGATIVE = king in danger.
     *
     * @param kingColor  the color of the king being evaluated
     * @param kingSq     square of the king
     * @param occupied   full board occupancy
     * @param ownPawns   friendly pawns
     * @param enemyPawns enemy pawns
     * @param enemyKnights/Bishops/Rooks/Queens  enemy piece bitboards
     * @param enemyPieces all enemy pieces combined
     * @param factor     endgame factor (0 = all pieces, 1 = endgame)
     */
    private double evaluateKingSafety(Board board, PieceColor kingColor, int kingSq, long occupied,
                                       long ownPawns, long enemyPawns,
                                       long enemyKnights, long enemyBishops,
                                       long enemyRooks, long enemyQueens,
                                       long enemyPieces, double factor) {
        int safety = 0;

        // --- Pawn Shield ---
        int kingFile = kingSq % 8;
        int kingRank = kingSq / 8;
        boolean isWhite = (kingColor == PieceColor.WHITE);
        int shieldRank1 = isWhite ? kingRank + 1 : kingRank - 1;
        int shieldRank2 = isWhite ? kingRank + 2 : kingRank - 2;

        int fMin = Math.max(0, kingFile - 1);
        int fMax = Math.min(7, kingFile + 1);
        for (int f = fMin; f <= fMax; f++) {
            if (shieldRank1 >= 0 && shieldRank1 <= 7) {
                if ((ownPawns & (1L << (shieldRank1 * 8 + f))) != 0) {
                    safety += 12;
                } else if (shieldRank2 >= 0 && shieldRank2 <= 7 &&
                           (ownPawns & (1L << (shieldRank2 * 8 + f))) != 0) {
                    safety += 4;
                } else {
                    safety -= 14;
                }
            }
        }

        // --- Open/Semi-open files near king ---
        for (int f = fMin; f <= fMax; f++) {
            long fileMask = 0x0101010101010101L << f;
            if ((ownPawns & fileMask) == 0) {
                safety -= 18;
                if ((enemyPawns & fileMask) == 0) {
                    safety -= 12;
                }
            }
        }

        // --- Enemy queen proximity penalty ---
        if (enemyQueens != 0) {
            int queenSq = Long.numberOfTrailingZeros(enemyQueens);
            int queenDist = Math.max(Math.abs(queenSq % 8 - kingFile), Math.abs(queenSq / 8 - kingRank));
            if (queenDist <= 3) {
                safety -= (4 - queenDist) * 15;
            }
        }

        // --- Enemy piece count near king (cheap: just bitCount on king zone intersection) ---
        long kingZone = BitboardHelper.getKingAttacks(kingSq);
        int enemyNearKing = Long.bitCount(kingZone & enemyPieces);
        safety -= enemyNearKing * 10;

        // Scale by game phase
        return safety * (1.0 - factor);
    }



    private int getPieceValue(PieceType pt) {
        if (pt == null) return 0;
        switch(pt) {
            case PAWN: return 100;
            case KNIGHT: return 300;
            case BISHOP: return 320;
            case ROOK: return 500;
            case QUEEN: return 900;
            case KING: return 20000;
            default: return 0;
        }
    }

    public int see(Board board, short move) {
        int fromSq = CompactMove.getStartSquare(move);
        int toSq = CompactMove.getTargetSquare(move);
        Piece targetPiece = board.getPieceAtIndex(toSq);
        int moveFlag = CompactMove.getMoveFlag(move);
        
        int targetValue = 0;
        if (targetPiece != null) targetValue = getPieceValue(targetPiece.getType());
        else if (moveFlag == CompactMove.EnPassantCaptureFlag) targetValue = getPieceValue(PieceType.PAWN);
        
        int attackerValue = getPieceValue(board.getPieceAtIndex(fromSq).getType());
        if (moveFlag >= CompactMove.PromoteToQueenFlag) {
            PieceType promo = CompactMove.getPromotionPieceType(move);
            if (promo != null) {
                targetValue += getPieceValue(promo) - getPieceValue(PieceType.PAWN);
                attackerValue = getPieceValue(promo);
            }
        }
        
        int[] gain = seeGain;
        gain[0] = targetValue;
        
        long occupied = board.getOccupiedBitboard();
        occupied ^= (1L << fromSq);
        if (moveFlag == CompactMove.EnPassantCaptureFlag) {
            PieceColor c = board.getPieceAtIndex(fromSq).getColor();
            int epSq = c == PieceColor.WHITE ? toSq - 8 : toSq + 8;
            occupied ^= (1L << epSq);
        }
        
        PieceColor colorToMove = board.getPieceAtIndex(fromSq).getColor().opposite();
        int d = 0;
        
        while (true) {
            d++;
            long attackers = BitboardHelper.getAttackers(board, toSq, occupied);
            long currentAttackers = attackers & board.getColorBitboard(colorToMove) & occupied;
            
            if (currentAttackers == 0) break;
            
            int lowestAttackerSq = -1;
            int lowestAttackerVal = Integer.MAX_VALUE;
            
            long tempAttackers = currentAttackers;
            while (tempAttackers != 0) {
                int sq = Long.numberOfTrailingZeros(tempAttackers);
                Piece p = board.getPieceAtIndex(sq);
                if (p != null) {
                    int pVal = getPieceValue(p.getType());
                    if (pVal < lowestAttackerVal) {
                        lowestAttackerVal = pVal;
                        lowestAttackerSq = sq;
                    }
                }
                tempAttackers &= tempAttackers - 1;
            }
            
            gain[d] = attackerValue - gain[d - 1];
            attackerValue = lowestAttackerVal;
            occupied ^= (1L << lowestAttackerSq);
            colorToMove = colorToMove.opposite();
        }
        
        while (--d > 0) {
            gain[d - 1] = -Math.max(-gain[d - 1], gain[d]);
        }
        
        return gain[0];
    }

    private static class TT16 {
        public static final byte FLAG_NONE = 0;
        public static final byte FLAG_EXACT = 1;
        public static final byte FLAG_LOWERBOUND = 2;
        public static final byte FLAG_UPPERBOUND = 3;

        private static final double MATE_THRESHOLD = 25000.0;

        private final int mask = (1 << 20) - 1;
        private final long[] keys = new long[1 << 20];
        private final double[] scores = new double[1 << 20];
        private final int[] depths = new int[1 << 20];
        private final byte[] flags = new byte[1 << 20];
        private final short[] bestMoves = new short[1 << 20];

        private final TTEntry probeResult = new TTEntry(0.0, 0, (byte)0, (short)0);

        public static class TTEntry {
            public double score;
            public int depth;
            public byte flag;
            public short bestMove;
            public TTEntry(double score, int depth, byte flag, short bestMove) {
                this.score = score; this.depth = depth; this.flag = flag; this.bestMove = bestMove;
            }
        }

        public TTEntry probe(long key, int ply) {
            int idx = (int) (key & mask);
            if (flags[idx] != FLAG_NONE && keys[idx] == key) {
                double score = scores[idx];
                if (score >= MATE_THRESHOLD) score -= ply;
                else if (score <= -MATE_THRESHOLD) score += ply;
                probeResult.score = score;
                probeResult.depth = depths[idx];
                probeResult.flag = flags[idx];
                probeResult.bestMove = bestMoves[idx];
                return probeResult;
            }
            return null;
        }

        public void store(long key, int depth, byte flag, double score, short bestMove, int ply) {
            int idx = (int) (key & mask);
            if (flags[idx] == FLAG_NONE || keys[idx] == key || depth >= depths[idx]) {
                keys[idx] = key; depths[idx] = depth; flags[idx] = flag;
                if (bestMove != 0 || keys[idx] != key) bestMoves[idx] = bestMove;
                if (score >= MATE_THRESHOLD) scores[idx] = score + ply;
                else if (score <= -MATE_THRESHOLD) scores[idx] = score - ply;
                else scores[idx] = score;
            }
        }
    }
}
