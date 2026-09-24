package com.jchess.tournament;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.clock.ChessClock;
import com.jchess.core.engine.ChessEngine;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.rules.GameStatus;
import com.jchess.core.rules.MoveGenerator;

import java.util.*;

/**
 * Kernklasse des JChess Turnier-Frameworks.
 * Ermöglicht automatisierte Rundenturniere (Round-Robin) oder Head-to-Head-Duelle
 * zwischen verschiedenen ChessEngine-Implementierungen mit Farbentausch,
 * Zeiterfassung, Regelüberwachung und Auswertung.
 */
public class Tournament {

    private final String tournamentName;
    private final TournamentConfig config;
    private final List<ChessEngine> participants = new ArrayList<>();
    private final List<GameResult> results = new ArrayList<>();
    private final Map<ChessEngine, EngineStats> statsMap = new LinkedHashMap<>();

    public Tournament(String tournamentName) {
        this(tournamentName, new TournamentConfig());
    }

    public Tournament(String tournamentName, TournamentConfig config) {
        this.tournamentName = tournamentName;
        this.config = config != null ? config : new TournamentConfig();
    }

    public void addParticipant(ChessEngine engine) {
        if (engine != null && !participants.contains(engine)) {
            participants.add(engine);
            statsMap.put(engine, new EngineStats(engine));
        }
    }

    public void addParticipants(Collection<ChessEngine> engines) {
        if (engines != null) {
            for (ChessEngine e : engines) {
                addParticipant(e);
            }
        }
    }

    public List<ChessEngine> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public List<GameResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    public List<EngineStats> getRanking() {
        List<EngineStats> list = new ArrayList<>(statsMap.values());
        Collections.sort(list);
        return list;
    }

    /**
     * Startet das Turnier und führt alle Paarungen aus.
     */
    public List<EngineStats> run() {
        return run(new DefaultConsoleTournamentListener());
    }

    public List<EngineStats> run(TournamentListener listener) {
        if (participants.size() < 2) {
            throw new IllegalStateException("Mindestens 2 Teilnehmer für ein Turnier erforderlich.");
        }

        results.clear();
        for (EngineStats s : statsMap.values()) {
            // Stats zurücksetzen
            statsMap.put(s.getEngine(), new EngineStats(s.getEngine()));
        }

        // Paarungen generieren
        List<Pairing> pairings = generatePairings();
        int totalGames = pairings.size();

        if (listener != null) {
            listener.onTournamentStarted(totalGames, participants);
        }

        for (int i = 0; i < totalGames; i++) {
            Pairing p = pairings.get(i);
            int gameNum = i + 1;

            if (listener != null) {
                listener.onGameStarted(gameNum, totalGames, p.whiteEngine, p.blackEngine);
            }

            GameResult result = playGame(p.whiteEngine, p.blackEngine, p.openingFen);
            results.add(result);

            // Statistik aktualisieren
            statsMap.get(p.whiteEngine).recordGame(result);
            statsMap.get(p.blackEngine).recordGame(result);

            if (listener != null) {
                listener.onGameFinished(gameNum, totalGames, result);
            }
        }

        List<EngineStats> ranking = getRanking();
        if (listener != null) {
            listener.onTournamentFinished(ranking, results);
        }

        return ranking;
    }

