package com.jchess.core.board;

import com.jchess.core.engine.Zobrist;
import com.jchess.core.piece.Piece;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 8x8 Schachbrett mit vollständiger Zustandsverwaltung:
 * Figurenpositionen, Zugrecht, Rochaderechte, En Passant,
 * Halbzugzähler (50-Züge-Regel) und Zugausführung mit schnellem Undo.
 */
public final class Board {
    // Bitmasken für Rochaderechte
    public static final int CASTLE_WHITE_KINGSIDE  = 1;
    public static final int CASTLE_WHITE_QUEENSIDE = 2;
    public static final int CASTLE_BLACK_KINGSIDE  = 4;
    public static final int CASTLE_BLACK_QUEENSIDE = 8;
    public static final int CASTLE_ALL = 15;

    private final Piece[] squares = new Piece[64];
    private PieceColor activePlayer = PieceColor.WHITE;
    private int castlingRights = CASTLE_ALL;
    private Position enPassantTarget = null;
    private int halfmoveClock = 0;
    private int fullmoveNumber = 1;

    private Position whiteKingPos = Position.of(4, 0); // e1
    private Position blackKingPos = Position.of(4, 7); // e8
    private int whiteKingSquare = 4;
    private int blackKingSquare = 60;

    // Bitboard-Zustände: 6 Typen x 2 Farben (White Pawn..King (0..5), Black Pawn..King (6..11))
    private final long[] pieceBitboards = new long[12];
    private final long[] colorBitboards = new long[2]; // 0: WHITE, 1: BLACK
    private long occupiedBitboard = 0L;

    // 64-Bit Zobrist-Schlüssel und Stellungshistorie für 3-fache Wiederholung
    private long zobristKey = 0L;
    // Primitive Arrays statt ArrayList, um Autoboxing (Long) und ArrayList-Overhead zu vermeiden
    private long[] positionHistory = new long[512];
    private int positionHistorySize = 0;

    private Move[] moveHistory = new Move[512];
    private int moveHistorySize = 0;

    // Historie für Null-Moves
    private int[] nullCastlingHistory = new int[256];
    private Position[] nullEnPassantHistory = new Position[256];
    private int[] nullHalfmoveHistory = new int[256];
    private int nullHistorySize = 0;

    public Board() {
        setupInitialPosition();
    }

    /**
     * Erstellt ein neues Schachbrett in der Standard-Ausgangsstellung.
     */
    public static Board initial() {
        return new Board();
    }

    /**
     * Erstellt eine leere Instanz (für FEN oder Tests).
     */
    public static Board empty() {
        Board b = new Board();
        b.clear();
        return b;
    }

    /**
     * Löscht alle Figuren und setzt den Zustand zurück.
     */
    public void clear() {
        Arrays.fill(squares, null);
        Arrays.fill(pieceBitboards, 0L);
        colorBitboards[0] = 0L;
        colorBitboards[1] = 0L;
        occupiedBitboard = 0L;
        whiteKingSquare = -1;
        blackKingSquare = -1;

        activePlayer = PieceColor.WHITE;
        castlingRights = 0;
        enPassantTarget = null;
        halfmoveClock = 0;
        fullmoveNumber = 1;
        whiteKingPos = null;
        blackKingPos = null;
        moveHistorySize = 0;
        zobristKey = 0L;
        positionHistorySize = 0;
    }

