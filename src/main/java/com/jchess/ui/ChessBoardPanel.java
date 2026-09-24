package com.jchess.ui;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.board.Position;
import com.jchess.core.piece.Piece;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;
import com.jchess.core.rules.MoveGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interaktives Schachbrett-Panel mit:
 * - Klick- und Drag-and-Drop-Steuerung
 * - Schönem Farbthema (Grün/Creme im Stil moderner Schachplattformen)
 * - Koordinatenbeschriftung (a-h, 1-8)
 * - Hervorhebung von letztem Zug, ausgewählter Figur und legalen Zielfeldern
 * - Schach-Warnanzeige auf dem König
 */
public class ChessBoardPanel extends JPanel {

    // Farben: Warmes Turnier-Holz-Thema (Tournament Wood)
    private static final Color LIGHT_SQUARE = new Color(240, 217, 181); // Helles Holz-Creme (#F0D9B5)
    private static final Color DARK_SQUARE = new Color(181, 136, 99);   // Klassisches warmes Nussbaum-Braun (#B58863)
    private static final Color HIGHLIGHT_LIGHT = new Color(205, 210, 106, 210); // Warmes Gold-Gelb auf Hell
    private static final Color HIGHLIGHT_DARK = new Color(170, 162, 58, 210);  // Warmes Oliv-Gold auf Dunkel
    private static final Color SELECTED_COLOR = new Color(247, 210, 84, 180);   // Leuchtendes Gold
    private static final Color CHECK_COLOR = new Color(235, 60, 50, 190);      // Sanftes Rot
    private static final Color MOVE_DOT_COLOR = new Color(55, 40, 30, 80);     // Warmes Dunkelbraun für Zugpunkte

    private final Board board;
    private final PieceRenderer pieceRenderer;
    private Consumer<Move> moveListener;

    private boolean flipped = false; // false = Weiß unten, true = Schwarz unten
    private Position selectedSquare = null;
    private List<Move> legalMovesFromSelected = Collections.emptyList();

    // Drag-and-Drop Zustand
    private boolean isDragging = false;
    private Position dragSource = null;
    private Point dragCurrentPoint = null;
    private Piece draggedPiece = null;

    private Move bestMove = null;
    private boolean showBestMoveArrow = false;

    private static final int BORDER_MARGIN = 24; // Platz für Koordinatenbeschriftung

