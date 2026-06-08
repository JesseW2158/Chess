package Chess.Core.AI.Util;

import static Chess.Core.AI.Util.Position.*;

/**
 * Static evaluation in centipawns, ALWAYS from White's point of view
 * (positive favours White). The engine negates it for the side to move.
 *
 * Tapered between middlegame (mg) and endgame (eg) by remaining material phase.
 */
public final class Evaluation {
    private Evaluation() {
    }

    // index by type 1..6 (0 unused)
    private static final int[] VALUE = { 0, 100, 320, 330, 500, 900, 0 };
    // phase weights; full board = 24
    private static final int[] PHASE = { 0, 0, 1, 1, 2, 4, 0 };

    private static final int BISHOP_PAIR = 30;
    private static final int ISOLATED = 15;
    private static final int DOUBLED = 15;
    private static final int ROOK_OPEN = 20;
    private static final int ROOK_SEMI = 10;
    // Passed-pawn bonus indexed by ranks advanced from the home rank (0..5).
    private static final int[] PASSED = { 0, 10, 20, 35, 60, 100 };

    private static final int[] PAWN_MG = {
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5, 5, 10, 25, 25, 10, 5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, -5, -10, 0, 0, -10, -5, 5,
            5, 10, 10, -20, -20, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] PAWN_EG = {
            0, 0, 0, 0, 0, 0, 0, 0,
            80, 80, 80, 80, 80, 80, 80, 80,
            50, 50, 50, 50, 50, 50, 50, 50,
            30, 30, 30, 30, 30, 30, 30, 30,
            20, 20, 20, 20, 20, 20, 20, 20,
            10, 10, 10, 10, 10, 10, 10, 10,
            10, 10, 10, 10, 10, 10, 10, 10,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] KNIGHT_PST = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
    };

    private static final int[] BISHOP_PST = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 5, 5, 10, 10, 5, 5, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
    };

    private static final int[] ROOK_PST = {
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, 10, 10, 10, 10, 5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            0, 0, 0, 5, 5, 0, 0, 0
    };

    private static final int[] QUEEN_PST = {
            -20, -10, -10, -5, -5, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 5, 5, 5, 0, -10,
            -5, 0, 5, 5, 5, 5, 0, -5,
            0, 0, 5, 5, 5, 5, 0, -5,
            -10, 5, 5, 5, 5, 5, 0, -10,
            -10, 0, 5, 0, 0, 0, 0, -10,
            -20, -10, -10, -5, -5, -10, -10, -20
    };

    private static final int[] KING_MG = {
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20, 20, 0, 0, 0, 0, 20, 20,
            20, 30, 10, 0, 0, 10, 30, 20
    };

    private static final int[] KING_EG = {
            -50, -40, -30, -20, -20, -30, -40, -50,
            -30, -20, -10, 0, 0, -10, -20, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -30, 0, 0, 0, 0, -30, -30,
            -50, -30, -30, -30, -30, -30, -30, -50
    };

    public static int evaluate(Position p) {
        int mg = 0, eg = 0, phase = 0;

        for (int sq = 0; sq < 64; sq++) {
            int pc = p.board[sq];

            if (pc == 0) {
                continue;
            }

            int type = Math.abs(pc);
            boolean white = pc > 0;
            int x = sq % 8, y = sq / 8;
            // PST is written rank-8-first from White's view; mirror for Black.
            int idx = white ? (y * 8 + x) : ((7 - y) * 8 + x);
            int mgVal = VALUE[type] + pstMg(type, idx);
            int egVal = VALUE[type] + pstEg(type, idx);

            if (white) {
                mg += mgVal;
                eg += egVal;
            } else {
                mg -= mgVal;
                eg -= egVal;
            }

            phase += PHASE[type];
        }
        int ph = Math.min(phase, 24);
        int tapered = (mg * ph + eg * (24 - ph)) / 24;

        return tapered + structureScore(p);
    }

    /** White-relative bonus for bishop pair, pawn structure, and rook files. */
    private static int structureScore(Position p) {
        int[] wp = new int[8], bp = new int[8];
        int wB = 0, bB = 0;

        for (int sq = 0; sq < 64; sq++) {
            int pc = p.board[sq];
            if (pc == 0) continue;
            int x = sq % 8;
            if (pc == PAWN) wp[x]++;
            else if (pc == -PAWN) bp[x]++;
            else if (pc == BISHOP) wB++;
            else if (pc == -BISHOP) bB++;
        }

        int score = 0;

        if (wB >= 2) score += BISHOP_PAIR;
        if (bB >= 2) score -= BISHOP_PAIR;

        for (int f = 0; f < 8; f++) {
            if (wp[f] > 1) score -= DOUBLED * (wp[f] - 1);
            if (bp[f] > 1) score += DOUBLED * (bp[f] - 1);

            boolean wNeighbor = (f > 0 && wp[f - 1] > 0) || (f < 7 && wp[f + 1] > 0);
            if (wp[f] > 0 && !wNeighbor) score -= ISOLATED * wp[f];

            boolean bNeighbor = (f > 0 && bp[f - 1] > 0) || (f < 7 && bp[f + 1] > 0);
            if (bp[f] > 0 && !bNeighbor) score += ISOLATED * bp[f];
        }

        for (int sq = 0; sq < 64; sq++) {
            int pc = p.board[sq];
            if (pc == 0) continue;
            int x = sq % 8, y = sq / 8;

            if (pc == PAWN) {
                if (y >= 1 && y <= 6 && isPassedWhite(p, x, y)) score += PASSED[6 - y];
            } else if (pc == -PAWN) {
                if (y >= 1 && y <= 6 && isPassedBlack(p, x, y)) score -= PASSED[y - 1];
            } else if (pc == ROOK) {
                score += rookFileBonus(wp[x], bp[x]);
            } else if (pc == -ROOK) {
                score -= rookFileBonus(bp[x], wp[x]);
            }
        }

        return score;
    }

    /** No black pawn on this or an adjacent file ahead of the white pawn (toward y=0). */
    private static boolean isPassedWhite(Position p, int x, int y) {
        for (int yy = y - 1; yy >= 0; yy--)
            for (int dx = -1; dx <= 1; dx++) {
                int xx = x + dx;
                if (xx >= 0 && xx < 8 && p.board[yy * 8 + xx] == -PAWN) return false;
            }
        return true;
    }

    /** No white pawn on this or an adjacent file ahead of the black pawn (toward y=7). */
    private static boolean isPassedBlack(Position p, int x, int y) {
        for (int yy = y + 1; yy < 8; yy++)
            for (int dx = -1; dx <= 1; dx++) {
                int xx = x + dx;
                if (xx >= 0 && xx < 8 && p.board[yy * 8 + xx] == PAWN) return false;
            }
        return true;
    }

    private static int rookFileBonus(int ownPawns, int enemyPawns) {
        if (ownPawns == 0 && enemyPawns == 0) return ROOK_OPEN;
        if (ownPawns == 0) return ROOK_SEMI;
        return 0;
    }

    private static int pstMg(int type, int idx) {
        return switch (type) {
            case PAWN -> PAWN_MG[idx];
            case KNIGHT -> KNIGHT_PST[idx];
            case BISHOP -> BISHOP_PST[idx];
            case ROOK -> ROOK_PST[idx];
            case QUEEN -> QUEEN_PST[idx];
            case KING -> KING_MG[idx];
            default -> 0;
        };
    }

    private static int pstEg(int type, int idx) {
        return switch (type) {
            case PAWN -> PAWN_EG[idx];
            case KING -> KING_EG[idx];
            default -> pstMg(type, idx); // knight/bishop/rook/queen share one table
        };
    }
}