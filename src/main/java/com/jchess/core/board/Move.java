package com.jchess.core.board;

import com.jchess.core.piece.Piece;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;

import java.util.Objects;

/**
 * Repräsentiert einen Schachzug mit allen relevanten Metadaten
 * für Regelausführung, Notation und Undo-Operationen in einer Engine.
 * <p>
 * Unterstützt zusätzlich eine kompakte 25-Bit Integer-Kodierung ({@link #encode()}, {@link #decode(int)}),
 * die alle Zuginformationen in einem einzelnen {@code int} speichert. Dies ermöglicht:
 * <ul>
 *   <li>Blitzschnelle Gleichheitsprüfung über Integer-Vergleich</li>
 *   <li>Cache-freundliche Zuglisten als {@code int[]}</li>
 *   <li>Minimale Speicher- und GC-Belastung in Engine-Suchen</li>
 * </ul>
 */
public final class Move {
    private final Position from;
    private final Position to;
    private final Piece movedPiece;
    private final Piece capturedPiece;
    private final PieceType promotionType;
    private final boolean isCastle;
    private final boolean isEnPassant;
    private final boolean isDoublePawnPush;

    /** Gecachte Integer-Kodierung, wird einmalig im Konstruktor berechnet. */
    private final int encoded;

    // Gespeicherte Zustände vor Ausführung des Zuges (für schnelles Undo in der Engine)
    private int prevCastlingRights = -1;
    private Position prevEnPassantTarget = null;
    private int prevHalfmoveClock = 0;
    private long prevZobristKey = 0L;

    public Move(Position from, Position to, Piece movedPiece, Piece capturedPiece,
                PieceType promotionType, boolean isCastle, boolean isEnPassant, boolean isDoublePawnPush) {
        this.from = Objects.requireNonNull(from, "from darf nicht null sein");
        this.to = Objects.requireNonNull(to, "to darf nicht null sein");
        this.movedPiece = Objects.requireNonNull(movedPiece, "movedPiece darf nicht null sein");
        this.capturedPiece = capturedPiece;
        this.promotionType = promotionType;
        this.isCastle = isCastle;
        this.isEnPassant = isEnPassant;
        this.isDoublePawnPush = isDoublePawnPush;
        this.encoded = computeEncoding();
    }

    /**
     * Erstellt einen normalen Zug ohne Spezialeffekte.
     */
    public static Move normal(Position from, Position to, Piece movedPiece, Piece capturedPiece) {
        return new Move(from, to, movedPiece, capturedPiece, null, false, false, false);
    }

    /**
     * Erstellt einen Doppelschritt eines Bauern.
     */
    public static Move doublePawnPush(Position from, Position to, Piece movedPiece) {
        return new Move(from, to, movedPiece, null, null, false, false, true);
    }

    /**
     * Erstellt einen En-Passant-Schlagzug.
     */
    public static Move enPassant(Position from, Position to, Piece movedPiece, Piece capturedPawn) {
        return new Move(from, to, movedPiece, capturedPawn, null, false, true, false);
    }

    /**
     * Erstellt einen Rochade-Zug.
     */
    public static Move castle(Position from, Position to, Piece king) {
        return new Move(from, to, king, null, null, true, false, false);
    }

