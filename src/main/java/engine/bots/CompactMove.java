package engine.bots;

import com.jchess.core.board.Board;
import com.jchess.core.board.Move;
import com.jchess.core.board.Position;
import com.jchess.core.piece.Piece;
import com.jchess.core.piece.PieceType;

/**
 * 16-bit compact move representation.
 * Needs the Board to reconstruct the full Move object.
 */
public final class CompactMove {

    // Flags
    public static final int NoFlag = 0b0000;
    public static final int EnPassantCaptureFlag = 0b0001;
    public static final int CastleFlag = 0b0010;
    public static final int PawnTwoUpFlag = 0b0011;

    public static final int PromoteToQueenFlag = 0b0100;
    public static final int PromoteToKnightFlag = 0b0101;
    public static final int PromoteToRookFlag = 0b0110;
    public static final int PromoteToBishopFlag = 0b0111;

    // Masks
    private static final int startSquareMask = 0b0000000000111111;
    private static final int targetSquareMask = 0b0000111111000000;

    private CompactMove() {}

    public static short createMove(int startSquare, int targetSquare, int flag) {
        return (short) (startSquare | (targetSquare << 6) | (flag << 12));
    }

    public static int getStartSquare(short moveValue) {
        return moveValue & startSquareMask;
    }

    public static int getTargetSquare(short moveValue) {
        return (moveValue & targetSquareMask) >> 6;
    }

    public static int getMoveFlag(short moveValue) {
        return (moveValue & 0xFFFF) >>> 12;
    }

    public static PieceType getPromotionPieceType(short moveValue) {
        return switch (getMoveFlag(moveValue)) {
            case PromoteToRookFlag -> PieceType.ROOK;
            case PromoteToKnightFlag -> PieceType.KNIGHT;
            case PromoteToBishopFlag -> PieceType.BISHOP;
            case PromoteToQueenFlag -> PieceType.QUEEN;
            default -> null;
        };
    }
    
    public static short fromStandardMove(Move move) {
        int flag = NoFlag;
        if (move.isPromotion()) {
            flag = switch (move.getPromotionType()) {
                case QUEEN -> PromoteToQueenFlag;
                case ROOK -> PromoteToRookFlag;
                case BISHOP -> PromoteToBishopFlag;
                case KNIGHT -> PromoteToKnightFlag;
                default -> NoFlag;
            };
        } else if (move.isEnPassant()) {
            flag = EnPassantCaptureFlag;
        } else if (move.isCastle()) {
            flag = CastleFlag;
        } else if (move.isDoublePawnPush()) {
            flag = PawnTwoUpFlag;
        }
        return createMove(move.getFrom().getIndex(), move.getTo().getIndex(), flag);
    }

    public static Move toStandardMove(short compactMove, Board board) {
        int fromSq = getStartSquare(compactMove);
        int toSq = getTargetSquare(compactMove);
        int flag = getMoveFlag(compactMove);

        Piece movedPiece = board.getPieceAtIndex(fromSq);
        Piece capturedPiece = board.getPieceAtIndex(toSq);
        PieceType promoType = null;
        boolean isEnPassant = false;
        boolean isCastle = false;
        boolean isDoublePawnPush = false;

        if (flag >= PromoteToQueenFlag) {
            promoType = getPromotionPieceType(compactMove);
        } else if (flag == EnPassantCaptureFlag) {
            isEnPassant = true;
            int epPawnSq = (fromSq / 8) * 8 + (toSq % 8);
            capturedPiece = board.getPieceAtIndex(epPawnSq);
        } else if (flag == CastleFlag) {
            isCastle = true;
        } else if (flag == PawnTwoUpFlag) {
            isDoublePawnPush = true;
        }

        return new Move(
            Position.fromIndex(fromSq),
            Position.fromIndex(toSq),
            movedPiece,
            capturedPiece,
            promoType,
            isCastle,
            isEnPassant,
            isDoublePawnPush
        );
    }
}
