package Chess.Core.Pieces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Chess.ChessGame;
import Chess.Core.ChessBoard;
import Chess.Core.Util.ChessSquare;
import Chess.Core.Util.Color;
import Chess.Core.Util.IO.DataHelper;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;

public abstract class Piece implements Serializable {
    public transient static final DataFormat CHESS_FIGURE = new DataFormat("chess.piece");
    private transient static Map<String, Image> imageCache = new HashMap<>();

    public transient ChessSquare square;

    public int x, y = -1;
    public Color color;

    private String name;
    private String imageFileName;
    private int firstTurn = -1;

    public Piece(Color color, String name, ChessSquare square) {
        this.color = color;
        this.name = name;

        setSquare(square);

        imageFileName = "Images/" + color.getName() + "_" + name + ".png";

        if (!imageCache.containsKey(imageFileName)) {
            imageCache.put(imageFileName, DataHelper.loadImage(imageFileName, 75, 75));
        }

        if (square != null) {
            this.x = square.getX();
            this.y = square.getY();
            square.setPiece(this, true);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Color getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public String getPos() {
        return ((char) (x + 97)) + "" + ((char) (7 - y + 49));
    }

    // returns whether the Piece was successfully moved
    public Piece move(ChessSquare square, boolean graphic) {
        Piece killedPiece = square.getPiece();
        square.setPiece(this, graphic);
        // never clear the destination when it is also the origin, otherwise a
        // move onto the piece's own square would delete the piece from the board
        if (this.square != null && this.square != square) {
            this.square.setPiece(null, graphic);
        }
        int oldX = x;
        int oldY = y;
        x = square.getX();
        y = square.getY();
        setSquare(square);
        Piece postPiece = postTurnAction(ChessGame.getBoard().getSquare(oldX, oldY), square, graphic);
        if (graphic && firstTurn < 0) {
            firstTurn = square.getBoard().getCurrentTurn();
        }
        if (graphic && (killedPiece != null || !(postPiece instanceof Pawn))) {
            ChessGame.getBoard().set50MoveRuleTurns(0);
        }
        return killedPiece == null ? postPiece : killedPiece;
    }

    public boolean canMoveTo(ChessSquare square) {
        return canMove() && getAllAccessibleSquares().contains(square);
    }

    public boolean canMove() {
        ChessBoard board = getSquare().getBoard();
        return !board.isAiThinking() && board.getTurn() == color;
    }

    public void setSquare(ChessSquare square) {
        this.square = square;
    }

    public ChessSquare getSquare() {
        return square;
    }

    public ImageView getImageView() {
        return new ImageView(imageCache.get(imageFileName));
    }

    public Image getImage() {
        return imageCache.get(imageFileName);
    }

    // executed after this Piece has been moved to the parameter square
    public Piece postTurnAction(ChessSquare oldSquare, ChessSquare newSquare, boolean graphic) {
        return null;
    }

    // this also considers the king being in check
    public List<ChessSquare> getAllAccessibleSquares() {
        List<ChessSquare> squares = getAccessibleSquares();
        King king = square.getBoard().getKing(color);
        if (king != null) {
            List<ChessSquare> trueSquares = new ArrayList<>();
            ChessSquare oldSquare = square;
            for (ChessSquare to : squares) {
                // simulate move to that square
                Piece killedPiece = move(to, false);
                square.getBoard().recalculateAttackedSquares();
                boolean check = king.isCheck();
                if (!check) {
                    trueSquares.add(to);
                }
                // revert move
                move(oldSquare, false);
                if (killedPiece != null) {
                    killedPiece.square.setPiece(killedPiece, false);
                }
                square.getBoard().recalculateAttackedSquares();
            }
            squares = trueSquares;
        }
        return squares;
    }

    public int getFirstTurn() {
        return firstTurn;
    }

    public void setFirstTurn(int firstTurn) {
        this.firstTurn = firstTurn;
    }

    // squares this piece controls for attack purposes (may differ from move squares)
    public List<ChessSquare> getAttackedSquares() {
        return getAccessibleSquares();
    }

    // returns a list of all accessible squares of this piece, including squares
    // with
    // opponent's pieces
    // that can be beaten
    public abstract List<ChessSquare> getAccessibleSquares();

    @Override
    public String toString() {
        return "Piece: <" + color.getName() + " " + name + " " + "x=" + x + " y=" + y + " Square = " + square + ">";
    }
}