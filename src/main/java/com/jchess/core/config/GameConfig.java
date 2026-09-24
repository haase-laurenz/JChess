package com.jchess.core.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Verwaltet die Spieleinstellungen aus 'config.properties'.
 * Erstellt die Datei automatisch mit Standardwerten, falls sie noch nicht existiert.
 */
public class GameConfig {

    public static final String DEFAULT_CONFIG_FILE = "config.properties";

    private static final String KEY_TIME_MINUTES = "time_control_minutes";
    private static final String KEY_INCREMENT_SECONDS = "increment_seconds";
    private static final String KEY_TIME_ENABLED = "time_control_enabled";
    private static final String KEY_BOT_PERCENT = "bot_remaining_time_percent";

    private static final int DEFAULT_TIME_MINUTES = 1;
    private static final int DEFAULT_INCREMENT_SECONDS = 1;
    private static final boolean DEFAULT_TIME_ENABLED = true;
    private static final double DEFAULT_BOT_PERCENT = 5.0;

    private static GameConfig instance;

    private int timeMinutes;
    private int incrementSeconds;
    private boolean timeControlEnabled;
    private double botRemainingTimePercent;
    private final Path configPath;

    public GameConfig() {
        this(Paths.get(DEFAULT_CONFIG_FILE));
    }

    public GameConfig(Path configPath) {
        this.configPath = configPath;
        load();
    }

    public static synchronized GameConfig getInstance() {
        if (instance == null) {
            instance = new GameConfig();
        }
        return instance;
    }

    /**
     * Lädt die Konfigurationsdatei neu ein.
     */
    public synchronized void load() {
        Properties props = new Properties();

        if (Files.exists(configPath)) {
            try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                System.err.println("Warnung: Fehler beim Lesen von " + configPath + ", nutze Standardwerte: " + e.getMessage());
            }
        } else {
            // Datei existiert noch nicht -> Mit Standardwerten anlegen
            saveDefaults();
        }

        this.timeMinutes = parsePositiveInt(props.getProperty(KEY_TIME_MINUTES), DEFAULT_TIME_MINUTES, 1, 180);
        this.incrementSeconds = parsePositiveInt(props.getProperty(KEY_INCREMENT_SECONDS), DEFAULT_INCREMENT_SECONDS, 0, 60);
        this.timeControlEnabled = parseBoolean(props.getProperty(KEY_TIME_ENABLED), DEFAULT_TIME_ENABLED);
        this.botRemainingTimePercent = parseDouble(props.getProperty(KEY_BOT_PERCENT), DEFAULT_BOT_PERCENT, 0.1, 50.0);
    }

    private void saveDefaults() {
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            String content = "# ==============================================================================\n" +
                    "# JChess Konfigurationsdatei\n" +
                    "# ==============================================================================\n" +
                    "# Diese Datei steuert die Spieleinstellungen wie z.B. die Zeitkontrolle.\n" +
                    "# Aenderungen werden beim naechsten Start oder beim Neustart einer Partie wirksam.\n\n" +
                    "# Zeit pro Spieler in Minuten (z.B. 1 für Bullet, 3 oder 5 für Blitz, 10 für Rapid)\n" +
                    KEY_TIME_MINUTES + "=" + DEFAULT_TIME_MINUTES + "\n\n" +
                    "# Bonuszeit (Inkrement) pro ausgefuehrten Zug in Sekunden (Fischer-Inkrement, z.B. 0 oder 2)\n" +
                    KEY_INCREMENT_SECONDS + "=" + DEFAULT_INCREMENT_SECONDS + "\n\n" +
                    "# Zeitkontrolle aktivieren (true / false)\n" +
                    "# Wenn 'false', laeuft keine Schachuhr und die Partie hat unbegrenzte Bedenkzeit.\n" +
                    KEY_TIME_ENABLED + "=" + DEFAULT_TIME_ENABLED + "\n\n" +
                    "# Prozentsatz der verbleibenden Restzeit, den der Bot pro Zug nutzt (Standard: 5.0 = 5%)\n" +
                    KEY_BOT_PERCENT + "=" + DEFAULT_BOT_PERCENT + "\n";
            Files.writeString(configPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Warnung: Konnte Standard-Konfiguration nicht schreiben: " + e.getMessage());
        }
    }

    public synchronized void setTimeMinutes(int timeMinutes) {
        this.timeMinutes = Math.max(1, Math.min(180, timeMinutes));
    }

    public synchronized void setIncrementSeconds(int incrementSeconds) {
        this.incrementSeconds = Math.max(0, Math.min(60, incrementSeconds));
    }

    public synchronized void setTimeControlEnabled(boolean enabled) {
        this.timeControlEnabled = enabled;
    }

    public synchronized void setBotRemainingTimePercent(double percent) {
        this.botRemainingTimePercent = Math.max(0.1, Math.min(50.0, percent));
    }

    /**
     * Schreibt die aktuellen Konfigurationseinstellungen zurück in die Datei.
     */
    public synchronized void save() {
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            String content = "# ==============================================================================\n" +
                    "# JChess Konfigurationsdatei\n" +
                    "# ==============================================================================\n" +
                    "# Diese Datei steuert die Spieleinstellungen wie z.B. die Zeitkontrolle.\n\n" +
                    KEY_TIME_MINUTES + "=" + timeMinutes + "\n" +
                    KEY_INCREMENT_SECONDS + "=" + incrementSeconds + "\n" +
                    KEY_TIME_ENABLED + "=" + timeControlEnabled + "\n" +
                    KEY_BOT_PERCENT + "=" + botRemainingTimePercent + "\n";
            Files.writeString(configPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Warnung: Fehler beim Speichern von " + configPath + ": " + e.getMessage());
        }
    }

    private double parseDouble(String value, double fallback, double min, double max) {
        if (value == null || value.isBlank()) return fallback;
        try {
            double parsed = Double.parseDouble(value.trim().replace(',', '.'));
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int parsePositiveInt(String value, int fallback, int min, int max) {
        if (value == null || value.isBlank()) return fallback;
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean parseBoolean(String value, boolean fallback) {
        if (value == null || value.isBlank()) return fallback;
        return Boolean.parseBoolean(value.trim());
    }

    public int getTimeMinutes() {
        return timeMinutes;
    }

    public int getIncrementSeconds() {
        return incrementSeconds;
    }

    public boolean isTimeControlEnabled() {
        return timeControlEnabled;
    }

    public long getInitialTimeMillis() {
        return (long) timeMinutes * 60 * 1000;
    }

    public long getIncrementMillis() {
        return (long) incrementSeconds * 1000;
    }

    public double getBotRemainingTimePercent() {
        return botRemainingTimePercent;
    }

    public double getBotRemainingTimeFactor() {
        return botRemainingTimePercent / 100.0;
    }

    public Path getConfigPath() {
        return configPath;
    }
}
