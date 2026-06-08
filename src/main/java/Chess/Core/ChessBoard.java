package Chess.Core;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Chess.ChessGame;
import Chess.Core.AI.Engine;
import Chess.Core.AI.EngineBridge;
import Chess.Core.AI.Util.Difficulty;
import Chess.Core.AI.Util.Move;
import Chess.Core.AI.Util.Position;
import Chess.Core.Pieces.King;
import Chess.Core.Pieces.Piece;
import Chess.Core.Util.ChessSquare;
import Chess.Core.Util.Color;
import Chess.Core.Util.IO.ChessIO;
import Chess.Core.Util.IO.DataHelper;
import Chess.Core.Util.IO.json.ChessLoader;
import javafx.scene.layout.GridPane;

public class ChessBoard extends GridPane {
    private ChessSquare[] squares = new ChessSquare[64];
    private Map<Color, Set<ChessSquare>> attackedSquares = new HashMap<>();

    private int currentTurn = 1;
    private int ruleOf50 = 0;

    private Map<String, ChessIO> io = new LinkedHashMap<>();
    private Color aiColor = Color.BLACK; // null = no AI; set via a menu later
    private Difficulty difficulty = Difficulty.MEDIUM; // engine thinking-time budget

    private boolean aiThinking = false;
    private boolean gameOver = false;
    private boolean inCheck = false;

    private final List<Long> positionKeys = new ArrayList<>();
    private int lastMoveFrom = -1;
    private int lastMoveTo = -1;
    private final Deque<BoardSnapshot> history = new ArrayDeque<>();
    private final Chess.Core.Util.IO.json.ChessLoader snapshotIO = new ChessLoader();

    /**
     * Everything needed to rewind one position: board JSON, repetition-key count,
     * last-move squares.
     */
    private record BoardSnapshot(byte[] state, int keyCount, int lastFrom, int lastTo) {
    }

    public ChessBoard() {
        resetAttackedSquares();

        for (int i = 0; i < 64; i++) {
            int x = getX(i);
            int y = getY(i);
            ChessSquare square = new ChessSquare(this, x, y);
            add(square, x, y);
            squares[i] = square;
        }

        recalculateAttackedSquares();
    }

    private void resetAttackedSquares() {
        attackedSquares.put(Color.BLACK, new HashSet<>());
        attackedSquares.put(Color.WHITE, new HashSet<>());
    }

    private int getX(int index) {
        return index % 8;
    }

    private int getY(int index) {
        return (index - getX(index)) / 8;
    }

    public ChessSquare getSquare(int x, int y) {
        return x < 0 || x > 7 || y < 0 || y > 7 ? null : squares[y * 8 + x];
    }

