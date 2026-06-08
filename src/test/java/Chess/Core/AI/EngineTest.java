package Chess.Core.AI;

import static org.junit.Assert.*;
import org.junit.Test;

import Chess.Core.AI.Util.Move;
import Chess.Core.AI.Util.MoveGen;
import Chess.Core.AI.Util.Position;

/**
 * Black-box tests for the search. The engine exposes only {@link Engine#findBestMove},
 * so every behaviour is checked through the move it returns for a hand-built position.
 *
 * Squares use the project convention: index = y*8 + x, y=0 is rank 8
 * (a8=0, h8=7, a1=56, h1=63). {@code idx("e4")} converts algebraic to that index.
 */
public class EngineTest {
    static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /** Comfortable budget: every test below is decided within depth 1-2. */
    private static final long BUDGET_MS = 200;

    @Test
    public void returnsALegalMoveFromTheStartPosition() {
        Position p = Position.fromFen(START);
        Move best = new Engine(p).findBestMove(BUDGET_MS);

        assertNotNull("engine should return a move when legal moves exist", best);
        assertTrue("returned move must be one of the legal moves",
                MoveGen.generateLegal(p).contains(best));
    }

    /** Re1-e8#: the black king on g8 is boxed in by its own f7/g7/h7 pawns. */
    @Test
    public void findsTheBackRankMateInOneForWhite() {
        Position p = Position.fromFen("6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1");
        Move best = new Engine(p).findBestMove(BUDGET_MS);

        assertEquals(idx("e1"), best.from());
        assertEquals(idx("e8"), best.to());
        assertCheckmateAfter(p, best);
    }

    /**
     * Mirror of the above with Black to move: ...Re8-e1#. Worth its own test because
     * the root score is side-to-move-relative, so this exercises the sign handling
     * for the side that isn't White.
     */
    @Test
    public void findsTheBackRankMateInOneForBlack() {
        Position p = Position.fromFen("4r1k1/8/8/8/8/8/5PPP/6K1 b - - 0 1");
        Move best = new Engine(p).findBestMove(BUDGET_MS);

        assertEquals(idx("e8"), best.from());
        assertEquals(idx("e1"), best.to());
        assertCheckmateAfter(p, best);
    }

    /** The black queen on d3 is undefended; Rd1xd3 wins it outright. */
    @Test
    public void grabsAHangingQueen() {
        Position p = Position.fromFen("4k3/8/8/8/8/3q4/8/3RK3 w - - 0 1");
        Move best = new Engine(p).findBestMove(BUDGET_MS);

        assertEquals(idx("d1"), best.from());
        assertEquals(idx("d3"), best.to());
    }

    /** No legal moves while in check: search has nothing to confirm and returns null. */
    @Test
    public void returnsNullWhenSideToMoveIsCheckmated() {
        // Black is mated: Qg7 is supported by Kg6 and every flight square is covered.
        Position p = Position.fromFen("6k1/6Q1/6K1/8/8/8/8/8 b - - 0 1");
        assertNull(new Engine(p).findBestMove(BUDGET_MS));
    }

    /** No legal moves and not in check: stalemate also yields null. */
    @Test
    public void returnsNullWhenSideToMoveIsStalemated() {
        Position p = Position.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        assertNull(new Engine(p).findBestMove(BUDGET_MS));
    }

    /**
     * Iterative deepening must keep the completed depth-1 result rather than discard
     * everything when the clock runs out mid-search, so even a 1 ms budget yields a move.
     */
    @Test
    public void returnsAMoveEvenUnderAOneMillisecondBudget() {
        Position p = Position.fromFen(START);
        Move best = new Engine(p).findBestMove(1);

        assertNotNull(best);
        assertTrue(MoveGen.generateLegal(p).contains(best));
    }

    /** Searching does not corrupt the position: it is restored byte-for-byte afterward. */
    @Test
    public void leavesThePositionUnchangedAfterSearching() {
        Position p = Position.fromFen(START);
        int[] before = p.board.clone();

        new Engine(p).findBestMove(BUDGET_MS);

        assertArrayEquals("board mutated by search", before, p.board);
        assertTrue("side to move flipped by search", p.whiteToMove);
        assertEquals("ep square changed by search", -1, p.epSquare);
    }

