package Chess.Core.AI;

import java.util.ArrayList;
import java.util.List;

import Chess.Core.AI.Util.Evaluation;
import Chess.Core.AI.Util.Move;
import Chess.Core.AI.Util.MoveGen;
import Chess.Core.AI.Util.Position;
import Chess.Core.AI.Util.Repetition;

import static Chess.Core.AI.Util.Position.*;

/**
 * Negamax alpha-beta with quiescence, MVV-LVA + killer + history ordering,
 * and iterative deepening under a time budget. NO JavaFX.
 *
 * evaluate() is White-relative; search works in side-to-move-relative scores,
 * so the root score's sign depends on whose turn it is.
 */
public final class Engine {
    private static final int INF = 1_000_000;
    private static final int MATE = 100_000; // mate score base; ply is subtracted so faster mates win
    private static final int MAX_PLY = 64;

    private final Position pos;
    private long deadlineNanos;
    private boolean cancelled;
    private long nodes;

    private Move rootBest; // best move of the in-progress iteration
    private final int[][] history = new int[2][64 * 64]; // [colorIndex][from*64+to]
    private final Move[][] killers = new Move[MAX_PLY][2];
    private final TranspositionTable tt = new TranspositionTable(20); // ~1M entries
    private final long[] repHistory = new long[2048]; // seeded game keys, then search-path keys
    private int repBase; // count of seeded game keys
    private int repLen;  // current length (repBase + plies pushed during search)
    private final Move[][] pvTable = new Move[MAX_PLY + 1][MAX_PLY + 1];
    private final int[] pvLength = new int[MAX_PLY + 1];
    private Move[] completedPv = new Move[0]; // PV of the last fully completed iteration

    private final List<RootScore> rootScores = new ArrayList<>(); // root scores of the in-progress iteration
    private List<RootScore> completedRootScores = new ArrayList<>(); // ...of the last fully completed iteration

    private record RootScore(Move move, int score) {}

    public Engine(Position pos) {
        this.pos = pos;
    }

    /** Seed the keys of positions that occurred earlier in the real game so the
     *  search counts repetitions across the game boundary, not just within its tree. */
    public void setGameHistory(long[] keys) {
        // Keep only the most recent keys, leaving headroom for the search path so
        // repHistory[repLen++] cannot overflow even in a pathologically long game.
        int cap = repHistory.length - MAX_PLY - 8;
        repBase = Math.min(keys.length, cap);
        System.arraycopy(keys, keys.length - repBase, repHistory, 0, repBase);
    }

    /** Searches until the time budget runs out; returns the best move found. */
    public Move findBestMove(long budgetMillis) {
        deadlineNanos = System.nanoTime() + budgetMillis * 1_000_000L;
        cancelled = false;
        nodes = 0;
        tt.newSearch();

        Move confirmed = null;
        int reachedDepth = 0;

        for (int depth = 1; depth <= MAX_PLY; depth++) {
            rootBest = null;
            rootScores.clear();
            repLen = repBase;
            int score = negamax(depth, -INF, INF, 0);

            if (cancelled) {
                break; // discard the half-finished iteration
            }

            confirmed = rootBest;
            reachedDepth = depth;
            completedRootScores = new ArrayList<>(rootScores); // keep the deepest finished depth's scores
            completedPv = java.util.Arrays.copyOf(pvTable[0], pvLength[0]);
            if (Math.abs(score) >= MATE - MAX_PLY) {
                break; // forced mate found, stop early
            }
        }

        System.out.println("Furthest depth reached: " + reachedDepth);
        printRootScores(reachedDepth);
        printPrincipalVariation();
        System.out.println("Best move: " + confirmed);

        return confirmed;
    }

    /**
     * Prints each root move's score at the deepest fully completed depth, sorted
     * best-first, from the moving side's point of view (positive = good for the
     * side to move). These are the scores the search actually used to pick its
     * move. Note: because of alpha-beta pruning, every move except the best is an
     * UPPER BOUND ("no better than this"); only the chosen move's score is exact.
     */
    private void printRootScores(int depth) {
        List<RootScore> sorted = new ArrayList<>(completedRootScores);
        sorted.sort((a, b) -> Integer.compare(b.score(), a.score()));

        System.out.println("Depth " + depth + " scores:");
        for (RootScore s : sorted) {
            System.out.println("  " + s.move() + " : " + s.score());
        }
    }

    /** The principal variation (best line) from the last completed iteration. */
    public java.util.List<Move> getPrincipalVariation() {
        return java.util.List.of(completedPv);
    }