    public ChessBoardPanel(Board board, PieceRenderer pieceRenderer) {
        this.board = board;
        this.pieceRenderer = pieceRenderer;
        setPreferredSize(new Dimension(640, 640));
        setBackground(new Color(44, 38, 33));

        MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setMoveListener(Consumer<Move> listener) {
        this.moveListener = listener;
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

    public void clearSelection() {
        selectedSquare = null;
        legalMovesFromSelected = Collections.emptyList();
        isDragging = false;
        draggedPiece = null;
        dragSource = null;
        repaint();
    }

    public void setBestMove(Move move) {
        this.bestMove = move;
        if (showBestMoveArrow) {
            repaint();
        }
    }

    public void clearBestMove() {
        if (this.bestMove != null) {
            this.bestMove = null;
            if (showBestMoveArrow) {
                repaint();
            }
        }
    }

    public void setShowBestMoveArrow(boolean show) {
        this.showBestMoveArrow = show;
        repaint();
    }

    public boolean isShowBestMoveArrow() {
        return showBestMoveArrow;
    }

    public Move getBestMove() {
        return bestMove;
    }

    private int getSquareSize() {
        int w = getWidth() - 2 * BORDER_MARGIN;
        int h = getHeight() - 2 * BORDER_MARGIN;
        int size = Math.min(w, h);
        return Math.max(20, size / 8);
    }

    private Point getBoardOrigin(int squareSize) {
        int boardPixels = squareSize * 8;
        int ox = (getWidth() - boardPixels) / 2;
        int oy = (getHeight() - boardPixels) / 2;
        return new Point(ox, oy);
    }

    private Position screenToPosition(int screenX, int screenY) {
        int squareSize = getSquareSize();
        Point origin = getBoardOrigin(squareSize);

        int relX = screenX - origin.x;
        int relY = screenY - origin.y;

        if (relX < 0 || relY < 0 || relX >= squareSize * 8 || relY >= squareSize * 8) {
            return null;
        }

        int col = relX / squareSize;
        int row = relY / squareSize;

        int file = flipped ? (7 - col) : col;
        int rank = flipped ? row : (7 - row);

        return Position.of(file, rank);
    }

    private Point positionToScreen(Position pos, int squareSize, Point origin) {
        int col = flipped ? (7 - pos.getFile()) : pos.getFile();
        int row = flipped ? pos.getRank() : (7 - pos.getRank());

        int x = origin.x + col * squareSize;
        int y = origin.y + row * squareSize;
        return new Point(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int squareSize = getSquareSize();
        Point origin = getBoardOrigin(squareSize);

        // 1. Koordinaten an den Rändern zeichnen
        drawCoordinates(g2, squareSize, origin);

        // 2. Schachbrett-Felder zeichnen
        Move lastMove = board.getLastMove();
        Position inCheckKingPos = MoveGenerator.isKingInCheck(board, board.getActivePlayer())
                ? board.getKingPosition(board.getActivePlayer())
                : null;

        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Position pos = Position.of(file, rank);
                Point p = positionToScreen(pos, squareSize, origin);

                // Grundfarbe
                Color sqColor = pos.isLightSquare() ? LIGHT_SQUARE : DARK_SQUARE;

                // Letzter Zug Markierung
                if (lastMove != null && (pos.equals(lastMove.getFrom()) || pos.equals(lastMove.getTo()))) {
                    sqColor = pos.isLightSquare() ? HIGHLIGHT_LIGHT : HIGHLIGHT_DARK;
                }

                g2.setColor(sqColor);
                g2.fillRect(p.x, p.y, squareSize, squareSize);

                // Schach-Alarm auf König
                if (pos.equals(inCheckKingPos)) {
                    g2.setColor(CHECK_COLOR);
                    g2.fillRect(p.x, p.y, squareSize, squareSize);
                }

                // Ausgewähltes Feld
                if (pos.equals(selectedSquare)) {
                    g2.setColor(SELECTED_COLOR);
                    g2.fillRect(p.x, p.y, squareSize, squareSize);
                }
            }
        }

        // 3. Figuren zeichnen
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Position pos = Position.of(file, rank);
                if (isDragging && pos.equals(dragSource)) {
                    continue; // Während Drag nicht auf dem Ursprungsfeld zeichnen
                }

                Piece piece = board.getPieceAt(pos);
                if (piece != null) {
                    Point p = positionToScreen(pos, squareSize, origin);
                    pieceRenderer.drawPiece(g2, piece, p.x, p.y, squareSize);
                }
            }
        }

        // 4. Legale Zielfelder hervorheben
        for (Move m : legalMovesFromSelected) {
            Position to = m.getTo();
            Point p = positionToScreen(to, squareSize, origin);
            Piece targetPiece = board.getPieceAt(to);

            if (targetPiece == null && !m.isEnPassant()) {
                // Punkt in der Mitte für leere Felder
                int dotSize = Math.max(10, squareSize / 3);
                int dotX = p.x + (squareSize - dotSize) / 2;
                int dotY = p.y + (squareSize - dotSize) / 2;
                g2.setColor(MOVE_DOT_COLOR);
                g2.fill(new Ellipse2D.Double(dotX, dotY, dotSize, dotSize));
            } else {
                // Kreisring / Eckenmarkierung für Schlagzüge
                g2.setColor(new Color(230, 70, 70, 160));
                g2.setStroke(new BasicStroke(Math.max(3, squareSize / 14f)));
                int ringMargin = 4;
                g2.drawOval(p.x + ringMargin, p.y + ringMargin, squareSize - 2 * ringMargin, squareSize - 2 * ringMargin);
            }
        }

        // 4.5 Bester Zug Pfeil (falls aktiviert)
        if (showBestMoveArrow && bestMove != null && !isDragging) {
            drawBestMoveArrow(g2, bestMove, squareSize, origin);
        }

        // 5. Dragging-Figur unter dem Mauszeiger zeichnen
        if (isDragging && draggedPiece != null && dragCurrentPoint != null) {
            int px = dragCurrentPoint.x - squareSize / 2;
            int py = dragCurrentPoint.y - squareSize / 2;
            pieceRenderer.drawPiece(g2, draggedPiece, px, py, squareSize);
        }

