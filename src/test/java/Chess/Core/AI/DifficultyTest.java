package Chess.Core.AI;

import static org.junit.Assert.*;
import org.junit.Test;

import Chess.Core.AI.Util.Difficulty;

/** Pins each difficulty level to its agreed thinking-time budget (in milliseconds). */
public class DifficultyTest {
    @Test
    public void budgetsMatchTheSpec() {
        assertEquals(1_000L, Difficulty.EASY.budgetMillis());
        assertEquals(2_000L, Difficulty.MEDIUM.budgetMillis());
        assertEquals(5_000L, Difficulty.HARD.budgetMillis());
        assertEquals(10_000L, Difficulty.IMPOSSIBLE.budgetMillis());
    }

    @Test
    public void budgetsRiseWithDifficulty() {
        Difficulty[] order = {Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.IMPOSSIBLE};
        for (int i = 1; i < order.length; i++) {
            assertTrue("each level should think longer than the one before",
                    order[i].budgetMillis() > order[i - 1].budgetMillis());
        }
    }
}
