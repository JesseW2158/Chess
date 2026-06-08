package Chess.Core.Util;

import java.util.List;

import Chess.ChessGame;
import Chess.Core.ChessBoard;
import Chess.Core.Pieces.Pawn;
import Chess.Core.Pieces.Piece;
import Chess.Core.Util.UI.GodmodeMenu;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

public class ChessSquare extends Label {
    public Piece piece;

    private int x, y;
    private ChessBoard board;

    private static final String defaultStyleBlack = "-fx-background-color: gray;";
    private static final String defaultStyleWhite = "-fx-background-color: white;";
    private static final String highlightStyleBlack = "-fx-background-color: forestgreen;";
    private static final String highlightStyleWhite = "-fx-background-color: palegreen;";
    private static final String highlightKillStyleBlack = "-fx-background-color: darkred;";
    private static final String highlightKillStyleWhite = "-fx-background-color: #ff5555;";
    private static final String lastMoveStyleBlack = "-fx-background-color: #b9a44c;";
    private static final String lastMoveStyleWhite = "-fx-background-color: #f6eb91;";

    private static final int dragViewOffset;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            dragViewOffset = 25;
        } else {
            dragViewOffset = 0;
        }
    }

    public ChessSquare(ChessBoard board, int x, int y) {
        this.board = board;

        this.x = x;
        this.y = y;

        setAlignment(Pos.CENTER);

        resetBackgroundColor();

        setOnDragDetected(this::onDragDetected);
        setOnDragOver(this::onDragOver);

        setOnDragDropped(this::onDragDropped);
        setOnDragDone(this::onDragDone);

        setOnMouseEntered(e -> onMouseEntered());
        setOnMouseExited(e -> onMouseExited());

        if (ChessGame.isGodmode()) {
            setContextMenu(new GodmodeMenu(this));
        }

        setMinSize(75, 75);
        setMaxSize(75, 75);
    }

    private void setHighlightEmpty() {
        setStyle(getColor() == Color.BLACK ? highlightStyleBlack : highlightStyleWhite);
    }

    private void setHighlightKill() {
        setStyle(getColor() == Color.BLACK ? highlightKillStyleBlack : highlightKillStyleWhite);
    }

    public void resetBackgroundColor() {
        if (board.isLastMoveSquare(y * 8 + x)) {
            setStyle(getColor() == Color.BLACK ? lastMoveStyleBlack : lastMoveStyleWhite);
        } else {
            setStyle(getColor() == Color.BLACK ? defaultStyleBlack : defaultStyleWhite);
        }
    }

    private boolean isEnPassantSquare(Piece movingPiece) {
        Piece piece;

        return movingPiece instanceof Pawn && (y == 2 && (piece = board.getSquare(x, 3).getPiece()) instanceof Pawn
                && board.getCurrentTurn() - piece.getFirstTurn() == 1
                || y == 5 && (piece = board.getSquare(x, 4).getPiece()) instanceof Pawn
                        && board.getCurrentTurn() - piece.getFirstTurn() == 1);
    }

    private Color getColor() {
        return x % 2 == 1 && y % 2 == 1 || x % 2 == 0 && y % 2 == 0 ? Color.WHITE : Color.BLACK;
    }

    public ChessBoard getBoard() {
        return board;
    }

    public void setPiece(Piece piece, boolean graphic) {
        this.piece = piece;
        if (graphic) {
            if (piece == null) {
                setGraphic(null);
            } else {
                setGraphic(piece.getImageView());
            }
        }
    }

    public Piece getPiece() {
        return piece;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    private void onMouseEntered() {
        List<ChessSquare> trueSquares;
        if (piece != null && piece.canMove() && (trueSquares = piece.getAllAccessibleSquares()).size() > 0) {
            for (ChessSquare field : trueSquares) {
                if (field.piece != null || field.isEnPassantSquare(piece)) {
                    field.setHighlightKill();
                } else {
                    field.setHighlightEmpty();
                }
            }
        }
    }

    private void onMouseExited() {
        List<ChessSquare> trueSquares;
        if (piece != null && piece.canMove() && (trueSquares = piece.getAllAccessibleSquares()).size() > 0) {
            trueSquares.forEach(ChessSquare::resetBackgroundColor);
        }
    }

    private void onDragDetected(MouseEvent e) {
        List<ChessSquare> trueSquares;
        if (piece != null && piece.canMove() && (trueSquares = piece.getAllAccessibleSquares()).size() > 0) {
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            db.setDragView(piece.getImage());
            db.setDragViewOffsetX(dragViewOffset);
            db.setDragViewOffsetY(dragViewOffset);
            ClipboardContent content = new ClipboardContent();
            content.put(Piece.CHESS_FIGURE, piece);
            db.setContent(content);
            for (ChessSquare field : trueSquares) {
                if (field.piece != null || field.isEnPassantSquare(piece)) {
                    field.setHighlightKill();
                } else {
                    field.setHighlightEmpty();
                }
            }
            e.consume();
        }
    }

    private void onDragOver(DragEvent e) {
        if (e.getDragboard().hasContent(Piece.CHESS_FIGURE)) {
            e.acceptTransferModes(TransferMode.MOVE);
        }
        e.consume();
    }

    private void onDragDone(DragEvent e) {
        Dragboard db = e.getDragboard();
        if (db.hasContent(Piece.CHESS_FIGURE)) {
            Piece source = deserializePiece(db);
            // use the cloned instance from the Dragboard because we don't want to touch the
            // original instance anymore
            source.getAccessibleSquares().forEach(ChessSquare::resetBackgroundColor);
        }
        e.consume();
    }

    private void onDragDropped(DragEvent e) {
        Dragboard db = e.getDragboard();
        if (db.hasContent(Piece.CHESS_FIGURE)) {
            Piece source = deserializePiece(db);
            source = board.getSquare(source.getX(), source.getY()).getPiece();
            
            if (source.canMoveTo(this)) {
                int from = source.getY() * 8 + source.getX();

                getBoard().pushHistory();
                resetBackgroundColor();
                
                source.getAccessibleSquares().forEach(ChessSquare::resetBackgroundColor);
                source.move(this, true);
                
                getBoard().setLastMove(from, y * 8 + x);
                getBoard().nextTurn();
            }
        }
        e.consume();
    }

    // returns a NEW INSTANCE of the piece that was serialized on the Dragboard
    private Piece deserializePiece(Dragboard db) {
        Piece source = (Piece) db.getContent(Piece.CHESS_FIGURE);
        source.setSquare(ChessGame.getBoard().getSquare(source.getX(), source.getY()));
        return source;
    }

    @Override
    public String toString() {
        return "<" + x + "," + y + ">";
    }
}