    /**
     * Erstellt einen Bauernumwandlungs-Zug.
     */
    public static Move promotion(Position from, Position to, Piece pawn, Piece capturedPiece, PieceType promoType) {
        return new Move(from, to, pawn, capturedPiece, promoType, false, false, false);
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public Piece getMovedPiece() {
        return movedPiece;
    }

    public Piece getCapturedPiece() {
        return capturedPiece;
    }

    public PieceType getPromotionType() {
        return promotionType;
    }

    public boolean isCapture() {
        return capturedPiece != null;
    }

    public boolean isCastle() {
        return isCastle;
    }

    public boolean isEnPassant() {
        return isEnPassant;
    }

    public boolean isDoublePawnPush() {
        return isDoublePawnPush;
    }

    public boolean isPromotion() {
        return promotionType != null;
    }

    // Speicherung für Undo-Funktionalität
    public void recordPreviousState(int castlingRights, Position enPassantTarget, int halfmoveClock) {
        this.prevCastlingRights = castlingRights;
        this.prevEnPassantTarget = enPassantTarget;
        this.prevHalfmoveClock = halfmoveClock;
    }

    public void recordPreviousState(int castlingRights, Position enPassantTarget, int halfmoveClock, long zobristKey) {
        this.prevCastlingRights = castlingRights;
        this.prevEnPassantTarget = enPassantTarget;
        this.prevHalfmoveClock = halfmoveClock;
        this.prevZobristKey = zobristKey;
    }

    public int getPrevCastlingRights() {
        return prevCastlingRights;
    }

    public Position getPrevEnPassantTarget() {
        return prevEnPassantTarget;
    }

    public int getPrevHalfmoveClock() {
        return prevHalfmoveClock;
    }

    public long getPrevZobristKey() {
        return prevZobristKey;
    }

    /**
     * Einfache algebraische Textform (z.B. "e2e4", "e7e8q", "e1g1").
     */
    public String toUci() {
        String uci = from.toAlgebraic() + to.toAlgebraic();
        if (promotionType != null) {
            uci += Character.toLowerCase(promotionType.getSymbol());
        }
        return uci;
    }

    /**
     * Lesbare Standard-Notation (z.B. "Nf3", "exd5", "O-O", "e8=Q").
     */
    public String toSimpleNotation() {
        if (isCastle) {
            return to.getFile() > from.getFile() ? "O-O" : "O-O-O";
        }
        StringBuilder sb = new StringBuilder();
        if (movedPiece.getType() != PieceType.PAWN) {
            sb.append(movedPiece.getType().getSymbol());
        } else if (isCapture()) {
            sb.append((char) ('a' + from.getFile()));
        }

        if (isCapture()) {
            sb.append("x");
        }

        sb.append(to.toAlgebraic());

        if (promotionType != null) {
            sb.append("=").append(promotionType.getSymbol());
        }
        return sb.toString();
    }

    // ==================================================================================
    //  Kompakte 25-Bit Integer-Kodierung für maximale Engine-Performance
    // ==================================================================================
    //
    //  Bit-Layout (int, 32 Bit, davon 25 genutzt):
    //  ┌─────────┬─────────┬───────────┬───────┬──────────┬───────────┬────────┬─────┬────────┐
    //  │ 24      │ 23      │ 22        │ 21-19 │ 18-16    │ 15        │ 14-12  │ 11-6│ 5-0    │
    //  │ dblPawn │ enPass  │ castle    │ promo │ captured │ color     │ moved  │ to  │ from   │
    //  │ flag    │ flag    │ flag      │ type  │ type     │ (0W/1B)   │ type   │ sq  │ sq     │
    //  └─────────┴─────────┴───────────┴───────┴──────────┴───────────┴────────┴─────┴────────┘
    //
    //  - captured type 7 = kein Schlag,  promotion type 7 = keine Umwandlung
    //  - PieceType ordinals: PAWN=0, KNIGHT=1, BISHOP=2, ROOK=3, QUEEN=4, KING=5
    //

    private static final int MASK_6  = 0x3F;   // 6 Bits
    private static final int MASK_3  = 0x07;   // 3 Bits
    private static final int MASK_1  = 0x01;   // 1 Bit
    private static final int NO_PIECE = 7;     // Sentinel für "kein Schlag" / "keine Umwandlung"

    private static final int SHIFT_TO       = 6;
    private static final int SHIFT_MOVED    = 12;
    private static final int SHIFT_COLOR    = 15;
    private static final int SHIFT_CAPTURED = 16;
    private static final int SHIFT_PROMO    = 19;
    private static final int SHIFT_CASTLE   = 22;
    private static final int SHIFT_EP       = 23;
    private static final int SHIFT_DOUBLE   = 24;

    private static final PieceType[] PIECE_TYPES = PieceType.values();
    private static final PieceColor[] PIECE_COLORS = PieceColor.values();

    /**
     * Berechnet die Integer-Kodierung (intern, einmalig im Konstruktor aufgerufen).
     */
    private int computeEncoding() {
        int enc = from.getIndex();
        enc |= to.getIndex() << SHIFT_TO;
        enc |= movedPiece.getType().ordinal() << SHIFT_MOVED;
        enc |= movedPiece.getColor().ordinal() << SHIFT_COLOR;
        enc |= (capturedPiece != null ? capturedPiece.getType().ordinal() : NO_PIECE) << SHIFT_CAPTURED;
        enc |= (promotionType != null ? promotionType.ordinal() : NO_PIECE) << SHIFT_PROMO;
        if (isCastle)         enc |= 1 << SHIFT_CASTLE;
        if (isEnPassant)      enc |= 1 << SHIFT_EP;
        if (isDoublePawnPush) enc |= 1 << SHIFT_DOUBLE;
        return enc;
    }

    /**
     * Liefert die kompakte 25-Bit Integer-Kodierung dieses Zuges.
     * Wird im Konstruktor einmalig berechnet und gecacht — Aufruf ist O(1).
     * <p>
     * Undo-Zustände (prevCastlingRights etc.) sind NICHT Teil der Kodierung,
     * da sie erst bei makeMove gesetzt werden und keine Zugeigenschaft sind.
     */
    public int encode() {
        return encoded;
    }

    /**
     * Dekodiert einen kompakten 25-Bit int zurück in ein vollständiges Move-Objekt.
     * Die geschlagene Figur wird aus Typ + gegnerischer Farbe rekonstruiert.
     */
    public static Move decode(int encoded) {
        int fromSq       = encoded & MASK_6;
        int toSq         = (encoded >>> SHIFT_TO) & MASK_6;
        int movedTypeOrd = (encoded >>> SHIFT_MOVED) & MASK_3;
        int colorOrd     = (encoded >>> SHIFT_COLOR) & MASK_1;
        int capTypeOrd   = (encoded >>> SHIFT_CAPTURED) & MASK_3;
        int promoTypeOrd = (encoded >>> SHIFT_PROMO) & MASK_3;
        boolean castle   = ((encoded >>> SHIFT_CASTLE) & MASK_1) != 0;
        boolean ep       = ((encoded >>> SHIFT_EP) & MASK_1) != 0;
        boolean dblPawn  = ((encoded >>> SHIFT_DOUBLE) & MASK_1) != 0;

        Position from = Position.fromIndex(fromSq);
        Position to   = Position.fromIndex(toSq);

        PieceColor movedColor = PIECE_COLORS[colorOrd];
        Piece movedPiece = Piece.of(PIECE_TYPES[movedTypeOrd], movedColor);

        Piece capturedPiece = null;
        if (capTypeOrd != NO_PIECE) {
            // Geschlagene Figur hat immer die gegnerische Farbe
            capturedPiece = Piece.of(PIECE_TYPES[capTypeOrd], movedColor.opposite());
        }

        PieceType promotionType = null;
        if (promoTypeOrd != NO_PIECE) {
            promotionType = PIECE_TYPES[promoTypeOrd];
        }

        return new Move(from, to, movedPiece, capturedPiece, promotionType, castle, ep, dblPawn);
    }

    /**
     * Alias für {@link #decode(int)} — erstellt ein Move-Objekt aus einem kodierten int.
     */
    public static Move fromEncoded(int encoded) {
        return decode(encoded);
    }

    // ==================================================================================
    //  Statische Hilfsmethoden zum direkten Auslesen aus einem kodierten int,
    //  ohne ein Move-Objekt zu erzeugen (für zukünftige int[]-basierte Zuglisten).
    // ==================================================================================

    /** Startfeld-Index (0..63) aus kodiertem Zug. */
    public static int encodedFrom(int encoded)         { return encoded & MASK_6; }
    /** Zielfeld-Index (0..63) aus kodiertem Zug. */
    public static int encodedTo(int encoded)           { return (encoded >>> SHIFT_TO) & MASK_6; }
    /** PieceType-Ordinal der gezogenen Figur aus kodiertem Zug. */
    public static int encodedMovedType(int encoded)    { return (encoded >>> SHIFT_MOVED) & MASK_3; }
    /** Farb-Ordinal der gezogenen Figur (0=WHITE, 1=BLACK). */
    public static int encodedColor(int encoded)        { return (encoded >>> SHIFT_COLOR) & MASK_1; }
    /** PieceType-Ordinal der geschlagenen Figur (7 = kein Schlag). */
    public static int encodedCapturedType(int encoded) { return (encoded >>> SHIFT_CAPTURED) & MASK_3; }
    /** PieceType-Ordinal der Umwandlungsfigur (7 = keine Umwandlung). */
    public static int encodedPromoType(int encoded)    { return (encoded >>> SHIFT_PROMO) & MASK_3; }
    /** true wenn Rochade. */
    public static boolean encodedIsCastle(int encoded)      { return ((encoded >>> SHIFT_CASTLE) & MASK_1) != 0; }
    /** true wenn En Passant. */
    public static boolean encodedIsEnPassant(int encoded)   { return ((encoded >>> SHIFT_EP) & MASK_1) != 0; }
    /** true wenn Bauern-Doppelschritt. */
    public static boolean encodedIsDoublePawn(int encoded)  { return ((encoded >>> SHIFT_DOUBLE) & MASK_1) != 0; }
    /** true wenn Schlagzug (captured type != 7). */
    public static boolean encodedIsCapture(int encoded)     { return ((encoded >>> SHIFT_CAPTURED) & MASK_3) != NO_PIECE; }
    /** true wenn Bauernumwandlung (promo type != 7). */
    public static boolean encodedIsPromotion(int encoded)   { return ((encoded >>> SHIFT_PROMO) & MASK_3) != NO_PIECE; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move move)) return false;
        // Kompakter Vergleich über die gecachte Integer-Kodierung:
        // Ein einziger int-Vergleich statt Multi-Feld-Objektvergleich.
        return this.encoded == move.encoded;
    }

    @Override
    public int hashCode() {
        return encoded;
    }

    @Override
    public String toString() {
        return toSimpleNotation();
    }
}
