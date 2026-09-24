package com.jchess.ui;

import com.jchess.core.piece.Piece;
import com.jchess.core.piece.PieceColor;
import com.jchess.core.piece.PieceType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Verantwortlich für das Rendern der Schachfiguren.
 * Unterstützt:
 * 1. PIXEL_ART: Scharfes Hochskalieren des gelieferten Spritesheets (chess_pieces.png)
 * 2. VECTOR_HD: Gestochen scharfe Vektordarstellung in modernem Look
 */
public class PieceRenderer {

    public enum RenderStyle {
        HD_SPRITES("HD Sprites (Neu)"),
        VECTOR_HD("High-Res Vektoren"),
        PIXEL_ART("Retro Pixel-Art (Spritesheet)");

        private final String label;

        RenderStyle(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private RenderStyle currentStyle = RenderStyle.HD_SPRITES;
    private final Map<PieceColor, Map<PieceType, BufferedImage>> hdSpriteMap = new EnumMap<>(PieceColor.class);
    private boolean hdSpritesLoaded = false;
    private final Map<PieceColor, Map<PieceType, BufferedImage>> spriteMap = new EnumMap<>(PieceColor.class);
    private boolean spritesheetLoaded = false;

    public PieceRenderer() {
        loadHDSprites();
        loadSpritesheet();
    }

    private void loadHDSprites() {
        hdSpriteMap.put(PieceColor.WHITE, new EnumMap<>(PieceType.class));
        hdSpriteMap.put(PieceColor.BLACK, new EnumMap<>(PieceType.class));

        int loadedCount = 0;
        for (PieceColor color : PieceColor.values()) {
            String colorSuffix = (color == PieceColor.WHITE) ? "W" : "B";
            for (PieceType type : PieceType.values()) {
                String typePrefix = switch (type) {
                    case PAWN -> "Pawn";
                    case KNIGHT -> "Knight";
                    case BISHOP -> "Bishop";
                    case ROOK -> "Rook";
                    case QUEEN -> "Queen";
                    case KING -> "King";
                };
                String fileName = typePrefix + colorSuffix + ".png";

                BufferedImage img = loadSingleImage(fileName);
                if (img != null) {
                    hdSpriteMap.get(color).put(type, img);
                    loadedCount++;
                }
            }
        }

        if (loadedCount == 12) {
            hdSpritesLoaded = true;
        } else {
            System.err.println("Warnung: Nur " + loadedCount + " von 12 HD-Sprites geladen.");
        }
    }

    private BufferedImage loadSingleImage(String fileName) {
        String[] possibleDirs = {
                "src/main/resources/Pieces/",
                "ressources/Pieces/",
                "Pieces/"
        };

        for (String dir : possibleDirs) {
            File f = new File(dir + fileName);
            if (f.exists()) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) return img;
                } catch (Exception ignored) {}
            }
        }