    /**
     * Führt eine einzelne Partie zwischen zwei Engines durch.
     */
    public GameResult playGame(ChessEngine whiteEngine, ChessEngine blackEngine, String startFen) {
        Board board = (startFen != null && !startFen.isBlank())
                ? Board.fromFen(startFen)
                : Board.initial();

        ChessClock clock = null;
        if (config.isTimeControlEnabled()) {
            clock = new ChessClock(config.getInitialTimeMs(), config.getIncrementMs(), true);
            clock.start(PieceColor.WHITE);
        }

        List<String> moveHistory = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        int moveCount = 0;

        GameResult.Outcome outcome = null;
        GameResult.TerminationReason reason = null;

        while (true) {
            PieceColor activeColor = board.getActivePlayer();
            ChessEngine currentEngine = (activeColor == PieceColor.WHITE) ? whiteEngine : blackEngine;
            ChessEngine opponentEngine = (activeColor == PieceColor.WHITE) ? blackEngine : whiteEngine;

            // 1. Schachuhr-Timeout prüfen
            if (clock != null && clock.isEnabled()) {
                if (clock.isWhiteTimedOut()) {
                    outcome = GameResult.Outcome.BLACK_WIN;
                    reason = GameResult.TerminationReason.TIMEOUT;
                    break;
                } else if (clock.isBlackTimedOut()) {
                    outcome = GameResult.Outcome.WHITE_WIN;
                    reason = GameResult.TerminationReason.TIMEOUT;
                    break;
                }
            }

            // 2. Spielstatus prüfen (Matt, Patt, ungenügendes Material, 3-fache Wiederholung, 50-Züge-Regel)
            GameStatus status = MoveGenerator.evaluateGameStatus(board);
            if (status == GameStatus.CHECKMATE) {
                // Der aktive Spieler steht im Matt -> der andere gewinnt!
                outcome = (activeColor == PieceColor.WHITE) ? GameResult.Outcome.BLACK_WIN : GameResult.Outcome.WHITE_WIN;
                reason = GameResult.TerminationReason.CHECKMATE;
                break;
            } else if (status == GameStatus.STALEMATE) {
                outcome = GameResult.Outcome.DRAW;
                reason = GameResult.TerminationReason.STALEMATE;
                break;
            } else if (status == GameStatus.DRAW_INSUFFICIENT_MATERIAL) {
                outcome = GameResult.Outcome.DRAW;
                reason = GameResult.TerminationReason.INSUFFICIENT_MATERIAL;
                break;
            } else if (status == GameStatus.DRAW_THREEFOLD_REPETITION || board.isThreefoldRepetition()) {
                outcome = GameResult.Outcome.DRAW;
                reason = GameResult.TerminationReason.THREEFOLD_REPETITION;
                break;
            } else if (status == GameStatus.DRAW_FIFTY_MOVES) {
                outcome = GameResult.Outcome.DRAW;
                reason = GameResult.TerminationReason.FIFTY_MOVES;
                break;
            } else if (board.isDrawByRepetition()) {
                outcome = GameResult.Outcome.DRAW;
                reason = GameResult.TerminationReason.THREEFOLD_REPETITION;
                break;
            }

            // 3. Maximale Zuganzahl
            if (moveCount >= config.getMaxMovesPerGame()) {
                outcome = GameResult.Outcome.DRAW;
                reason = GameResult.TerminationReason.MAX_MOVES_DRAW;
                break;
            }

            // 4. Zugberechnung durch Bot (mit thread-sicherer Kopie)
            Board botCopy = board.copy();
            Move bestMove;
            try {
                bestMove = currentEngine.findBestMove(botCopy, activeColor, clock);
            } catch (Exception e) {
                // Bei Engine-Crash gewinnt der Gegner durch Forfeit
                outcome = (activeColor == PieceColor.WHITE) ? GameResult.Outcome.BLACK_WIN : GameResult.Outcome.WHITE_WIN;
                reason = GameResult.TerminationReason.ILLEGAL_MOVE;
                break;
            }

            // Timeout nach der Berechnung nochmals prüfen
            if (clock != null && clock.isEnabled()) {
                if ((activeColor == PieceColor.WHITE && clock.isWhiteTimedOut()) ||
                    (activeColor == PieceColor.BLACK && clock.isBlackTimedOut())) {
                    outcome = (activeColor == PieceColor.WHITE) ? GameResult.Outcome.BLACK_WIN : GameResult.Outcome.WHITE_WIN;
                    reason = GameResult.TerminationReason.TIMEOUT;
                    break;
                }
            }

            // 5. Validierung des Zugs
            List<Move> legalMoves = MoveGenerator.generateLegalMoves(board);
            if (bestMove == null || !legalMoves.contains(bestMove)) {
                // Unzulässiger Zug oder null -> Forfeit-Niederlage
                outcome = (activeColor == PieceColor.WHITE) ? GameResult.Outcome.BLACK_WIN : GameResult.Outcome.WHITE_WIN;
                reason = GameResult.TerminationReason.ILLEGAL_MOVE;
                break;
            }

            // 6. Zug ausführen
            board.makeMove(bestMove);
            moveHistory.add(bestMove.toSimpleNotation());
            moveCount++;

            if (clock != null && clock.isEnabled()) {
                clock.onMoveMade(activeColor);
            }
        }

        if (clock != null) {
            clock.pause();
        }

        long durationMs = System.currentTimeMillis() - startTime;
        return new GameResult(whiteEngine, blackEngine, outcome, reason, moveCount, durationMs, moveHistory);
    }

    private List<Pairing> generatePairings() {
        List<Pairing> list = new ArrayList<>();
        int gamesPerPairing = config.getGamesPerPairing();
        List<String> openings = config.getOpeningFens();

        for (int i = 0; i < participants.size(); i++) {
            for (int j = i + 1; j < participants.size(); j++) {
                ChessEngine e1 = participants.get(i);
                ChessEngine e2 = participants.get(j);

                for (int round = 0; round < gamesPerPairing; round++) {
                    String fen = openings.isEmpty() ? null : openings.get(round % openings.size());
                    if (round % 2 == 0) {
                        list.add(new Pairing(e1, e2, fen));
                    } else {
                        list.add(new Pairing(e2, e1, fen));
                    }
                }
            }
        }
        return list;
    }