        g2.dispose();
    }

    /**
     * Zeichnet einen modernen, hochauflösenden Vektorpfeil für den besten Zug
     * im Stil professioneller Schachplattformen (Lichess/Chess.com).
     */
    private void drawBestMoveArrow(Graphics2D g2, Move move, int squareSize, Point origin) {
        if (move == null || move.getFrom() == null || move.getTo() == null) return;

        Point pFrom = positionToScreen(move.getFrom(), squareSize, origin);
        Point pTo = positionToScreen(move.getTo(), squareSize, origin);

        double x1 = pFrom.x + squareSize / 2.0;
        double y1 = pFrom.y + squareSize / 2.0;
        double x2 = pTo.x + squareSize / 2.0;
        double y2 = pTo.y + squareSize / 2.0;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        if (length < 1.0) return;

        double ux = dx / length;
        double uy = dy / length;
        double nx = -uy;
        double ny = ux;

        double headLength = Math.min(squareSize * 0.40, length * 0.45);
        double headWidth = squareSize * 0.22;
        double shaftHalfWidth = squareSize * 0.075;

        // Basis des Pfeilkopfes
        double bx = x2 - ux * headLength;
        double by = y2 - uy * headLength;

        // Flügel des Pfeilkopfes
        double lx = bx + nx * headWidth;
        double ly = by + ny * headWidth;
        double rx = bx - nx * headWidth;
        double ry = by - ny * headWidth;

        // Schaft und Kopf zu einem geschlossenen Polygon verbinden
        Path2D.Double arrowPath = new Path2D.Double();
        arrowPath.moveTo(x1 + nx * shaftHalfWidth, y1 + ny * shaftHalfWidth);
        arrowPath.lineTo(bx + nx * shaftHalfWidth, by + ny * shaftHalfWidth);
        arrowPath.lineTo(lx, ly);
        arrowPath.lineTo(x2, y2); // Pfeilspitze am Zielfeldzentrum
        arrowPath.lineTo(rx, ry);
        arrowPath.lineTo(bx - nx * shaftHalfWidth, by - ny * shaftHalfWidth);
        arrowPath.lineTo(x1 - nx * shaftHalfWidth, y1 - ny * shaftHalfWidth);
        arrowPath.closePath();

        // Leuchtendes Smaragdgrün mit edler Transparenz
        Color arrowColor = new Color(46, 204, 113, 195);
        Color borderColor = new Color(25, 135, 75, 230);

        // Runder Startpunkt am Ausgangsfeld für sanften Übergang
        double circleRadius = shaftHalfWidth * 1.5;
        Ellipse2D.Double startCircle = new Ellipse2D.Double(
                x1 - circleRadius, y1 - circleRadius,
                circleRadius * 2, circleRadius * 2
        );

        Graphics2D gArrow = (Graphics2D) g2.create();
        gArrow.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Füllung
        gArrow.setColor(arrowColor);
        gArrow.fill(startCircle);
        gArrow.fill(arrowPath);

        // Feine Kontur für herausragenden Kontrast auf allen Brettfeldern
        gArrow.setColor(borderColor);
        gArrow.setStroke(new BasicStroke(Math.max(1.5f, squareSize * 0.025f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        gArrow.draw(arrowPath);
        gArrow.draw(startCircle);

        gArrow.dispose();
    }

    private void drawCoordinates(Graphics2D g2, int squareSize, Point origin) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(195, 185, 170));

        for (int i = 0; i < 8; i++) {
            // Spalten (a-h)
            char fileChar = (char) ('a' + (flipped ? (7 - i) : i));
            String fileStr = String.valueOf(fileChar);
            int fx = origin.x + i * squareSize + (squareSize - fm.stringWidth(fileStr)) / 2;
            int fyBottom = origin.y + squareSize * 8 + 16;
            g2.drawString(fileStr, fx, fyBottom);

            // Reihen (1-8)
            int rankNum = flipped ? (i + 1) : (8 - i);
            String rankStr = String.valueOf(rankNum);
            int ry = origin.y + i * squareSize + (squareSize + fm.getAscent()) / 2 - 2;
            int rxLeft = origin.x - 16;
            g2.drawString(rankStr, rxLeft, ry);
        }
    }

    private void handleSquareClicked(Position clickedPos) {
        if (clickedPos == null) {
            clearSelection();
            return;
        }

        Piece clickedPiece = board.getPieceAt(clickedPos);

        // Falls bereits eine Figur ausgewählt ist: Versuche Zug auszuführen
        if (selectedSquare != null) {
            Move candidateMove = resolveMove(clickedPos);
            if (candidateMove != null) {
                executeMove(candidateMove);
                return;
            } else if (!isSquareLegalTarget(clickedPos)) {
                // Bei Klick auf ungültiges Feld Auswahl aufheben
                clearSelection();
            }
        }

        // Falls auf eigene Figur geklickt: Auswählen
        if (clickedPiece != null && clickedPiece.getColor() == board.getActivePlayer()) {
            selectedSquare = clickedPos;
            legalMovesFromSelected = MoveGenerator.generateLegalMovesFromSquare(board, clickedPos);
            repaint();
        } else {
            clearSelection();
        }
    }

    private boolean isSquareLegalTarget(Position pos) {
        for (Move m : legalMovesFromSelected) {
            if (m.getTo().equals(pos)) return true;
        }
        return false;
    }

    private Move resolveMove(Position targetPos) {
        List<Move> matchingMoves = new ArrayList<>();
        for (Move m : legalMovesFromSelected) {
            if (m.getTo().equals(targetPos)) {
                matchingMoves.add(m);
            }
        }

        if (matchingMoves.isEmpty()) {
            return null;
        }

        if (matchingMoves.get(0).isPromotion()) {
            PieceType promoType = askPromotionPiece(board.getActivePlayer());
            if (promoType == null) {
                return null; // Benutzer hat abgebrochen
            }
            for (Move m : matchingMoves) {
                if (m.getPromotionType() == promoType) {
                    return m;
                }
            }
            return null;
        }

        return matchingMoves.get(0);
    }

    private PieceType askPromotionPiece(PieceColor color) {
        boolean isWhite = (color == PieceColor.WHITE);
        String[] options = isWhite ? new String[]{
                "♕ Dame",
                "♖ Turm",
                "♗ Läufer",
                "♘ Springer"
        } : new String[]{
                "♛ Dame",
                "♜ Turm",
                "♝ Läufer",
                "♞ Springer"
        };

        int choice = JOptionPane.showOptionDialog(
                this,
                "Wähle die gewünschte Figur für die Bauernumwandlung:",
                "Bauernumwandlung (Promotion)",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        return switch (choice) {
            case 0 -> PieceType.QUEEN;
            case 1 -> PieceType.ROOK;
            case 2 -> PieceType.BISHOP;
            case 3 -> PieceType.KNIGHT;
            default -> null; // Abgebrochen
        };
    }

    private void executeMove(Move move) {
        clearSelection();
        if (moveListener != null) {
            moveListener.accept(move);
        }
    }

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            Position pos = screenToPosition(e.getX(), e.getY());
            if (pos == null) return;

            Piece piece = board.getPieceAt(pos);
            if (piece != null && piece.getColor() == board.getActivePlayer()) {
                isDragging = true;
                dragSource = pos;
                draggedPiece = piece;
                dragCurrentPoint = e.getPoint();
                selectedSquare = pos;
                legalMovesFromSelected = MoveGenerator.generateLegalMovesFromSquare(board, pos);
                repaint();
            } else if (selectedSquare != null) {
                handleSquareClicked(pos);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (isDragging) {
                dragCurrentPoint = e.getPoint();
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!isDragging) return;

            Position dropTarget = screenToPosition(e.getX(), e.getY());
            if (dropTarget != null && !dropTarget.equals(dragSource)) {
                Move candidateMove = resolveMove(dropTarget);
                if (candidateMove != null) {
                    executeMove(candidateMove);
                    return;
                }
            }

            // Drag abgebrochen / ungültiges Zielfeld
            isDragging = false;
            draggedPiece = null;
            dragCurrentPoint = null;
            repaint();
        }
    }
}
