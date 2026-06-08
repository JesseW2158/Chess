package Chess.Core.AI;

import Chess.Core.ChessBoard;
import Chess.Core.AI.Util.Move;
import Chess.Core.AI.Util.Position;
import Chess.Core.Pieces.Piece;
import Chess.Core.Util.ChessSquare;
import Chess.Core.Util.Color;
import Chess.Core.Util.PieceFactory;

/** The ONLY class that touches both JavaFX and the engine. */
public final class EngineBridge {
    private EngineBridge() {}

    /** Build a plain Position snapshot from the live GUI board. */
    public static Position fromBoard(ChessBoard b) {
        Position p = new Position();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece pc = b.getSquare(x, y).getPiece();
                if (pc != null) p.board[y * 8 + x] = encode(pc);
            }
        }
        p.whiteToMove = b.getTurn() == Color.WHITE;

        // Castling rights: king and rook still on home squares with firstTurn < 0.
        p.wK = homeUnmoved(b, 4, 7) && homeUnmoved(b, 7, 7);
        p.wQ = homeUnmoved(b, 4, 7) && homeUnmoved(b, 0, 7);
        p.bK = homeUnmoved(b, 4, 0) && homeUnmoved(b, 7, 0);
        p.bQ = homeUnmoved(b, 4, 0) && homeUnmoved(b, 0, 0);

        // En-passant target: a pawn of the side that JUST moved, double-pushed last turn.
        // Mirrors the GUI's own ep rule (currentTurn - firstTurn == 1).
        p.epSquare = -1;
        boolean blackJustMoved = p.whiteToMove;
        int pawnRank = blackJustMoved ? 3 : 4;        // black landed on y=3, white on y=4
        int epRank   = blackJustMoved ? 2 : 5;        // square skipped over
        for (int x = 0; x < 8; x++) {
            Piece pc = b.getSquare(x, pawnRank).getPiece();
            if (pc != null && pc.getName().equalsIgnoreCase("pawn")
                    && pc.getColor() == (blackJustMoved ? Color.BLACK : Color.WHITE)
                    && pc.getFirstTurn() >= 0
                    && b.getCurrentTurn() - pc.getFirstTurn() == 1) {
                p.epSquare = epRank * 8 + x;
                break;
            }
        }

        // Seed the half-move clock and the Zobrist key. ruleOf50 is essential: the
        // search's repetition window is min(historyLen, ruleOf50), so without it the
        // seeded game history (see ChessBoard) would fall outside the scan and be ignored.
        p.ruleOf50 = b.get50MoveRuleTurns();
        p.recomputeZobrist();
        return p;
    }

    private static boolean homeUnmoved(ChessBoard b, int x, int y) {
        Piece pc = b.getSquare(x, y).getPiece();
        return pc != null && pc.getFirstTurn() < 0;
    }

    private static int encode(Piece pc) {
        int sign = pc.getColor() == Color.WHITE ? 1 : -1;
        return sign * (PieceFactory.getPieceID(pc) == 0 ? Position.PAWN : switch (PieceFactory.getPieceID(pc)) {
            case 0b100 -> Position.ROOK;
            case 0b101 -> Position.KNIGHT;
            case 0b110 -> Position.BISHOP;
            case 0b1110 -> Position.QUEEN;
            case 0b1111 -> Position.KING;
            default -> Position.PAWN;
        });
    }

    /** Apply an engine Move to the live GUI board, then advance the turn. Runs on the FX thread. */
    public static void applyMove(ChessBoard b, Move m) {
        b.pushHistory();
        int fx = m.from() % 8, fy = m.from() / 8;
        int tx = m.to() % 8,   ty = m.to() / 8;
        Piece piece = b.getSquare(fx, fy).getPiece();
        ChessSquare target = b.getSquare(tx, ty);

        if (m.flag() == Move.PROMOTION) {
            // Tell the GUI Pawn to auto-promote instead of opening the dialog (see Step 3).
            Chess.Core.Pieces.Pawn.aiPromotionType = m.promo();
        }
        piece.move(target, true);   // castling & en-passant capture are handled by postTurnAction
        Chess.Core.Pieces.Pawn.aiPromotionType = 0;
        b.setLastMove(m.from(), m.to());
        b.nextTurn();
    }
}