    /**
     * Richtet die Standard-Schach-Startaufstellung ein.
     */
    public void setupInitialPosition() {
        clear();
        castlingRights = CASTLE_ALL;
        activePlayer = PieceColor.WHITE;
        fullmoveNumber = 1;

        // Weiße Offiziere (Rang 1 / Index 0)
        setPieceAt(Position.of(0, 0), Piece.of(PieceType.ROOK, PieceColor.WHITE));
        setPieceAt(Position.of(1, 0), Piece.of(PieceType.KNIGHT, PieceColor.WHITE));
        setPieceAt(Position.of(2, 0), Piece.of(PieceType.BISHOP, PieceColor.WHITE));
        setPieceAt(Position.of(3, 0), Piece.of(PieceType.QUEEN, PieceColor.WHITE));
        setPieceAt(Position.of(4, 0), Piece.of(PieceType.KING, PieceColor.WHITE));
        setPieceAt(Position.of(5, 0), Piece.of(PieceType.BISHOP, PieceColor.WHITE));
        setPieceAt(Position.of(6, 0), Piece.of(PieceType.KNIGHT, PieceColor.WHITE));
        setPieceAt(Position.of(7, 0), Piece.of(PieceType.ROOK, PieceColor.WHITE));

        // Weiße Bauern (Rang 2 / Index 1)
        for (int f = 0; f < 8; f++) {
            setPieceAt(Position.of(f, 1), Piece.of(PieceType.PAWN, PieceColor.WHITE));
        }

        // Schwarze Bauern (Rang 7 / Index 6)
        for (int f = 0; f < 8; f++) {
            setPieceAt(Position.of(f, 6), Piece.of(PieceType.PAWN, PieceColor.BLACK));
        }

        // Schwarze Offiziere (Rang 8 / Index 7)
        setPieceAt(Position.of(0, 7), Piece.of(PieceType.ROOK, PieceColor.BLACK));
        setPieceAt(Position.of(1, 7), Piece.of(PieceType.KNIGHT, PieceColor.BLACK));
        setPieceAt(Position.of(2, 7), Piece.of(PieceType.BISHOP, PieceColor.BLACK));
        setPieceAt(Position.of(3, 7), Piece.of(PieceType.QUEEN, PieceColor.BLACK));
        setPieceAt(Position.of(4, 7), Piece.of(PieceType.KING, PieceColor.BLACK));
        setPieceAt(Position.of(5, 7), Piece.of(PieceType.BISHOP, PieceColor.BLACK));
        setPieceAt(Position.of(6, 7), Piece.of(PieceType.KNIGHT, PieceColor.BLACK));
        setPieceAt(Position.of(7, 7), Piece.of(PieceType.ROOK, PieceColor.BLACK));

        whiteKingPos = Position.of(4, 0);
        blackKingPos = Position.of(4, 7);
        whiteKingSquare = 4;
        blackKingSquare = 60;

        this.zobristKey = Zobrist.computeHash(this);
        this.positionHistorySize = 0;
        this.positionHistory[this.positionHistorySize++] = this.zobristKey;
    }

    public Piece getPieceAt(Position pos) {
        if (pos == null) return null;
        return squares[pos.getIndex()];
    }

    /**
     * Direkter Zugriff über Square-Index (0..63) ohne Position-Objekt.
     * Für Performance-kritische Pfade (MoveGenerator, Perft).
     */
    public Piece getPieceAtIndex(int sq) {
        return squares[sq];
    }

    public Piece getPieceAt(int file, int rank) {
        if (!Position.isValid(file, rank)) return null;
        return squares[rank * 8 + file];
    }

    public void setPieceAt(Position pos, Piece piece) {
        if (pos == null) return;
        int sq = pos.getIndex();
        Piece oldPiece = squares[sq];
        if (oldPiece != null) {
            int oldIdx = oldPiece.getColor().ordinal() * 6 + oldPiece.getType().ordinal();
            long mask = ~(1L << sq);
            pieceBitboards[oldIdx] &= mask;
            colorBitboards[oldPiece.getColor().ordinal()] &= mask;
        }

        squares[sq] = piece;

        if (piece != null) {
            int newIdx = piece.getColor().ordinal() * 6 + piece.getType().ordinal();
            long mask = 1L << sq;
            pieceBitboards[newIdx] |= mask;
            colorBitboards[piece.getColor().ordinal()] |= mask;
            if (piece.getType() == PieceType.KING) {
                if (piece.getColor() == PieceColor.WHITE) {
                    whiteKingPos = pos;
                    whiteKingSquare = sq;
                } else {
                    blackKingPos = pos;
                    blackKingSquare = sq;
                }
            }
        } else {
            if (oldPiece != null && oldPiece.getType() == PieceType.KING) {
                if (oldPiece.getColor() == PieceColor.WHITE) {
                    whiteKingPos = null;
                    whiteKingSquare = -1;
                } else {
                    blackKingPos = null;
                    blackKingSquare = -1;
                }
            }
        }
        occupiedBitboard = colorBitboards[0] | colorBitboards[1];
    }

    public PieceColor getActivePlayer() {
        return activePlayer;
    }

    public void setActivePlayer(PieceColor color) {
        this.activePlayer = color;
    }

