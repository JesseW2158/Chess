package Chess.Core.AI.Util;

import java.util.Random;

/**
 * Fixed random keys for Zobrist hashing, seeded so every run (and every test)
 * produces identical keys. PIECE is indexed [pieceIndex][square], where
 * pieceIndex packs type+color: (type-1) for white, (type-1)+6 for black.
 */
public final class Zobrist {
    private Zobrist() {}

    public static final long[][] PIECE = new long[12][64];
    public static final long SIDE;            // XOR into the key when black is to move
    public static final long[] CASTLE = new long[4]; // wK, wQ, bK, bQ
    public static final long[] EP_FILE = new long[8];

    static {
        Random r = new Random(0x9E3779B97F4A7C15L);
        for (int pc = 0; pc < 12; pc++)
            for (int sq = 0; sq < 64; sq++)
                PIECE[pc][sq] = r.nextLong();
        SIDE = r.nextLong();
        for (int i = 0; i < 4; i++) CASTLE[i] = r.nextLong();
        for (int i = 0; i < 8; i++) EP_FILE[i] = r.nextLong();
    }

    /** PIECE row for a signed board code (white > 0, black < 0, magnitude 1..6). */
    public static int pieceIndex(int code) {
        int type = Math.abs(code); // 1..6
        return (type - 1) + (code > 0 ? 0 : 6);
    }
}
