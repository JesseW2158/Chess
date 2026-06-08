package Chess.Core.Pieces;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import Chess.Core.Util.ChessSquare;
import Chess.Core.Util.Color;

public class King extends Piece {
    public King(Color color, ChessSquare square) {
        super(color, "king", square);
    }

    @Override
	public List<ChessSquare> getAccessibleSquares() {
		List<ChessSquare> squares = new ArrayList<>();
		Set<ChessSquare> attackedSquares = square.getBoard().getAllAccessibleSquares(color.revert());
		//check for squares that are not attacked and not occupied by own pieces or enemy king
		for (int kx = -1; kx <= 1; kx++) {
			for (int ky = -1; ky <= 1; ky++) {
				ChessSquare f;
				if ((f = square.getBoard().getSquare(square.getX() + kx, square.getY() + ky)) != null
						&& f != square
						&& !attackedSquares.contains(f)
						&& (f.getPiece() == null
						|| f.getPiece() != null && f.getPiece().color != color)) {
					squares.add(f);
				}
			}
		}
		//check for castling (only from the king's home file; during AI search a
		//simulated king never has its firstTurn stamped, so without the x == 4 guard
		//a king already on g/c-file would "castle" onto its own square and be deleted)
		if (getFirstTurn() < 0 && x == 4) {
			ChessSquare rookSquare = square.getBoard().getSquare(7, y);
			if (rookSquare.getPiece() instanceof Rook && rookSquare.getPiece().getFirstTurn() < 0) {
				//check right castling
				boolean right = true;
				for (int i = x + 1; i < 7; i++) {
					ChessSquare current = square.getBoard().getSquare(i, y);
					if (current.getPiece() != null || attackedSquares.contains(current)) {
						right = false;
						break;
					}
				}
				if (right) {
					squares.add(square.getBoard().getSquare(6, y));
				}
			}
			rookSquare = square.getBoard().getSquare(0, y);
			if (rookSquare.getPiece() instanceof Rook && rookSquare.getPiece().getFirstTurn() < 0) {
				//check left castling
				boolean left = true;
				for (int i = x - 1; i > 0; i--) {
					ChessSquare current = square.getBoard().getSquare(i, y);
					if (current.getPiece() != null || i > 1 && attackedSquares.contains(current)) {
						left = false;
						break;
					}
				}
				if (left) {
					squares.add(square.getBoard().getSquare(2, y));
				}
			}
		}
		return squares;
	}

	public boolean isCheck() {
		return square.getBoard().getAllAccessibleSquares(color.revert()).contains(square);
	}

	public boolean isCheckMate() {
		if (isCheck()) {
			for (Piece piece : square.getBoard().getPieces(color)) {
				if (piece.getAllAccessibleSquares().size() > 0) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isStaleMate() {
		if (!isCheck()) {
			for (Piece piece : square.getBoard().getPieces(color)) {
				if (piece.getAllAccessibleSquares().size() > 0) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Piece postTurnAction(ChessSquare oldSquare, ChessSquare newSquare, boolean graphic) {
		if (graphic && getFirstTurn() < 0) {
			if (x == 2) {
				square.getBoard().getSquare(0, y).getPiece().move(square.getBoard().getSquare(3, y), true);
			} else if (x == 6) {
				square.getBoard().getSquare(7, y).getPiece().move(square.getBoard().getSquare(5, y), true);
			}
		}
		return null;
	}
}
