package com.jchess.tournament;

import com.jchess.core.engine.ChessEngine;

/**
 * Erfasst aggregierte Statistiken einer Engine in einem Turnier.
 */
public class EngineStats implements Comparable<EngineStats> {

    private final ChessEngine engine;
    private int wins = 0;
    private int draws = 0;
    private int losses = 0;

    private int whiteWins = 0;
    private int whiteDraws = 0;
    private int whiteLosses = 0;

    private int blackWins = 0;
    private int blackDraws = 0;
    private int blackLosses = 0;

    public EngineStats(ChessEngine engine) {
        this.engine = engine;
    }

    public void recordGame(GameResult result) {
        boolean isWhite = result.getWhiteEngine() == engine;
        boolean isBlack = result.getBlackEngine() == engine;

        if (!isWhite && !isBlack) return;

        if (result.isDraw()) {
            draws++;
            if (isWhite) whiteDraws++;
            else blackDraws++;
        } else if (result.getWinnerEngine() == engine) {
            wins++;
            if (isWhite) whiteWins++;
            else blackWins++;
        } else {
            losses++;
            if (isWhite) whiteLosses++;
            else blackLosses++;
        }
    }

    public ChessEngine getEngine() {
        return engine;
    }

    public String getName() {
        return engine.getName();
    }

    public int getWins() {
        return wins;
    }

    public int getDraws() {
        return draws;
    }

    public int getLosses() {
        return losses;
    }

    public int getGamesPlayed() {
        return wins + draws + losses;
    }

    public double getPoints() {
        return wins * 1.0 + draws * 0.5;
    }

    public double getPointsPercentage() {
        int played = getGamesPlayed();
        return played > 0 ? (getPoints() / played) * 100.0 : 0.0;
    }

    public int getWhiteWins() {
        return whiteWins;
    }

    public int getWhiteDraws() {
        return whiteDraws;
    }

    public int getWhiteLosses() {
        return whiteLosses;
    }

    public int getBlackWins() {
        return blackWins;
    }

    public int getBlackDraws() {
        return blackDraws;
    }

    public int getBlackLosses() {
        return blackLosses;
    }

    @Override
    public int compareTo(EngineStats o) {
        // Höchste Punktzahl zuerst
        int cmp = Double.compare(o.getPoints(), this.getPoints());
        if (cmp != 0) return cmp;

        // Bei Punktgleichheit: Mehr Siege bevorzugen
        cmp = Integer.compare(o.wins, this.wins);
        if (cmp != 0) return cmp;

        // Alphabetisch nach Name
        return this.getName().compareToIgnoreCase(o.getName());
    }
}
