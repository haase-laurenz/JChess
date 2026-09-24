package engine.bots;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.engine.ChessEngine;
import com.jchess.core.piece.PieceColor;

/**
 * Weiterleitung / Wrapper für /engine/bots/Random.java.
 * Ermöglicht die Verwendung von 'engine.bots.Random' ebenso wie 'com.jchess.engine.bots.Random'.
 */
public class Random implements ChessEngine {

    private final com.jchess.engine.bots.Random delegate;

    public Random() {
        this.delegate = new com.jchess.engine.bots.Random();
    }

    public Random(String name) {
        this.delegate = new com.jchess.engine.bots.Random(name);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Move findBestMove(Board board, PieceColor color) {
        return delegate.findBestMove(board, color);
    }
}
