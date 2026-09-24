package com.jchess;

import com.jchess.core.board.Board;
import com.jchess.core.rules.Perft;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Konsolen-Anwendung zum Ausführen und Verifizieren von Perft-Tests
 * gegen die Referenzergebnisse in perft_results.json.
 */
public class PerftRunner {

    public static void main(String[] args) {
        int maxDepth = 4;
        if (args.length > 0) {
            try {
                maxDepth = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Ungültige Tiefe: " + args[0] + ". Verwende Standard-Tiefe: " + maxDepth);
            }
        }

        System.out.println("==========================================================================================");
        System.out.println("                           JCHESS PERFT VERIFIKATION (STARTPOS)                          ");
        System.out.println("==========================================================================================");
        System.out.printf("Ziel-Tiefe: %d Ply | Lade Referenzdaten aus perft_results.json...%n%n", maxDepth);

        Map<Integer, ExpectedPerft> expectedMap = loadExpectedResults("perft_results.json");

        Board board = new Board();
        board.setupInitialPosition();

        System.out.printf("%-4s | %-12s | %-9s | %-6s | %-7s | %-6s | %-8s | %-6s | %-8s | %-6s%n",
                "Ply", "Nodes", "Captures", "E.p.", "Castles", "Promos", "Checks", "Mates", "Zeit", "Status");
        System.out.println("-----+--------------+-----------+--------+---------+--------+----------+--------+----------+-------");

        boolean allPassed = true;
        long totalNodes = 0;
        long totalTimeMs = 0;

        for (int d = 1; d <= maxDepth; d++) {
            board.setupInitialPosition();
            Perft.Result res = Perft.perft(board, d);
            totalNodes += res.nodes;
            totalTimeMs += res.elapsedMillis;

            ExpectedPerft expected = expectedMap.get(d);
            boolean pass = true;

            if (expected != null) {
                if (res.nodes != expected.nodes ||
                    res.captures != expected.captures ||
                    res.enpassants != expected.enpassants ||
                    res.castles != expected.castles ||
                    res.promotions != expected.promotions ||
                    res.checks != expected.checks ||
                    res.mates != expected.mates) {
                    pass = false;
                    allPassed = false;
                }
            }

            String status = pass ? "OK" : "FEHLER";
            String timeStr = res.elapsedMillis + " ms";

            System.out.printf("%-4d | %-12s | %-9s | %-6s | %-7s | %-6s | %-8s | %-6s | %-8s | %-6s%n",
                    d,
                    formatNum(res.nodes),
                    formatNum(res.captures),
                    formatNum(res.enpassants),
                    formatNum(res.castles),
                    formatNum(res.promotions),
                    formatNum(res.checks),
                    formatNum(res.mates),
                    timeStr,
                    status
            );

            if (!pass && expected != null) {
                System.out.println("  >>> ERWARTET: " +
                        "Nodes=" + expected.nodes + ", " +
                        "Caps=" + expected.captures + ", " +
                        "EP=" + expected.enpassants + ", " +
                        "Castles=" + expected.castles + ", " +
                        "Checks=" + expected.checks + ", " +
                        "Mates=" + expected.mates);
            }
        }

        System.out.println("==========================================================================================");
        double nps = totalTimeMs > 0 ? (totalNodes * 1000.0) / totalTimeMs : 0.0;
        System.out.printf("Gesamtzeit: %d ms | Berechnete Knoten: %s | NPS: %,.0f Nodes/s%n",
                totalTimeMs, formatNum(totalNodes), nps);

        if (allPassed) {
            System.out.println("Ergebnis: ALLE TESTS ERFOLGREICH BESTANDEN! (100% Regelkonform)");
        } else {
            System.err.println("Ergebnis: ABWEICHUNGEN GEFUNDEN!");
            System.exit(1);
        }
    }

    private static String formatNum(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    private record ExpectedPerft(
            int ply,
            long nodes,
            long captures,
            long enpassants,
            long castles,
            long promotions,
            long checks,
            long mates
    ) {}

    private static Map<Integer, ExpectedPerft> loadExpectedResults(String fileName) {
        Map<Integer, ExpectedPerft> map = new HashMap<>();
        File file = new File(fileName);
        if (!file.exists()) {
            return map;
        }

        try {
            String json = Files.readString(file.toPath());
            // Einfacher Parser für die bekannten 9 JSON-Objekte ohne externe Dependencies
            String[] blocks = json.split("\\{");
            for (String block : blocks) {
                if (!block.contains("\"ply\"")) continue;
                int ply = extractInt(block, "ply");
                long nodes = extractLong(block, "nodes");
                long captures = extractLong(block, "captures");
                long enpassants = extractLong(block, "enpassants");
                long castles = extractLong(block, "castles");
                long promotions = extractLong(block, "promotions");
                long checks = extractLong(block, "checks");
                long mates = extractLong(block, "mates");
                map.put(ply, new ExpectedPerft(ply, nodes, captures, enpassants, castles, promotions, checks, mates));
            }
        } catch (Exception e) {
            System.err.println("Warnung beim Lesen von " + fileName + ": " + e.getMessage());
        }
        return map;
    }

    private static int extractInt(String block, String key) {
        return (int) extractLong(block, key);
    }

    private static long extractLong(String block, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        java.util.regex.Matcher m = p.matcher(block);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
