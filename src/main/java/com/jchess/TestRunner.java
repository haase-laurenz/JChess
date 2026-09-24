package com.jchess;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.board.Position;
import com.jchess.core.engine.RandomBot;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;
import com.jchess.core.rules.GameStatus;
import com.jchess.core.rules.MoveGenerator;

import java.util.List;

/**
 * Automatisierter Test-Runner zur Validierung der Schachlogik,
 * Zuggenerierung, Undo-Mechanik und Regelsicherheit.
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("=== Starte JChess Logik-Tests ===");
        int passed = 0;
        int total = 0;

        // Test 1: Startaufstellung
        total++;
        Board board = new Board();
        if (board.getActivePlayer() == PieceColor.WHITE &&
            board.getPieceAt(Position.of(4, 0)).getType() == PieceType.KING &&
            board.getPieceAt(Position.of(4, 7)).getType() == PieceType.KING) {
            System.out.println("  [PASS] Test 1: Startaufstellung korrekt");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 1: Startaufstellung fehlerhaft");
        }

        // Test 2: Anzahl Startzüge für Weiß (muss exakt 20 sein: 16 Bauernzüge + 4 Springerzüge)
        total++;
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(board);
        if (legalMoves.size() == 20) {
            System.out.println("  [PASS] Test 2: Exakt 20 legale Eröffnungszüge für Weiß");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 2: Erwartet 20 Züge, erhalten: " + legalMoves.size());
        }

        // Test 3: Zugausführung und Undo-Mechanik
        total++;
        Move e2e4 = legalMoves.stream()
                .filter(m -> m.getFrom().equals(Position.of(4, 1)) && m.getTo().equals(Position.of(4, 3)))
                .findFirst().orElse(null);

        if (e2e4 != null) {
            board.makeMove(e2e4);
            boolean movedCorrectly = board.getPieceAt(Position.of(4, 3)) != null &&
                                     board.getPieceAt(Position.of(4, 1)) == null &&
                                     board.getActivePlayer() == PieceColor.BLACK &&
                                     Position.of(4, 2).equals(board.getEnPassantTarget());

            board.undoMove();
            boolean undoneCorrectly = board.getPieceAt(Position.of(4, 1)) != null &&
                                      board.getPieceAt(Position.of(4, 3)) == null &&
                                      board.getActivePlayer() == PieceColor.WHITE &&
                                      board.getEnPassantTarget() == null;

            if (movedCorrectly && undoneCorrectly) {
                System.out.println("  [PASS] Test 3: makeMove() und undoMove() funktionieren einwandfrei");
                passed++;
            } else {
                System.err.println("  [FAIL] Test 3: Undo-Zustand weicht ab");
            }
        } else {
            System.err.println("  [FAIL] Test 3: e2-e4 nicht gefunden");
        }

        // Test 4: RandomBot Zugberechnung
        total++;
        RandomBot bot = new RandomBot();
        Move botMove = bot.findBestMove(board, PieceColor.WHITE);
        if (botMove != null && legalMoves.contains(botMove)) {
            System.out.println("  [PASS] Test 4: Bot wählt validen legalen Zug: " + botMove.toSimpleNotation());
            passed++;
        } else {
            System.err.println("  [FAIL] Test 4: Bot-Zug ungültig");
        }

        // Test 5: Fools Mate (Narrenmatt: 1. f3 e5 2. g4 Qh4#)
        total++;
        Board foolsBoard = new Board();
        // 1. f3
        makeMoveByCoords(foolsBoard, 5, 1, 5, 2);
        // 1... e5
        makeMoveByCoords(foolsBoard, 4, 6, 4, 4);
        // 2. g4
        makeMoveByCoords(foolsBoard, 6, 1, 6, 3);
        // 2... Qh4#
        makeMoveByCoords(foolsBoard, 3, 7, 7, 3);

        GameStatus status = MoveGenerator.evaluateGameStatus(foolsBoard);
        if (status == GameStatus.CHECKMATE) {
            System.out.println("  [PASS] Test 5: Narrenmatt erfolgreich als CHECKMATE erkannt");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 5: Schachmatt nicht erkannt! Status: " + status);
        }

        // Test 6: PieceRenderer HD_SPRITES Validierung & Default
        total++;
        com.jchess.ui.PieceRenderer renderer = new com.jchess.ui.PieceRenderer();
        if (renderer.getCurrentStyle() == com.jchess.ui.PieceRenderer.RenderStyle.HD_SPRITES && renderer.isHDSpritesLoaded()) {
            System.out.println("  [PASS] Test 6: Neue HD-Figurensprites geladen und standardmäßig als HD_SPRITES aktiv");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 6: Neue HD-Figurensprites nicht korrekt geladen oder nicht Standard: style=" +
                    renderer.getCurrentStyle() + ", loaded=" + renderer.isHDSpritesLoaded());
        }

        // Test 7: com.jchess.engine.bots.Random & engine.bots.Random
        total++;
        com.jchess.engine.bots.Random newBot = new com.jchess.engine.bots.Random();
        engine.bots.Random wrapperBot = new engine.bots.Random();
        Move moveFromNewBot = newBot.findBestMove(board, PieceColor.WHITE);
        Move moveFromWrapper = wrapperBot.findBestMove(board, PieceColor.WHITE);

        if (moveFromNewBot != null && legalMoves.contains(moveFromNewBot) &&
            moveFromWrapper != null && legalMoves.contains(moveFromWrapper)) {
            System.out.println("  [PASS] Test 7: Neuer Bot in /engine/bots/Random.java funktioniert einwandfrei");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 7: Neuer Bot liefert ungültigen Zug");
        }

        // Test 8: PositionEvaluator (Startposition, 1. e4 und Fools Mate)
        total++;
        double startEval = com.jchess.core.evaluation.PositionEvaluator.evaluate(board);
        double mateEval = com.jchess.core.evaluation.PositionEvaluator.evaluate(foolsBoard);
        String formattedStart = com.jchess.core.evaluation.PositionEvaluator.formatScore(startEval, GameStatus.ACTIVE);
        String formattedMate = com.jchess.core.evaluation.PositionEvaluator.formatScore(mateEval, GameStatus.CHECKMATE);

        if (Math.abs(startEval) < 0.1 && mateEval <= -com.jchess.core.evaluation.PositionEvaluator.MATE_SCORE + 10 &&
            "+M".equals(formattedMate) || "-M".equals(formattedMate)) {
            System.out.println("  [PASS] Test 8: PositionEvaluator bewertet Start (0.0) und Matt korrekt (" + formattedMate + ")");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 8: PositionEvaluator fehlerhaft: start=" + startEval + ", mate=" + mateEval);
        }

        // Test 9: KingLBot1 Zugberechnung und Matt-Erkennung
        total++;
        engine.bots.KingLBot1 bot1 = new engine.bots.KingLBot1();
        com.jchess.engine.bots.KingLBot1 bot1Wrapper = new com.jchess.engine.bots.KingLBot1();
        Move bot1Move = bot1.findBestMove(board, PieceColor.WHITE);
        Move bot1WrapperMove = bot1Wrapper.findBestMove(board, PieceColor.WHITE);

        // Prüfen, ob KingLBot1 bei 1 Zug vor Narrenmatt das sofortige Matt findet (Qh4#)
        Board preFoolsMate = new Board();
        makeMoveByCoords(preFoolsMate, 5, 1, 5, 2); // 1. f3
        makeMoveByCoords(preFoolsMate, 4, 6, 4, 4); // 1... e5
        makeMoveByCoords(preFoolsMate, 6, 1, 6, 3); // 2. g4
        Move mateMove = bot1.findBestMove(preFoolsMate, PieceColor.BLACK);

        if (bot1Move != null && legalMoves.contains(bot1Move) &&
            bot1WrapperMove != null && legalMoves.contains(bot1WrapperMove) &&
            mateMove != null && mateMove.getFrom().equals(Position.of(3, 7)) && mateMove.getTo().equals(Position.of(7, 3))) {
            System.out.println("  [PASS] Test 9: KingLBot1 berechnet Züge und findet forciertes Matt (Qh4#)");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 9: KingLBot1 fehlerhaft: move=" + bot1Move + ", mateMove=" + mateMove);
        }

        // Test 10: GameConfig, ChessClock und Engine-Timer-Integration
        total++;
        com.jchess.core.config.GameConfig config = com.jchess.core.config.GameConfig.getInstance();
        boolean configOk = config.getTimeMinutes() > 0 && config.isTimeControlEnabled();

        com.jchess.core.clock.ChessClock testClock = new com.jchess.core.clock.ChessClock(1000, 500, true);
        testClock.onMoveMade(PieceColor.WHITE); // Weiß zieht -> Weiß erhält 500ms Inkrement = 1500ms, Schwarz ist am Zug
        boolean clockOk = testClock.getWhiteTimeMillis() == 1500 && testClock.getActiveColor() == PieceColor.BLACK;

        // Engine-Schnittstelle mit Timer testen
        Move clockMove = bot1.findBestMove(board, PieceColor.WHITE, testClock);
        boolean engineClockOk = clockMove != null && legalMoves.contains(clockMove);

        if (configOk && clockOk && engineClockOk) {
            System.out.println("  [PASS] Test 10: GameConfig, ChessClock und Engine-Timer-Integration funktionieren");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 10: configOk=" + configOk + ", clockOk=" + clockOk + ", engineClockOk=" + engineClockOk);
        }

        // Test 11: JS_ONE Logik (Symmetrische Startbewertung, Endspiel-Interpolation & Iterative Deepening)
        total++;
        double initialEval = bot1.evaluate(board, PieceColor.WHITE);
        boolean symOk = Math.abs(initialEval) < 0.001;

        // Test Endspiel-Faktor bei wenigen Figuren
        Board endgameBoard = Board.empty();
        endgameBoard.setPieceAt(Position.of(4, 0), com.jchess.core.piece.Piece.of(com.jchess.core.piece.PieceType.KING, PieceColor.WHITE));
        endgameBoard.setPieceAt(Position.of(4, 7), com.jchess.core.piece.Piece.of(com.jchess.core.piece.PieceType.KING, PieceColor.BLACK));
        endgameBoard.setPieceAt(Position.of(4, 6), com.jchess.core.piece.Piece.of(com.jchess.core.piece.PieceType.PAWN, PieceColor.WHITE)); // e7
        double endgameEval = bot1.evaluate(endgameBoard, PieceColor.WHITE);
        // Bauer auf e7 hat im Endspiel PAWNS_END[52] = 80 + 100 Material = 180 + Königswerte
        boolean endgameOk = endgameEval > 100.0;

        // Test feste Tiefe und Iterative Deepening Metriken
        engine.bots.KingLBot1 fixedDepthBot = new engine.bots.KingLBot1(3);
        Move fixedMove = fixedDepthBot.findBestMove(board, PieceColor.WHITE);
        boolean fixedOk = fixedMove != null && fixedDepthBot.getLastDepthReached() == 3 && fixedDepthBot.getLastNodesEvaluated() > 0;

        if (symOk && endgameOk && fixedOk) {
            System.out.println("  [PASS] Test 11: JS_ONE Portierung erfolgreich (PST, Endspiel-Interpolation, Iterative Deepening)");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 11: symOk=" + symOk + ", endgameOk=" + endgameOk + ", fixedOk=" + fixedOk + 
                    ", initialEval=" + initialEval + ", depthReached=" + fixedDepthBot.getLastDepthReached());
        }

        // Test 12: FEN Loader & Anderssen Evergreen Position
        total++;
        String fen = "rn1k3r/1b1q1ppp/p2P4/2B2p2/8/1QNBR3/PP3PPP/2R3K1 w - - 1 0";
        Board fenBoard = Board.fromFen(fen);
        boolean fenPlacementOk = fenBoard.getActivePlayer() == PieceColor.WHITE &&
                fenBoard.getPieceAt(Position.fromAlgebraic("d8")).getType() == com.jchess.core.piece.PieceType.KING &&
                fenBoard.getPieceAt(Position.fromAlgebraic("c5")).getType() == com.jchess.core.piece.PieceType.BISHOP &&
                fenBoard.getPieceAt(Position.fromAlgebraic("d6")).getType() == com.jchess.core.piece.PieceType.PAWN;

        // FEN Export muss äquivalent sein
        String exportedFen = fenBoard.toFen();
        boolean exportOk = exportedFen.startsWith("rn1k3r/1b1q1ppp/p2P4/2B2p2/8/1QNBR3/PP3PPP/2R3K1 w - - 1 0");

        // KingLBot1 soll in dieser Stellung einen der forcierten Gewinnzüge finden (Rxe7 oder Bb6+)
        engine.bots.KingLBot1 fenBot = new engine.bots.KingLBot1(3);
        Move fenMove = fenBot.findBestMove(fenBoard, PieceColor.WHITE);
        boolean tacticOk = fenMove != null && (
                (fenMove.getFrom().equals(Position.fromAlgebraic("e3")) && fenMove.getTo().equals(Position.fromAlgebraic("e7"))) ||
                (fenMove.getFrom().equals(Position.fromAlgebraic("c5")) && fenMove.getTo().equals(Position.fromAlgebraic("b6")))
        );

        if (fenPlacementOk && exportOk && tacticOk) {
            System.out.println("  [PASS] Test 12: FEN Loader & Taktikprüfung (Evergreen: " + fenMove + ") erfolgreich");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 12: fenPlacementOk=" + fenPlacementOk + ", exportOk=" + exportOk +
                    ", tacticOk=" + tacticOk + ", bestMove=" + fenMove);
        }

        // Test 13: Zobrist Hashing & Inkrementelle Konsistenz
        total++;
        Board zBoard = new Board();
        boolean zobristInitOk = zBoard.getZobristKey() == com.jchess.core.engine.Zobrist.computeHash(zBoard);

        // Züge mit Rochade, Bauernzügen und Figuren ausführen
        String[] testMoves = {"e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "e1g1", "g8f6"};
        boolean zobristMovesOk = true;
        for (String uci : testMoves) {
            Position f = Position.fromAlgebraic(uci.substring(0, 2));
            Position t = Position.fromAlgebraic(uci.substring(2, 4));
            Move m = MoveGenerator.generateLegalMovesFromSquare(zBoard, f).stream()
                    .filter(cand -> cand.getTo().equals(t))
                    .findFirst().orElse(null);
            if (m == null) {
                zobristMovesOk = false;
                break;
            }
            zBoard.makeMove(m);
            long expected = com.jchess.core.engine.Zobrist.computeHash(zBoard);
            if (zBoard.getZobristKey() != expected) {
                zobristMovesOk = false;
                break;
            }
        }

        // Alle Züge zurücknehmen und jeweils prüfen
        boolean zobristUndoOk = true;
        while (!zBoard.getMoveHistory().isEmpty()) {
            zBoard.undoMove();
            long expected = com.jchess.core.engine.Zobrist.computeHash(zBoard);
            if (zBoard.getZobristKey() != expected) {
                zobristUndoOk = false;
                break;
            }
        }

        // Transpositions-Test: 1. d4 Nf6 2. c4 e6 vs 1. c4 Nf6 2. d4 e6
        Board transA = new Board();
        Board transB = new Board();
        String[] movesA = {"d2d4", "g8f6", "c2c4", "e7e6"};
        String[] movesB = {"c2c4", "g8f6", "d2d4", "e7e6"};
        for (String uci : movesA) {
            Move m = MoveGenerator.generateLegalMovesFromSquare(transA, Position.fromAlgebraic(uci.substring(0, 2))).stream()
                    .filter(c -> c.getTo().equals(Position.fromAlgebraic(uci.substring(2, 4))))
                    .findFirst().get();
            transA.makeMove(m);
        }
        for (String uci : movesB) {
            Move m = MoveGenerator.generateLegalMovesFromSquare(transB, Position.fromAlgebraic(uci.substring(0, 2))).stream()
                    .filter(c -> c.getTo().equals(Position.fromAlgebraic(uci.substring(2, 4))))
                    .findFirst().get();
            transB.makeMove(m);
        }
        boolean transpositionOk = transA.getZobristKey() == transB.getZobristKey();

        if (zobristInitOk && zobristMovesOk && zobristUndoOk && transpositionOk) {
            System.out.println("  [PASS] Test 13: Zobrist Hashing & inkrementelle Hash-Konsistenz einwandfrei");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 13: init=" + zobristInitOk + ", moves=" + zobristMovesOk +
                    ", undo=" + zobristUndoOk + ", transposition=" + transpositionOk);
        }

        // Test 14: Threefold Repetition & Transposition Table
        total++;
        Board repBoard = new Board();
        // Springer-Zyklus für 3-fache Stellungswiederholung (Startstellung 3x erreicht)
        String[] repMoves = {"g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8"};
        for (String uci : repMoves) {
            Move m = MoveGenerator.generateLegalMovesFromSquare(repBoard, Position.fromAlgebraic(uci.substring(0, 2))).stream()
                    .filter(c -> c.getTo().equals(Position.fromAlgebraic(uci.substring(2, 4))))
                    .findFirst().get();
            repBoard.makeMove(m);
        }
        boolean repetitionOk = repBoard.isThreefoldRepetition() && repBoard.isDrawByRepetition();

        // TT Funktionalität prüfen
        engine.bots.TranspositionTable tt = new engine.bots.TranspositionTable(1024);
        long sampleKey = 0xABCDEF1234567890L;
        tt.store(sampleKey, 4, engine.bots.TranspositionTable.FLAG_EXACT, 1.25, null, 0);
        engine.bots.TranspositionTable.TTEntry sampleEntry = tt.probe(sampleKey, 0);
        boolean ttStoreOk = sampleEntry != null && Math.abs(sampleEntry.score - 1.25) < 0.001 && sampleEntry.depth == 4;

        // TT Treffer in KingLBot1 verifizieren
        engine.bots.KingLBot1 ttBot = new engine.bots.KingLBot1(4);
        ttBot.findBestMove(board, PieceColor.WHITE);
        boolean ttHitsOk = ttBot.getTranspositionTable().getHits() > 0;

        if (repetitionOk && ttStoreOk && ttHitsOk) {
            System.out.println("  [PASS] Test 14: Threefold Repetition (3x Hash) & Transposition Table (Hits: " +
                    ttBot.getTranspositionTable().getHits() + ") erfolgreich");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 14: repetitionOk=" + repetitionOk + ", ttStoreOk=" + ttStoreOk +
                    ", ttHitsOk=" + ttHitsOk + " (Hits: " + ttBot.getTranspositionTable().getHits() + ")");
        }

        // Test 15: MoveGenerator 4 Promotionszüge (Knight, Bishop, Rook, Queen)
        total++;
        Board promoTestBoard = Board.empty();
        promoTestBoard.setPieceAt(Position.of(4, 0), com.jchess.core.piece.Piece.of(com.jchess.core.piece.PieceType.KING, PieceColor.WHITE));
        promoTestBoard.setPieceAt(Position.of(0, 7), com.jchess.core.piece.Piece.of(com.jchess.core.piece.PieceType.KING, PieceColor.BLACK));
        promoTestBoard.setPieceAt(Position.of(4, 6), com.jchess.core.piece.Piece.of(com.jchess.core.piece.PieceType.PAWN, PieceColor.WHITE)); // e7
        promoTestBoard.setActivePlayer(PieceColor.WHITE);

        List<Move> e7Moves = MoveGenerator.generateLegalMovesFromSquare(promoTestBoard, Position.of(4, 6));
        boolean count4Ok = e7Moves.size() == 4;
        boolean promoTypesOk = count4Ok &&
                e7Moves.get(0).getPromotionType() == com.jchess.core.piece.PieceType.KNIGHT &&
                e7Moves.get(1).getPromotionType() == com.jchess.core.piece.PieceType.BISHOP &&
                e7Moves.get(2).getPromotionType() == com.jchess.core.piece.PieceType.ROOK &&
                e7Moves.get(3).getPromotionType() == com.jchess.core.piece.PieceType.QUEEN;

        if (count4Ok && promoTypesOk) {
            System.out.println("  [PASS] Test 15: MoveGenerator generiert 4 Promotionszüge (Knight, Bishop, Rook, Queen) erfolgreich");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 15: count4Ok=" + count4Ok + ", promoTypesOk=" + promoTypesOk);
        }

        // Test 16: Dynamische Bedenkzeit (% der Remaining Time)
        total++;
        engine.bots.KingLBot1 pctBot = new engine.bots.KingLBot1(0.05); // 5%
        boolean pctInitOk = Math.abs(pctBot.getRemainingTimePercentage() - 0.05) < 0.001;
        pctBot.setRemainingTimePercentage(0.025);
        boolean pctSetOk = Math.abs(pctBot.getRemainingTimePercentage() - 0.025) < 0.001;

        // Prüfen, ob Bot bei kurzer verbleibender Zeit trotzdem schnell und sicher zieht
        com.jchess.core.clock.ChessClock shortClock = new com.jchess.core.clock.ChessClock(500, 0, true);
        Move fastMove = pctBot.findBestMove(board, PieceColor.WHITE, shortClock);
        boolean fastMoveOk = fastMove != null && pctBot.getLastSearchTimeMs() < 500;

        if (pctInitOk && pctSetOk && fastMoveOk) {
            System.out.println("  [PASS] Test 16: Dynamische Bedenkzeit (% der Remaining Time) & Zeitnot-Sicherheit erfolgreich");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 16: pctInitOk=" + pctInitOk + ", pctSetOk=" + pctSetOk + ", fastMoveOk=" + fastMoveOk);
        }

        // Test 17: Best Move Pfeil UI & Board-Integration
        total++;
        com.jchess.ui.PieceRenderer boardRenderer = new com.jchess.ui.PieceRenderer();
        com.jchess.ui.ChessBoardPanel boardPanel = new com.jchess.ui.ChessBoardPanel(board, boardRenderer);
        com.jchess.ui.SidebarPanel sidebar = new com.jchess.ui.SidebarPanel();

        boolean initialArrowOff = !boardPanel.isShowBestMoveArrow();
        boardPanel.setShowBestMoveArrow(true);
        boolean arrowOn = boardPanel.isShowBestMoveArrow();

        Move sampleMove = Move.normal(Position.of(4, 1), Position.of(4, 3), com.jchess.core.piece.Piece.of(com.jchess.core.piece.PieceType.PAWN, PieceColor.WHITE), null); // e2-e4
        boardPanel.setBestMove(sampleMove);
        boolean moveSetOk = sampleMove.equals(boardPanel.getBestMove());

        boardPanel.clearBestMove();
        boolean moveClearedOk = boardPanel.getBestMove() == null;

        boolean[] toggleFired = new boolean[1];
        sidebar.addBestMoveToggleListener(state -> toggleFired[0] = state);
        sidebar.getShowBestMoveCheckbox().doClick();
        boolean toggleOk = toggleFired[0] && sidebar.isShowBestMoveSelected();

        if (initialArrowOff && arrowOn && moveSetOk && moveClearedOk && toggleOk) {
            System.out.println("  [PASS] Test 17: Best Move Pfeil UI (Checkbox, Umschalter & Board-Panel-Methoden) einwandfrei");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 17: initialArrowOff=" + initialArrowOff + ", arrowOn=" + arrowOn + ", moveSetOk=" + moveSetOk + ", moveClearedOk=" + moveClearedOk + ", toggleOk=" + toggleOk);
        }

        // Test 18: Tournament Framework (Rundenturnier, Farbentausch & Punktestatistik)
        total++;
        com.jchess.tournament.TournamentConfig testTourneyConfig = com.jchess.tournament.TournamentConfig.fastTest();
        testTourneyConfig.setGamesPerPairing(2);
        testTourneyConfig.setMaxMovesPerGame(30);

        com.jchess.tournament.Tournament testTourney = new com.jchess.tournament.Tournament("Test-Turnier", testTourneyConfig);
        testTourney.addParticipant(new engine.bots.KingLBot1(1));
        testTourney.addParticipant(new engine.bots.Random());

        List<com.jchess.tournament.EngineStats> ranking = testTourney.run(null); // Stiller Durchlauf
        boolean gamesCountOk = testTourney.getResults().size() == 2;
        boolean whiteSwapped = testTourney.getResults().get(0).getWhiteEngine() != testTourney.getResults().get(1).getWhiteEngine();
        boolean rankingOk = ranking.size() == 2;
        boolean pointsSumOk = Math.abs((ranking.get(0).getPoints() + ranking.get(1).getPoints()) - 2.0) < 0.001;
        String leaderboardStr = testTourney.getLeaderboardString();
        String crossTableStr = testTourney.getCrossTableString();
        boolean tableGenOk = leaderboardStr != null && !leaderboardStr.isBlank() && crossTableStr != null && !crossTableStr.isBlank();

        if (gamesCountOk && whiteSwapped && rankingOk && pointsSumOk && tableGenOk) {
            System.out.println("  [PASS] Test 18: Tournament Framework (Round-Robin, Farbentausch, Scoring & Tabellen) erfolgreich");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 18: gamesCountOk=" + gamesCountOk + ", whiteSwapped=" + whiteSwapped + ", rankingOk=" + rankingOk + ", pointsSumOk=" + pointsSumOk + ", tableGenOk=" + tableGenOk);
        }

        // Test 19: Erweiterte Remis-Logiken (Ungenügendes Material: King-King, Knight-Knight & 3-Peat Repetition)
        total++;
        // 1. K vs. K
        Board kvk = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        boolean kvkOk = kvk.hasInsufficientMaterial() && MoveGenerator.evaluateGameStatus(kvk) == GameStatus.DRAW_INSUFFICIENT_MATERIAL;

        // 2. K+N vs. K+N (Knight Knight)
        Board knvkn = Board.fromFen("4k3/8/5n2/8/8/2N5/8/4K3 w - - 0 1");
        boolean knvknOk = knvkn.hasInsufficientMaterial() && MoveGenerator.evaluateGameStatus(knvkn) == GameStatus.DRAW_INSUFFICIENT_MATERIAL;

        // 3. K+N vs. K und K+B vs. K
        Board knvk = Board.fromFen("4k3/8/8/8/8/2N5/8/4K3 w - - 0 1");
        Board kbvk = Board.fromFen("4k3/8/8/8/8/2B5/8/4K3 w - - 0 1");
        boolean singleMinorOk = knvk.hasInsufficientMaterial() && kbvk.hasInsufficientMaterial();

        // 4. K+B vs. K+B (gleiche Feldfarbe: c1=dark, f8=dark)
        Board kbvkbSame = Board.fromFen("5b1k/8/8/8/8/8/8/2B1K3 w - - 0 1");
        boolean sameBishopOk = kbvkbSame.hasInsufficientMaterial() && MoveGenerator.evaluateGameStatus(kbvkbSame) == GameStatus.DRAW_INSUFFICIENT_MATERIAL;

        // 5. K+Q vs. K (Genügend Material)
        Board kqvk = Board.fromFen("4k3/8/8/8/8/2Q5/8/4K3 w - - 0 1");
        boolean queenSufficientOk = !kqvk.hasInsufficientMaterial() && MoveGenerator.evaluateGameStatus(kqvk) == GameStatus.ACTIVE;

        // 6. 3-Peat Repetition (Dreifache Stellungswiederholung)
        Board rep3Board = Board.initial();
        makeMoveByCoords(rep3Board, 1, 0, 2, 2); // 1. Nc3
        makeMoveByCoords(rep3Board, 1, 7, 2, 5); // 1... Nc6
        makeMoveByCoords(rep3Board, 2, 2, 1, 0); // 2. Nb1
        makeMoveByCoords(rep3Board, 2, 5, 1, 7); // 2... Nb8 (2. Vorkommen)
        makeMoveByCoords(rep3Board, 1, 0, 2, 2); // 3. Nc3
        makeMoveByCoords(rep3Board, 1, 7, 2, 5); // 3... Nc6
        makeMoveByCoords(rep3Board, 2, 2, 1, 0); // 4. Nb1
        makeMoveByCoords(rep3Board, 2, 5, 1, 7); // 4... Nb8 (3. Vorkommen -> 3-Peat!)

        boolean rep3Ok = rep3Board.isThreefoldRepetition() && MoveGenerator.evaluateGameStatus(rep3Board) == GameStatus.DRAW_THREEFOLD_REPETITION;
        boolean rep3GameOver = MoveGenerator.evaluateGameStatus(rep3Board).isGameOver();

        if (kvkOk && knvknOk && singleMinorOk && sameBishopOk && queenSufficientOk && rep3Ok && rep3GameOver) {
            System.out.println("  [PASS] Test 19: Remis-Logiken (King-King, Knight-Knight, Insufficient Material & 3-Peat Repetition) einwandfrei");
            passed++;
        } else {
            System.err.println("  [FAIL] Test 19: kvk=" + kvkOk + ", knvkn=" + knvknOk + ", singleMinor=" + singleMinorOk +
                    ", sameBishop=" + sameBishopOk + ", queenSufficient=" + queenSufficientOk + ", rep3=" + rep3Ok + ", gameOver=" + rep3GameOver);
        }

        System.out.println("----------------------------------------");
        System.out.println("Ergebnis: " + passed + " / " + total + " Tests bestanden.");
        if (passed != total) {
            System.exit(1);
        }
    }

    private static void makeMoveByCoords(Board board, int f1, int r1, int f2, int r2) {
        Position from = Position.of(f1, r1);
        Position to = Position.of(f2, r2);
        List<Move> legal = MoveGenerator.generateLegalMoves(board);
        Move move = legal.stream()
                .filter(m -> m.getFrom().equals(from) && m.getTo().equals(to))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Ungültiger Zug: " + from + " -> " + to));
        board.makeMove(move);
    }
}
