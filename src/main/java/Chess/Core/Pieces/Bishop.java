package Chess.Core.Pieces;

import java.util.ArrayList;
import java.util.List;

import Chess.Core.Util.ChessSquare;
import Chess.Core.Util.Color;

public class Bishop extends Piece {
    public Bishop(Color color, ChessSquare square) {
        super(color, "Bishop", square);
    }

    @Override
    public List<ChessSquare> getAccessibleSquares() {
        List<ChessSquare> pieces = new ArrayList<>();
        for (int i = 1; x + i < 8 && y + i < 8; i++)
            if (addSquare(x + i, y + i, pieces))
                break;
        for (int i = 1; x + i < 8 && y - i >= 0; i++)
            if (addSquare(x + i, y - i, pieces))
                break;
        for (int i = 1; x - i >= 0 && y + i < 8; i++)
            if (addSquare(x - i, y + i, pieces))
                break;
        for (int i = 1; x - i >= 0 && y - i >= 0; i++)
            if (addSquare(x - i, y - i, pieces))
                break;
        return pieces;
    }

    private boolean addSquare(int x, int y, List<ChessSquare> pieces) {
        ChessSquare piece = this.square.getBoard().getSquare(x, y);
        if (piece != null) {
            if (piece.getPiece() == null) {
                pieces.add(piece);
                return false;
            } else if (piece.getPiece().color != color) {
                pieces.add(piece);
            }
        }
        return true;
    }
}
