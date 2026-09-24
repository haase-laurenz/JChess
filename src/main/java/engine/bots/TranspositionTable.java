package engine.bots;

import com.jchess.core.board.Move;

import java.util.Arrays;

/**
 * Transposition Table (Hash-Tabelle) für Schach-Engines:
 * Speichert bereits durchsuchte Stellungen mit Suchtiefe, Bewertung,
 * Schranken-Typ (EXACT, LOWERBOUND, UPPERBOUND) und bestem Zug.
 *
 * Eigenschaften:
 * - Schnelle 64-Bit-Indexierung über Zobrist-Hash mit Bitmask (Größe $2^{20} \approx 1$ Mio. Slots).
 * - Parallele primitive Arrays für minimale Speicherbelegung (~30 MB) ohne GC-Overhead.
 * - Automatische Normalisierung von Matt-Scores relativ zur Wurzeldistanz (ply).
 * - Bevorzugtes Ersetzen bei größerer Tiefe oder identischem Zobrist-Key.
 */
public class TranspositionTable {

    public static final byte FLAG_NONE = 0;
    public static final byte FLAG_EXACT = 1;       // PV-Knoten: Echter Minimax-Wert
    public static final byte FLAG_LOWERBOUND = 2;  // Fail-High / Beta-Cutoff (Wert >= beta)
    public static final byte FLAG_UPPERBOUND = 3;  // Fail-Low / All-Knoten (Wert <= alpha)

    private static final double MATE_THRESHOLD = 25000.0;

    private final int size;
    private final int mask;

    private final long[] keys;
    private final double[] scores;
    private final int[] depths;
    private final byte[] flags;
    private final Move[] bestMoves;

    // Metriken
    private long hits = 0;
    private long misses = 0;

    private final TTEntry probeResult = new TTEntry(0L, 0.0, 0, (byte)0, null);

    public static class TTEntry {
        public long key;
        public double score;
        public int depth;
        public byte flag;
        public Move bestMove;

        public TTEntry(long key, double score, int depth, byte flag, Move bestMove) {
            this.key = key;
            this.score = score;
            this.depth = depth;
            this.flag = flag;
            this.bestMove = bestMove;
        }
    }

    /**
     * Erstellt eine Transposition Table mit $2^{20} = 1.048.576$ Einträgen (~30 MB RAM).
     */
    public TranspositionTable() {
        this(1 << 20);
    }

    public TranspositionTable(int size) {
        // Größe auf Zweierpotenz aufrunden
        int pow2 = 1;
        while (pow2 < size) {
            pow2 <<= 1;
        }
        this.size = pow2;
        this.mask = pow2 - 1;

        this.keys = new long[pow2];
        this.scores = new double[pow2];
        this.depths = new int[pow2];
        this.flags = new byte[pow2];
        this.bestMoves = new Move[pow2];
    }

    /**
     * Sucht einen Eintrag für den gegebenen 64-Bit Zobrist-Key.
     * Normalisiert Matt-Scores relativ zum aktuellen Such-Ply.
     *
     * @param key 64-Bit Zobrist-Hash der aktuellen Stellung.
     * @param ply Distanz von der Suchwurzel.
     * @return TTEntry oder null, wenn kein Eintrag oder Hash-Miss.
     */
    public TTEntry probe(long key, int ply) {
        int idx = (int) (key & mask);
        if (flags[idx] != FLAG_NONE && keys[idx] == key) {
            hits++;
            double score = scores[idx];
            if (score >= MATE_THRESHOLD) {
                score -= ply;
            } else if (score <= -MATE_THRESHOLD) {
                score += ply;
            }
            probeResult.key = key;
            probeResult.score = score;
            probeResult.depth = depths[idx];
            probeResult.flag = flags[idx];
            probeResult.bestMove = bestMoves[idx];
            return probeResult;
        }
        misses++;
        return null;
    }

    /**
     * Speichert oder aktualisiert eine durchsuchte Stellung in der Tabelle.
     *
     * @param key 64-Bit Zobrist-Hash der Stellung.
     * @param depth Erreichte Suchtiefe.
     * @param flag EXACT, LOWERBOUND oder UPPERBOUND.
     * @param score Ermittelte Bewertung.
     * @param bestMove Bester Zug dieser Stellung (kann null sein).
     * @param ply Distanz von der Wurzel für Matt-Normalisierung.
     */
    public void store(long key, int depth, byte flag, double score, Move bestMove, int ply) {
        int idx = (int) (key & mask);

        // Ersetzungsstrategie: Neuer Eintrag, identischer Schlüssel oder größere/gleiche Tiefe
        if (flags[idx] == FLAG_NONE || keys[idx] == key || depth >= depths[idx]) {
            keys[idx] = key;
            depths[idx] = depth;
            flags[idx] = flag;
            if (bestMove != null || keys[idx] != key) {
                bestMoves[idx] = bestMove;
            }

            if (score >= MATE_THRESHOLD) {
                scores[idx] = score + ply;
            } else if (score <= -MATE_THRESHOLD) {
                scores[idx] = score - ply;
            } else {
                scores[idx] = score;
            }
        }
    }

    /**
     * Setzt alle Einträge und Metriken der Transposition Table zurück.
     */
    public void clear() {
        Arrays.fill(keys, 0L);
        Arrays.fill(scores, 0.0);
        Arrays.fill(depths, 0);
        Arrays.fill(flags, FLAG_NONE);
        Arrays.fill(bestMoves, null);
        hits = 0;
        misses = 0;
    }

    public long getHits() {
        return hits;
    }

    public long getMisses() {
        return misses;
    }

    public void resetMetrics() {
        hits = 0;
        misses = 0;
    }

    public int getSize() {
        return size;
    }
}
