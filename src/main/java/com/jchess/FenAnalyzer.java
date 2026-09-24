package com.jchess;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.piece.PieceColor;

public class FenAnalyzer {
    public static void main(String[] args) {
        String fen = "r7/pb6/3b1p2/1p3p2/1PpPkPp1/2P3Pp/R3N2P/2N3K1 b - - 94 144";
        Board board = new Board();
        board.loadFen(fen);
        
        System.out.println("Finding best move for Black...");
        engine.bots.JChessV1 bot = new engine.bots.JChessV1("Analyzer", 15000L, 0.10);
        bot.findBestMoveWithListener(board, PieceColor.BLACK, null, (depth, score, bestMove, nodes, elapsedMs) -> {
            System.out.printf("  Depth: %2d | Score: %6.2f | BestMove: %s%n", depth, score/100.0, bestMove);
        });
    }
}
