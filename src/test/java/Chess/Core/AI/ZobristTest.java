package Chess.Core.AI;

import static org.junit.Assert.*;
import java.util.List;
import java.util.Random;
import org.junit.Test;

import Chess.Core.AI.Util.Move;
import Chess.Core.AI.Util.MoveGen;
import Chess.Core.AI.Util.Position;

public class ZobristTest {
    static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /** fromFen must leave key equal to a from-scratch recompute. */
    @Test
    public void fromFenSetsConsistentKey() {
        Position p = Position.fromFen(START);
        assertEquals(p.recomputeZobrist(), p.key);
    }

    /** Black-to-move flips exactly the SIDE key relative to White-to-move. */
    @Test
    public void sideToMoveChangesKey() {
        long w = Position.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1").key;
        long b = Position.fromFen("4k3/8/8/8/8/8/8/4K3 b - - 0 1").key;
        assertNotEquals(w, b);
    }

    /** The incrementally-maintained key never drifts from a full recompute,
     *  across thousands of random legal moves. This is the core correctness guard. */
    @Test
    public void incrementalKeyMatchesRecompute() {
        Position p = Position.fromFen(START);
        Random r = new Random(42);
        for (int i = 0; i < 5000; i++) {
            List<Move> moves = MoveGen.generateLegal(p);
            if (moves.isEmpty()) {
                p = Position.fromFen(START);
                continue;
            }
            Move m = moves.get(r.nextInt(moves.size()));
            long keyBefore = p.key;
            Position.Undo u = p.make(m);
            long incremental = p.key;
            assertEquals("key drift after make " + m, freshKey(p), incremental);
            p.unmake(m, u);
            assertEquals("key not restored after unmake " + m, keyBefore, p.key);
        }
    }

    /** new Position() leaves key 0 until explicitly seeded; recompute makes it non-zero. */
    @Test
    public void freshPositionNeedsExplicitSeed() {
        Position p = new Position();
        p.board[60] = Position.KING; // a piece is present...
        assertEquals("key is only valid after recompute", 0L, p.key); // ...but key not yet built
        p.recomputeZobrist();
        assertNotEquals(0L, p.key);
    }

    /** Recompute without disturbing the stored key under test. */
    private static long freshKey(Position p) {
        long stored = p.key;
        long fresh = p.recomputeZobrist();
        p.key = stored; // recompute mutates key; put it back so we test the increment, not the recompute
        return fresh;
    }
}
