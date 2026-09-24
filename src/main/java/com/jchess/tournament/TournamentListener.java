package com.jchess.tournament;

import com.jchess.core.engine.ChessEngine;
import java.util.List;

/**
 * Listener-Schnittstelle zur Verfolgung des Turnierfortschritts.
 */
public interface TournamentListener {

    default void onTournamentStarted(int totalGames, List<ChessEngine> participants) {}

    default void onGameStarted(int gameNumber, int totalGames, ChessEngine white, ChessEngine black) {}

    default void onGameFinished(int gameNumber, int totalGames, GameResult result) {}

    default void onTournamentFinished(List<EngineStats> ranking, List<GameResult> allResults) {}
}
