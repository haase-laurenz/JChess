package com.jchess;

import com.jchess.ui.ChessFrame;

import javax.swing.*;

/**
 * Einstiegspunkt für die JChess Anwendung.
 */
public class Main {
    public static void main(String[] args) {
        // Modernes System Look and Feel verwenden
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Starten der Oberfläche im Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            ChessFrame frame = new ChessFrame();
            frame.setVisible(true);
        });
    }
}
