package com.jchess.engine.bots;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.clock.ChessClock;
import com.jchess.core.engine.ChessEngine;
import com.jchess.core.piece.PieceColor;

/**
 * Weiterleitung / Wrapper für engine.bots.KingLBot1.
 * Ermöglicht die Verwendung von 'com.jchess.engine.bots.KingLBot1' ebenso wie 'engine.bots.KingLBot1'.
 */
public class KingLBot1 implements ChessEngine {

    private final engine.bots.KingLBot1 delegate;

    public KingLBot1() {
        this.delegate = new engine.bots.KingLBot1();
    }

    public KingLBot1(long defaultTimeLimitMs) {
        this.delegate = new engine.bots.KingLBot1(defaultTimeLimitMs);
    }

    public KingLBot1(double remainingTimePercentage) {
        this.delegate = new engine.bots.KingLBot1(remainingTimePercentage);
    }

    public KingLBot1(long defaultTimeLimitMs, double remainingTimePercentage) {
        this.delegate = new engine.bots.KingLBot1(defaultTimeLimitMs, remainingTimePercentage);
    }

    public KingLBot1(String name, long defaultTimeLimitMs, double remainingTimePercentage) {
        this.delegate = new engine.bots.KingLBot1(name, defaultTimeLimitMs, remainingTimePercentage);
    }

    public KingLBot1(int searchDepth) {
        this.delegate = new engine.bots.KingLBot1(searchDepth);
    }

    public KingLBot1(String name, int searchDepth) {
        this.delegate = new engine.bots.KingLBot1(name, searchDepth);
    }

    public KingLBot1(String name, int searchDepth, boolean useFixedDepth) {
        this.delegate = new engine.bots.KingLBot1(name, searchDepth, useFixedDepth);
    }

    public KingLBot1(String name, int searchDepth, boolean useFixedDepth, long defaultTimeLimitMs) {
        this.delegate = new engine.bots.KingLBot1(name, searchDepth, useFixedDepth, defaultTimeLimitMs);
    }

    public KingLBot1(String name, int searchDepth, boolean useFixedDepth, long defaultTimeLimitMs, double remainingTimePercentage) {
        this.delegate = new engine.bots.KingLBot1(name, searchDepth, useFixedDepth, defaultTimeLimitMs, remainingTimePercentage);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Move findBestMove(Board board, PieceColor color) {
        return delegate.findBestMove(board, color);
    }

    @Override
    public Move findBestMove(Board board, PieceColor color, ChessClock clock) {
        return delegate.findBestMove(board, color, clock);
    }

    public Move findBestMoveWithListener(Board board, PieceColor color, engine.bots.KingLBot1.DepthListener listener) {
        return delegate.findBestMoveWithListener(board, color, null, listener);
    }

    public Move findBestMoveWithListener(Board board, PieceColor color, ChessClock clock, engine.bots.KingLBot1.DepthListener listener) {
        return delegate.findBestMoveWithListener(board, color, clock, listener);
    }

    public void stopSearch() {
        delegate.stopSearch();
    }

    public boolean isStopRequested() {
        return delegate.isStopRequested();
    }

    public double evaluate(Board board, PieceColor color) {
        return delegate.evaluate(board, color);
    }

    public int getSearchDepth() {
        return delegate.getSearchDepth();
    }

    public void setSearchDepth(int searchDepth) {
        delegate.setSearchDepth(searchDepth);
    }

    public boolean isUseFixedDepth() {
        return delegate.isUseFixedDepth();
    }

    public void setUseFixedDepth(boolean useFixedDepth) {
        delegate.setUseFixedDepth(useFixedDepth);
    }

    public double getRemainingTimePercentage() {
        return delegate.getRemainingTimePercentage();
    }

    public void setRemainingTimePercentage(double remainingTimePercentage) {
        delegate.setRemainingTimePercentage(remainingTimePercentage);
    }

    public long getDefaultTimeLimitMs() {
        return delegate.getDefaultTimeLimitMs();
    }

    public void setDefaultTimeLimitMs(long timeLimitMs) {
        delegate.setDefaultTimeLimitMs(timeLimitMs);
    }

    public long getLastNodesEvaluated() {
        return delegate.getLastNodesEvaluated();
    }

    public int getLastDepthReached() {
        return delegate.getLastDepthReached();
    }

    public long getLastSearchTimeMs() {
        return delegate.getLastSearchTimeMs();
    }

    public long getLastNps() {
        return delegate.getLastNps();
    }

    public engine.bots.TranspositionTable getTranspositionTable() {
        return delegate.getTranspositionTable();
    }

    public void clearTranspositionTable() {
        delegate.clearTranspositionTable();
    }
}
