package Chess.Core.AI;

import static org.junit.Assert.*;
import org.junit.Test;

import Chess.Core.AI.Util.Move;
import Chess.Core.AI.Util.Position;

public class PositionTest {
    static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Test
    public void parsesStartPosition() {
        Position p = Position.fromFen(START);
        assertTrue(p.whiteToMove);
        // White back rank (rank 1) is y=7: a1 rook = index 56
        assertEquals(Position.ROOK, p.board[56]);
        assertEquals(-Position.ROOK, p.board[0]); // a8 black rook
        assertEquals(Position.KING, p.board[60]); // e1 white king
        assertTrue(p.wK && p.wQ && p.bK && p.bQ);
        assertEquals(-1, p.epSquare);
        int pieces = 0;
        for (int sq = 0; sq < 64; sq++)
            if (p.board[sq] != 0)
                pieces++;
        assertEquals(32, pieces);
    }

    @Test
    public void makeUnmakeRestoresExactly() {
        Position p = Position.fromFen(START);
        int[] before = p.board.clone();
        // 1. e2-e4 : e2 is x=4,y=6 -> index 52 ; e4 is x=4,y=4 -> index 36
        Move e4 = new Move(52, 36, Move.DOUBLE_PUSH, 0);
        Position.Undo u = p.make(e4);
        assertEquals(44, p.epSquare); // skipped square e3 = x4,y5 = 5*8+4 = 44
        assertFalse(p.whiteToMove);
        p.unmake(e4, u);
        assertArrayEquals(before, p.board);
        assertTrue(p.whiteToMove);
        assertEquals(-1, p.epSquare);
    }

    // --- helpers -----------------------------------------------------------

    /** Board index for an algebraic square like "e4" (index = y*8 + x, y=0 is rank 8). */
    static int idx(String s) {
        int file = s.charAt(0) - 'a';
        int rank = s.charAt(1) - '1'; // 0 = rank 1
        return (7 - rank) * 8 + file; // rank 8 -> y = 0
    }

    /** make(m) then unmake(m) must restore the position byte-for-byte, including all state. */
    static void assertRoundTrips(Position p, Move m) {
        int[] board = p.board.clone();
        boolean stm = p.whiteToMove, wk = p.wK, wq = p.wQ, bk = p.bK, bq = p.bQ;
        int ep = p.epSquare, r50 = p.ruleOf50;

        Position.Undo u = p.make(m);
        p.unmake(m, u);

        assertArrayEquals("board not restored", board, p.board);
        assertEquals("whiteToMove", stm, p.whiteToMove);
        assertEquals("wK", wk, p.wK);
        assertEquals("wQ", wq, p.wQ);
        assertEquals("bK", bk, p.bK);
        assertEquals("bQ", bq, p.bQ);
        assertEquals("epSquare", ep, p.epSquare);
        assertEquals("ruleOf50", r50, p.ruleOf50);
    }

    // --- obscure make/unmake cases -----------------------------------------

    /** En passant: the captured pawn sits on a DIFFERENT square than `to`, so unmake
     * must put it back where it stood, not on the landing square. */
    @Test
    public void enPassantWhiteRemovesPawnBesideTarget() {
        Position p = Position.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        Move m = Move.special(idx("e5"), idx("d6"), Move.EN_PASSANT);

        assertRoundTrips(p, m); // restores the d5 pawn

        p.make(m);
        assertEquals(Position.PAWN, p.board[idx("d6")]);
        assertEquals(0, p.board[idx("e5")]);
        assertEquals(0, p.board[idx("d5")]); // captured pawn gone, off the `to` square
        assertFalse(p.whiteToMove);
    }

    /** Black en passant goes the other direction; the captured white pawn is one rank
     * up from the target square. */
    @Test
    public void enPassantBlackCapturesWhitePawn() {
        Position p = Position.fromFen("4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 1");
        Move m = Move.special(idx("d4"), idx("e3"), Move.EN_PASSANT);

        assertRoundTrips(p, m);

        p.make(m);
        assertEquals(-Position.PAWN, p.board[idx("e3")]);
        assertEquals(0, p.board[idx("d4")]);
        assertEquals(0, p.board[idx("e4")]); // white pawn captured
        assertTrue(p.whiteToMove);
    }

    /** White O-O moves the king two squares AND hops the h1 rook to f1; both rights drop. */
    @Test
    public void whiteKingsideCastleMovesRook() {
        Position p = Position.fromFen("4k3/8/8/8/8/8/8/4K2R w K - 0 1");
        Move m = Move.special(idx("e1"), idx("g1"), Move.CASTLE);

        assertRoundTrips(p, m); // rook returns to h1

        p.make(m);
        assertEquals(Position.KING, p.board[idx("g1")]);
        assertEquals(Position.ROOK, p.board[idx("f1")]);
        assertEquals(0, p.board[idx("h1")]);
        assertEquals(0, p.board[idx("e1")]);
        assertFalse(p.wK);
        assertFalse(p.wQ);
    }

    /** Black O-O-O sends the a8 rook all the way to d8. */
    @Test
    public void blackQueensideCastleMovesRook() {
        Position p = Position.fromFen("r3k3/8/8/8/8/8/8/4K3 b q - 0 1");
        Move m = Move.special(idx("e8"), idx("c8"), Move.CASTLE);

        assertRoundTrips(p, m);

        p.make(m);
        assertEquals(-Position.KING, p.board[idx("c8")]);
        assertEquals(-Position.ROOK, p.board[idx("d8")]);
        assertEquals(0, p.board[idx("a8")]);
        assertEquals(0, p.board[idx("e8")]);
        assertFalse(p.bK);
        assertFalse(p.bQ);
    }