    /** The TT must not change which move a fixed-depth search picks on tactical
     *  positions: same best move as the known answers, just faster. */
    @Test
    public void transpositionTableKeepsFindingTheBackRankMate() {
        Position p = Position.fromFen("6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1");
        Move best = new Engine(p).findBestMove(BUDGET_MS);
        assertEquals(idx("e1"), best.from());
        assertEquals(idx("e8"), best.to());
        assertCheckmateAfter(p, best);
    }

    /** Searching the same engine instance twice (TT now warm) still returns a legal,
     *  position-preserving result. */
    @Test
    public void warmTableStillLeavesPositionUnchanged() {
        Position p = Position.fromFen(START);
        Engine engine = new Engine(p);
        engine.findBestMove(BUDGET_MS);
        int[] before = p.board.clone();
        Move best = engine.findBestMove(BUDGET_MS);
        assertNotNull(best);
        assertArrayEquals(before, p.board);
        assertTrue(p.whiteToMove);
    }

    /** Repetition detection must not invent phantom draws: a real mate is still found. */
    @Test
    public void repetitionLogicStillAllowsMate() {
        Position p = Position.fromFen("6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1");
        Move best = new Engine(p).findBestMove(BUDGET_MS);
        assertEquals(idx("e1"), best.from());
        assertEquals(idx("e8"), best.to());
        assertCheckmateAfter(p, best);
    }

    /** Seeding prior game keys is accepted and does not corrupt search. */
    @Test
    public void acceptsSeededGameHistory() {
        Position p = Position.fromFen(START);
        Engine engine = new Engine(p);
        engine.setGameHistory(new long[] { 1L, 2L, 3L });
        Move best = engine.findBestMove(BUDGET_MS);
        assertNotNull(best);
        assertTrue(MoveGen.generateLegal(p).contains(best));
    }

    /** Pruning must not break tactics: still grabs the hanging queen. */
    @Test
    public void pruningStillGrabsHangingQueen() {
        Position p = Position.fromFen("4k3/8/8/8/8/3q4/8/3RK3 w - - 0 1");
        Move best = new Engine(p).findBestMove(BUDGET_MS);
        assertEquals(idx("d1"), best.from());
        assertEquals(idx("d3"), best.to());
    }

    /** Both mate-in-one tests must still hold with pruning on. */
    @Test
    public void pruningStillFindsMateForBlack() {
        Position p = Position.fromFen("4r1k1/8/8/8/8/8/5PPP/6K1 b - - 0 1");
        Move best = new Engine(p).findBestMove(BUDGET_MS);
        assertEquals(idx("e8"), best.from());
        assertEquals(idx("e1"), best.to());
        assertCheckmateAfter(p, best);
    }

    /** The engine exposes the principal variation, and its first move is the move played. */
    @Test
    public void principalVariationStartsWithTheChosenMove() {
        Position p = Position.fromFen("6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1");
        Engine engine = new Engine(p);
        Move best = engine.findBestMove(BUDGET_MS);

        java.util.List<Move> pv = engine.getPrincipalVariation();
        assertFalse("PV should not be empty", pv.isEmpty());
        assertEquals("PV[0] must equal the returned best move", best, pv.get(0));
    }

    // --- helpers -----------------------------------------------------------

    /** Board index for an algebraic square like "e4" (index = y*8 + x, y=0 is rank 8). */
    private static int idx(String s) {
        int file = s.charAt(0) - 'a';
        int rank = s.charAt(1) - '1'; // 0 = rank 1
        return (7 - rank) * 8 + file; // rank 8 -> y = 0
    }

    /** Playing {@code m} leaves the opponent checkmated: in check with no legal reply. */
    private static void assertCheckmateAfter(Position p, Move m) {
        p.make(m);
        int kingSq = MoveGen.findKing(p, p.whiteToMove); // side to move is now the mated side
        boolean inCheck = MoveGen.isSquareAttacked(p, kingSq, !p.whiteToMove);

        assertTrue("expected the mated king to be in check", inCheck);
        assertTrue("expected no legal escapes", MoveGen.generateLegal(p).isEmpty());
    }
}
