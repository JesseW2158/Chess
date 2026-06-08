package Chess.Core.Util.IO;

import Chess.Core.ChessBoard;

public interface ChessIO {
	void load(byte[] data, ChessBoard board);

	byte[] save(ChessBoard board);

	String getFileTypeDescription();

	String getFileExtension();
}
