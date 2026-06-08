package Chess.Core.AI;

import static org.junit.Assert.*;
import org.junit.Test;

import Chess.Core.AI.Util.Repetition;

public class RepetitionTest {

    @Test
    public void repeatWithinWindowIsDraw() {
        long[] h = { 10L, 20L, 30L, 40L };
        // current key 20 already appears at index 1, ruleOf50 large enough to see it
        assertTrue(Repetition.isDraw(20L, h, 4, 50));
    }

    @Test
    public void noRepeatIsNotDraw() {
        long[] h = { 10L, 20L, 30L, 40L };
        assertFalse(Repetition.isDraw(99L, h, 4, 50));
    }

    @Test
    public void repeatOutsideTheFiftyMoveWindowIsIgnored() {
        long[] h = { 7L, 1L, 2L, 3L, 4L }; // the 7 is 5 plies back
        // only the last 3 entries are reversible, so the old 7 must not count
        assertFalse(Repetition.isDraw(7L, h, 5, 3));
    }

    @Test
    public void emptyHistoryIsNotDraw() {
        assertFalse(Repetition.isDraw(5L, new long[0], 0, 50));
    }
}
