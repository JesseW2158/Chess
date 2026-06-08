package Chess.Core.AI;

import static org.junit.Assert.*;
import org.junit.Test;

import Chess.Core.AI.Util.Evaluation;
import Chess.Core.AI.Util.Position;

/**
 * Expected centipawn values here are worked out by hand from the algorithm in
 * {@link Evaluation} and its piece-square tables, NOT copied from the code's
 * output, so a regression in either the tables or the tapering math will trip a
 * test.
 *
 * Reminders used throughout:
 *   index = y*8 + x, y=0 is rank 8 (so a8=0, h8=7, a1=56, h1=63).
 *   VALUE   = {_, 100, 320, 330, 500, 900}  (pawn..queen, king 0)
 *   PHASE   = {_, 0, 1, 1, 2, 4, 0}         (full board = 24)
 *   eval    = (mg*phase + eg*(24-phase)) / 24, phase capped at 24.
 */
public class EvaluationTest {
    static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /** Nothing on the board: no material, phase 0, both halves zero. */
    @Test
    public void emptyBoardIsZero() {
        assertEquals(0, Evaluation.evaluate(Position.fromFen("8/8/8/8/8/8/8/8 w - - 0 1")));
    }

    /** The start position is a perfect colour mirror, so White's and Black's
     * contributions cancel to exactly zero. */
    @Test
    public void startPositionIsBalanced() {
        assertEquals(0, Evaluation.evaluate(Position.fromFen(START)));
    }

