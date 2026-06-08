package Chess.Core.AI;

import Chess.Core.AI.Util.Move;

/**
 * Fixed-size, power-of-two transposition table indexed by key & mask. Parallel
 * arrays keep memory tight. Replacement is depth-preferred within a search, with
 * stale entries from older searches always overwritten (tracked by generation).
 */
public final class TranspositionTable {
    public static final int EXACT = 0, LOWER = 1, UPPER = 2;

    public record Entry(int depth, int score, int flag, Move best) {}

    private final long[] keys;
    private final int[] depths;
    private final int[] scores;
    private final byte[] flags;
    private final Move[] best;
    private final int[] gens;
    private final int mask;
    private int generation; // 0 = "never written"; bumped by newSearch()

    /** sizePow2 = log2 of the entry count (e.g. 20 -> 1,048,576 entries). */
    public TranspositionTable(int sizePow2) {
        int size = 1 << sizePow2;
        keys = new long[size];
        depths = new int[size];
        scores = new int[size];
        flags = new byte[size];
        best = new Move[size];
        gens = new int[size];
        mask = size - 1;
    }

    /** Call once at the start of each root search so old entries age out. */
    public void newSearch() {
        generation++;
    }

    public Entry probe(long key) {
        int i = (int) (key & mask);
        if (gens[i] != 0 && keys[i] == key) {
            return new Entry(depths[i], scores[i], flags[i], best[i]);
        }
        return null;
    }

    public void store(long key, int depth, int score, int flag, Move bestMove) {
        int i = (int) (key & mask);

        // Keep a deeper entry that belongs to the CURRENT search; otherwise replace.
        if (gens[i] == generation && keys[i] == key && depths[i] > depth) {
            if (bestMove != null && best[i] == null) best[i] = bestMove;
            return;
        }

        keys[i] = key;
        depths[i] = depth;
        scores[i] = score;
        flags[i] = (byte) flag;
        best[i] = bestMove;
        gens[i] = generation;
    }
}
