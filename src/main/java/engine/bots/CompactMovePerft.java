package engine.bots;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.rules.MoveGenerator;

import java.util.List;

public class CompactMovePerft {

    public static void main(String[] args) {
        Board board = new Board();
        System.out.println("=== CompactMove (16-bit) Perft Benchmark ===");
        
        for (int depth = 1; depth <= 6; depth++) {
            long startTime = System.currentTimeMillis();
            long nodes = perftRecursive(board, depth);
            long elapsed = System.currentTimeMillis() - startTime;
            
            System.out.println("Depth " + depth + ": " + nodes + " nodes. Time: " + elapsed + "ms");
        }
    }

    private static long perftRecursive(Board board, int depth) {
        if (depth == 0) return 1;

        PieceColor sideToMove = board.getActivePlayer();
        List<Move> stdMoves = MoveGenerator.generatePseudoLegalMoves(board, sideToMove);
        
        // Convert to our compact 16-bit format
        short[] compactMoves = new short[stdMoves.size()];
        for (int i = 0; i < stdMoves.size(); i++) {
            compactMoves[i] = CompactMove.fromStandardMove(stdMoves.get(i));
        }

        long nodes = 0;

        for (int i = 0; i < compactMoves.length; i++) {
            short compactMove = compactMoves[i];
            
            // Reconstruct the full move using just the 16 bits and the board state
            Move decodedMove = CompactMove.toStandardMove(compactMove, board);

            board.makeMove(decodedMove);

            // Check legality inline (pseudo-legal traversal)
            if (!MoveGenerator.isKingInCheck(board, sideToMove)) {
                nodes += perftRecursive(board, depth - 1);
            }

            board.undoMove();
        }

        return nodes;
    }
}
