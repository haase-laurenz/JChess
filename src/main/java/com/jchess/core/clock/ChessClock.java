package com.jchess.core.clock;

import com.jchess.core.piece.PieceColor;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

/**
 * Schachuhr zur Zeitkontrolle beider Spieler:
 * - Verwaltet verbleibende Bedenkzeit für Weiß und Schwarz in Millisekunden.
 * - Unterstützt Fischer-Inkrement (Bonuszeit pro Zug).
 * - Aktualisiert die UI über ClockListener und erkennt Zeitüberschreitung (Flag Fall).
 */
public class ChessClock {

    public interface ClockListener {
        void onTick(long whiteTimeMs, long blackTimeMs, PieceColor activeColor);
        void onTimeOut(PieceColor losingColor);
    }

    private long whiteTimeMs;
    private long blackTimeMs;
    private long initialTimeMs;
    private long incrementMs;
    private boolean enabled;

    private PieceColor activeColor = PieceColor.WHITE;
    private boolean isRunning = false;
    private long lastTickNanoTime = 0;

    private final Timer timer;
    private final List<ClockListener> listeners = new ArrayList<>();

    public ChessClock(long initialTimeMs, long incrementMs, boolean enabled) {
        this.initialTimeMs = initialTimeMs;
        this.incrementMs = incrementMs;
        this.enabled = enabled;
        this.whiteTimeMs = initialTimeMs;
        this.blackTimeMs = initialTimeMs;

        // Taktet alle 50 ms für ruckelfreie Zehntelsekunden-Anzeige
        this.timer = new Timer(50, e -> updateClock());
    }

    public synchronized void addClockListener(ClockListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeClockListener(ClockListener listener) {
        listeners.remove(listener);
    }

    public synchronized void reset(long initialTimeMs, long incrementMs, boolean enabled) {
        pause();
        this.initialTimeMs = initialTimeMs;
        this.incrementMs = incrementMs;
        this.enabled = enabled;
        this.whiteTimeMs = initialTimeMs;
        this.blackTimeMs = initialTimeMs;
        this.activeColor = PieceColor.WHITE;
        notifyTick();
    }

    public synchronized void start(PieceColor colorToMove) {
        if (!enabled || isRunning) return;

        this.activeColor = colorToMove;
        this.isRunning = true;
        this.lastTickNanoTime = System.nanoTime();
        timer.start();
    }

    public synchronized void pause() {
        if (!isRunning) return;
        updateElapsed();
        isRunning = false;
        timer.stop();
        notifyTick();
    }

    /**
     * Wird aufgerufen, wenn ein Spieler einen Zug abgeschlossen hat.
     * Addiert das Inkrement und schaltet die Uhr auf den nächsten Spieler um.
     */
    public synchronized void onMoveMade(PieceColor movedColor) {
        if (!enabled) return;

        updateElapsed();

        // Inkrement gutschreiben
        if (incrementMs > 0) {
            if (movedColor == PieceColor.WHITE) {
                whiteTimeMs += incrementMs;
            } else {
                blackTimeMs += incrementMs;
            }
        }

        // Spieler umschalten
        this.activeColor = movedColor.opposite();
        this.lastTickNanoTime = System.nanoTime();

        if (!isRunning) {
            isRunning = true;
            timer.start();
        }

        notifyTick();
    }

    private void updateClock() {
        PieceColor timedOutColor = null;

        synchronized (this) {
            if (!isRunning || !enabled) return;

            updateElapsed();

            if (whiteTimeMs <= 0) {
                whiteTimeMs = 0;
                isRunning = false;
                timer.stop();
                timedOutColor = PieceColor.WHITE;
            } else if (blackTimeMs <= 0) {
                blackTimeMs = 0;
                isRunning = false;
                timer.stop();
                timedOutColor = PieceColor.BLACK;
            }
        }

        notifyTick();

        if (timedOutColor != null) {
            notifyTimeOut(timedOutColor);
        }
    }

    private void updateElapsed() {
        if (!isRunning) return;

        long now = System.nanoTime();
        long elapsedMs = (now - lastTickNanoTime) / 1_000_000L;
        lastTickNanoTime = now;

        if (activeColor == PieceColor.WHITE) {
            whiteTimeMs = Math.max(0, whiteTimeMs - elapsedMs);
        } else {
            blackTimeMs = Math.max(0, blackTimeMs - elapsedMs);
        }
    }

    private void notifyTick() {
        long w = whiteTimeMs;
        long b = blackTimeMs;
        PieceColor c = activeColor;
        for (ClockListener listener : listeners) {
            listener.onTick(w, b, c);
        }
    }

    private void notifyTimeOut(PieceColor losingColor) {
        for (ClockListener listener : listeners) {
            listener.onTimeOut(losingColor);
        }
    }

    public synchronized long getRemainingTimeMillis(PieceColor color) {
        return color == PieceColor.WHITE ? whiteTimeMs : blackTimeMs;
    }

    public synchronized long getWhiteTimeMillis() {
        return whiteTimeMs;
    }

    public synchronized long getBlackTimeMillis() {
        return blackTimeMs;
    }

    public synchronized boolean isWhiteTimedOut() {
        return enabled && whiteTimeMs <= 0;
    }

    public synchronized boolean isBlackTimedOut() {
        return enabled && blackTimeMs <= 0;
    }

    public synchronized boolean isTimedOut(PieceColor color) {
        return color == PieceColor.WHITE ? isWhiteTimedOut() : isBlackTimedOut();
    }

    public synchronized long getIncrementMillis() {
        return incrementMs;
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized PieceColor getActiveColor() {
        return activeColor;
    }

    /**
     * Formatiert eine Millisekunden-Angabe in eine lesbare Uhrenanzeige:
     * - Bei >= 20 Sekunden: "MM:SS" (z.B. "05:00")
     * - Bei < 20 Sekunden: "MM:SS.d" mit Zehntelsekunden (z.B. "00:08.4")
     */
    public static String formatTime(long totalMs) {
        if (totalMs < 0) totalMs = 0;

        long totalSeconds = totalMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (totalMs < 20_000) {
            long tenths = (totalMs % 1000) / 100;
            return String.format("%02d:%02d.%d", minutes, seconds, tenths);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
