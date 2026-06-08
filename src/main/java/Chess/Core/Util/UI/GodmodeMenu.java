package Chess.Core.Util.UI;

import java.util.HashMap;
import java.util.Map;

import Chess.Core.Pieces.Bishop;
import Chess.Core.Pieces.King;
import Chess.Core.Pieces.Knight;
import Chess.Core.Pieces.Pawn;
import Chess.Core.Pieces.Piece;
import Chess.Core.Pieces.Queen;
import Chess.Core.Pieces.Rook;
import Chess.Core.Util.ChessSquare;
import Chess.Core.Util.Color;
import Chess.Core.Util.IO.DataHelper;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class GodmodeMenu extends ContextMenu {

	private ChessSquare field;

	public GodmodeMenu(ChessSquare field) {
		this.field = field;
		getItems().add(new GodItem("Images/remove.png", "Remove Piece", e -> setPiece(null)));
		getItems().add(new GodItem("Images/white_queen.png", "Add White Queen", e -> setPiece(new Queen(Color.WHITE, field))));
		getItems().add(new GodItem("Images/white_bishop.png", "Add White Bishop", e -> setPiece(new Bishop(Color.WHITE, field))));
		getItems().add(new GodItem("Images/white_knight.png", "Add White Knight", e -> setPiece(new Knight(Color.WHITE, field))));
		getItems().add(new GodItem("Images/white_rook.png", "Add White Rook", e -> setPiece(new Rook(Color.WHITE, field))));
		getItems().add(new GodItem("Images/white_king.png", "Add White King", e -> setPiece(new King(Color.WHITE, field))));
		getItems().add(new GodItem("Images/white_pawn.png", "Add White Pawn", e -> setPiece(new Pawn(Color.WHITE, field))));
		getItems().add(new GodItem("Images/black_queen.png", "Add Black Queen", e -> setPiece(new Queen(Color.BLACK, field))));
		getItems().add(new GodItem("Images/black_bishop.png", "Add Black Bishop", e -> setPiece(new Bishop(Color.BLACK, field))));
		getItems().add(new GodItem("Images/black_knight.png", "Add Black Knight", e -> setPiece(new Knight(Color.BLACK, field))));
		getItems().add(new GodItem("Images/black_rook.png", "Add Black Rook", e -> setPiece(new Rook(Color.BLACK, field))));
		getItems().add(new GodItem("Images/black_king.png", "Add Black King", e -> setPiece(new King(Color.BLACK, field))));
		getItems().add(new GodItem("Images/black_pawn.png", "Add Black Pawn", e -> setPiece(new Pawn(Color.BLACK, field))));
	}

	private void setPiece(Piece piece) {
		field.setPiece(piece, true);
		field.getBoard().recalculateAttackedSquares();
		field.getBoard().gameStateTest();
	}

	private static class GodItem extends MenuItem {

		static Map<String, Image> cachedIcons = new HashMap<>();

		GodItem(String resourceIcon, String text, EventHandler<ActionEvent> event) {
			if (!cachedIcons.containsKey(resourceIcon)) {
				cachedIcons.put(resourceIcon, DataHelper.loadImage(resourceIcon, 16, 16));
			}
			setGraphic(new ImageView(cachedIcons.get(resourceIcon)));
			setText(text);
			setOnAction(event);
		}
	}
}