    public void setPiece(Piece piece) {
        getSquare(piece.getX(), piece.getY()).setPiece(piece, true);
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrenTurn(int currentTurn) {
        this.currentTurn = currentTurn;
    }

    public int get50MoveRuleTurns() {
        return ruleOf50;
    }

    public void set50MoveRuleTurns(int turns) {
        this.ruleOf50 = turns;
    }

    public void nextTurn() {
        currentTurn++;
        ruleOf50++;
        recalculateAttackedSquares();
        pushPositionKey();
        gameStateTest();
        triggerAIMoveIfNeeded();
    }

    /**
     * Record the current position's Zobrist key for threefold-repetition tracking.
     */
    private void pushPositionKey() {
        positionKeys.add(EngineBridge.fromBoard(this).key);
    }

    /** Record the move just played and repaint the affected squares (old + new). */
    public void setLastMove(int from, int to) {
        int oldFrom = lastMoveFrom, oldTo = lastMoveTo;

        lastMoveFrom = from;
        lastMoveTo = to;

        repaintSquare(oldFrom);
        repaintSquare(oldTo);
        repaintSquare(from);
        repaintSquare(to);
    }

    public boolean isLastMoveSquare(int index) {
        return index >= 0 && (index == lastMoveFrom || index == lastMoveTo);
    }

    private void repaintSquare(int index) {
        if (index >= 0) {
            squares[index].resetBackgroundColor();
        }
    }

    /** Snapshot the CURRENT position (call this just before a move is applied). */
    public void pushHistory() {
        history.push(new BoardSnapshot(snapshotIO.save(this), positionKeys.size(), lastMoveFrom, lastMoveTo));
    }

    /** Take back to the previous human-to-move position (or one ply with no AI). */
    public void undo() {
        if (aiThinking || history.isEmpty()) {
            return;
        }
        do {
            restore(history.pop());
        } while (aiColor != null && getTurn() == aiColor && !history.isEmpty());

        gameOver = false;
        recalculateAttackedSquares();
        ChessGame.displayStatusText("");
        gameStateTest();
    }

    private void restore(BoardSnapshot snap) {
        for (ChessSquare square : squares) {
            square.setPiece(null, true);
        }

        snapshotIO.load(snap.state(), this); // restores pieces, current turn, 50-move counter

        while (positionKeys.size() > snap.keyCount()) {
            positionKeys.remove(positionKeys.size() - 1);
        }

        setLastMove(snap.lastFrom(), snap.lastTo());
    }

    public void setAiColor(Color c) {
        this.aiColor = c;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isAiThinking() {
        return aiThinking;
    }

    private void triggerAIMoveIfNeeded() {
        if (gameOver || aiColor == null || aiThinking || getTurn() != aiColor) {
            return;
        }

        aiThinking = true;
        ChessGame.displayStatusText("AI is thinking...");

        final Position snapshot = EngineBridge.fromBoard(this);
        final long[] seed = new long[positionKeys.size()];

        for (int i = 0; i < seed.length; i++) {
            seed[i] = positionKeys.get(i);
        }

        new Thread(() -> {
            Engine engine = new Engine(snapshot);
            engine.setGameHistory(seed);
            Move move = engine.findBestMove(difficulty.budgetMillis());
            javafx.application.Platform.runLater(() -> {
                if (move != null) {
                    EngineBridge.applyMove(this, move);
                    
                    if (!gameOver || !inCheck) {
                        ChessGame.displayStatusText("AI move: " + move);
                    }
                }
                aiThinking = false;
            });
        }, "chess-ai").start();
    }

    public Color getTurn() {
        return currentTurn % 2 == 0 ? Color.BLACK : Color.WHITE;
    }

    public void gameStateTest() {
        King king = getKing(getTurn());

        if (king != null) {
            if (king.isCheck()) {
                if (king.isCheckMate()) {
                    System.out.println("Check mate! " + king.getColor().revert().getFancyName() + " wins.");
                    ChessGame.displayStatusText("Check mate! " + king.getColor().revert().getFancyName() + " wins.");
                    gameOver = true;
                    return;
                } else {
                    ChessGame.displayStatusText("Check! " + king.getColor().getFancyName() + " has to defend.");
                    inCheck = true;
                    return;
                }
            } else if (king.isStaleMate()) {
                ChessGame.displayStatusText("Stalemate! " + king.getColor().getFancyName() + " can't move.");
                gameOver = true;
                return;
            }
        }

        if (!positionKeys.isEmpty()) {
            long cur = positionKeys.get(positionKeys.size() - 1);
            int count = 0;

            for (long k : positionKeys) {
                if (k == cur)
                    count++;
            }
            
            if (count >= 3) {
                ChessGame.displayStatusText("Draw by threefold repetition.");
                gameOver = true;
                return;
            }
        }

        if (ruleOf50 >= 100) {
            ChessGame.displayStatusText("50-move-rule applies");
            gameOver = true;
        }
    }

    public void recalculateAttackedSquares() {
        resetAttackedSquares();
        Arrays.stream(squares)
                .filter(f -> f.piece != null && f.piece.getColor() == Color.WHITE)
                .forEach(f -> attackedSquares.get(Color.WHITE).addAll(f.getPiece().getAttackedSquares()));
        Arrays.stream(squares)
                .filter(f -> f.piece != null && f.piece.getColor() == Color.BLACK)
                .forEach(f -> attackedSquares.get(Color.BLACK).addAll(f.getPiece().getAttackedSquares()));
    }

    public Set<ChessSquare> getAllAccessibleSquares(Color color) {
        return attackedSquares.get(color);
    }

    public King getKing(Color color) {
        for (ChessSquare square : squares) {
            if (square.piece instanceof King && square.piece.getColor() == color) {
                return (King) square.piece;
            }
        }
        return null;
    }

    public List<Piece> getPieces() {
        return getPieces(null);
    }

    public List<Piece> getPieces(Color color) {
        List<Piece> pieces = new ArrayList<>();
        Arrays.stream(squares)
                .filter(f -> f.piece != null && (color == null || f.piece.getColor() == color))
                .forEach(f -> pieces.add(f.piece));
        return pieces;
    }

    public void clear() {
        for (ChessSquare square : squares) {
            square.setPiece(null, true);
        }

        recalculateAttackedSquares();
        currentTurn = 1;
        ruleOf50 = 0;
        gameOver = false;
        positionKeys.clear();
        setLastMove(-1, -1);
        ChessGame.displayStatusText("");
    }

    public void setIO(ChessIO io) {
        this.io.put(io.getFileExtension(), io);
    }

    public Map<String, ChessIO> getIO() {
        return io;
    }

    public void loadFromResource(String resource) {
        load(getFileExtension(resource), DataHelper.loadDataFromResource(resource));
    }

    public void load(File file) {
        load(getFileExtension(file.getName()), DataHelper.loadDataFromFile(file));
    }

    private String getFileExtension(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private void load(String type, byte[] s) {
        clear();
        io.get(type).load(s, this);
        recalculateAttackedSquares();
        gameStateTest();
        pushPositionKey();
    }

    public void save(File file) {
        byte[] s = io.get(getFileExtension(file.getName())).save(this);
        DataHelper.saveDataToFile(s, file);
    }
}