    public int getCastlingRights() {
        return castlingRights;
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    public Position getKingPosition(PieceColor color) {
        return color == PieceColor.WHITE ? whiteKingPos : blackKingPos;
    }

    public List<Move> getMoveHistory() {
        return Collections.unmodifiableList(Arrays.asList(Arrays.copyOf(moveHistory, moveHistorySize)));
    }

    public Move getLastMove() {
        if (moveHistorySize == 0) return null;
        return moveHistory[moveHistorySize - 1];
    }

    /**
     * Führt einen Zug auf dem Brett aus und aktualisiert alle Zustände.
     * Speichert frühere Zustände im Move für ein blitzschnelles undoMove().
     */
    public void makeMove(Move move) {
        int oldCastlingRights = this.castlingRights;
        Position oldEnPassantTarget = this.enPassantTarget;

        // Vorherige Zustände zur Wiederherstellung sichern (inkl. Zobrist-Key)
        move.recordPreviousState(castlingRights, enPassantTarget, halfmoveClock, zobristKey);

        Position from = move.getFrom();
        Position to = move.getTo();
        Piece piece = move.getMovedPiece();
        PieceColor color = piece.getColor();
        int colorIdx = color.ordinal();
        int oppColorIdx = 1 - colorIdx;

        int fromSq = from.getIndex();
        int toSq = to.getIndex();
        long fromMask = 1L << fromSq;
        long toMask = 1L << toSq;
        long moveMask = fromMask | toMask;

        // Figur von altem Feld entfernen
        int movedPieceIdx = Zobrist.getPieceIndex(piece.getType(), color);
        zobristKey ^= Zobrist.PIECE_SQUARE[movedPieceIdx][fromSq];

        // 50-Züge-Regel Zähler
        if (piece.getType() == PieceType.PAWN || move.isCapture()) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }

        // Figur von altem Feld entfernen
        squares[fromSq] = null;

        // Spezialbehandlung: En Passant
        if (move.isEnPassant()) {
            int capSq = (fromSq / 8) * 8 + (toSq % 8);
            squares[capSq] = null;
            long capMask = 1L << capSq;
            int oppPawnIdx = oppColorIdx * 6 + PieceType.PAWN.ordinal();
            pieceBitboards[oppPawnIdx] ^= capMask;
            colorBitboards[oppColorIdx] ^= capMask;

            int oppZobristPawnIdx = Zobrist.getPieceIndex(PieceType.PAWN, color.opposite());
            zobristKey ^= Zobrist.PIECE_SQUARE[oppZobristPawnIdx][capSq];
        }

        // Spezialbehandlung: Rochade (Turm umstellen)
        if (move.isCastle()) {
            int rank = fromSq / 8;
            int rookIdx = colorIdx * 6 + PieceType.ROOK.ordinal();
            int zobristRookIdx = Zobrist.getPieceIndex(PieceType.ROOK, color);
            if ((toSq % 8) == 6) {
                // Kurze Rochade (h-Turm von Spalte 7 auf Spalte 5)
                int rFrom = rank * 8 + 7;
                int rTo = rank * 8 + 5;
                Piece rook = squares[rFrom];
                squares[rFrom] = null;
                squares[rTo] = rook;
                long rookMoveMask = (1L << rFrom) | (1L << rTo);
                pieceBitboards[rookIdx] ^= rookMoveMask;
                colorBitboards[colorIdx] ^= rookMoveMask;

                zobristKey ^= Zobrist.PIECE_SQUARE[zobristRookIdx][rFrom];
                zobristKey ^= Zobrist.PIECE_SQUARE[zobristRookIdx][rTo];
            } else if ((toSq % 8) == 2) {
                // Lange Rochade (a-Turm von Spalte 0 auf Spalte 3)
                int rFrom = rank * 8 + 0;
                int rTo = rank * 8 + 3;
                Piece rook = squares[rFrom];
                squares[rFrom] = null;
                squares[rTo] = rook;
                long rookMoveMask = (1L << rFrom) | (1L << rTo);
                pieceBitboards[rookIdx] ^= rookMoveMask;
                colorBitboards[colorIdx] ^= rookMoveMask;

                zobristKey ^= Zobrist.PIECE_SQUARE[zobristRookIdx][rFrom];
                zobristKey ^= Zobrist.PIECE_SQUARE[zobristRookIdx][rTo];
            }
        }

        // Geschlagene Figur bei normalem Schlagzug entfernen
        if (move.isCapture() && !move.isEnPassant()) {
            Piece captured = move.getCapturedPiece();
            int capIdx = oppColorIdx * 6 + captured.getType().ordinal();
            pieceBitboards[capIdx] ^= toMask;
            colorBitboards[oppColorIdx] ^= toMask;

            int capZobristIdx = Zobrist.getPieceIndex(captured.getType(), color.opposite());
            zobristKey ^= Zobrist.PIECE_SQUARE[capZobristIdx][toSq];
        }

        // Zielfeld belegen (eventuelle Bauernumwandlung)
        Piece placedPiece = piece;
        if (move.isPromotion()) {
            placedPiece = Piece.of(move.getPromotionType(), color);
            int pawnIdx = colorIdx * 6 + PieceType.PAWN.ordinal();
            int promoIdx = colorIdx * 6 + move.getPromotionType().ordinal();
            pieceBitboards[pawnIdx] ^= fromMask;
            pieceBitboards[promoIdx] ^= toMask;
            colorBitboards[colorIdx] ^= moveMask;

            int promoZobristIdx = Zobrist.getPieceIndex(move.getPromotionType(), color);
            zobristKey ^= Zobrist.PIECE_SQUARE[promoZobristIdx][toSq];
        } else {
            int movedIdx = colorIdx * 6 + piece.getType().ordinal();
            pieceBitboards[movedIdx] ^= moveMask;
            colorBitboards[colorIdx] ^= moveMask;

            zobristKey ^= Zobrist.PIECE_SQUARE[movedPieceIdx][toSq];
        }
        squares[toSq] = placedPiece;

        // Gesamtbelegung aktualisieren
        occupiedBitboard = colorBitboards[0] | colorBitboards[1];

        // Königspositionen tracken & Rochaderechte aktualisieren bei Königszug
        if (piece.getType() == PieceType.KING) {
            if (color == PieceColor.WHITE) {
                whiteKingPos = to;
                whiteKingSquare = toSq;
                castlingRights &= ~(CASTLE_WHITE_KINGSIDE | CASTLE_WHITE_QUEENSIDE);
            } else {
                blackKingPos = to;
                blackKingSquare = toSq;
                castlingRights &= ~(CASTLE_BLACK_KINGSIDE | CASTLE_BLACK_QUEENSIDE);
            }
        }

        // Rochaderechte aktualisieren bei Turmzug oder Turmschlag
        updateCastlingRightsOnSquare(fromSq);
        updateCastlingRightsOnSquare(toSq);

        // Zobrist für Rochaderechte aktualisieren
        zobristKey ^= Zobrist.CASTLING_RIGHTS[oldCastlingRights & 15];
        zobristKey ^= Zobrist.CASTLING_RIGHTS[this.castlingRights & 15];

        // En-Passant-Zielfeld aktualisieren
        if (move.isDoublePawnPush()) {
            int dir = color.getPawnDirection();
            enPassantTarget = Position.of(fromSq % 8, fromSq / 8 + dir);
        } else {
            enPassantTarget = null;
        }

        // Zobrist für En Passant aktualisieren
        if (oldEnPassantTarget != null) {
            zobristKey ^= Zobrist.EN_PASSANT_FILE[oldEnPassantTarget.getFile()];
        }
        if (this.enPassantTarget != null) {
            zobristKey ^= Zobrist.EN_PASSANT_FILE[this.enPassantTarget.getFile()];
        }

        // Zähler & Rundenwechsel
        if (activePlayer == PieceColor.BLACK) {
            fullmoveNumber++;
        }
        activePlayer = activePlayer.opposite();
        zobristKey ^= Zobrist.SIDE_TO_MOVE;

        // Array-Kapazität prüfen und ggf. erweitern
        if (moveHistorySize == moveHistory.length) {
            moveHistory = Arrays.copyOf(moveHistory, moveHistory.length * 2);
        }
        moveHistory[moveHistorySize++] = move;

        if (positionHistorySize == positionHistory.length) {
            positionHistory = Arrays.copyOf(positionHistory, positionHistory.length * 2);
        }
        positionHistory[positionHistorySize++] = zobristKey;
    }