    /** Two lone kings sit on vertically-mirrored squares (e1 / e8), so their PST
     * terms cancel in both the middlegame and endgame halves. */
    @Test
    public void symmetricKingsAreZero() {
        assertEquals(0, Evaluation.evaluate(Position.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")));
    }

    /**
     * A single extra White pawn on e2 with only kings on the board.
     *
     * phase = 0 (pawns/kings carry no phase) so eval is pure endgame.
     *   pawn e2 (idx 52): VALUE 100 + PAWN_EG[52] 10            = 110
     *   king e1 (idx 60): KING_EG[60] -30   } these two cancel
     *   king e8 (idx 60): KING_EG[60] -30   } (mirrored to idx 60)
     * eval = 110.
     */
    @Test
    public void singleExtraPawnIsEndgameValue() {
        // 110 material/PST, minus 15 for the now-isolated e-pawn (no d/f neighbours).
        assertEquals(95, Evaluation.evaluate(Position.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1")));
    }

    /**
     * Forces the middlegame/endgame blend to actually run: a White queen (phase 4)
     * plus an e2 pawn whose mg and eg PST differ, kings cancelling.
     *
     *   mg = queen(900-5) + pawn(100-20)          = 975
     *   eg = queen(900-5) + pawn(100+10)          = 1005
     *   phase = 4  ->  (975*4 + 1005*20) / 24     = 24000/24 = 1000
     *
     * If the taper were wrong (e.g. used only mg or only eg) this would read
     * 975 or 1005, never 1000.
     */
    @Test
    public void taperedQueenAndPawnBlendsMgAndEg() {
        // 1000 tapered material/PST, minus 15 for the isolated e-pawn.
        assertEquals(985, Evaluation.evaluate(Position.fromFen("4k3/8/8/8/8/8/4P3/3QK3 w - - 0 1")));
    }

    /** The doc contract: eval is always from White's POV and ignores whose turn it
     * is. The same board with Black to move must score identically. */
    @Test
    public void evaluationIgnoresSideToMove() {
        String board = "4k3/8/8/8/8/8/4P3/3QK3";
        int whiteToMove = Evaluation.evaluate(Position.fromFen(board + " w - - 0 1"));
        int blackToMove = Evaluation.evaluate(Position.fromFen(board + " b - - 0 1"));
        assertEquals(whiteToMove, blackToMove);
    }

    /** A centred knight (e4, KNIGHT_PST +20) is worth more than one buried in the
     * corner (a1, KNIGHT_PST -50); kings cancel and the knight shares one table. */
    @Test
    public void centralKnightIsWorthMoreThanCornerKnight() {
        int central = Evaluation.evaluate(Position.fromFen("4k3/8/8/8/4N3/8/8/4K3 w - - 0 1"));
        int corner = Evaluation.evaluate(Position.fromFen("4k3/8/8/8/8/8/8/N3K3 w - - 0 1"));
        assertEquals(320 + 20, central); // 340
        assertEquals(320 - 50, corner); // 270
        assertTrue(central > corner);
    }

    /** Mirror invariant: flipping every piece's colour and rank must negate the
     * score exactly (Java truncates toward zero, so -(x/24) == (-x)/24). */
    @Test
    public void colorFlipNegatesEvaluation() {
        String[] fens = {
                START,
                "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", // kiwipete
                "4k3/8/8/8/8/8/4P3/3QK3 w - - 0 1", // queen + pawn (scores 1000)
                "4k3/8/8/8/4N3/8/8/4K3 w - - 0 1", // central knight
                "rnb1kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", // White up a queen
        };
        boolean sawNonZero = false;
        for (String fen : fens) {
            Position p = Position.fromFen(fen);
            int original = Evaluation.evaluate(p);
            int flipped = Evaluation.evaluate(colorFlip(p));
            assertEquals("flip of " + fen, -original, flipped);
            sawNonZero |= original != 0;
        }
        assertTrue("test would be vacuous if every position scored 0", sawNonZero);
    }

    /** Material shows up in the sign: removing a side's queen from the start
     * position swings the score toward the other side. */
    @Test
    public void missingQueenSwingsTheSign() {
        // start position with the black queen (d8) removed
        int whiteUp = Evaluation.evaluate(
                Position.fromFen("rnb1kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        // start position with the white queen (d1) removed
        int blackUp = Evaluation.evaluate(
                Position.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNB1KBNR w KQkq - 0 1"));
        assertTrue("White up a queen should score positive, was " + whiteUp, whiteUp > 0);
        assertTrue("Black up a queen should score negative, was " + blackUp, blackUp < 0);
    }

    /** Two bishops earn the pair bonus (+30) on top of the second bishop's value. */
    @Test
    public void bishopPairAddsThirtyOverASingleBishop() {
        int one = Evaluation.evaluate(Position.fromFen("4k3/1B6/8/8/8/8/8/4K3 w - - 0 1"));
        int two = Evaluation.evaluate(Position.fromFen("4k3/1B4B1/8/8/8/8/8/4K3 w - - 0 1"));
        assertEquals(330, one);          // lone bishop on b7 (PST 0), kings cancel
        assertEquals(690, two);          // 330+330 bishops + 30 pair bonus
    }

    /** Connected pawns keep full value; splitting them so each is isolated costs 15 apiece. */
    @Test
    public void isolatedPawnsArePenalized() {
        int connected = Evaluation.evaluate(Position.fromFen("4k3/8/8/8/8/8/3PP3/4K3 w - - 0 1"));
        int isolated = Evaluation.evaluate(Position.fromFen("4k3/8/8/8/8/8/3P1P2/4K3 w - - 0 1"));
        assertEquals(220, connected);    // d2+e2, neither isolated, both rank-2 passed (bonus 0)
        assertEquals(190, isolated);     // d2+f2, both isolated: 220 - 2*15
    }

    /** Doubling pawns on a file costs 15 per extra pawn. */
    @Test
    public void doubledPawnsArePenalized() {
        // a2+a4: 110+120 material, -15 doubled, -30 isolated (x2), +20 passed (a4 on rank4).
        assertEquals(205, Evaluation.evaluate(Position.fromFen("4k3/8/8/8/P7/8/P7/4K3 w - - 0 1")));
    }

    /** A passed pawn on the 5th rank earns its rank bonus (35) on top of value, less isolation. */
    @Test
    public void passedPawnGetsRankBonus() {
        // e5: 130 material/PST(eg), -15 isolated, +35 passed = 150.
        assertEquals(150, Evaluation.evaluate(Position.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1")));
    }

    /** A rook on a fully open file (no pawns of either colour) earns +20. */
    @Test
    public void rookOnOpenFileGetsBonus() {
        // a1 rook (PST 0) + open a-file bonus 20, kings cancel.
        assertEquals(520, Evaluation.evaluate(Position.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 0 1")));
    }

    // --- helpers -----------------------------------------------------------

    /** Swap every piece's colour and mirror it vertically (rank r -> rank 7-r).
     * Evaluation is colour-symmetric, so evaluate(colorFlip(p)) == -evaluate(p). */
    static Position colorFlip(Position p) {
        Position f = new Position();
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++)
                f.board[(7 - y) * 8 + x] = -p.board[y * 8 + x];
        return f;
    }
}
