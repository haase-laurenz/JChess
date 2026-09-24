package com.jchess.ui;

import com.jchess.core.clock.ChessClock;
import com.jchess.core.piece.PieceColor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Optisch ansprechende digitale Schachuhr für einen Spieler (Weiß oder Schwarz):
 * - Zeigt Spielername / Figuren-Symbol und die verbleibende Zeit.
 * - Hebt den aktiven Spieler mit Akzentfarbe hervor.
 * - Wechselt bei Zeitnot (< 30s) auf Signalfarbe Rot.
 * - Zeigt Zehntelsekunden bei unter 20s.
 */
public class PlayerClockPanel extends JPanel {

    private final PieceColor playerColor;
    private final JLabel nameLabel;
    private final JLabel timeLabel;

    private boolean isActive = false;
    private boolean isLowTime = false;
    private boolean isTimedOut = false;

    public PlayerClockPanel(PieceColor color, String defaultName) {
        this(color, defaultName, com.jchess.core.config.GameConfig.getInstance().getInitialTimeMillis());
    }

    public PlayerClockPanel(PieceColor color, String defaultName, long initialTimeMs) {
        this.playerColor = color;

        setLayout(new BorderLayout(8, 0));
        setPreferredSize(new Dimension(220, 38));
        setBorder(new EmptyBorder(4, 12, 4, 12));
        setOpaque(false);

        String symbol = (color == PieceColor.WHITE) ? "♔ Weiß" : "♚ Schwarz";
        nameLabel = new JLabel(defaultName != null ? defaultName : symbol);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        nameLabel.setForeground(new Color(220, 220, 220));

        timeLabel = new JLabel(ChessClock.formatTime(initialTimeMs));
        timeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
        timeLabel.setForeground(new Color(245, 245, 245));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        add(nameLabel, BorderLayout.WEST);
        add(timeLabel, BorderLayout.EAST);
    }

    public void setInitialTime(long initialTimeMs) {
        this.isActive = false;
        this.isLowTime = false;
        this.isTimedOut = false;
        timeLabel.setText(ChessClock.formatTime(initialTimeMs));
        timeLabel.setForeground(new Color(245, 245, 245));
        repaint();
    }

    public void updateDisplay(long remainingMs, boolean active) {
        this.isActive = active;
        this.isLowTime = remainingMs < 30_000 && remainingMs > 0;
        this.isTimedOut = remainingMs <= 0;

        timeLabel.setText(ChessClock.formatTime(remainingMs));

        if (isTimedOut) {
            timeLabel.setText("00:00.0 (Zeit!)");
            timeLabel.setForeground(new Color(255, 75, 75));
            nameLabel.setForeground(new Color(255, 120, 120));
        } else if (isLowTime) {
            timeLabel.setForeground(new Color(255, 95, 95));
            nameLabel.setForeground(new Color(240, 210, 210));
        } else if (isActive) {
            timeLabel.setForeground(new Color(255, 255, 255));
            nameLabel.setForeground(new Color(245, 245, 245));
        } else {
            timeLabel.setForeground(new Color(175, 175, 175));
            nameLabel.setForeground(new Color(170, 170, 170));
        }

        repaint();
    }

    public void setPlayerLabel(String text) {
        nameLabel.setText(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Hintergrund je nach Zustand
        Color bgColor;
        Color borderColor;

        if (isTimedOut) {
            bgColor = new Color(70, 20, 20, 220);
            borderColor = new Color(220, 50, 50);
        } else if (isLowTime && isActive) {
            bgColor = new Color(60, 24, 24, 210);
            borderColor = new Color(230, 80, 80);
        } else if (isActive) {
            bgColor = new Color(62, 54, 46, 230);
            borderColor = new Color(170, 145, 105);
        } else {
            bgColor = new Color(34, 32, 29, 180);
            borderColor = new Color(55, 52, 48);
        }

        // Abgerundetes Rechteck
        g2.setColor(bgColor);
        g2.fillRoundRect(2, 2, w - 4, h - 4, 10, 10);

        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(isActive ? 1.8f : 1.0f));
        g2.drawRoundRect(2, 2, w - 4, h - 4, 10, 10);

        g2.dispose();
        super.paintComponent(g);
    }
}