    /**
     * Führt einen "Null-Zug" aus (die eigene Runde überspringen).
     * Ändert nur den aktiven Spieler, das En-Passant-Feld und Zobrist-Keys.
     */
    public void makeNullMove() {
        if (nullHistorySize == nullCastlingHistory.length) {
            nullCastlingHistory = Arrays.copyOf(nullCastlingHistory, nullCastlingHistory.length * 2);
            nullEnPassantHistory = Arrays.copyOf(nullEnPassantHistory, nullEnPassantHistory.length * 2);
            nullHalfmoveHistory = Arrays.copyOf(nullHalfmoveHistory, nullHalfmoveHistory.length * 2);
        }

        nullCastlingHistory[nullHistorySize] = castlingRights;
        nullEnPassantHistory[nullHistorySize] = enPassantTarget;
        nullHalfmoveHistory[nullHistorySize] = halfmoveClock;
        nullHistorySize++;

        if (enPassantTarget != null) {
            zobristKey ^= Zobrist.EN_PASSANT_FILE[enPassantTarget.getFile()];
            enPassantTarget = null;
        }

        activePlayer = activePlayer.opposite();
        zobristKey ^= Zobrist.SIDE_TO_MOVE;

        if (positionHistorySize == positionHistory.length) {
            positionHistory = Arrays.copyOf(positionHistory, positionHistory.length * 2);
        }
        positionHistory[positionHistorySize++] = zobristKey;
    }

    /**
     * Nimmt einen Null-Zug zurück.
     */
    public void undoNullMove() {
        if (nullHistorySize == 0) return;
        nullHistorySize--;

        positionHistorySize--;
        
        activePlayer = activePlayer.opposite();
        zobristKey ^= Zobrist.SIDE_TO_MOVE;

        castlingRights = nullCastlingHistory[nullHistorySize];
        enPassantTarget = nullEnPassantHistory[nullHistorySize];
        halfmoveClock = nullHalfmoveHistory[nullHistorySize];

        if (enPassantTarget != null) {
            zobristKey ^= Zobrist.EN_PASSANT_FILE[enPassantTarget.getFile()];
        }
    }