    private void printPrincipalVariation() {
        StringBuilder sb = new StringBuilder("PV:");
        for (Move m : completedPv) {
            sb.append(' ').append(m);
        }
        System.out.println(sb);
    }

    private boolean timeUp() {
        if ((nodes & 2047) == 0 && System.nanoTime() > deadlineNanos) {
            System.out.println("Canceled");
            cancelled = true;
        }
        
        return cancelled;
    }

    private int negamax(int depth, int alpha, int beta, int ply) {
        if (timeUp()) {
            return 0;
        }

        pvLength[ply] = 0;

        if (ply > 0 && Repetition.isDraw(pos.key, repHistory, repLen, pos.ruleOf50)) {
            return 0;
        }

        if (depth == 0) {
            return quiescence(alpha, beta, ply);
        }

        nodes++;

        int alphaOrig = alpha;
        Move ttMove = null;
        TranspositionTable.Entry e = tt.probe(pos.key);
        if (e != null) {
            ttMove = e.best();
            if (e.depth() >= depth && ply > 0) { // never cut at root; rootBest must come from a real search
                int s = fromTT(e.score(), ply);
                if (e.flag() == TranspositionTable.EXACT) return s;
                if (e.flag() == TranspositionTable.LOWER && s >= beta) return s;
                if (e.flag() == TranspositionTable.UPPER && s <= alpha) return s;
            }
        }

        int kingSq = MoveGen.findKing(pos, pos.whiteToMove);
        boolean inCheck = MoveGen.isSquareAttacked(pos, kingSq, !pos.whiteToMove);

        // Null-move pruning: if passing still beats beta, the real position is too good to bother.
        if (!inCheck && depth >= 3 && beta < MATE - MAX_PLY
                && pos.hasNonPawnMaterial(pos.whiteToMove)) {
            Position.Undo nu = pos.makeNull();
            int R = 2;
            int nullScore = -negamax(depth - 1 - R, -beta, -beta + 1, ply + 1);
            pos.unmakeNull(nu);
            if (cancelled) {
                return 0;
            }
            if (nullScore >= beta) {
                return beta;
            }
        }

        List<Move> moves = MoveGen.generateLegal(pos);

        if (moves.isEmpty()) {
            return inCheck ? -MATE + ply : 0; // checkmate (deeper = less bad) or stalemate
        }

        orderMoves(moves, ply, ttMove);

        repHistory[repLen++] = pos.key; // children can now see this position

        int best = -INF;
        Move bestMove = null;
        int moveIndex = 0;

        for (Move m : moves) {
            // Compute `quiet` BEFORE make(m): afterwards the moving piece sits on
            // m.to(), so isCapture(m) would report every move as a capture and LMR
            // would never fire.
            boolean quiet = !isCapture(m) && m.flag() != Move.PROMOTION;

            Position.Undo u = pos.make(m);

            int score;

            if (ply > 0 && depth >= 3 && moveIndex >= 3 && quiet && !inCheck) {
                // Late-move reduction: search shallow with a null window first.
                score = -negamax(depth - 2, -alpha - 1, -alpha, ply + 1);
                if (score > alpha) { // promising — re-search at full depth/window
                    score = -negamax(depth - 1, -beta, -alpha, ply + 1);
                }
            } else {
                score = -negamax(depth - 1, -beta, -alpha, ply + 1);
            }

            pos.unmake(m, u);

            if (cancelled) {
                return 0;
            }

            if (ply == 0) {
                rootScores.add(new RootScore(m, score));
            }

            if (score > best) {
                best = score;
                bestMove = m;

                if (ply == 0) {
                    rootBest = m;
                }
            }

            if (score > alpha) {
                alpha = score;

                if (ply < MAX_PLY) { // splice child's PV onto this move
                    pvTable[ply][0] = m;
                    System.arraycopy(pvTable[ply + 1], 0, pvTable[ply], 1, pvLength[ply + 1]);
                    pvLength[ply] = pvLength[ply + 1] + 1;
                }
            }

            if (alpha >= beta) { // cutoff
                if (!isCapture(m)) {
                    recordKillerHistory(m, ply, depth);
                }

                break;
            }

            moveIndex++;
        }

        repLen--; // pop

        int flag = best <= alphaOrig ? TranspositionTable.UPPER
                : best >= beta ? TranspositionTable.LOWER
                : TranspositionTable.EXACT;
        tt.store(pos.key, depth, toTT(best, ply), flag, bestMove);

        return best;
    }

