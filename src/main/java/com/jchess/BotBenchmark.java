package com.jchess;

import com.jchess.core.board.Board;
import com.jchess.core.piece.PieceColor;
import engine.bots.KingLBot1;
import engine.bots.JChessV2;
import engine.bots.JChessV3;

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
        
        System.out.println("\n--- KingLBot1 Results ---");
        System.out.println("Time elapsed:  " + elapsed + " ms");
        System.out.println("Best move:     " + bestMove);
        System.out.println("Depth reached: " + bot.getLastDepthReached() + " ply");
        System.out.println("Nodes eval'd:  " + String.format("%,d", bot.getLastNodesEvaluated()));
        System.out.println("NPS:           " + String.format("%,d", bot.getLastNps()));
        
        System.out.println("\n=== JChessV2 Benchmark ===");
        JChessV2 v2Bot = new JChessV2("JChessV2", 64, false, 1000L, 0.05);

        startTime = System.currentTimeMillis();
        bestMove = v2Bot.findBestMove(board, PieceColor.WHITE);
        elapsed = System.currentTimeMillis() - startTime;

        System.out.println("\n--- JChessV2 Results ---");
        System.out.println("Time elapsed:  " + elapsed + " ms");
        System.out.println("Best move:     " + bestMove);
        System.out.println("Depth reached: " + v2Bot.getLastDepthReached() + " ply");
        System.out.println("Nodes eval'd:  " + String.format("%,d", v2Bot.getLastNodesEvaluated()));
        System.out.println("NPS:           " + String.format("%,d", v2Bot.getLastNps()));

        System.out.println("\n=== JChessV3 Benchmark ===");
        JChessV3 v3Bot = new JChessV3("JChessV3", 64, false, 1000L, 0.05);
        
        startTime = System.currentTimeMillis();
        bestMove = v3Bot.findBestMove(board, PieceColor.WHITE);
        elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println("\n--- JChessV3 Results ---");
        System.out.println("Time elapsed:  " + elapsed + " ms");
        System.out.println("Best move:     " + bestMove);
        System.out.println("Depth reached: " + v3Bot.getLastDepthReached() + " ply");
        System.out.println("Nodes eval'd:  " + String.format("%,d", v3Bot.getLastNodesEvaluated()));
        System.out.println("NPS:           " + String.format("%,d", v3Bot.getLastNps()));
    }
}