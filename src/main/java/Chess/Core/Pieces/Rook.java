package Chess.Core.Pieces;

import java.util.ArrayList;
import java.util.List;

import Chess.Core.Util.ChessSquare;
import Chess.Core.Util.Color;

public class Rook extends Piece {
	public Rook(Color color, ChessSquare square) {
		super(color, "rook", square);
	}

	@Override
	public List<ChessSquare> getAccessibleSquares() {
		List<ChessSquare> squares = new ArrayList<>();
		for (int i = 1; x + i < 8; i++)
			if (addSquare(x + i, y, squares))
				break;
		for (int i = 1; x - i >= 0; i++)
			if (addSquare(x - i, y, squares))
				break;
		for (int i = 1; y + i < 8; i++)
			if (addSquare(x, y + i, squares))
				break;
		for (int i = 1; y - i >= 0; i++)
			if (addSquare(x, y - i, squares))
				break;
		return squares;
	}

	private boolean addSquare(int x, int y, List<ChessSquare> squares) {
		ChessSquare square = this.square.getBoard().getSquare(x, y);
		if (square.getPiece() == null) {
			squares.add(square);
			return false;
		} else if (square.getPiece().color != color) {
			squares.add(square);
		}
		return true;
	}
}