    private static class Pairing {
        final ChessEngine whiteEngine;
        final ChessEngine blackEngine;
        final String openingFen;

        Pairing(ChessEngine whiteEngine, ChessEngine blackEngine, String openingFen) {
            this.whiteEngine = whiteEngine;
            this.blackEngine = blackEngine;
            this.openingFen = openingFen;
        }
    }

    /**
     * Erstellt eine formatierte Ranglisten-Tabelle.
     */
    public String getLeaderboardString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%n========================================================================================================%n"));
        sb.append(String.format(" Turniertabelle: %s%n", tournamentName));
        sb.append(String.format("========================================================================================================%n"));
        sb.append(String.format("%-5s %-32s %-10s %-8s %-5s %-5s %-5s %-8s %-12s %-12s%n",
                "Platz", "Engine", "Punkte", "Spiele", "S", "R", "N", "Quote", "Weiß (S/R/N)", "Schwarz (S/R/N)"));
        sb.append("--------------------------------------------------------------------------------------------------------\n");

        List<EngineStats> ranking = getRanking();
        int rank = 1;
        for (EngineStats s : ranking) {
            String pts = String.format(Locale.US, "%.1f/%d", s.getPoints(), s.getGamesPlayed());
            String quote = String.format(Locale.US, "%.1f%%", s.getPointsPercentage());
            String whiteRec = String.format("%d/%d/%d", s.getWhiteWins(), s.getWhiteDraws(), s.getWhiteLosses());
            String blackRec = String.format("%d/%d/%d", s.getBlackWins(), s.getBlackDraws(), s.getBlackLosses());

            sb.append(String.format("%-5s %-32s %-10s %-8d %-5d %-5d %-5d %-8s %-12s %-12s%n",
                    rank + ".", s.getName(), pts, s.getGamesPlayed(), s.getWins(), s.getDraws(), s.getLosses(),
                    quote, whiteRec, blackRec));
            rank++;
        }
        sb.append("========================================================================================================\n");
        return sb.toString();
    }

    /**
     * Erstellt eine Kreuztabelle (Head-to-Head Matrix) aller Duelle.
     */
    public String getCrossTableString() {
        List<EngineStats> ranking = getRanking();
        int n = ranking.size();
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%n====================================================================%n"));
        sb.append(" Kreuztabelle (Head-to-Head Duelle)\n");
        sb.append("====================================================================\n");

        sb.append(String.format("%-4s %-25s", "#", "Engine"));
        for (int i = 1; i <= n; i++) {
            sb.append(String.format(" %3d ", i));
        }
        sb.append("   Gesamt\n");
        sb.append("--------------------------------------------------------------------\n");

        for (int i = 0; i < n; i++) {
            ChessEngine e1 = ranking.get(i).getEngine();
            sb.append(String.format("%-4s %-25s", (i + 1) + ".", truncate(e1.getName(), 25)));

            for (int j = 0; j < n; j++) {
                if (i == j) {
                    sb.append("  x  ");
                } else {
                    ChessEngine e2 = ranking.get(j).getEngine();
                    double headToHead = 0.0;
                    for (GameResult r : results) {
                        if ((r.getWhiteEngine() == e1 && r.getBlackEngine() == e2) ||
                            (r.getWhiteEngine() == e2 && r.getBlackEngine() == e1)) {
                            headToHead += r.getScoreForEngine(e1);
                        }
                    }
                    sb.append(String.format(Locale.US, " %3.1f ", headToHead));
                }
            }
            sb.append(String.format(Locale.US, "   %4.1f Pkt%n", ranking.get(i).getPoints()));
        }
        sb.append("====================================================================\n");
        return sb.toString();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 1) + "…" : text;
    }

    /**
     * Standard-Listener für Konsolenausgaben während des Turniers.
     */
    public static class DefaultConsoleTournamentListener implements TournamentListener {
        @Override
        public void onTournamentStarted(int totalGames, List<ChessEngine> participants) {
            System.out.printf("%n=== Starte Turnier (%d Partien, %d Teilnehmer) ===%n", totalGames, participants.size());
        }

        @Override
        public void onGameStarted(int gameNumber, int totalGames, ChessEngine white, ChessEngine black) {
            System.out.printf("[%2d/%2d] %s (Weiß) vs. %s (Schwarz)... ", gameNumber, totalGames, white.getName(), black.getName());
        }

        @Override
        public void onGameFinished(int gameNumber, int totalGames, GameResult result) {
            System.out.printf("Ergebnis: %s (%s, %d Züge, %d ms)%n",
                    result.getOutcome().getNotation(), result.getReason().getDescription(), result.getTotalMoves(), result.getDurationMs());
        }

        @Override
        public void onTournamentFinished(List<EngineStats> ranking, List<GameResult> allResults) {
            System.out.println("\nTurnier erfolgreich beendet!");
        }
    }
}