    /**
     * Aktualisiert Rochaderechte basierend auf einem Feld-Index.
     * Verwendet direkten int-Index für maximale Performance.
     */
    private void updateCastlingRightsOnSquare(int sq) {
        // a1=0, h1=7, a8=56, h8=63
        if (sq == 0)       castlingRights &= ~CASTLE_WHITE_QUEENSIDE;
        else if (sq == 7)  castlingRights &= ~CASTLE_WHITE_KINGSIDE;
        else if (sq == 56) castlingRights &= ~CASTLE_BLACK_QUEENSIDE;
        else if (sq == 63) castlingRights &= ~CASTLE_BLACK_KINGSIDE;
    }

    /**
     * Nimmt den zuletzt ausgeführten Zug zurück.
     * Perfekt für Minimax/Alpha-Beta-Suchen in einer Schach-Engine!
     */
    public boolean undoMove() {
        if (moveHistorySize == 0) return false;

        Move move = moveHistory[--moveHistorySize];
        moveHistory[moveHistorySize] = null; // GC-freundlich
        Position from = move.getFrom();
        Position to = move.getTo();
        Piece movedPiece = move.getMovedPiece();
        PieceColor color = movedPiece.getColor();
        int colorIdx = color.ordinal();
        int oppColorIdx = 1 - colorIdx;

        int fromSq = from.getIndex();
        int toSq = to.getIndex();
        long fromMask = 1L << fromSq;
        long toMask = 1L << toSq;
        long moveMask = fromMask | toMask;

        // Rundenwechsel rückgängig
        activePlayer = activePlayer.opposite();
        if (activePlayer == PieceColor.BLACK) {
            fullmoveNumber--;
        }

        // Zustände wiederherstellen
        castlingRights = move.getPrevCastlingRights();
        enPassantTarget = move.getPrevEnPassantTarget();
        halfmoveClock = move.getPrevHalfmoveClock();

        // Gezogene Figur auf Ursprungsfeld setzen
        squares[fromSq] = movedPiece;

        // Zielfeld wiederherstellen (geschlagene Figur oder leer)
        Piece captured = move.getCapturedPiece();
        squares[toSq] = move.isEnPassant() ? null : captured;

        // Bitboards der bewegten Figur zurücksetzen
        if (move.isPromotion()) {
            int pawnIdx = colorIdx * 6 + PieceType.PAWN.ordinal();
            int promoIdx = colorIdx * 6 + move.getPromotionType().ordinal();
            pieceBitboards[pawnIdx] ^= fromMask;
            pieceBitboards[promoIdx] ^= toMask;
            colorBitboards[colorIdx] ^= moveMask;
        } else {
            int movedIdx = colorIdx * 6 + movedPiece.getType().ordinal();
            pieceBitboards[movedIdx] ^= moveMask;
            colorBitboards[colorIdx] ^= moveMask;
        }

        // Geschlagene Figur im Bitboard wiederherstellen
        if (move.isCapture() && !move.isEnPassant()) {
            int capIdx = oppColorIdx * 6 + captured.getType().ordinal();
            pieceBitboards[capIdx] ^= toMask;
            colorBitboards[oppColorIdx] ^= toMask;
        }

        // En Passant geschlagenen Bauern wiederherstellen
        if (move.isEnPassant()) {
            int capSq = (fromSq / 8) * 8 + (toSq % 8);
            squares[capSq] = captured;
            long capMask = 1L << capSq;
            int oppPawnIdx = oppColorIdx * 6 + PieceType.PAWN.ordinal();
            pieceBitboards[oppPawnIdx] ^= capMask;
            colorBitboards[oppColorIdx] ^= capMask;
        }

        // Rochade Turm rückgängig machen
        if (move.isCastle()) {
            int rank = fromSq / 8;
            int rookIdx = colorIdx * 6 + PieceType.ROOK.ordinal();
            if ((toSq % 8) == 6) {
                // Turm von 5 zurück auf 7
                int rFrom = rank * 8 + 7;
                int rTo = rank * 8 + 5;
                Piece rook = squares[rTo];
                squares[rTo] = null;
                squares[rFrom] = rook;
                long rookMoveMask = (1L << rFrom) | (1L << rTo);
                pieceBitboards[rookIdx] ^= rookMoveMask;
                colorBitboards[colorIdx] ^= rookMoveMask;
            } else if ((toSq % 8) == 2) {
                // Turm von 3 zurück auf 0
                int rFrom = rank * 8 + 0;
                int rTo = rank * 8 + 3;
                Piece rook = squares[rTo];
                squares[rTo] = null;
                squares[rFrom] = rook;
                long rookMoveMask = (1L << rFrom) | (1L << rTo);
                pieceBitboards[rookIdx] ^= rookMoveMask;
                colorBitboards[colorIdx] ^= rookMoveMask;
            }
        }

        occupiedBitboard = colorBitboards[0] | colorBitboards[1];

        // Königsposition tracken
        if (movedPiece.getType() == PieceType.KING) {
            if (color == PieceColor.WHITE) {
                whiteKingPos = from;
                whiteKingSquare = fromSq;
            } else {
                blackKingPos = from;
                blackKingSquare = fromSq;
            }
        }

        // Zobrist-Key und Position-History wiederherstellen
        this.zobristKey = move.getPrevZobristKey();
        if (positionHistorySize > 0) {
            positionHistorySize--;
        }

        return true;
    }

