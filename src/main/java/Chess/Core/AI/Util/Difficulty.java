package Chess.Core.AI.Util;

/**
 * AI difficulty levels. Each level is simply a thinking-time budget handed to
 * {@link Chess.Core.AI.Engine#findBestMove(long)}: more time means a deeper
 * search and a stronger move.
 */
public enum Difficulty {
    EASY(1_000),
    MEDIUM(2_000),
    HARD(5_000),
    IMPOSSIBLE(10_000);

    private final long budgetMillis;

    Difficulty(long budgetMillis) {
        this.budgetMillis = budgetMillis;
    }

    /** The engine's per-move thinking budget, in milliseconds. */
    public long budgetMillis() {
        return budgetMillis;
    }
}
