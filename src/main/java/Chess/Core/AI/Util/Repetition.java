package Chess.Core.AI.Util;

/**
 * Draw-by-repetition detection over a flat key history. Stateless and pure so it
 * can be unit-tested directly. Search treats the FIRST repetition as a draw: if a
 * position can be reached again, it can be reached a third time, so it is claimable.
 */
public final class Repetition {
    private Repetition() {}

    /**
     * True if {@code currentKey} appears anywhere in the last {@code ruleOf50}
     * entries of {@code history[0..len-1]}. Entries older than the fifty-move
     * window cannot legally repeat (an irreversible move changed material/pawns),
     * so they are skipped. The SIDE bit in the key already prevents matching a
     * same-board-but-other-side position, so a plain linear scan is correct.
     */
    public static boolean isDraw(long currentKey, long[] history, int len, int ruleOf50) {
        int limit = Math.min(len, ruleOf50);
        for (int i = len - 1; i >= len - limit && i >= 0; i--) {
            if (history[i] == currentKey) return true;
        }
        return false;
    }
}
