package com.jchess.ui;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.clock.ChessClock;
import com.jchess.core.config.GameConfig;
import com.jchess.core.engine.ChessEngine;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.rules.GameStatus;
import com.jchess.core.rules.MoveGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Hauptfenster der JChess Anwendung.
 * Koordiniert Brett, vertikale Eval-Bar, Schachuhr, Seitenleiste und asynchrone Berechnungen der Engine.
 */
public class ChessFrame extends JFrame {

    private final Board board;
    private final PieceRenderer pieceRenderer;
    private final ChessBoardPanel boardPanel;
    private final EvaluationBarPanel evalBarPanel;
    private final SidebarPanel sidebarPanel;

    private final GameConfig gameConfig;
    private final ChessClock chessClock;
    private final PlayerClockPanel whiteClockPanel;
    private final PlayerClockPanel blackClockPanel;
    private final JPanel topClockContainer;
    private final JPanel bottomClockContainer;

    private ChessEngine customOverrideEngine = null;
    private boolean isEngineThinking = false;

    // Asynchrone Hintergrund-Evaluation für die Eval-Bar mittels JChessV1
    private final ExecutorService evalExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "EvalBar-Analyzer-Thread");
        t.setDaemon(true);
        return t;
    });
    private engine.bots.JChessV1 backgroundEvalBot;
    private Future<?> currentEvalTask;

    public ChessFrame() {
        super("JChess - Java Chess Engine & GUI");

        this.board = new Board();
        this.pieceRenderer = new PieceRenderer();
        this.boardPanel = new ChessBoardPanel(board, pieceRenderer);
        this.evalBarPanel = new EvaluationBarPanel();
        this.sidebarPanel = new SidebarPanel();

        this.gameConfig = GameConfig.getInstance();
        this.chessClock = new ChessClock(
                gameConfig.getInitialTimeMillis(),
                gameConfig.getIncrementMillis(),
                gameConfig.isTimeControlEnabled()
        );
        this.whiteClockPanel = new PlayerClockPanel(PieceColor.WHITE, "♔ Weiß", gameConfig.getInitialTimeMillis());
        this.blackClockPanel = new PlayerClockPanel(PieceColor.BLACK, "♚ Schwarz", gameConfig.getInitialTimeMillis());
        this.topClockContainer = new JPanel(new BorderLayout());
        this.bottomClockContainer = new JPanel(new BorderLayout());

        initUI();
        initEvents();
        updateClockPositions();
        updatePlayerClockLabels();
        whiteClockPanel.updateDisplay(chessClock.getWhiteTimeMillis(), false);
        blackClockPanel.updateDisplay(chessClock.getBlackTimeMillis(), false);
        sidebarPanel.updateGameState(board);
        updateEvaluationVisibility();

        if (gameConfig.isTimeControlEnabled()) {
            chessClock.start(board.getActivePlayer());
        }
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopBackgroundEvaluation();
                evalExecutor.shutdownNow();
            }
        });

        JPanel boardContainer = new JPanel(new BorderLayout());
        boardContainer.setBackground(new Color(44, 38, 33));
        boardContainer.add(evalBarPanel, BorderLayout.WEST);

        topClockContainer.setOpaque(false);
        bottomClockContainer.setOpaque(false);
        topClockContainer.setBorder(new EmptyBorder(6, 6, 4, 6));
        bottomClockContainer.setBorder(new EmptyBorder(4, 6, 6, 6));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(topClockContainer, BorderLayout.NORTH);
        centerPanel.add(boardPanel, BorderLayout.CENTER);
        centerPanel.add(bottomClockContainer, BorderLayout.SOUTH);

        boardContainer.add(centerPanel, BorderLayout.CENTER);

        add(boardContainer, BorderLayout.CENTER);
        add(sidebarPanel, BorderLayout.EAST);

        // Menüleiste
        setJMenuBar(createMenuBar());

        pack();
        setMinimumSize(new Dimension(960, 720));
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu gameMenu = new JMenu("Spiel");
        JMenuItem newItem = new JMenuItem("Neues Spiel");
        newItem.addActionListener(e -> startNewGame());

        JMenuItem loadFenItem = new JMenuItem("FEN laden...");
        loadFenItem.addActionListener(e -> showLoadFenDialog());

        JMenuItem copyFenItem = new JMenuItem("FEN kopieren");
        copyFenItem.addActionListener(e -> copyFenToClipboard());

        JMenuItem undoItem = new JMenuItem("Zug zurücknehmen");
        undoItem.addActionListener(e -> undoMove());

        JMenuItem exitItem = new JMenuItem("Beenden");
        exitItem.addActionListener(e -> System.exit(0));

        gameMenu.add(newItem);
        gameMenu.add(loadFenItem);
        gameMenu.add(copyFenItem);
        gameMenu.addSeparator();
        gameMenu.add(undoItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Hilfe");
        JMenuItem infoItem = new JMenuItem("Über JChess");
        infoItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "JChess - Java Chess Engine & GUI\n" +
                "Mit konfigurierbarer Schachuhr (config.properties)\n" +
                "und erweiterbarer Engine-Schnittstelle!",
                "Über JChess", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(infoItem);

        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void initEvents() {
        // Zug vom Brett erhalten
        boardPanel.setMoveListener(this::handlePlayerMove);

        // Schachuhr-Listener
        chessClock.addClockListener(new ChessClock.ClockListener() {
            @Override
            public void onTick(long whiteTimeMs, long blackTimeMs, PieceColor activeColor) {
                SwingUtilities.invokeLater(() -> {
                    whiteClockPanel.updateDisplay(whiteTimeMs, activeColor == PieceColor.WHITE);
                    blackClockPanel.updateDisplay(blackTimeMs, activeColor == PieceColor.BLACK);
                });
            }

            @Override
            public void onTimeOut(PieceColor losingColor) {
                handleTimeOut(losingColor);
            }
        });

        // Sidebar Buttons
        sidebarPanel.getNewGameButton().addActionListener(e -> startNewGame());
        sidebarPanel.getUndoButton().addActionListener(e -> undoMove());
        sidebarPanel.getFenButton().addActionListener(e -> showLoadFenDialog());
        sidebarPanel.getFlipButton().addActionListener(e -> {
            boolean nextFlipped = !boardPanel.isFlipped();
            boardPanel.setFlipped(nextFlipped);
            evalBarPanel.setFlipped(nextFlipped);
            updateClockPositions();
        });

        // Bester-Zug-Pfeil Umschalter (nur bei Mensch vs Mensch aktiv)
        sidebarPanel.addBestMoveToggleListener(show -> {
            boolean isHumanVsHuman = (sidebarPanel.getSelectedGameMode() == SidebarPanel.GameMode.HUMAN_VS_HUMAN);
            if (!isHumanVsHuman) {
                boardPanel.setShowBestMoveArrow(false);
                boardPanel.clearBestMove();
                return;
            }
            boardPanel.setShowBestMoveArrow(show);
            if (show) {
                startBackgroundEvaluation();
            } else {
                boardPanel.clearBestMove();
            }
        });

        // Stil-Umschalter
        sidebarPanel.getStyleComboBox().addActionListener(e -> {
            PieceRenderer.RenderStyle selected = (PieceRenderer.RenderStyle) sidebarPanel.getStyleComboBox().getSelectedItem();
            if (selected != null) {
                pieceRenderer.setCurrentStyle(selected);
                boardPanel.repaint();
            }
        });

        // Modus-Umschalter
        sidebarPanel.getModeComboBox().addActionListener(e -> {
            SidebarPanel.GameMode mode = sidebarPanel.getSelectedGameMode();
            if (mode == SidebarPanel.GameMode.HUMAN_BLACK_VS_BOT) {
                boardPanel.setFlipped(true); // Schwarz unten
                evalBarPanel.setFlipped(true);
            } else if (mode == SidebarPanel.GameMode.HUMAN_WHITE_VS_BOT) {
                boardPanel.setFlipped(false); // Weiß unten
                evalBarPanel.setFlipped(false);
            }
            updateClockPositions();
            updatePlayerClockLabels();
            updateEvaluationVisibility();
            checkAndTriggerBot();
        });

        // Bot-Umschalter
        sidebarPanel.getBotWhiteComboBox().addActionListener(e -> {
            updatePlayerClockLabels();
            checkAndTriggerBot();
        });
        sidebarPanel.getBotBlackComboBox().addActionListener(e -> {
            updatePlayerClockLabels();
            checkAndTriggerBot();
        });

        // Bedenkzeit & Inkrement Listener
        sidebarPanel.addTimeControlListener((minutes, incrementSeconds, enabled) -> {
            gameConfig.setTimeMinutes(minutes);
            gameConfig.setIncrementSeconds(incrementSeconds);
            gameConfig.setTimeControlEnabled(enabled);
            gameConfig.save();

            // Uhr zurücksetzen und aktualisieren
            chessClock.reset(
                    gameConfig.getInitialTimeMillis(),
                    gameConfig.getIncrementMillis(),
                    enabled
            );
            whiteClockPanel.setInitialTime(gameConfig.getInitialTimeMillis());
            blackClockPanel.setInitialTime(gameConfig.getInitialTimeMillis());

            // Falls die Partie noch unberührt ist, Uhr starten
            if (board.getMoveHistory().isEmpty() && enabled) {
                chessClock.start(board.getActivePlayer());
            }
        });
    }

    private ChessEngine getEngineForColor(PieceColor color) {
        if (customOverrideEngine != null) {
            return customOverrideEngine;
        }
        SidebarPanel.BotType botType = (color == PieceColor.WHITE)
                ? sidebarPanel.getSelectedWhiteBot()
                : sidebarPanel.getSelectedBlackBot();
        if (botType != null) {
            return botType.createEngine();
        }
        return new engine.bots.KingLBot1();
    }

    private void handlePlayerMove(Move move) {
        if (isEngineThinking) return;

        board.makeMove(move);
        boardPanel.repaint();
        sidebarPanel.updateGameState(board);
        startBackgroundEvaluation();

        // Uhr umschalten
        chessClock.onMoveMade(move.getMovedPiece().getColor());

        GameStatus status = MoveGenerator.evaluateGameStatus(board);
        if (status.isGameOver()) {
            chessClock.pause();
        }

        checkAndTriggerBot();
    }

    private void checkAndTriggerBot() {
        GameStatus status = MoveGenerator.evaluateGameStatus(board);
        if (status.isGameOver() || isEngineThinking) {
            if (status.isGameOver()) {
                chessClock.pause();
            }
            return;
        }

        SidebarPanel.GameMode mode = sidebarPanel.getSelectedGameMode();
        boolean botShouldMove = false;

        if (mode == SidebarPanel.GameMode.HUMAN_WHITE_VS_BOT && board.getActivePlayer() == PieceColor.BLACK) {
            botShouldMove = true;
        } else if (mode == SidebarPanel.GameMode.HUMAN_BLACK_VS_BOT && board.getActivePlayer() == PieceColor.WHITE) {
            botShouldMove = true;
        } else if (mode == SidebarPanel.GameMode.BOT_VS_BOT) {
            botShouldMove = true;
        }

        if (botShouldMove) {
            triggerBotMove();
        }
    }

    /**
     * Führt die Berechnung des Bot-Zuges asynchron im Hintergrund aus,
     * damit die Swing-GUI flüssig bleibt.
     */
    private void triggerBotMove() {
        if (isEngineThinking) return;
        isEngineThinking = true;
        PieceColor botColor = board.getActivePlayer();
        ChessEngine engine = getEngineForColor(botColor);

        // Thread-Sicherheit: Eigene Kopie des Bretts für die Hintergrundsuche,
        // damit parallele Zugoperationen im Bot-Suchbaum niemals das UI-Brett manipulieren!
        Board botBoard = board.copy();

        CompletableFuture.supplyAsync(() -> {
            try {
                // Kurze Bedenkzeit für natürliches Spielgefühl bei Bot vs Bot
                Thread.sleep(150);
            } catch (InterruptedException ignored) {}

            // Timer an die ChessEngine übergeben
            return engine.findBestMove(botBoard, botColor, chessClock);
        }).thenAccept(bestMove -> SwingUtilities.invokeLater(() -> {
            isEngineThinking = false;
            if (bestMove != null && board.getActivePlayer() == botColor) {
                board.makeMove(bestMove);
                boardPanel.repaint();
                sidebarPanel.updateGameState(board);

                // Uhr nach Bot-Zug umschalten
                chessClock.onMoveMade(bestMove.getMovedPiece().getColor());

                GameStatus status = MoveGenerator.evaluateGameStatus(board);
                if (status.isGameOver()) {
                    chessClock.pause();
                } else if (sidebarPanel.getSelectedGameMode() == SidebarPanel.GameMode.BOT_VS_BOT) {
                    checkAndTriggerBot();
                }
            }
        }));
    }

    public void startNewGame() {
        gameConfig.load(); // Konfigurationsdatei neu einlesen, falls der Nutzer sie editiert hat
        sidebarPanel.syncWithGameConfig(gameConfig);
        board.setupInitialPosition();
        boardPanel.clearSelection();
        sidebarPanel.updateGameState(board);
        startBackgroundEvaluation();
        boardPanel.repaint();

        chessClock.reset(
                gameConfig.getInitialTimeMillis(),
                gameConfig.getIncrementMillis(),
                gameConfig.isTimeControlEnabled()
        );
        updateClockPositions();
        updatePlayerClockLabels();
        whiteClockPanel.setInitialTime(gameConfig.getInitialTimeMillis());
        blackClockPanel.setInitialTime(gameConfig.getInitialTimeMillis());

        if (gameConfig.isTimeControlEnabled()) {
            chessClock.start(board.getActivePlayer());
        }

        SidebarPanel.GameMode mode = sidebarPanel.getSelectedGameMode();
        if (mode == SidebarPanel.GameMode.HUMAN_BLACK_VS_BOT) {
            triggerBotMove();
        } else if (mode == SidebarPanel.GameMode.BOT_VS_BOT) {
            triggerBotMove();
        }
    }

    public void showLoadFenDialog() {
        String defaultInput = "";
        try {
            java.awt.datatransfer.Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (cb.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                String clip = (String) cb.getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                if (clip != null && clip.contains("/")) {
                    defaultInput = clip.trim();
                }
            }
        } catch (Exception ignored) {}

        if (defaultInput.isEmpty()) {
            defaultInput = "rn1k3r/1b1q1ppp/p2P4/2B2p2/8/1QNBR3/PP3PPP/2R3K1 w - - 1 0";
        }

        String fen = (String) JOptionPane.showInputDialog(
                this,
                "Gib eine FEN-Notation ein:\n(Bsp: rn1k3r/1b1q1ppp/p2P4/2B2p2/8/1QNBR3/PP3PPP/2R3K1 w - - 1 0)",
                "FEN laden",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                defaultInput
        );

        if (fen != null && !fen.isBlank()) {
            loadFen(fen.trim());
        }
    }

    public void copyFenToClipboard() {
        String fen = board.toFen();
        try {
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(fen);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            JOptionPane.showMessageDialog(this,
                    "Aktuelle FEN in Zwischenablage kopiert:\n" + fen,
                    "FEN kopiert", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Kopieren fehlgeschlagen:\n" + fen,
                    "FEN", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void loadFen(String fen) {
        try {
            board.loadFen(fen);
            boardPanel.clearSelection();
            boardPanel.repaint();
            sidebarPanel.updateGameState(board);
            startBackgroundEvaluation();

            if (gameConfig.isTimeControlEnabled()) {
                chessClock.reset(gameConfig.getInitialTimeMillis(), gameConfig.getIncrementMillis(), true);
                whiteClockPanel.setInitialTime(gameConfig.getInitialTimeMillis());
                blackClockPanel.setInitialTime(gameConfig.getInitialTimeMillis());
                chessClock.start(board.getActivePlayer());
            }
            updatePlayerClockLabels();
            checkAndTriggerBot();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Laden der FEN:\n" + ex.getMessage(),
                    "Ungültige FEN", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void undoMove() {
        if (isEngineThinking) return;

        SidebarPanel.GameMode mode = sidebarPanel.getSelectedGameMode();
        if (mode == SidebarPanel.GameMode.HUMAN_WHITE_VS_BOT || mode == SidebarPanel.GameMode.HUMAN_BLACK_VS_BOT) {
            // Gegen den Bot 2 Halbzüge zurücknehmen, damit der Spieler wieder am Zug ist
            board.undoMove();
            board.undoMove();
        } else {
            board.undoMove();
        }

        boardPanel.clearSelection();
        sidebarPanel.updateGameState(board);
        startBackgroundEvaluation();
        boardPanel.repaint();

        if (chessClock.isRunning()) {
            chessClock.pause();
            chessClock.start(board.getActivePlayer());
        }
    }

    private void updateClockPositions() {
        topClockContainer.removeAll();
        bottomClockContainer.removeAll();

        if (!boardPanel.isFlipped()) {
            topClockContainer.add(blackClockPanel, BorderLayout.CENTER);
            bottomClockContainer.add(whiteClockPanel, BorderLayout.CENTER);
        } else {
            topClockContainer.add(whiteClockPanel, BorderLayout.CENTER);
            bottomClockContainer.add(blackClockPanel, BorderLayout.CENTER);
        }

        topClockContainer.revalidate();
        bottomClockContainer.revalidate();
        topClockContainer.repaint();
        bottomClockContainer.repaint();
    }

    private void updatePlayerClockLabels() {
        SidebarPanel.GameMode mode = sidebarPanel.getSelectedGameMode();
        switch (mode) {
            case HUMAN_VS_HUMAN -> {
                whiteClockPanel.setPlayerLabel("♔ Spieler 1 (Weiß)");
                blackClockPanel.setPlayerLabel("♚ Spieler 2 (Schwarz)");
            }
            case HUMAN_WHITE_VS_BOT -> {
                whiteClockPanel.setPlayerLabel("♔ Du (Weiß)");
                blackClockPanel.setPlayerLabel("♚ " + sidebarPanel.getSelectedBlackBot().getLabel());
            }
            case HUMAN_BLACK_VS_BOT -> {
                whiteClockPanel.setPlayerLabel("♔ " + sidebarPanel.getSelectedWhiteBot().getLabel());
                blackClockPanel.setPlayerLabel("♚ Du (Schwarz)");
            }
            case BOT_VS_BOT -> {
                whiteClockPanel.setPlayerLabel("♔ " + sidebarPanel.getSelectedWhiteBot().getLabel());
                blackClockPanel.setPlayerLabel("♚ " + sidebarPanel.getSelectedBlackBot().getLabel());
            }
        }
    }

    private void handleTimeOut(PieceColor losingColor) {
        SwingUtilities.invokeLater(() -> {
            isEngineThinking = false;
            String winner = (losingColor == PieceColor.WHITE) ? "Schwarz" : "Weiß";
            String loser = (losingColor == PieceColor.WHITE) ? "Weiß" : "Schwarz";
            String msg = "Zeit abgelaufen! " + winner + " gewinnt durch Zeitüberschreitung von " + loser + ".";
            sidebarPanel.setCustomStatus("Zeitüberschreitung!", new Color(255, 80, 80));
            JOptionPane.showMessageDialog(this, msg, "Partie beendet", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    /**
     * Steuert die Sichtbarkeit und Ausführung der Stellungsbewertung (EVAL).
     * EVAL wird nur im Modus "Mensch VS Mensch" angezeigt und berechnet.
     */
    private void updateEvaluationVisibility() {
        boolean isHumanVsHuman = (sidebarPanel.getSelectedGameMode() == SidebarPanel.GameMode.HUMAN_VS_HUMAN);

        evalBarPanel.setVisible(isHumanVsHuman);
        sidebarPanel.setEvaluationVisible(isHumanVsHuman);

        if (!isHumanVsHuman) {
            // Bei Bot-Spielen den Pfeil und die Hintergrund-Eval komplett abschalten
            boardPanel.setShowBestMoveArrow(false);
            boardPanel.clearBestMove();
            sidebarPanel.setShowBestMoveSelected(false);
            stopBackgroundEvaluation();
        }

        getContentPane().revalidate();
        getContentPane().repaint();

        if (isHumanVsHuman) {
            startBackgroundEvaluation();
        }
    }

    private synchronized void stopBackgroundEvaluation() {
        if (backgroundEvalBot != null) {
            backgroundEvalBot.stopSearch();
        }
        if (currentEvalTask != null && !currentEvalTask.isDone()) {
            currentEvalTask.cancel(true);
        }
    }

    /**
     * Startet die kontinuierliche Stellungsanalyse im Hintergrund mittels KingLBot1.
     * Nutzt ein Zeitlimit von 1.000.000 ms, sodass mit zunehmender Bedenkzeit immer tiefere
     * Suchtiefen (T1, T2, T3, ...) berechnet und die Eval-Bar bzw. der Pfeil kontinuierlich präzisiert wird.
     * Wird ausschließlich im Modus Mensch VS Mensch ausgeführt (bei Bot-Spielen komplett deaktiviert).
     */
    private synchronized void startBackgroundEvaluation() {
        boolean isHumanVsHuman = (sidebarPanel.getSelectedGameMode() == SidebarPanel.GameMode.HUMAN_VS_HUMAN);

        if (!isHumanVsHuman) {
            stopBackgroundEvaluation();
            boardPanel.clearBestMove();
            return;
        }

        // 1. Vorherige Hintergrundsuche abbrechen & bisherigen Pfeil zurücksetzen
        stopBackgroundEvaluation();
        boardPanel.clearBestMove();

        // 2. Sofortige statische Heuristik anzeigen
        engine.bots.JChessV1 staticEvalBot = new engine.bots.JChessV1(1);
        double immediateScore = staticEvalBot.evaluate(board, PieceColor.WHITE);
        GameStatus status = MoveGenerator.evaluateGameStatus(board);
        evalBarPanel.setEvaluation(immediateScore, status);
        sidebarPanel.updateEvaluationWithScore(immediateScore, status, 0);

        if (status.isGameOver()) {
            boardPanel.clearBestMove();
            return;
        }

        // 3. Kopie des Bretts für threadsichere parallele Berechnung erstellen
        Board analysisBoard = board.copy();
        PieceColor activeColor = analysisBoard.getActivePlayer();

        // 4. JChessV1 mit TimeLimit 1_000_000 ms (~16,6 Minuten) und max. Tiefe 64
        engine.bots.JChessV1 analyzer = new engine.bots.JChessV1("EvalBar", 64, false, 1_000_000L, 0.05);
        this.backgroundEvalBot = analyzer;

        currentEvalTask = evalExecutor.submit(() -> {
            try {
                analyzer.findBestMoveWithListener(analysisBoard, activeColor, null,
                        (depth, centipawnScore, bestMove, nodes, elapsedMs) -> {
                            if (analyzer.isStopRequested() || Thread.currentThread().isInterrupted()) {
                                return;
                            }

                            // Bewertung in Bauerneinheiten aus Sicht von Weiß umrechnen
                            double whiteScore;
                            if (centipawnScore >= 25000.0) {
                                int mateInPly = (int) Math.round(30000.0 - centipawnScore);
                                int mateMoves = Math.max(1, (mateInPly + 1) / 2);
                                whiteScore = (activeColor == PieceColor.WHITE)
                                        ? (1000.0 - mateMoves)
                                        : (-1000.0 + mateMoves);
                            } else if (centipawnScore <= -25000.0) {
                                int mateInPly = (int) Math.round(30000.0 + centipawnScore);
                                int mateMoves = Math.max(1, (mateInPly + 1) / 2);
                                whiteScore = (activeColor == PieceColor.WHITE)
                                        ? (-1000.0 + mateMoves)
                                        : (1000.0 - mateMoves);
                            } else {
                                double pawns = centipawnScore / 100.0;
                                whiteScore = (activeColor == PieceColor.WHITE) ? pawns : -pawns;
                            }

                            SwingUtilities.invokeLater(() -> {
                                if (analyzer.isStopRequested()) return;
                                if (isHumanVsHuman) {
                                    evalBarPanel.setEvaluation(whiteScore, status);
                                    sidebarPanel.updateEvaluationWithScore(whiteScore, status, depth);
                                }
                                boardPanel.setBestMove(bestMove);
                            });
                        });
            } catch (Exception ignored) {
                // Bei Abbruch oder Thread-Unterbrechung
            }
        });
    }

    /**
     * Erlaubt das Austauschen der Engine zur Laufzeit.
     */
    public void setEngine(ChessEngine engine) {
        this.customOverrideEngine = engine;
    }
}