    /**
     * Search only captures/promotions until the position is quiet — kills the
     * horizon effect.
     */
    private int quiescence(int alpha, int beta, int ply) {
        if (timeUp()) {
            return 0;
        }

        nodes++;

        // Searching all evasions when in check (below) breaks the capture-only
        // "material strictly drops each ply" depth bound, so a long checking
        // sequence could recurse without limit. Cap it here; this also keeps
        // ply within killers[]'s MAX_PLY range for orderMoves().
        if (ply >= MAX_PLY) {
            return sideRelative(Evaluation.evaluate(pos));
        }

        int kingSq = MoveGen.findKing(pos, pos.whiteToMove);
        boolean inCheck = MoveGen.isSquareAttacked(pos, kingSq, !pos.whiteToMove);

        List<Move> moves;

        if (inCheck) {
            // Standing pat is illegal in check, and the only legal reply may be a
            // quiet evasion, so search every legal move or we can miss a mate.
            moves = MoveGen.generateLegal(pos);

            if (moves.isEmpty()) {
                return -MATE + ply; // checkmate
            }

            orderMoves(moves, ply, null);
        } else {
            int standPat = sideRelative(Evaluation.evaluate(pos));

            if (standPat >= beta) {
                return beta;
            }

            if (standPat > alpha) {
                alpha = standPat;
            }

            moves = MoveGen.generateLegalCaptures(pos);
            orderCaptures(moves);
        }

        for (Move m : moves) {
            Position.Undo u = pos.make(m);
            int score = -quiescence(-beta, -alpha, ply + 1);

            pos.unmake(m, u);

            if (cancelled) {
                return 0;
            }

            if (score >= beta) {
                return beta;
            }

            if (score > alpha) {
                alpha = score;
            }
        }

        return alpha;
    }

    private int sideRelative(int whiteScore) {
        return pos.whiteToMove ? whiteScore : -whiteScore;
    }

    /** Adjust a mate score for STORING: encode distance-to-mate relative to this node. */
    private int toTT(int score, int ply) {
        if (score >= MATE - MAX_PLY) return score + ply;
        if (score <= -MATE + MAX_PLY) return score - ply;
        return score;
    }

    /** Adjust a mate score read FROM the table back to this node's frame. */
    private int fromTT(int score, int ply) {
        if (score >= MATE - MAX_PLY) return score - ply;
        if (score <= -MATE + MAX_PLY) return score + ply;
        return score;
    }

    // --- ordering ----------------------------------------------------------

    private boolean isCapture(Move m) {
        return pos.board[m.to()] != 0 || m.flag() == Move.EN_PASSANT;
    }

    private void orderMoves(List<Move> moves, int ply, Move ttMove) {
        int colorIdx = pos.whiteToMove ? 1 : 0;
        Move k0 = killers[ply][0], k1 = killers[ply][1];
        moves.sort((a, b) -> Integer.compare(
                score(b, ply, colorIdx, k0, k1, ttMove),
                score(a, ply, colorIdx, k0, k1, ttMove)));
    }

    private int score(Move m, int ply, int colorIdx, Move k0, Move k1, Move ttMove) {
        if (m.equals(ttMove)) {
            return 2_000_000; // above all captures and quiets
        }

        int victim = pos.board[m.to()];

        if (victim != 0 || m.flag() == Move.EN_PASSANT) {
            int v = victim == 0 ? PAWN : Math.abs(victim); // ep victim is a pawn
            int attacker = Math.abs(pos.board[m.from()]);
            return 1_000_000 + v * 10 - attacker; // MVV-LVA, above all quiets
        }

        if (m.equals(k0)) {
            return 900_000;
        }

        if (m.equals(k1)) {
            return 800_000;
        }

        return history[colorIdx][m.from() * 64 + m.to()];
    }

    private void orderCaptures(List<Move> caps) {
        caps.sort((a, b) -> Integer.compare(mvvLva(b), mvvLva(a)));
    }

    private int mvvLva(Move m) {
        int victim = pos.board[m.to()];
        int v = victim == 0 ? PAWN : Math.abs(victim);
        int attacker = Math.abs(pos.board[m.from()]);

        return v * 10 - attacker;
    }

    private void recordKillerHistory(Move m, int ply, int depth) {
        if (!m.equals(killers[ply][0])) {
            killers[ply][1] = killers[ply][0];
            killers[ply][0] = m;
        }

        int colorIdx = pos.whiteToMove ? 1 : 0;
        history[colorIdx][m.from() * 64 + m.to()] += depth * depth;
    }
}