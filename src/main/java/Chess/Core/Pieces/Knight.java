package Chess.Core.Pieces;

import java.util.ArrayList;
import java.util.List;

import Chess.Core.Util.ChessSquare;
import Chess.Core.Util.Color;

public class Knight extends Piece {
    public Knight(Color color, ChessSquare square) {
        super(color, "Knight", square);
    }

    @Override
	public List<ChessSquare> getAccessibleSquares() {
		List<ChessSquare> squares = new ArrayList<>();
		addSquares(x + 2, y + 1, squares);
		addSquares(x + 2, y - 1, squares);
		addSquares(x + 1, y + 2, squares);
		addSquares(x + 1, y - 2, squares);
		addSquares(x - 2, y + 1, squares);
		addSquares(x - 2, y - 1, squares);
		addSquares(x - 1, y + 2, squares);
		addSquares(x - 1, y - 2, squares);
		return squares;
	}

    private void addSquares(int x, int y, List<ChessSquare> squares) {
		ChessSquare square = this.square.getBoard().getSquare(x, y);
		if (square != null && (square.getPiece() == null || square.getPiece().color != color)) {
			squares.add(square);
		}
	}
}
