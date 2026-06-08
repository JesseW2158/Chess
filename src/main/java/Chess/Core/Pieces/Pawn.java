package Chess.Core.Pieces;

import java.util.ArrayList;
import java.util.List;

import Chess.Core.Util.ChessSquare;
import Chess.Core.Util.Color;
import Chess.Core.Util.UI.PromotionDialog;

public class Pawn extends Piece {
    public static int aiPromotionType = 0;

    public Pawn(Color color, ChessSquare square) {
        super(color, "Pawn", square);
    }

    // black: from top to bottom
    @Override
    public List<ChessSquare> getAccessibleSquares() {
        List<ChessSquare> squares = new ArrayList<>();

        ChessSquare square;

        if (color == Color.WHITE) {
            if ((square = this.square.getBoard().getSquare(x, y - 1)) != null && square.getPiece() == null) {
                squares.add(square);
                if (y == 6 && (square = this.square.getBoard().getSquare(x, y - 2)).getPiece() == null) {
                    squares.add(square);
                }
            }
            if (x + 1 < 8 && (square = this.square.getBoard().getSquare(x + 1, y - 1)) != null
                    && square.getPiece() != null
                    && square.getPiece().color != color) {
                squares.add(square);
            }
            if (x - 1 >= 0 && (square = this.square.getBoard().getSquare(x - 1, y - 1)) != null
                    && square.getPiece() != null
                    && square.getPiece().color != color) {
                squares.add(square);
            }
            if (y == 3) {
                if (x - 1 >= 0 && (square = this.square.getBoard().getSquare(x - 1, y)).getPiece() != null
                        && square.getPiece().color != color
                        && square.getPiece() instanceof Pawn
                        && square.getBoard().getCurrentTurn() - square.getPiece().getFirstTurn() == 1) {
                    squares.add(this.square.getBoard().getSquare(x - 1, y - 1));
                }
                if (x + 1 < 8 && (square = this.square.getBoard().getSquare(x + 1, y)).getPiece() != null
                        && square.getPiece().color != color
                        && square.getPiece() instanceof Pawn
                        && square.getBoard().getCurrentTurn() - square.getPiece().getFirstTurn() == 1) {
                    squares.add(this.square.getBoard().getSquare(x + 1, y - 1));
                }
            }
        } else {
            if ((square = this.square.getBoard().getSquare(x, y + 1)) != null && square.getPiece() == null) {
                squares.add(square);
                if (y == 1 && (square = this.square.getBoard().getSquare(x, y + 2)) != null
                        && square.getPiece() == null) {
                    squares.add(square);
                }
            }
            if (x + 1 < 8 && (square = this.square.getBoard().getSquare(x + 1, y + 1)) != null
                    && square.getPiece() != null
                    && square.getPiece().color != color) {
                squares.add(square);
            }
            if (x - 1 >= 0 && (square = this.square.getBoard().getSquare(x - 1, y + 1)) != null
                    && square.getPiece() != null
                    && square.getPiece().color != color) {
                squares.add(square);
            }
            if (y == 4) {
                if (x - 1 >= 0 && (square = this.square.getBoard().getSquare(x - 1, y)).getPiece() != null
                        && square.getPiece().color != color
                        && square.getPiece() instanceof Pawn
                        && square.getBoard().getCurrentTurn() - square.getPiece().getFirstTurn() == 1) {
                    squares.add(this.square.getBoard().getSquare(x - 1, y + 1));
                }
                if (x + 1 < 8 && (square = this.square.getBoard().getSquare(x + 1, y)).getPiece() != null
                        && square.getPiece().color != color
                        && square.getPiece() instanceof Pawn
                        && square.getBoard().getCurrentTurn() - square.getPiece().getFirstTurn() == 1) {
                    squares.add(this.square.getBoard().getSquare(x + 1, y + 1));
                }
            }
        }

        return squares;
    }

    @Override
    public List<ChessSquare> getAttackedSquares() {
        List<ChessSquare> squares = new ArrayList<>();
        int dy = (color == Color.WHITE) ? -1 : 1;
        ChessSquare sq;
        if (x + 1 < 8 && (sq = this.square.getBoard().getSquare(x + 1, y + dy)) != null)
            squares.add(sq);
        if (x - 1 >= 0 && (sq = this.square.getBoard().getSquare(x - 1, y + dy)) != null)
            squares.add(sq);
        return squares;
    }

    @Override
    public Piece postTurnAction(ChessSquare oldSquare, ChessSquare newSquare, boolean graphic) {
        // kill en passant pawn
        Piece pawn = null;
        if (color == Color.BLACK && square.getY() == 5
                && (pawn = square.getBoard().getSquare(x, 4).getPiece()) != null
                && pawn instanceof Pawn
                && square.getBoard().getCurrentTurn() - pawn.getFirstTurn() == 1) {
            pawn.getSquare().setPiece(null, graphic);
        }
        if (color == Color.WHITE && square.getY() == 2
                && (pawn = square.getBoard().getSquare(x, 3).getPiece()) != null
                && pawn instanceof Pawn
                && square.getBoard().getCurrentTurn() - pawn.getFirstTurn() == 1) {
            pawn.getSquare().setPiece(null, graphic);
        }
        if (graphic && (square.getY() == 0 || square.getY() == 7)) {
            if (aiPromotionType > 0) {
                String type = switch (aiPromotionType) {
                    case 2 -> "knight";
                    case 3 -> "bishop";
                    case 4 -> "rook";
                    default -> "queen";
                };
                Piece promoted = Chess.Core.Util.PieceFactory.createPiece(
                        color, type, getPos(), getFirstTurn());
                promoted.move(square, true);
            } else {
                java.util.Optional<Piece> result = new PromotionDialog(this).showAndWait();
                result.ifPresent(f -> f.move(square, true));
            }
        }
        return pawn; // keep the existing return value
    }
}
