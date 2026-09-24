package com.jchess.ui;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.engine.ChessEngine;
import com.jchess.core.evaluation.PositionEvaluator;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.rules.GameStatus;
import com.jchess.core.rules.MoveGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Seitenleiste mit Spielsteuerung:
 * - Anzeige für aktuellen Spieler & Schach/Matt-Status
 * - Live-Stellungsbewertung & Materialbilanz
 * - Spielmodus (Mensch vs. Mensch, Mensch vs. Bot)
 * - Bot-Auswahl (KingLBot1, RandomBot etc.)
 * - Figurenstil-Umschalter (Pixel-Art / High-Res)
 * - Aktions-Buttons: Neues Spiel, Undo, Brett drehen
 * - Zug-Historie mit Standard-Notation
 */
public class SidebarPanel extends JPanel {

    public enum GameMode {
        HUMAN_VS_HUMAN("Mensch vs. Mensch"),
        HUMAN_WHITE_VS_BOT("Mensch (Weiß) vs. Bot"),
        HUMAN_BLACK_VS_BOT("Mensch (Schwarz) vs. Bot"),
        BOT_VS_BOT("Bot vs. Bot (Demo)");

        private final String label;
        GameMode(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    public enum BotType {
        JCHESS_V2("JChessV2 (Advanced Eval)", () -> new engine.bots.JChessV2("JChessV2 (Advanced)", 5000L, 0.05)),
        JCHESS_V1(            "JChessV1 (5% Restzeit / 5s)",     () -> new engine.bots.JChessV1("JChessV1 (5%)", 5000L, 0.05)),
        JCHESS_V1_FAST(       "JChessV1 (2.5% Restzeit / 1s)",   () -> new engine.bots.JChessV1("JChessV1 (2.5%)", 1000L, 0.025)),
        JCHESS_V1_DEEP(       "JChessV1 (10% Restzeit / 10s)",   () -> new engine.bots.JChessV1("JChessV1 (10%)", 10000L, 0.10)),
        KINGL_BOT_1(          "KingLBot1 (Original)",            () -> new engine.bots.KingLBot1("KingLBot1 (5%)", 5000L, 0.05)),
        RANDOM(               "Random Bot (Zufall)",             () -> new engine.bots.Random());


        private final String label;
        private final Supplier<ChessEngine> supplier;

        BotType(String label, Supplier<ChessEngine> supplier) {
            this.label = label;
            this.supplier = supplier;
        }

        public String getLabel() { return label; }
        public ChessEngine createEngine() { return supplier.get(); }
        @Override public String toString() { return label; }
    }

    public interface TimeControlListener {
        void onTimeControlChanged(int minutes, int incrementSeconds, boolean enabled);
    }

    public static class TimeOption {
        private final String label;
        private final int minutes;
        private final boolean enabled;

        public TimeOption(String label, int minutes, boolean enabled) {
            this.label = label;
            this.minutes = minutes;
            this.enabled = enabled;
        }

        public String getLabel() { return label; }
        public int getMinutes() { return minutes; }
        public boolean isEnabled() { return enabled; }
        @Override public String toString() { return label; }
    }

    public static class IncrementOption {
        private final String label;
        private final int seconds;

        public IncrementOption(String label, int seconds) {
            this.label = label;
            this.seconds = seconds;
        }

        public String getLabel() { return label; }
        public int getSeconds() { return seconds; }
        @Override public String toString() { return label; }
    }

    private final JLabel turnLabel;
    private final JLabel statusLabel;
    private final JPanel evalPanel;
    private final JLabel evalScoreLabel;
    private final JLabel evalDescLabel;
    private final JLabel materialLabel;
    private final JCheckBox showBestMoveCheckbox;
    private final JComboBox<GameMode> modeComboBox;
    private final JLabel botWhiteLabel;
    private final JComboBox<BotType> botWhiteComboBox;
    private final JLabel botBlackLabel;
    private final JComboBox<BotType> botBlackComboBox;
    private final JComboBox<TimeOption> timeComboBox;
    private final JComboBox<IncrementOption> incrementComboBox;
    private final JComboBox<PieceRenderer.RenderStyle> styleComboBox;
    private final JButton newGameButton;
    private final JButton undoButton;
    private final JButton flipButton;
    private final JButton fenButton;
    private final DefaultTableModel historyTableModel;
    private final JTable historyTable;
    private final java.util.List<TimeControlListener> timeControlListeners = new java.util.ArrayList<>();

    public SidebarPanel() {
        setLayout(new BorderLayout(12, 12));
        setPreferredSize(new Dimension(280, 680));
        setBackground(new Color(32, 30, 27));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // --- OBERER BEREICH: Status & Info ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("JChess Engine");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        titleLabel.setForeground(new Color(235, 235, 235));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        turnLabel = new JLabel("Am Zug: Weiß");
        turnLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        turnLabel.setForeground(new Color(210, 210, 210));
        turnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel("Status: Partie läuft");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        statusLabel.setForeground(new Color(140, 200, 120));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- EVALUATION PANEL ---
        evalPanel = new JPanel();
        evalPanel.setLayout(new BoxLayout(evalPanel, BoxLayout.Y_AXIS));
        evalPanel.setBackground(new Color(42, 40, 36));
        evalPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(64, 61, 56), 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        evalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel evalHeader = new JPanel(new BorderLayout());
        evalHeader.setOpaque(false);
        JLabel evalTitle = new JLabel("Bewertung (Live):");
        evalTitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        evalTitle.setForeground(new Color(175, 175, 175));

        evalScoreLabel = new JLabel("0.0");
        evalScoreLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        evalScoreLabel.setForeground(new Color(240, 240, 240));
        evalHeader.add(evalTitle, BorderLayout.WEST);
        evalHeader.add(evalScoreLabel, BorderLayout.EAST);

        evalDescLabel = new JLabel("Ausgeglichen");
        evalDescLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        evalDescLabel.setForeground(new Color(200, 200, 200));

        materialLabel = new JLabel("Material: Gleich");
        materialLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        materialLabel.setForeground(new Color(155, 155, 155));

        showBestMoveCheckbox = new JCheckBox("Bester Zug (Pfeil)");
        showBestMoveCheckbox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        showBestMoveCheckbox.setForeground(new Color(215, 215, 215));
        showBestMoveCheckbox.setOpaque(false);
        showBestMoveCheckbox.setFocusPainted(false);
        showBestMoveCheckbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showBestMoveCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        evalPanel.add(evalHeader);
        evalPanel.add(Box.createVerticalStrut(4));
        evalPanel.add(evalDescLabel);
        evalPanel.add(Box.createVerticalStrut(3));
        evalPanel.add(materialLabel);

        topPanel.add(titleLabel);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(turnLabel);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(statusLabel);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(evalPanel);
        topPanel.add(Box.createVerticalStrut(12));

        // --- EINSTELLUNGEN: Modus, Bot & Stil ---
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setOpaque(false);

        modeComboBox = new JComboBox<>(GameMode.values());
        modeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        modeComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        botWhiteComboBox = new JComboBox<>(BotType.values());
        botWhiteComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        botWhiteComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        botWhiteComboBox.setSelectedItem(BotType.JCHESS_V2);

        botBlackComboBox = new JComboBox<>(BotType.values());
        botBlackComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        botBlackComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        botBlackComboBox.setSelectedItem(BotType.JCHESS_V2);

        // Zeit & Inkrement aus GameConfig initialisieren
        com.jchess.core.config.GameConfig cfg = com.jchess.core.config.GameConfig.getInstance();
        java.util.List<TimeOption> timeOptions = new java.util.ArrayList<>(java.util.List.of(
                new TimeOption("1 Min (Bullet)", 1, true),
                new TimeOption("2 Min (Bullet)", 2, true),
                new TimeOption("3 Min (Blitz)", 3, true),
                new TimeOption("5 Min (Blitz)", 5, true),
                new TimeOption("10 Min (Rapid)", 10, true),
                new TimeOption("15 Min (Rapid)", 15, true),
                new TimeOption("30 Min", 30, true),
                new TimeOption("Ohne Uhr", 0, false)
        ));

        TimeOption initialTime = null;
        if (!cfg.isTimeControlEnabled()) {
            initialTime = timeOptions.get(timeOptions.size() - 1);
        } else {
            for (TimeOption opt : timeOptions) {
                if (opt.isEnabled() && opt.getMinutes() == cfg.getTimeMinutes()) {
                    initialTime = opt;
                    break;
                }
            }
            if (initialTime == null) {
                initialTime = new TimeOption(cfg.getTimeMinutes() + " Min", cfg.getTimeMinutes(), true);
                timeOptions.add(0, initialTime);
            }
        }

        java.util.List<IncrementOption> incOptions = new java.util.ArrayList<>(java.util.List.of(
                new IncrementOption("+0s", 0),
                new IncrementOption("+1s", 1),
                new IncrementOption("+2s", 2),
                new IncrementOption("+3s", 3),
                new IncrementOption("+5s", 5),
                new IncrementOption("+10s", 10)
        ));

        IncrementOption initialInc = null;
        for (IncrementOption opt : incOptions) {
            if (opt.getSeconds() == cfg.getIncrementSeconds()) {
                initialInc = opt;
                break;
            }
        }
        if (initialInc == null) {
            initialInc = new IncrementOption("+" + cfg.getIncrementSeconds() + "s", cfg.getIncrementSeconds());
            incOptions.add(0, initialInc);
        }

        timeComboBox = new JComboBox<>(timeOptions.toArray(new TimeOption[0]));
        timeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        timeComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        timeComboBox.setSelectedItem(initialTime);

        incrementComboBox = new JComboBox<>(incOptions.toArray(new IncrementOption[0]));
        incrementComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        incrementComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        incrementComboBox.setSelectedItem(initialInc);

        timeComboBox.addActionListener(e -> fireTimeControlChanged());
        incrementComboBox.addActionListener(e -> fireTimeControlChanged());

        styleComboBox = new JComboBox<>(PieceRenderer.RenderStyle.values());
        styleComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        styleComboBox.setSelectedItem(PieceRenderer.RenderStyle.HD_SPRITES);
        styleComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PieceRenderer.RenderStyle s) {
                    setText(s.getLabel());
                }
                return this;
            }
        });

        controlsPanel.add(createControlLabel("Spielmodus:"));
        controlsPanel.add(Box.createVerticalStrut(3));
        controlsPanel.add(modeComboBox);
        controlsPanel.add(Box.createVerticalStrut(6));

        botWhiteLabel = createControlLabel("Bot (Gegner Weiß):");
        controlsPanel.add(botWhiteLabel);
        controlsPanel.add(Box.createVerticalStrut(3));
        controlsPanel.add(botWhiteComboBox);
        controlsPanel.add(Box.createVerticalStrut(6));

        botBlackLabel = createControlLabel("Bot (Gegner Schwarz):");
        controlsPanel.add(botBlackLabel);
        controlsPanel.add(Box.createVerticalStrut(3));
        controlsPanel.add(botBlackComboBox);
        controlsPanel.add(Box.createVerticalStrut(6));

        // Zeit & Inkrement
        JPanel timePanel = new JPanel(new GridLayout(1, 2, 6, 0));
        timePanel.setOpaque(false);
        timePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        timePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel timeSubPanel = new JPanel();
        timeSubPanel.setLayout(new BoxLayout(timeSubPanel, BoxLayout.Y_AXIS));
        timeSubPanel.setOpaque(false);
        timeSubPanel.add(createControlLabel("Bedenkzeit:"));
        timeSubPanel.add(Box.createVerticalStrut(3));
        timeSubPanel.add(timeComboBox);

        JPanel incSubPanel = new JPanel();
        incSubPanel.setLayout(new BoxLayout(incSubPanel, BoxLayout.Y_AXIS));
        incSubPanel.setOpaque(false);
        incSubPanel.add(createControlLabel("Inkrement:"));
        incSubPanel.add(Box.createVerticalStrut(3));
        incSubPanel.add(incrementComboBox);

        timePanel.add(timeSubPanel);
        timePanel.add(incSubPanel);

        controlsPanel.add(timePanel);
        controlsPanel.add(Box.createVerticalStrut(6));

        controlsPanel.add(createControlLabel("Grafikstil:"));
        controlsPanel.add(Box.createVerticalStrut(3));
        controlsPanel.add(styleComboBox);
        controlsPanel.add(Box.createVerticalStrut(10));
        controlsPanel.add(showBestMoveCheckbox);

        modeComboBox.addActionListener(e -> updateBotSelectorVisibility());
        updateBotSelectorVisibility();

        topPanel.add(controlsPanel);
        topPanel.add(Box.createVerticalStrut(14));

        // --- BUTTONS ---
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 4, 4));
        buttonPanel.setOpaque(false);

        newGameButton = new JButton("Neu");
        undoButton = new JButton("Undo");
        flipButton = new JButton("Drehen");
        fenButton = new JButton("FEN");

        buttonPanel.add(newGameButton);
        buttonPanel.add(undoButton);
        buttonPanel.add(flipButton);
        buttonPanel.add(fenButton);
        topPanel.add(buttonPanel);

        add(topPanel, BorderLayout.NORTH);

        // --- ZUG-HISTORIE ---
        historyTableModel = new DefaultTableModel(new Object[]{"#", "Weiß", "Schwarz"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(historyTableModel);
        historyTable.setBackground(new Color(40, 38, 35));
        historyTable.setForeground(new Color(230, 230, 230));
        historyTable.getTableHeader().setBackground(new Color(50, 48, 44));
        historyTable.getTableHeader().setForeground(new Color(200, 200, 200));
        historyTable.setFillsViewportHeight(true);
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(35);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 58, 54)),
                "Zug-Historie",
                0, 0, null, new Color(180, 180, 180)
        ));
        scrollPane.getViewport().setBackground(new Color(40, 38, 35));

        add(scrollPane, BorderLayout.CENTER);
    }

    public GameMode getSelectedGameMode() {
        return (GameMode) modeComboBox.getSelectedItem();
    }

    public void updateGameState(Board board) {
        this.lastBoard = board;

        // Spieleranzeige
        PieceColor active = board.getActivePlayer();
        turnLabel.setText("Am Zug: " + (active == PieceColor.WHITE ? "Weiß ♔" : "Schwarz ♚"));

        // Spielstatus
        GameStatus status = MoveGenerator.evaluateGameStatus(board);
        statusLabel.setText("Status: " + status.getDescription());

        if (status == GameStatus.CHECKMATE) {
            statusLabel.setForeground(new Color(255, 80, 80));
        } else if (status == GameStatus.CHECK) {
            statusLabel.setForeground(new Color(255, 170, 50));
        } else if (status.isGameOver()) {
            statusLabel.setForeground(new Color(180, 180, 255));
        } else {
            statusLabel.setForeground(new Color(140, 200, 120));
        }

        // Historie aktualisieren
        updateHistoryTable(board.getMoveHistory());

        // Stellungsbewertung aktualisieren
        updateEvaluation(board);
    }

    private Board lastBoard;

    public void updateEvaluation(Board board) {
        this.lastBoard = board;
        double score = PositionEvaluator.evaluate(board);
        GameStatus status = MoveGenerator.evaluateGameStatus(board);
        updateEvaluationWithScore(score, status, 0);
    }

    public void updateEvaluationWithScore(double score, GameStatus status, int depth) {
        String formatted = PositionEvaluator.formatScore(score, status);
        if (depth > 0 && (status == GameStatus.ACTIVE || status == GameStatus.CHECK)) {
            formatted += " (T" + depth + ")";
        }
        String desc = PositionEvaluator.getAdvantageText(score, status);
        if (lastBoard != null) {
            PositionEvaluator.MaterialBalance mat = PositionEvaluator.getMaterialBalance(lastBoard);
            materialLabel.setText(mat.getDifferenceLabel());
        }

        evalScoreLabel.setText(formatted);
        evalDescLabel.setText(desc);

        if (status == GameStatus.CHECKMATE) {
            evalScoreLabel.setForeground(new Color(255, 85, 85));
        } else if (score >= 1.0) {
            evalScoreLabel.setForeground(new Color(250, 250, 250));
        } else if (score <= -1.0) {
            evalScoreLabel.setForeground(new Color(175, 175, 175));
        } else {
            evalScoreLabel.setForeground(new Color(215, 215, 215));
        }
    }

    private void updateHistoryTable(List<Move> history) {
        historyTableModel.setRowCount(0);
        for (int i = 0; i < history.size(); i += 2) {
            int moveNum = (i / 2) + 1;
            String whiteMove = history.get(i).toSimpleNotation();
            String blackMove = (i + 1 < history.size()) ? history.get(i + 1).toSimpleNotation() : "";
            historyTableModel.addRow(new Object[]{moveNum + ".", whiteMove, blackMove});
        }

        // Automatisch nach unten scrollen
        if (historyTable.getRowCount() > 0) {
            historyTable.scrollRectToVisible(historyTable.getCellRect(historyTable.getRowCount() - 1, 0, true));
        }
    }

    private JLabel createControlLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        label.setForeground(new Color(180, 180, 180));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public void updateBotSelectorVisibility() {
        GameMode mode = getSelectedGameMode();
        switch (mode) {
            case HUMAN_VS_HUMAN -> {
                botWhiteLabel.setVisible(false);
                botWhiteComboBox.setVisible(false);
                botBlackLabel.setVisible(false);
                botBlackComboBox.setVisible(false);
            }
            case HUMAN_WHITE_VS_BOT -> {
                botWhiteLabel.setVisible(false);
                botWhiteComboBox.setVisible(false);
                botBlackLabel.setText("Bot (Gegner Schwarz):");
                botBlackLabel.setVisible(true);
                botBlackComboBox.setVisible(true);
            }
            case HUMAN_BLACK_VS_BOT -> {
                botWhiteLabel.setText("Bot (Gegner Weiß):");
                botWhiteLabel.setVisible(true);
                botWhiteComboBox.setVisible(true);
                botBlackLabel.setVisible(false);
                botBlackComboBox.setVisible(false);
            }
            case BOT_VS_BOT -> {
                botWhiteLabel.setText("Bot (Weiß):");
                botWhiteLabel.setVisible(true);
                botWhiteComboBox.setVisible(true);
                botBlackLabel.setText("Bot (Schwarz):");
                botBlackLabel.setVisible(true);
                botBlackComboBox.setVisible(true);
            }
        }

        // Bester Zug Pfeil nur im Modus "Mensch vs. Mensch" erlauben, bei Bot-Spielen komplett deaktivieren
        boolean isHumanVsHuman = (mode == GameMode.HUMAN_VS_HUMAN);
        showBestMoveCheckbox.setVisible(isHumanVsHuman);
        if (!isHumanVsHuman) {
            showBestMoveCheckbox.setSelected(false);
        }

        revalidate();
        repaint();
    }

    public BotType getSelectedWhiteBot() {
        return (BotType) botWhiteComboBox.getSelectedItem();
    }

    public BotType getSelectedBlackBot() {
        return (BotType) botBlackComboBox.getSelectedItem();
    }

    public void setCustomStatus(String text, Color color) {
        statusLabel.setText(text);
        if (color != null) {
            statusLabel.setForeground(color);
        }
    }

    public void setEvaluationVisible(boolean visible) {
        evalPanel.setVisible(visible);
        revalidate();
        repaint();
    }

    public boolean isEvaluationVisible() {
        return evalPanel.isVisible();
    }

    public void addBestMoveToggleListener(Consumer<Boolean> listener) {
        showBestMoveCheckbox.addActionListener(e -> listener.accept(showBestMoveCheckbox.isSelected()));
    }

    public boolean isShowBestMoveSelected() {
        return showBestMoveCheckbox.isSelected();
    }

    public void setShowBestMoveSelected(boolean selected) {
        showBestMoveCheckbox.setSelected(selected);
    }

    public JCheckBox getShowBestMoveCheckbox() {
        return showBestMoveCheckbox;
    }

    public JLabel getStatusLabel() { return statusLabel; }
    public JComboBox<BotType> getBotWhiteComboBox() { return botWhiteComboBox; }
    public JComboBox<BotType> getBotBlackComboBox() { return botBlackComboBox; }
    public JComboBox<GameMode> getModeComboBox() { return modeComboBox; }
    public JComboBox<PieceRenderer.RenderStyle> getStyleComboBox() { return styleComboBox; }
    public void syncWithGameConfig(com.jchess.core.config.GameConfig cfg) {
        if (cfg == null) return;
        boolean enabled = cfg.isTimeControlEnabled();
        int mins = cfg.getTimeMinutes();
        int inc = cfg.getIncrementSeconds();

        for (int i = 0; i < timeComboBox.getItemCount(); i++) {
            TimeOption opt = timeComboBox.getItemAt(i);
            if (!enabled && !opt.isEnabled()) {
                timeComboBox.setSelectedIndex(i);
                break;
            } else if (enabled && opt.isEnabled() && opt.getMinutes() == mins) {
                timeComboBox.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < incrementComboBox.getItemCount(); i++) {
            IncrementOption opt = incrementComboBox.getItemAt(i);
            if (opt.getSeconds() == inc) {
                incrementComboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    public void addTimeControlListener(TimeControlListener listener) {
        if (listener != null) {
            timeControlListeners.add(listener);
        }
    }

    private void fireTimeControlChanged() {
        TimeOption tOpt = (TimeOption) timeComboBox.getSelectedItem();
        IncrementOption iOpt = (IncrementOption) incrementComboBox.getSelectedItem();
        if (tOpt != null && iOpt != null) {
            for (TimeControlListener l : timeControlListeners) {
                l.onTimeControlChanged(tOpt.getMinutes(), iOpt.getSeconds(), tOpt.isEnabled());
            }
        }
    }

    public JComboBox<TimeOption> getTimeComboBox() { return timeComboBox; }
    public JComboBox<IncrementOption> getIncrementComboBox() { return incrementComboBox; }

    public JButton getNewGameButton() { return newGameButton; }
    public JButton getUndoButton() { return undoButton; }
    public JButton getFlipButton() { return flipButton; }
    public JButton getFenButton() { return fenButton; }
}
