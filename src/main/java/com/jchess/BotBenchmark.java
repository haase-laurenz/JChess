package com.jchess;

import com.jchess.core.board.Board;
import com.jchess.core.piece.PieceColor;
import engine.bots.KingLBot1;

public class BotBenchmark {
    public static void main(String[] args) {
        System.out.println("=== KingLBot1 Benchmark ===");
        Board board = new Board();
        
        System.out.println("Warming up JVM (100ms search)...");
        KingLBot1 warmupBot = new KingLBot1(100L);
        warmupBot.findBestMove(board, PieceColor.WHITE);
        
        System.out.println("\nRunning 1-second benchmark...");
        KingLBot1 bot = new KingLBot1(1000L);
        
        long startTime = System.currentTimeMillis();
        com.jchess.core.board.Move bestMove = bot.findBestMove(board, PieceColor.WHITE);
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println("\n--- Benchmark Results ---");
        System.out.println("Time elapsed:  " + elapsed + " ms");
        System.out.println("Best move:     " + bestMove);
        System.out.println("Depth reached: " + bot.getLastDepthReached() + " ply");
        System.out.println("Nodes eval'd:  " + String.format("%,d", bot.getLastNodesEvaluated()));
        System.out.println("NPS:           " + String.format("%,d", bot.getLastNps()));
        System.out.println("TT Hits:       " + String.format("%,d", bot.getTranspositionTable().getHits()));
    }
}