    /** Promotion that is ALSO a capture: pawn takes the a8 rook and becomes a queen,
     * which additionally strips Black's queenside castling right. */
    @Test
    public void promotionCaptureBecomesQueenAndClearsRight() {
        Position p = Position.fromFen("r3k3/1P6/8/8/8/8/8/4K3 w q - 0 1");
        Move m = Move.promo(idx("b7"), idx("a8"), Position.QUEEN);

        assertRoundTrips(p, m); // pawn comes back, rook comes back, bQ comes back

        p.make(m);
        assertEquals(Position.QUEEN, p.board[idx("a8")]);
        assertEquals(0, p.board[idx("b7")]);
        assertFalse(p.bQ); // a8 rook captured
    }

    /** Underpromotion to a knight (not a queen) with no capture. */
    @Test
    public void underpromotionToKnight() {
        Position p = Position.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        Move m = Move.promo(idx("a7"), idx("a8"), Position.KNIGHT);

        assertRoundTrips(p, m);

        p.make(m);
        assertEquals(Position.KNIGHT, p.board[idx("a8")]);
        assertEquals(0, p.board[idx("a7")]);
        assertFalse(p.whiteToMove);
    }

    /** A plain capture of a rook still standing on its home square clears the matching
     * castling right via `capturedSquare`, not via the moved piece's `from`. */
    @Test
    public void capturingHomeRookClearsRight() {
        Position p = Position.fromFen("4k2r/6B1/8/8/8/8/8/4K3 w k - 0 1");
        Move m = Move.normal(idx("g7"), idx("h8"));

        assertRoundTrips(p, m); // bK restored on unmake

        p.make(m);
        assertEquals(Position.BISHOP, p.board[idx("h8")]);
        assertEquals(0, p.board[idx("g7")]);
        assertFalse(p.bK);
    }

    /** A capture resets the fifty-move clock to 0; unmake must restore the prior count. */
    @Test
    public void captureResetsHalfmoveClock() {
        Position p = Position.fromFen("4k3/8/4p3/8/3N4/8/8/4K3 w - - 17 1");
        Move m = Move.normal(idx("d4"), idx("e6")); // knight takes pawn

        assertRoundTrips(p, m); // ruleOf50 returns to 17

        p.make(m);
        assertEquals(Position.KNIGHT, p.board[idx("e6")]);
        assertEquals(0, p.board[idx("d4")]);
        assertEquals(0, p.ruleOf50);
    }

    /** A quiet non-pawn move bumps the fifty-move clock; a king step also clears both
     * of that side's castling rights. */
    @Test
    public void quietKingMoveIncrementsClockAndClearsBothRights() {
        Position p = Position.fromFen("4k3/8/8/8/8/8/8/R3K2R w KQ - 10 1");
        Move m = Move.normal(idx("e1"), idx("f1"));

        assertRoundTrips(p, m); // ruleOf50 -> 10, wK/wQ -> true

        p.make(m);
        assertEquals(Position.KING, p.board[idx("f1")]);
        assertEquals(0, p.board[idx("e1")]);
        assertEquals(11, p.ruleOf50);
        assertFalse(p.wK);
        assertFalse(p.wQ);
    }

    /** A double push overwrites an already-set en passant square; unmake must restore the
     * PREVIOUS ep square, not just clear it to -1. */
    @Test
    public void doublePushRestoresPriorEpSquare() {
        Position p = Position.fromFen("4k3/8/8/2p5/8/8/4P3/4K3 w - c6 0 1");
        assertEquals(idx("c6"), p.epSquare); // parsed prior ep
        Move m = Move.special(idx("e2"), idx("e4"), Move.DOUBLE_PUSH);

        assertRoundTrips(p, m); // epSquare returns to c6, not -1

        p.make(m);
        assertEquals(idx("e3"), p.epSquare); // new skipped square
        assertEquals(Position.PAWN, p.board[idx("e4")]);
        assertEquals(0, p.board[idx("e2")]);
    }

    // --- null move ---------------------------------------------------------

    /** A null move flips the side, clears ep, and round-trips key + state exactly. */
    @Test
    public void nullMoveRoundTrips() {
        Position p = Position.fromFen("4k3/8/8/2p5/8/8/4P3/4K3 w - c6 0 1");
        long keyBefore = p.key;
        boolean stm = p.whiteToMove;
        int ep = p.epSquare;

        Position.Undo u = p.makeNull();
        assertFalse("side must flip", p.whiteToMove == stm);
        assertEquals("ep must clear", -1, p.epSquare);
        assertEquals("key must match a recompute of the null position", p.recomputeFresh(), p.key);

        p.unmakeNull(u);
        assertEquals("key restored", keyBefore, p.key);
        assertEquals("ep restored", ep, p.epSquare);
        assertEquals("side restored", stm, p.whiteToMove);
    }

    @Test
    public void hasNonPawnMaterialDetectsPiecesByColor() {
        Position kp = Position.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"); // only kings + a pawn
        assertFalse("white has no non-pawn piece beyond the king", kp.hasNonPawnMaterial(true));
        assertFalse("black has nothing but the king", kp.hasNonPawnMaterial(false));

        Position withKnight = Position.fromFen("4k3/8/8/8/4N3/8/8/4K3 w - - 0 1");
        assertTrue(withKnight.hasNonPawnMaterial(true));
        assertFalse(withKnight.hasNonPawnMaterial(false));
    }
}