    public long getBitboard(PieceType type, PieceColor color) {
        return pieceBitboards[color.ordinal() * 6 + type.ordinal()];
    }

    public long getColorBitboard(PieceColor color) {
        return colorBitboards[color.ordinal()];
    }

    public long getOccupiedBitboard() {
        return occupiedBitboard;
    }

    public int getKingSquare(PieceColor color) {
        return color == PieceColor.WHITE ? whiteKingSquare : blackKingSquare;
    }

    /**
     * Gibt true zurück, wenn der Spieler neben König und Bauern noch weitere Figuren hat (für NMP).
     */
    public boolean hasNonPawnMaterial(PieceColor color) {
        int colorIdx = color.ordinal();
        long material = pieceBitboards[colorIdx * 6 + PieceType.KNIGHT.ordinal()] |
                        pieceBitboards[colorIdx * 6 + PieceType.BISHOP.ordinal()] |
                        pieceBitboards[colorIdx * 6 + PieceType.ROOK.ordinal()]   |
                        pieceBitboards[colorIdx * 6 + PieceType.QUEEN.ordinal()];
        return material != 0L;
    }

    /**
     * Erstellt eine unabhängige Kopie dieses Brettes.
     */
    public Board copy() {
        Board copy = new Board();
        System.arraycopy(this.squares, 0, copy.squares, 0, 64);
        System.arraycopy(this.pieceBitboards, 0, copy.pieceBitboards, 0, 12);
        System.arraycopy(this.colorBitboards, 0, copy.colorBitboards, 0, 2);
        copy.occupiedBitboard = this.occupiedBitboard;
        copy.whiteKingSquare = this.whiteKingSquare;
        copy.blackKingSquare = this.blackKingSquare;
        copy.activePlayer = this.activePlayer;
        copy.castlingRights = this.castlingRights;
        copy.enPassantTarget = this.enPassantTarget;
        copy.halfmoveClock = this.halfmoveClock;
        copy.fullmoveNumber = this.fullmoveNumber;
        copy.whiteKingPos = this.whiteKingPos;
        copy.blackKingPos = this.blackKingPos;
        copy.moveHistory = Arrays.copyOf(this.moveHistory, Math.max(this.moveHistory.length, this.moveHistorySize));
        copy.moveHistorySize = this.moveHistorySize;
        copy.zobristKey = this.zobristKey;
        copy.positionHistory = Arrays.copyOf(this.positionHistory, Math.max(this.positionHistory.length, this.positionHistorySize));
        copy.positionHistorySize = this.positionHistorySize;
        return copy;
    }

