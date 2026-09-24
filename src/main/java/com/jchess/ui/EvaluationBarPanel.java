package com.jchess.ui;

import com.jchess.core.board.Board;
import com.jchess.core.evaluation.PositionEvaluator;
import com.jchess.core.rules.GameStatus;
import com.jchess.core.rules.MoveGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Vertikale Bewertungsleiste (Eval-Bar) im modernen Lichess/Chess.com-Stil.
 * - Zeigt die aktuelle Stellungsbewertung dynamisch und weich animiert an.
 * - Berücksichtigt Brettdrehung (flipped).
 * - Hohe Kontraste mit geschmeidigen Kanten (Antialiasing).
 */
public class EvaluationBarPanel extends JPanel {

    private static final Color BG_COLOR = new Color(44, 38, 33);
    private static final Color WHITE_BAR_COLOR = new Color(242, 242, 242);
    private static final Color BLACK_BAR_COLOR = new Color(48, 46, 43);
    private static final Color BORDER_COLOR = new Color(85, 75, 68);

    private double currentProbability = 0.5; // Für sanfte Animation
    private double targetProbability = 0.5;
    private double currentScore = 0.0;
    private GameStatus currentStatus = GameStatus.ACTIVE;
    private boolean flipped = false;

    private final Timer animationTimer;

    public EvaluationBarPanel() {
        setPreferredSize(new Dimension(32, 640));
        setMinimumSize(new Dimension(28, 200));
        setBackground(BG_COLOR);
        setToolTipText("Stellungsbewertung (Live)");

        // 60-FPS Animation für weiche Balkenbewegungen
        animationTimer = new Timer(16, e -> {
            double diff = targetProbability - currentProbability;
            if (Math.abs(diff) > 0.002) {
                currentProbability += diff * 0.25;
                repaint();
            } else {
                currentProbability = targetProbability;
                ((Timer) e.getSource()).stop();
                repaint();
            }
        });
    }

    /**
     * Aktualisiert die Anzeige anhand des aktuellen Brettzustands.
     */
    public void updateEvaluation(Board board) {
        double score = PositionEvaluator.evaluate(board);
        GameStatus status = MoveGenerator.evaluateGameStatus(board);
        setEvaluation(score, status);
    }

    public void setEvaluation(double score, GameStatus status) {
        this.currentScore = score;
        this.currentStatus = status;
        this.targetProbability = PositionEvaluator.getWinProbabilityWhite(score);

        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        repaint();
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

    public boolean isFlipped() {
        return flipped;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int padX = 6;
        int padY = 24; // Passt zur BORDER_MARGIN des ChessBoardPanels
        int barW = Math.max(16, getWidth() - 2 * padX);
        int barH = Math.max(40, getHeight() - 2 * padY);

        int barX = (getWidth() - barW) / 2;
        int barY = padY;

        // Form des abgerundeten Balkens
        RoundRectangle2D barShape = new RoundRectangle2D.Float(barX, barY, barW, barH, 8, 8);

        // Standardmäßig (unflipped): Weiß unten, Schwarz oben
        // Flipped: Schwarz unten, Weiß oben
        double whiteRatio = Math.clamp(currentProbability, 0.0, 1.0);
        double bottomRatio = flipped ? (1.0 - whiteRatio) : whiteRatio;

        int splitY = barY + (int) Math.round(barH * (1.0 - bottomRatio));

        // Clip auf die abgerundete Form
        Shape oldClip = g2.getClip();
        g2.clip(barShape);

        if (!flipped) {
            // Oben Schwarz, unten Weiß
            g2.setColor(BLACK_BAR_COLOR);
            g2.fillRect(barX, barY, barW, splitY - barY);

            g2.setColor(WHITE_BAR_COLOR);
            g2.fillRect(barX, splitY, barW, barY + barH - splitY);
        } else {
            // Oben Weiß, unten Schwarz
            g2.setColor(WHITE_BAR_COLOR);
            g2.fillRect(barX, barY, barW, splitY - barY);

            g2.setColor(BLACK_BAR_COLOR);
            g2.fillRect(barX, splitY, barW, barY + barH - splitY);
        }

        // Trennlinie
        g2.setColor(new Color(120, 120, 120, 100));
        g2.drawLine(barX, splitY, barX + barW, splitY);

        g2.setClip(oldClip);

        // Rand zeichnen
        g2.setColor(BORDER_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(barShape);

        // Textbewertung (z.B. "+0.5" oder "-1.2")
        String scoreText = PositionEvaluator.formatScore(currentScore, currentStatus);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(scoreText);

        int textX = barX + (barW - textW) / 2;

        // Platziere den Text in der größeren Hälfte für optimalen Kontrast
        boolean showAtBottom;
        if (!flipped) {
            showAtBottom = currentScore >= 0;
        } else {
            showAtBottom = currentScore <= 0;
        }

        int textY;
        if (showAtBottom) {
            textY = barY + barH - 8;
            g2.setColor(flipped ? (currentScore <= 0 ? Color.WHITE : Color.BLACK) : Color.BLACK);
        } else {
            textY = barY + 16;
            g2.setColor(flipped ? (currentScore > 0 ? Color.BLACK : Color.WHITE) : Color.WHITE);
        }

        g2.drawString(scoreText, textX, textY);
        g2.dispose();
    }
}
