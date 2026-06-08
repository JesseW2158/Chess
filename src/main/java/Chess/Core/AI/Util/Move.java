package Chess.Core.AI.Util;

/**
 * An immutable move. `from`/`to` are 0..63 board indices (index = y*8 + x).
 * `flag` classifies special moves; `promo` is the promotion piece TYPE
 * (Position.QUEEN/ROOK/BISHOP/KNIGHT) and is 0 for non-promotions.
 *
 * A capture is NOT a flag — detect it from the board (board[to] != 0, or
 * EN_PASSANT).
 */
public record Move(int from, int to, int flag, int promo) {
    public static final int NORMAL = 0;
    public static final int DOUBLE_PUSH = 1; // a pawn's two-square advance (sets the ep square)
    public static final int EN_PASSANT = 2; // pawn captures the pawn that just double-pushed
    public static final int CASTLE = 3; // king's two-square move; side inferred from `to` file
    public static final int PROMOTION = 4; // `promo` holds the new piece type; may also be a capture

    public static Move normal(int from, int to) {
        return new Move(from, to, NORMAL, 0);
    }

    public static Move special(int from, int to, int f) {
        return new Move(from, to, f, 0);
    }

    public static Move promo(int from, int to, int t) {
        return new Move(from, to, PROMOTION, t);
    }

    /**
     * Algebraic like "e2e4" or "a7a8q", using this plan's coordinate convention.
     */
    @Override
    public String toString() {
        String s = "" + (char) ('a' + from % 8) + (char) ('8' - from / 8) + " -> " + (char) ('a' + to % 8)
                + (char) ('8' - to / 8);

        if (flag == PROMOTION) {
            s += "nbrq".charAt(promo - 2); // KNIGHT=2..QUEEN=5
        }

        return s;
    }
}