    /**
     * Generiert die FEN (Forsyth-Edwards Notation) des aktuellen Brettzustands.
     */
    public String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            int emptyCount = 0;
            for (int f = 0; f < 8; f++) {
                Piece piece = getPieceAt(f, r);
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount);
                        emptyCount = 0;
                    }
                    char c = piece.getType().getSymbol();
                    sb.append(piece.isWhite() ? Character.toUpperCase(c) : Character.toLowerCase(c));
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount);
            }
            if (r > 0) sb.append('/');
        }

        sb.append(' ').append(activePlayer == PieceColor.WHITE ? 'w' : 'b');

        sb.append(' ');
        if (castlingRights == 0) {
            sb.append('-');
        } else {
            if ((castlingRights & CASTLE_WHITE_KINGSIDE) != 0) sb.append('K');
            if ((castlingRights & CASTLE_WHITE_QUEENSIDE) != 0) sb.append('Q');
            if ((castlingRights & CASTLE_BLACK_KINGSIDE) != 0) sb.append('k');
            if ((castlingRights & CASTLE_BLACK_QUEENSIDE) != 0) sb.append('q');
        }

        sb.append(' ');
        if (enPassantTarget == null) {
            sb.append('-');
        } else {
            sb.append(enPassantTarget.toAlgebraic());
        }

        sb.append(' ').append(halfmoveClock);
        sb.append(' ').append(fullmoveNumber);

        return sb.toString();
    }

    /**
     * Erstellt ein Brett aus einer FEN-Zeichenkette.
     */
    public static Board fromFen(String fen) {
        Board board = empty();
        board.loadFen(fen);
        return board;
    }

    /**
     * Lädt eine FEN-Stellung in das aktuelle Brett.
     * Unterstützt Standard-FEN mit allen 6 Feldern:
     * 1. Figurenplatzierung (z.B. rn1k3r/1b1q1ppp/p2P4/2B2p2/8/1QNBR3/PP3PPP/2R3K1)
     * 2. Am Zug ('w' oder 'b')
     * 3. Rochaderechte ('KQkq' oder '-')
     * 4. En-Passant-Feld (z.B. 'e3' oder '-')
     * 5. Halbzugzähler (optional, default 0)
     * 6. Volle Zugnummer (optional, default 1)
     */
    public void loadFen(String fen) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("FEN darf nicht leer sein.");
        }

        clear();
        String[] parts = fen.trim().split("\\s+");
        if (parts.length < 1) {
            throw new IllegalArgumentException("Ungültiges FEN-Format: " + fen);
        }

        // 1. Figuren-Platzierung
        String piecePlacement = parts[0];
        String[] ranks = piecePlacement.split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN muss genau 8 Ränge enthalten: " + piecePlacement);
        }

        for (int r = 7; r >= 0; r--) {
            String rankStr = ranks[7 - r]; // Rang 8 ist Index 0 in FEN
            int file = 0;
            for (int i = 0; i < rankStr.length(); i++) {
                char c = rankStr.charAt(i);
                if (Character.isDigit(c)) {
                    file += (c - '0');
                } else {
                    if (file >= 8) {
                        throw new IllegalArgumentException("Zu viele Figuren auf Rang " + (r + 1) + ": " + rankStr);
                    }
                    PieceColor color = Character.isUpperCase(c) ? PieceColor.WHITE : PieceColor.BLACK;
                    PieceType type = PieceType.fromSymbol(c);
                    setPieceAt(Position.of(file, r), Piece.of(type, color));
                    file++;
                }
            }
            if (file != 8) {
                throw new IllegalArgumentException("Rang " + (r + 1) + " hat nicht 8 Felder: " + rankStr);
            }
        }

        // 2. Aktiver Spieler
        if (parts.length > 1) {
            this.activePlayer = "b".equalsIgnoreCase(parts[1]) ? PieceColor.BLACK : PieceColor.WHITE;
        } else {
            this.activePlayer = PieceColor.WHITE;
        }

        // 3. Rochaderechte
        this.castlingRights = 0;
        if (parts.length > 2 && !"-".equals(parts[2])) {
            String castling = parts[2];
            if (castling.contains("K")) this.castlingRights |= CASTLE_WHITE_KINGSIDE;
            if (castling.contains("Q")) this.castlingRights |= CASTLE_WHITE_QUEENSIDE;
            if (castling.contains("k")) this.castlingRights |= CASTLE_BLACK_KINGSIDE;
            if (castling.contains("q")) this.castlingRights |= CASTLE_BLACK_QUEENSIDE;
        }

        // 4. En-Passant-Zielfeld
        this.enPassantTarget = null;
        if (parts.length > 3 && !"-".equals(parts[3])) {
            try {
                this.enPassantTarget = Position.fromAlgebraic(parts[3]);
            } catch (Exception ignored) {}
        }

        // 5. Halbzugzähler (50-Züge-Regel)
        this.halfmoveClock = 0;
        if (parts.length > 4) {
            try {
                this.halfmoveClock = Integer.parseInt(parts[4]);
            } catch (NumberFormatException ignored) {}
        }

        // 6. Volle Zugnummer
        this.fullmoveNumber = 1;
        if (parts.length > 5) {
            try {
                this.fullmoveNumber = Integer.parseInt(parts[5]);
            } catch (NumberFormatException ignored) {}
        }

        this.zobristKey = Zobrist.computeHash(this);
        this.positionHistorySize = 0;
        this.positionHistory[this.positionHistorySize++] = this.zobristKey;
    }

    public long getZobristKey() {
        return zobristKey;
    }

    public List<Long> getPositionHistory() {
        List<Long> list = new ArrayList<>(positionHistorySize);
        for (int i = 0; i < positionHistorySize; i++) {
            list.add(positionHistory[i]);
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Zählt, wie oft der übergebene Zobrist-Hash in der Partiehistorie vorkommt.
     */
    public int getRepetitionCount(long key) {
        int count = 0;
        int start = Math.max(0, positionHistorySize - 1 - halfmoveClock);
        for (int i = positionHistorySize - 1; i >= start; i--) {
            if (positionHistory[i] == key) {
                count++;
            }
        }
        return count;
    }

    public static final long LIGHT_SQUARES = 0x55AA55AA55AA55AAL;
    public static final long DARK_SQUARES  = ~LIGHT_SQUARES;

    /**
     * Prüft, ob ungenügendes Material für ein Schachmatt vorliegt (Draw by Insufficient Material):
     * - K vs. K (König gegen König)
     * - K+N vs. K (König + Springer gegen König) oder K vs. K+N
     * - K+B vs. K (König + Läufer gegen König) oder K vs. K+B
     * - K+N vs. K+N (Springer gegen Springer)
     * - K+N+N vs. K (2 Springer gegen König können kein Matt forcieren) oder K vs. K+N+N
     * - K+B vs. K+B, wenn alle Läufer auf Feldern derselben Farbe stehen
     */
    public boolean hasInsufficientMaterial() {
        // Wenn Bauern, Türme oder Damen auf dem Brett sind, reicht das Material aus
        long pawns = pieceBitboards[PieceType.PAWN.ordinal()] | pieceBitboards[6 + PieceType.PAWN.ordinal()];
        long rooks = pieceBitboards[PieceType.ROOK.ordinal()] | pieceBitboards[6 + PieceType.ROOK.ordinal()];
        long queens = pieceBitboards[PieceType.QUEEN.ordinal()] | pieceBitboards[6 + PieceType.QUEEN.ordinal()];
        if ((pawns | rooks | queens) != 0L) {
            return false;
        }

        long whiteKnights = pieceBitboards[PieceType.KNIGHT.ordinal()];
        long blackKnights = pieceBitboards[6 + PieceType.KNIGHT.ordinal()];
        long whiteBishops = pieceBitboards[PieceType.BISHOP.ordinal()];
        long blackBishops = pieceBitboards[6 + PieceType.BISHOP.ordinal()];

        int numWhiteKnights = Long.bitCount(whiteKnights);
        int numBlackKnights = Long.bitCount(blackKnights);
        int numWhiteBishops = Long.bitCount(whiteBishops);
        int numBlackBishops = Long.bitCount(blackBishops);

        int totalKnights = numWhiteKnights + numBlackKnights;
        int totalBishops = numWhiteBishops + numBlackBishops;
        int totalMinors = totalKnights + totalBishops;

        // 1. K vs. K (King King)
        if (totalMinors == 0) {
            return true;
        }

        // 2. Nur Springer auf dem Brett (keine Läufer)
        if (totalBishops == 0) {
            // K+N vs. K oder K vs. K+N (1 Springer insgesamt)
            if (totalKnights == 1) {
                return true;
            }
            // K+N vs. K+N (Knight Knight, je 1 Springer pro Seite)
            if (numWhiteKnights == 1 && numBlackKnights == 1) {
                return true;
            }
            // K+N+N vs. K oder K vs. K+N+N (2 Springer einer Seite gegen bloßen König)
            if (totalKnights == 2 && (numWhiteKnights == 0 || numBlackKnights == 0)) {
                return true;
            }
            return false;
        }

        // 3. Nur Läufer auf dem Brett (keine Springer)
        if (totalKnights == 0) {
            // Wenn alle vorhandenen Läufer auf Feldern derselben Farbe stehen:
            // Deckt K+B vs. K, K vs. K+B sowie K+B vs. K+B (gleiche Feldfarbe) ab.
            long allBishops = whiteBishops | blackBishops;
            boolean allOnLight = (allBishops & DARK_SQUARES) == 0L;
            boolean allOnDark = (allBishops & LIGHT_SQUARES) == 0L;
            if (allOnLight || allOnDark) {
                return true;
            }
            return false;
        }

        return false;
    }

    /**
     * Prüft, ob die aktuelle Stellung bereits mindestens 3-mal entstanden ist (Dreifache Stellungswiederholung).
     */
    public boolean isThreefoldRepetition() {
        return getRepetitionCount(this.zobristKey) >= 3;
    }

    /**
     * Prüft, ob ein technisches Remis vorliegt (Dreifache Stellungswiederholung oder 50-Züge-Regel).
     */
    public boolean isDrawByRepetition() {
        return isThreefoldRepetition() || halfmoveClock >= 100;
    }

    /**
     * Prüft, ob ein automatisches Remis vorliegt (Stellungswiederholung, ungenügendes Material oder 50 Züge).
     */
    public boolean isDraw() {
        return isThreefoldRepetition() || hasInsufficientMaterial() || halfmoveClock >= 100;
    }
}
