package Chess.Core.AI.Util;

import static Chess.Core.AI.Util.Position.*;

import java.util.ArrayList;
import java.util.List;

/** Stateless move generation and attack queries. NO JavaFX. */
public final class MoveGen {
    private MoveGen() {
    }

    private static final int[][] KNIGHT_DIR = {
            { 1, 2 }, { 2, 1 }, { 2, -1 }, { 1, -2 }, { -1, -2 }, { -2, -1 }, { -2, 1 }, { -1, 2 }
    };
    private static final int[][] BISHOP_DIR = { { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
    private static final int[][] ROOK_DIR = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
    private static final int[][] KING_DIR = {
            { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 }
    };

    private static boolean inBounds(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    // --- legal moves -------------------------------------------------------

    public static List<Move> generateLegal(Position p) {
        List<Move> pseudo = generatePseudoLegal(p);
        List<Move> legal = new ArrayList<>(pseudo.size());
        boolean mover = p.whiteToMove;

        for (Move m : pseudo) {
            Position.Undo u = p.make(m);

            int kingSq = findKing(p, mover);

            if (!isSquareAttacked(p, kingSq, !mover)) {
                legal.add(m);
            }

            p.unmake(m, u);
        }

        return legal;
    }

    /**
     * Captures, en passant, and ALL promotions only (used by quiescence). Legal.
     */
    public static List<Move> generateLegalCaptures(Position p) {
        List<Move> legal = new ArrayList<>();
        boolean mover = p.whiteToMove;

        for (Move m : generatePseudoLegal(p)) {
            boolean isCapture = p.board[m.to()] != 0 || m.flag() == Move.EN_PASSANT || m.flag() == Move.PROMOTION;

            if (!isCapture) {
                continue;
            }

            Position.Undo u = p.make(m);
            int kingSq = findKing(p, mover);

            if (!isSquareAttacked(p, kingSq, !mover)) {
                legal.add(m);
            }

            p.unmake(m, u);
        }

        return legal;
    }

    public static int findKing(Position p, boolean white) {
        int target = white ? KING : -KING;

        for (int sq = 0; sq < 64; sq++) {
            if (p.board[sq] == target) {
                return sq;
            }
        }

        return -1; // only happens in test positions without a king
    }

    // --- pseudo-legal ------------------------------------------------------

    public static List<Move> generatePseudoLegal(Position p) {
        List<Move> moves = new ArrayList<>(48);
        boolean white = p.whiteToMove;

        for (int sq = 0; sq < 64; sq++) {
            int pc = p.board[sq];

            if (pc == 0 || (pc > 0) != white) {
                continue;
            }

            int x = sq % 8, y = sq / 8;

            switch (Math.abs(pc)) {
                case PAWN -> genPawn(p, sq, x, y, white, moves);
                case KNIGHT -> genStep(p, sq, x, y, white, KNIGHT_DIR, moves);
                case BISHOP -> genSlide(p, sq, x, y, white, BISHOP_DIR, moves);
                case ROOK -> genSlide(p, sq, x, y, white, ROOK_DIR, moves);
                case QUEEN -> {
                    genSlide(p, sq, x, y, white, BISHOP_DIR, moves);
                    genSlide(p, sq, x, y, white, ROOK_DIR, moves);
                }
                case KING -> {
                    genStep(p, sq, x, y, white, KING_DIR, moves);
                    genCastle(p, sq, y, white, moves);
                }
            }
        }

        return moves;
    }

    private static void genStep(Position p, int from, int x, int y, boolean white, int[][] dirs, List<Move> out) {
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];

            if (!inBounds(nx, ny)) {
                continue;
            }

            int t = ny * 8 + nx, tp = p.board[t];

            if (tp == 0 || (tp > 0) != white) {
                out.add(Move.normal(from, t));
            }
        }
    }

    private static void genSlide(Position p, int from, int x, int y, boolean white, int[][] dirs, List<Move> out) {
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];

            while (inBounds(nx, ny)) {
                int t = ny * 8 + nx, tp = p.board[t];

                if (tp == 0) {
                    out.add(Move.normal(from, t));
                } else {
                    if ((tp > 0) != white) {
                        out.add(Move.normal(from, t));
                    }

                    break;
                }

                nx += d[0];
                ny += d[1];
            }
        }
    }

    private static void genPawn(Position p, int from, int x, int y, boolean white, List<Move> out) {
        int dir = white ? -1 : 1; // forward in y
        int startRank = white ? 6 : 1;
        int promoRank = white ? 0 : 7; // y the pawn lands on to promote
        int ny = y + dir;

        // single + double push
        if (inBounds(x, ny) && p.board[ny * 8 + x] == 0) {
            int t = ny * 8 + x;

            if (ny == promoRank) {
                addPromotions(from, t, out);
            } else {
                out.add(Move.normal(from, t));

                int ny2 = y + 2 * dir;

                if (y == startRank && p.board[ny2 * 8 + x] == 0) {
                    out.add(Move.special(from, ny2 * 8 + x, Move.DOUBLE_PUSH));
                }
            }
        }
        // captures (incl. promotion captures) and en passant
        for (int dx = -1; dx <= 1; dx += 2) {
            int nx = x + dx;

            if (!inBounds(nx, ny)) {
                continue;
            }

            int t = ny * 8 + nx, tp = p.board[t];

            if (tp != 0 && (tp > 0) != white) {
                if (ny == promoRank) {
                    addPromotions(from, t, out);
                } else {
                    out.add(Move.normal(from, t));
                }
            } else if (t == p.epSquare && p.epSquare != -1) {
                out.add(Move.special(from, t, Move.EN_PASSANT));
            }
        }
    }

    private static void addPromotions(int from, int to, List<Move> out) {
        out.add(Move.promo(from, to, QUEEN));
        out.add(Move.promo(from, to, ROOK));
        out.add(Move.promo(from, to, BISHOP));
        out.add(Move.promo(from, to, KNIGHT));
    }

    private static void genCastle(Position p, int from, int y, boolean white, List<Move> out) {
        // Only from the king's home square, never while in check.
        int home = white ? 60 : 4;

        if (from != home) {
            return;
        }

        boolean kRight = white ? p.wK : p.bK;
        boolean qRight = white ? p.wQ : p.bQ;
        boolean opp = !white;

        if (isSquareAttacked(p, home, opp)) {
            return; // can't castle out of check
        }

        if (kRight && p.board[y * 8 + 5] == 0 && p.board[y * 8 + 6] == 0 && !isSquareAttacked(p, y * 8 + 5, opp)
                && !isSquareAttacked(p, y * 8 + 6, opp)) {
            out.add(Move.special(from, y * 8 + 6, Move.CASTLE));
        }

        if (qRight && p.board[y * 8 + 1] == 0 && p.board[y * 8 + 2] == 0 && p.board[y * 8 + 3] == 0
                && !isSquareAttacked(p, y * 8 + 3, opp) && !isSquareAttacked(p, y * 8 + 2, opp)) {
            out.add(Move.special(from, y * 8 + 2, Move.CASTLE));
        }
    }

    // --- attack query (early exit) -----------------------------------------

    /**
     * Is `sq` attacked by side `byWhite`? Scans outward from sq, returns on first
     * hit.
     */
    public static boolean isSquareAttacked(Position p, int sq, boolean byWhite) {
        int x = sq % 8, y = sq / 8;
        int sign = byWhite ? 1 : -1;
        int pr = byWhite ? y + 1 : y - 1; // pawns: a white attacker sits one rank toward larger y (it captures upward)

        for (int dx = -1; dx <= 1; dx += 2) {
            int px = x + dx;

            if (inBounds(px, pr) && p.board[pr * 8 + px] == sign * PAWN) {
                return true;
            }
        }
        // knights
        for (int[] d : KNIGHT_DIR) {
            int nx = x + d[0], ny = y + d[1];

            if (inBounds(nx, ny) && p.board[ny * 8 + nx] == sign * KNIGHT) {
                return true;
            }
        }
        // king
        for (int[] d : KING_DIR) {
            int nx = x + d[0], ny = y + d[1];

            if (inBounds(nx, ny) && p.board[ny * 8 + nx] == sign * KING) {
                return true;
            }
        }
        // diagonal sliders (bishop/queen)
        if (rayHits(p, x, y, BISHOP_DIR, sign * BISHOP, sign * QUEEN)) {
            return true;
        }
        // orthogonal sliders (rook/queen)
        if (rayHits(p, x, y, ROOK_DIR, sign * ROOK, sign * QUEEN)) {
            return true;
        }

        return false;
    }

    private static boolean rayHits(Position p, int x, int y, int[][] dirs, int slider, int queen) {
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];

            while (inBounds(nx, ny)) {
                int pc = p.board[ny * 8 + nx];

                if (pc != 0) {
                    if (pc == slider || pc == queen) {
                        return true;
                    }

                    break;
                }

                nx += d[0];
                ny += d[1];
            }
        }

        return false;
    }

    // --- perft (test/debug only) -------------------------------------------

    public static long perft(Position p, int depth) {
        if (depth == 0) {
            return 1;
        }

        long nodes = 0;

        for (Move m : generateLegal(p)) {
            Position.Undo u = p.make(m);
            nodes += perft(p, depth - 1);
            p.unmake(m, u);
        }

        return nodes;
    }
}