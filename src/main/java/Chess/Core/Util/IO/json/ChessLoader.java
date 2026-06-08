package Chess.Core.Util.IO.json;

import com.google.gson.*;
import Chess.Core.Util.IO.ChessIO;
import Chess.Core.ChessBoard;
import Chess.Core.Pieces.Piece;
import Chess.Core.Util.Color;
import Chess.Core.Util.PieceFactory;

public class ChessLoader implements ChessIO {
	@Override
	public void load(byte[] json, ChessBoard board) {
		JsonObject element = JsonParser.parseString(new String(json)).getAsJsonObject();
		loadPieces(Color.BLACK, element.getAsJsonArray("black"), board);
		loadPieces(Color.WHITE, element.getAsJsonArray("white"), board);
		board.setCurrenTurn(element.get("current_turn").getAsInt());
		board.set50MoveRuleTurns(element.get("50_move_rule_turns").getAsInt());
	}

	private void loadPieces(Color color, JsonArray jsonPieces, ChessBoard board) {
		for (JsonElement jsonPiece : jsonPieces) {
			String type = jsonPiece.getAsJsonObject().get("type").getAsString();
			String pos = jsonPiece.getAsJsonObject().get("pos").getAsString();
			int firstTurn = jsonPiece.getAsJsonObject().get("first_turn").getAsInt();
			Piece figure = PieceFactory.createPiece(color, type, pos, firstTurn);
			board.setPiece(figure);
		}
	}

	@Override
	public byte[] save(ChessBoard board) {
		JsonObject main = new JsonObject();
		main.addProperty("current_turn", board.getCurrentTurn());
		main.addProperty("50_move_rule_turns", board.get50MoveRuleTurns());
		JsonArray black = new JsonArray();
		JsonArray white = new JsonArray();
		for (Piece figure : board.getPieces()) {
			JsonObject jsonPiece = new JsonObject();
			jsonPiece.addProperty("type", figure.getName());
			jsonPiece.addProperty("pos", figure.getPos());
			jsonPiece.addProperty("first_turn", figure.getFirstTurn());
			if (figure.getColor() == Color.BLACK) {
				black.add(jsonPiece);
			} else {
				white.add(jsonPiece);
			}
		}
		main.add("black", black);
		main.add("white", white);
		return new GsonBuilder().create().toJson(main).getBytes();
	}

	@Override
	public String getFileTypeDescription() {
		return "JSON files (*.json)";
	}

	@Override
	public String getFileExtension() {
		return "json";
	}
}

