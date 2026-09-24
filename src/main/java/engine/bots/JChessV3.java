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
        short[] legalMoves = new short[256];
        int legalCount = generateLegalMoves(board, color, legalMoves);
        
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
        short[] moves = new short[256];
        int count = generateLegalMoves(board, color, moves);

        TT16.TTEntry ttEntry = transpositionTable.probe(board.getZobristKey(), 0);
        short preferredMove = (pvMove != 0) ? pvMove : (ttEntry != null ? ttEntry.bestMove : 0);
        orderMoves(moves, count, preferredMove, 0, color, board);

        double maxScore = -MATE_SCORE;

        for (int i=0; i<count; i++) {
            if (stopRequested || Thread.currentThread().isInterrupted() || System.currentTimeMillis() >= deadline) {
                timeExceeded = true;
                break;
            }
            short move = moves[i];
            board.makeMove(CompactMove.toStandardMove(move, board));
            double score = -search(board, -beta, -alpha, depth - 1, 1, 0, color.opposite(), deadline, true);
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

        short[] moves = new short[256];
        int count = generateLegalMoves(board, color, moves);

        if (count == 0) return inCheck ? -MATE_SCORE + ply : 0.0;

        short ttMove = (ttEntry != null) ? ttEntry.bestMove : 0;
        orderMoves(moves, count, ttMove, ply, color, board);

        double originalAlpha = alpha;
        short bestMove = 0;
        double maxScore = -MATE_SCORE;

        boolean fPrune = false;
        if (effectiveDepth <= 2 && !inCheck && alpha > -MATE_SCORE + 100 && beta < MATE_SCORE - 100) {
            double staticEval = evaluate(board, color);
            if (staticEval + effectiveDepth * 200 <= alpha) fPrune = true;
        }

        for (int i=0; i<count; i++) {
            short move = moves[i];
            Move stdMove = CompactMove.toStandardMove(move, board);
            board.makeMove(stdMove);
            boolean givesCheck = MoveGenerator.isKingInCheck(board, color.opposite());
            boolean isQuiet = !stdMove.isCapture() && !stdMove.isPromotion() && !givesCheck;
            
            if (fPrune && i > 0 && isQuiet) {
                board.undoMove();
                continue;
            }

            double score;
            if (depth >= 3 && i >= 4 && !inCheck && isQuiet) {
                int reduction = (i > 6) ? 2 : 1;
                score = -search(board, -beta, -alpha, depth - 1 + extension - reduction, ply + 1, numExtensions + extension, color.opposite(), deadline, true);
                if (score > alpha) {
                    score = -search(board, -beta, -alpha, depth - 1 + extension, ply + 1, numExtensions + extension, color.opposite(), deadline, true);
                }
            } else {
                score = -search(board, -beta, -alpha, depth - 1 + extension, ply + 1, numExtensions + extension, color.opposite(), deadline, true);
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
                    historyTable[color.ordinal()][CompactMove.getStartSquare(move)][CompactMove.getTargetSquare(move)] += depth * depth;
                }
                break;
            }
        }

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
            short[] evasions = new short[256];
            int count = generateLegalMoves(board, color, evasions);
            if (count == 0) return -MATE_SCORE + ply;
            orderMoves(evasions, count, (short)0, ply, color, board);
            for (int i=0; i<count; i++) {
                board.makeMove(CompactMove.toStandardMove(evasions[i], board));
                double score = -quiescence(board, -beta, -alpha, color.opposite(), ply + 1, deadline);
                board.undoMove();
                if (timeExceeded || stopRequested) return 0.0;
                if (score >= beta) return beta;
                if (score > alpha) alpha = score;
            }
            return alpha;
        }

        double standPat = evaluate(board, color);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;

        short[] moves = new short[256];
        int count = generatePseudoLegalMoves(board, color, moves);
        short[] tacticalMoves = new short[256];
        int tCount = 0;
        for (int i=0; i<count; i++) {
            short m = moves[i];
            if (CompactMove.getMoveFlag(m) >= CompactMove.PromoteToQueenFlag || board.getPieceAtIndex(CompactMove.getTargetSquare(m)) != null || CompactMove.getMoveFlag(m) == CompactMove.EnPassantCaptureFlag) {
                board.makeMove(CompactMove.toStandardMove(m, board));
                if (!MoveGenerator.isKingInCheck(board, color)) tacticalMoves[tCount++] = m;
                board.undoMove();
            }
        }
        
        if (tCount == 0) return standPat;
        orderMoves(tacticalMoves, tCount, (short)0, ply, color, board);

        for (int i=0; i<tCount; i++) {
            short move = tacticalMoves[i];
            board.makeMove(CompactMove.toStandardMove(move, board));
            double score = -quiescence(board, -beta, -alpha, color.opposite(), ply + 1, deadline);
            board.undoMove();
            if (timeExceeded || stopRequested) return 0.0;
            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    private void orderMoves(short[] moves, int count, short ttMove, int ply, PieceColor color, Board board) {
        int[] scores = new int[count];
        for (int i = 0; i < count; i++) {
            short m = moves[i];
            if (m == ttMove) {
                scores[i] = 10000000;
                continue;
            }
            int score = 0;
            int fromSq = CompactMove.getStartSquare(m);
            int toSq = CompactMove.getTargetSquare(m);
            Piece target = board.getPieceAtIndex(toSq);
            if (target != null) {
                score += 1000000 + target.getType().getBaseValue() * 10 - board.getPieceAtIndex(fromSq).getType().getBaseValue();
            } else if (CompactMove.getMoveFlag(m) == CompactMove.EnPassantCaptureFlag) {
                score += 1000000 + 1000 - board.getPieceAtIndex(fromSq).getType().getBaseValue();
            }
            if (CompactMove.getMoveFlag(m) >= CompactMove.PromoteToQueenFlag) {
                PieceType promo = CompactMove.getPromotionPieceType(m);
                score += 900000 + (promo != null ? promo.getBaseValue() : 0);
            }
            if (target == null && !(CompactMove.getMoveFlag(m) >= CompactMove.PromoteToQueenFlag)) {
                if (ply < killerMoves.length) {
                    if (m == killerMoves[ply][0]) score += 90000;
                    else if (m == killerMoves[ply][1]) score += 80000;
                }
                score += historyTable[color.ordinal()][fromSq][toSq];
            }
            scores[i] = score;
        }
        for (int i = 0; i < count - 1; i++) {
            for (int j = i + 1; j < count; j++) {
                if (scores[j] > scores[i]) {
                    int tempScore = scores[i]; scores[i] = scores[j]; scores[j] = tempScore;
                    short tempMove = moves[i]; moves[i] = moves[j]; moves[j] = tempMove;
                }
            }
        }
    }

    private int generateLegalMoves(Board board, PieceColor color, short[] moves) {
        short[] pseudo = new short[256];
        int pCount = generatePseudoLegalMoves(board, color, pseudo);
        int lCount = 0;
        for (int i = 0; i < pCount; i++) {
            short m = pseudo[i];
            board.makeMove(CompactMove.toStandardMove(m, board));
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

        totalPieces += Long.bitCount(wKnights) + Long.bitCount(bKnights) +
                       Long.bitCount(wBishops) + Long.bitCount(bBishops) +
                       Long.bitCount(wRooks)   + Long.bitCount(bRooks)   +
                       Long.bitCount(wQueens)  + Long.bitCount(bQueens);

        double factor = Math.max(0.0, Math.min(1.0, (16.0 - totalPieces) / 16.0));

        long pawnsW = board.getBitboard(PieceType.PAWN, PieceColor.WHITE);
        while (pawnsW != 0) {
            int sq = Long.numberOfTrailingZeros(pawnsW);
            score += 100 + PAWNS[sq] * (1 - factor) + PAWNS_END[sq] * factor;
            pawnsW &= pawnsW - 1;
        }
        long pawnsB = board.getBitboard(PieceType.PAWN, PieceColor.BLACK);
        while (pawnsB != 0) {
            int sq = Long.numberOfTrailingZeros(pawnsB);
            int mirrored = (7 - (sq / 8)) * 8 + (sq % 8);
            score -= 100 + PAWNS[mirrored] * (1 - factor) + PAWNS_END[mirrored] * factor;
            pawnsB &= pawnsB - 1;
        }
        
        while (wKnights != 0) {
            int sq = Long.numberOfTrailingZeros(wKnights);
            score += 300 + KNIGHTS[sq];
            wKnights &= wKnights - 1;
        }
        while (bKnights != 0) {
            int sq = Long.numberOfTrailingZeros(bKnights);
            int mirrored = (7 - (sq / 8)) * 8 + (sq % 8);
            score -= 300 + KNIGHTS[mirrored];
            bKnights &= bKnights - 1;
        }

        while (wBishops != 0) {
            int sq = Long.numberOfTrailingZeros(wBishops);
            score += 320 + BISHOPS[sq];
            wBishops &= wBishops - 1;
        }
        while (bBishops != 0) {
            int sq = Long.numberOfTrailingZeros(bBishops);
            int mirrored = (7 - (sq / 8)) * 8 + (sq % 8);
            score -= 320 + BISHOPS[mirrored];
            bBishops &= bBishops - 1;
        }

        while (wRooks != 0) {
            int sq = Long.numberOfTrailingZeros(wRooks);
            score += 500 + ROOKS[sq];
            wRooks &= wRooks - 1;
        }
        while (bRooks != 0) {
            int sq = Long.numberOfTrailingZeros(bRooks);
            int mirrored = (7 - (sq / 8)) * 8 + (sq % 8);
            score -= 500 + ROOKS[mirrored];
            bRooks &= bRooks - 1;
        }

        while (wQueens != 0) {
            int sq = Long.numberOfTrailingZeros(wQueens);
            score += 900 + QUEENS[sq];
            wQueens &= wQueens - 1;
        }
        while (bQueens != 0) {
            int sq = Long.numberOfTrailingZeros(bQueens);
            int mirrored = (7 - (sq / 8)) * 8 + (sq % 8);
            score -= 900 + QUEENS[mirrored];
            bQueens &= bQueens - 1;
        }

        int wKingSq = board.getKingSquare(PieceColor.WHITE);
        if (wKingSq >= 0) score += KING[wKingSq] * (1 - factor) + KING_END[wKingSq] * factor;
        
        int bKingSq = board.getKingSquare(PieceColor.BLACK);
        if (bKingSq >= 0) {
            int mirrored = (7 - (bKingSq / 8)) * 8 + (bKingSq % 8);
            score -= KING[mirrored] * (1 - factor) + KING_END[mirrored] * factor;
        }

        return activeColor == PieceColor.WHITE ? score : -score;
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