        String[] cpPaths = {
                "/Pieces/" + fileName,
                "/pieces/" + fileName,
                "/" + fileName
        };
        for (String cp : cpPaths) {
            try (InputStream is = getClass().getResourceAsStream(cp)) {
                if (is != null) {
                    BufferedImage img = ImageIO.read(is);
                    if (img != null) return img;
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    private void loadSpritesheet() {
        BufferedImage sheet = null;

        // Versuche verschiedene Pfade: Classpath und Dateisystem
        String[] possiblePaths = {
                "ressources/chess_pieces.png",
                "ressources/pieces.png",
                "src/main/resources/chess_pieces.png",
                "src/main/resources/pieces.png"
        };

        for (String p : possiblePaths) {
            File file = new File(p);
            if (file.exists()) {
                try {
                    sheet = ImageIO.read(file);
                    if (sheet != null) break;
                } catch (Exception ignored) {}
            }
        }

        if (sheet == null) {
            try (InputStream is = getClass().getResourceAsStream("/chess_pieces.png")) {
                if (is != null) {
                    sheet = ImageIO.read(is);
                }
            } catch (Exception ignored) {}
        }

        if (sheet != null) {
            sliceSprites(sheet);
            spritesheetLoaded = true;
        } else {
            System.err.println("Warnung: Spritesheet nicht gefunden, verwende Vektor-Modus.");
            currentStyle = RenderStyle.VECTOR_HD;
        }
    }

    /**
     * Schneidet die 12 Figuren aus dem 64x64 Spritesheet aus:
     * Reihe 0 = Weiß (Y: 22..31)
     * Reihe 1 = Schwarz (Y: 33..42)
     * Spalten: König (1..10), Dame (12..21), Läufer (23..32), Springer (33..43), Turm (45..52), Bauer (55..61)
     */
    private void sliceSprites(BufferedImage sheet) {
        spriteMap.put(PieceColor.WHITE, new EnumMap<>(PieceType.class));
        spriteMap.put(PieceColor.BLACK, new EnumMap<>(PieceType.class));

        // Subimage Bounding Boxes (x, y, w, h)
        int[][] whiteBounds = {
                {1, 22, 9, 9},   // KING
                {12, 22, 9, 9},  // QUEEN
                {23, 22, 9, 9},  // BISHOP
                {33, 22, 10, 9}, // KNIGHT
                {45, 22, 7, 9},  // ROOK
                {55, 22, 6, 9}   // PAWN
        };

        int[][] blackBounds = {
                {1, 33, 9, 9},   // KING
                {12, 33, 9, 9},  // QUEEN
                {23, 33, 9, 9},  // BISHOP
                {33, 33, 10, 9}, // KNIGHT
                {45, 33, 7, 9},  // ROOK
                {55, 33, 6, 9}   // PAWN
        };

        PieceType[] order = {
                PieceType.KING, PieceType.QUEEN, PieceType.BISHOP,
                PieceType.KNIGHT, PieceType.ROOK, PieceType.PAWN
        };

        for (int i = 0; i < order.length; i++) {
            PieceType type = order[i];
            int[] wb = whiteBounds[i];
            int[] bb = blackBounds[i];

            BufferedImage whiteSprite = extractSubimage(sheet, wb[0], wb[1], wb[2], wb[3]);
            BufferedImage blackSprite = extractSubimage(sheet, bb[0], bb[1], bb[2], bb[3]);

            spriteMap.get(PieceColor.WHITE).put(type, whiteSprite);
            spriteMap.get(PieceColor.BLACK).put(type, blackSprite);
        }
    }

    private BufferedImage extractSubimage(BufferedImage sheet, int x, int y, int w, int h) {
        // Sichere Randbegrenzungen
        int safeX = Math.max(0, Math.min(x, sheet.getWidth() - 1));
        int safeY = Math.max(0, Math.min(y, sheet.getHeight() - 1));
        int safeW = Math.min(w, sheet.getWidth() - safeX);
        int safeH = Math.min(h, sheet.getHeight() - safeY);

        return sheet.getSubimage(safeX, safeY, safeW, safeH);
    }

    public RenderStyle getCurrentStyle() {
        return currentStyle;
    }

    public void setCurrentStyle(RenderStyle style) {
        this.currentStyle = style;
    }

    public boolean isSpritesheetLoaded() {
        return spritesheetLoaded;
    }

    public boolean isHDSpritesLoaded() {
        return hdSpritesLoaded;
    }

    /**
     * Zeichnet eine Figur zentriert in ein Feld der Größe squareSize.
     */
    public void drawPiece(Graphics2D g2, Piece piece, int x, int y, int squareSize) {
        if (piece == null) return;

        if (currentStyle == RenderStyle.HD_SPRITES && hdSpritesLoaded) {
            drawHDPiece(g2, piece, x, y, squareSize);
        } else if (currentStyle == RenderStyle.PIXEL_ART && spritesheetLoaded) {
            drawPixelArtPiece(g2, piece, x, y, squareSize);
        } else {
            drawVectorPiece(g2, piece, x, y, squareSize);
        }
    }

    private void drawHDPiece(Graphics2D g2, Piece piece, int x, int y, int squareSize) {
        Map<PieceType, BufferedImage> colorMap = hdSpriteMap.get(piece.getColor());
        if (colorMap == null) {
            drawVectorPiece(g2, piece, x, y, squareSize);
            return;
        }

        BufferedImage sprite = colorMap.get(piece.getType());
        if (sprite == null) {
            drawVectorPiece(g2, piece, x, y, squareSize);
            return;
        }

        Graphics2D g = (Graphics2D) g2.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Figur füllt ca. 84% der Feldhöhe aus
            int targetH = (int) (squareSize * 0.84);
            double ratio = (double) targetH / sprite.getHeight();
            int targetW = (int) Math.round(sprite.getWidth() * ratio);

            int px = x + (squareSize - targetW) / 2;
            int py = y + (squareSize - targetH) / 2;

            g.drawImage(sprite, px, py, targetW, targetH, null);
        } finally {
            g.dispose();
        }
    }

    private void drawPixelArtPiece(Graphics2D g2, Piece piece, int x, int y, int squareSize) {
        Map<PieceType, BufferedImage> colorMap = spriteMap.get(piece.getColor());
        if (colorMap == null) return;

        BufferedImage sprite = colorMap.get(piece.getType());
        if (sprite == null) return;

        Graphics2D g = (Graphics2D) g2.create();
        try {
            // RenderHints für messerscharfe Pixel ohne Weichzeichner
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            // Skalierung: Figur soll ca. 75% der Feldhöhe einnehmen
            int targetH = (int) (squareSize * 0.76);
            double ratio = (double) targetH / sprite.getHeight();
            int targetW = (int) Math.round(sprite.getWidth() * ratio);

            int px = x + (squareSize - targetW) / 2;
            int py = y + (squareSize - targetH) / 2;

            g.drawImage(sprite, px, py, targetW, targetH, null);
        } finally {
            g.dispose();
        }
    }

    private void drawVectorPiece(Graphics2D g2, Piece piece, int x, int y, int squareSize) {
        Graphics2D g = (Graphics2D) g2.create();
        try {
            // Sauberes Anti-Aliasing für Vektoren
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Schöne Typografie mit Unicode-Glyphen als gestochen scharfe Vektor-Figuren
            int fontSize = (int) (squareSize * 0.78);
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
            g.setFont(font);

            String symbol = piece.getUnicodeSymbol();
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D bounds = fm.getStringBounds(symbol, g);

            int textX = x + (int) ((squareSize - bounds.getWidth()) / 2 - bounds.getX());
            int textY = y + (int) ((squareSize - bounds.getHeight()) / 2 - bounds.getY());

            // Subtiler Schlagschatten
            g.setColor(new Color(0, 0, 0, 45));
            g.drawString(symbol, textX + 2, textY + 2);

            // Hauptfarbe
            if (piece.isWhite()) {
                g.setColor(new Color(245, 245, 245));
            } else {
                g.setColor(new Color(30, 30, 35));
            }
            g.drawString(symbol, textX, textY);
        } finally {
            g.dispose();
        }
    }
}
