package Chess.Core.AI.Util;

/**
 * Plain-data chess position. NO JavaFX. index = y*8 + x, y=0 is rank 8.
 * White pieces are positive, black negative, 0 is empty.
 */
public final class Position {
    public static final int PAWN = 1, KNIGHT = 2, BISHOP = 3, ROOK = 4, QUEEN = 5, KING = 6;

    public final int[] board = new int[64];
    public boolean whiteToMove = true;
    public boolean wK, wQ, bK, bQ; // castling rights
    public int epSquare = -1; // square a pawn could capture ONTO via en passant, or -1
    public int ruleOf50 = 0;
    public long key; // Zobrist hash; maintained incrementally in make/unmake

    /** Saved state needed to undo one make(). */
    public static final class Undo {
        int captured; // piece code captured (0 if none)
        int capturedSquare; // where it stood (differs from `to` for en passant)
        int epSquare;
        boolean wK, wQ, bK, bQ;
        int ruleOf50;
        long key;
    }

    /** Rebuild the Zobrist key from scratch and store it in {@link #key}. */
    public long recomputeZobrist() {
        long k = 0;
        for (int sq = 0; sq < 64; sq++) {
            int pc = board[sq];
            if (pc != 0) k ^= Zobrist.PIECE[Zobrist.pieceIndex(pc)][sq];
        }
        if (!whiteToMove) k ^= Zobrist.SIDE;
        if (wK) k ^= Zobrist.CASTLE[0];
        if (wQ) k ^= Zobrist.CASTLE[1];
        if (bK) k ^= Zobrist.CASTLE[2];
        if (bQ) k ^= Zobrist.CASTLE[3];
        if (epSquare >= 0) k ^= Zobrist.EP_FILE[epSquare % 8];
        key = k;
        return k;
    }

    // --- FEN ---------------------------------------------------------------

    public static Position fromFen(String fen) {
        Position p = new Position();

        String[] parts = fen.trim().split("\\s+");
        String[] ranks = parts[0].split("/"); // ranks[0] is rank 8 -> y=0

        for (int y = 0; y < 8; y++) {
            int x = 0;
            for (char c : ranks[y].toCharArray()) {
                if (Character.isDigit(c)) {
                    System.out.println();
                    x += c - '0';
                    continue;
                }
                p.board[y * 8 + x] = fromFenChar(c);
                x++;
            }
        }

        p.whiteToMove = parts.length < 2 || parts[1].equals("w");

        String rights = parts.length > 2 ? parts[2] : "KQkq";

        p.wK = rights.contains("K");
        p.wQ = rights.contains("Q");
        p.bK = rights.contains("k");
        p.bQ = rights.contains("q");

        if (parts.length > 3 && !parts[3].equals("-")) {
            int fx = parts[3].charAt(0) - 'a';
            int fy = '8' - parts[3].charAt(1); // rank char -> y

            p.epSquare = fy * 8 + fx;
        }
        if (parts.length > 4)
            p.ruleOf50 = Integer.parseInt(parts[4]);
        p.recomputeZobrist();
        return p;
    }

    private static int fromFenChar(char c) {
        int sign = Character.isUpperCase(c) ? 1 : -1;
        int type = switch (Character.toLowerCase(c)) {
            case 'p' -> PAWN;
            case 'n' -> KNIGHT;
            case 'b' -> BISHOP;
            case 'r' -> ROOK;
            case 'q' -> QUEEN;
            case 'k' -> KING;
            default -> throw new IllegalArgumentException("Bad FEN char: " + c);
        };
        return sign * type;
    }

    private static char toFenChar(int piece) {
        if (piece == 0)
            return '.';
        char c = switch (Math.abs(piece)) {
            case PAWN -> 'p';
            case KNIGHT -> 'n';
            case BISHOP -> 'b';
            case ROOK -> 'r';
            case QUEEN -> 'q';
            case KING -> 'k';
            default -> '?';
        };
        return piece > 0 ? Character.toUpperCase(c) : c;
    }

    // --- make / unmake -----------------------------------------------------

    public Undo make(Move m) {
        Undo u = new Undo();
        u.epSquare = epSquare;
        u.ruleOf50 = ruleOf50;
        u.wK = wK;
        u.wQ = wQ;
        u.bK = bK;
        u.bQ = bQ;
        u.key = key;

        int from = m.from(), to = m.to();
        int piece = board[from];
        int color = piece > 0 ? 1 : -1;
        int captured = 0, capturedSquare = to;

        // Pull the OLD ep-file and castling contributions out; new ones re-added at the end.
        if (epSquare >= 0) key ^= Zobrist.EP_FILE[epSquare % 8];
        if (wK) key ^= Zobrist.CASTLE[0];
        if (wQ) key ^= Zobrist.CASTLE[1];
        if (bK) key ^= Zobrist.CASTLE[2];
        if (bQ) key ^= Zobrist.CASTLE[3];

        epSquare = -1;
        ruleOf50++;

        switch (m.flag()) {
            case Move.NORMAL -> {
                captured = board[to];
                if (captured != 0) key ^= Zobrist.PIECE[Zobrist.pieceIndex(captured)][to];
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(piece)][from];
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(piece)][to];
                board[to] = piece;
                board[from] = 0;
            }
            case Move.DOUBLE_PUSH -> {
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(piece)][from];
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(piece)][to];
                board[to] = piece;
                board[from] = 0;
                epSquare = (from + to) / 2; // the skipped square
            }
            case Move.EN_PASSANT -> {
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(piece)][from];
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(piece)][to];
                board[to] = piece;
                board[from] = 0;
                capturedSquare = (from / 8) * 8 + (to % 8); // captured pawn: mover's rank, target's file
                captured = board[capturedSquare];
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(captured)][capturedSquare];
                board[capturedSquare] = 0;
            }
            case Move.CASTLE -> {
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(piece)][from];
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(piece)][to];
                board[to] = piece;
                board[from] = 0;
                int rank = to / 8;
                if (to % 8 == 6) { // h->f
                    int rook = board[rank * 8 + 7];
                    key ^= Zobrist.PIECE[Zobrist.pieceIndex(rook)][rank * 8 + 7];
                    key ^= Zobrist.PIECE[Zobrist.pieceIndex(rook)][rank * 8 + 5];
                    board[rank * 8 + 5] = rook;
                    board[rank * 8 + 7] = 0;
                } else { // a->d
                    int rook = board[rank * 8 + 0];
                    key ^= Zobrist.PIECE[Zobrist.pieceIndex(rook)][rank * 8 + 0];
                    key ^= Zobrist.PIECE[Zobrist.pieceIndex(rook)][rank * 8 + 3];
                    board[rank * 8 + 3] = rook;
                    board[rank * 8 + 0] = 0;
                }
            }
            case Move.PROMOTION -> {
                captured = board[to];
                if (captured != 0) key ^= Zobrist.PIECE[Zobrist.pieceIndex(captured)][to];
                int promoted = color * m.promo();
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(piece)][from];   // remove pawn
                key ^= Zobrist.PIECE[Zobrist.pieceIndex(promoted)][to];  // add promoted piece
                board[to] = promoted;
                board[from] = 0;
            }
        }

        u.captured = captured;
        u.capturedSquare = capturedSquare;
        if (Math.abs(piece) == PAWN || captured != 0)
            ruleOf50 = 0;

        // castling-right updates
        if (Math.abs(piece) == KING) {
            if (color > 0) {
                wK = false;
                wQ = false;
            } else {
                bK = false;
                bQ = false;
            }
        }
        if (from == 56)
            wQ = false; // white a-rook left home
        if (from == 63)
            wK = false; // white h-rook
        if (from == 0)
            bQ = false; // black a-rook
        if (from == 7)
            bK = false; // black h-rook
        if (captured != 0 && Math.abs(captured) == ROOK) {
            if (capturedSquare == 56)
                wQ = false;
            if (capturedSquare == 63)
                wK = false;
            if (capturedSquare == 0)
                bQ = false;
            if (capturedSquare == 7)
                bK = false;
        }

        // Re-add NEW castling + ep contributions, then flip the side bit.
        if (wK) key ^= Zobrist.CASTLE[0];
        if (wQ) key ^= Zobrist.CASTLE[1];
        if (bK) key ^= Zobrist.CASTLE[2];
        if (bQ) key ^= Zobrist.CASTLE[3];
        if (epSquare >= 0) key ^= Zobrist.EP_FILE[epSquare % 8];
        key ^= Zobrist.SIDE;

        whiteToMove = !whiteToMove;
        return u;
    }

    public void unmake(Move m, Undo u) {
        whiteToMove = !whiteToMove; // back to the mover's side
        int from = m.from(), to = m.to();

        switch (m.flag()) {
            case Move.NORMAL, Move.DOUBLE_PUSH -> {
                board[from] = board[to];
                board[to] = u.captured;
            }
            case Move.EN_PASSANT -> {
                board[from] = board[to];
                board[to] = 0;
                board[u.capturedSquare] = u.captured;
            }
            case Move.CASTLE -> {
                board[from] = board[to];
                board[to] = 0;
                int rank = to / 8;
                if (to % 8 == 6) {
                    board[rank * 8 + 7] = board[rank * 8 + 5];
                    board[rank * 8 + 5] = 0;
                } else {
                    board[rank * 8 + 0] = board[rank * 8 + 3];
                    board[rank * 8 + 3] = 0;
                }
            }
            case Move.PROMOTION -> {
                int color = board[to] > 0 ? 1 : -1;
                board[from] = color * PAWN;
                board[to] = u.captured;
            }
        }

        epSquare = u.epSquare;
        ruleOf50 = u.ruleOf50;
        wK = u.wK;
        wQ = u.wQ;
        bK = u.bK;
        bQ = u.bQ;
        key = u.key;
    }

    /** Recompute the Zobrist key WITHOUT storing it (for assertions). */
    public long recomputeFresh() {
        long saved = key;
        long fresh = recomputeZobrist();
        key = saved;
        return fresh;
    }

    /** Play a "pass": flip the side to move and clear the ep square. Used by null-move pruning. */
    public Undo makeNull() {
        Undo u = new Undo();
        u.epSquare = epSquare;
        u.ruleOf50 = ruleOf50;
        u.wK = wK;
        u.wQ = wQ;
        u.bK = bK;
        u.bQ = bQ;
        u.key = key;

        if (epSquare >= 0) key ^= Zobrist.EP_FILE[epSquare % 8];
        epSquare = -1;
        key ^= Zobrist.SIDE;
        whiteToMove = !whiteToMove;
        return u;
    }

    public void unmakeNull(Undo u) {
        whiteToMove = !whiteToMove;
        epSquare = u.epSquare;
        ruleOf50 = u.ruleOf50;
        wK = u.wK;
        wQ = u.wQ;
        bK = u.bK;
        bQ = u.bQ;
        key = u.key;
    }

    /** Does {@code white} have at least one piece that is not a pawn or the king?
     *  Null-move pruning is unsafe without one (zugzwang). */
    public boolean hasNonPawnMaterial(boolean white) {
        for (int sq = 0; sq < 64; sq++) {
            int pc = board[sq];
            if (pc == 0 || (pc > 0) != white) continue;
            int t = Math.abs(pc);
            if (t != PAWN && t != KING) return true;
        }
        return false;
    }

    // --- debug -------------------------------------------------------------

    /**
     * Human-readable ASCII board (rank 8 at top) plus side, castling, ep, halfmove.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < 8; y++) { // y=0 is rank 8
            sb.append(8 - y).append(' ');
            for (int x = 0; x < 8; x++)
                sb.append(toFenChar(board[y * 8 + x])).append(' ');
            sb.append('\n');
        }
        sb.append("  a b c d e f g h\n");

        sb.append(whiteToMove ? "White" : "Black").append(" to move");

        StringBuilder rights = new StringBuilder();
        if (wK)
            rights.append('K');
        if (wQ)
            rights.append('Q');
        if (bK)
            rights.append('k');
        if (bQ)
            rights.append('q');
        sb.append("  Castling: ").append(rights.length() == 0 ? "-" : rights);

        sb.append("  EP: ").append(epSquare < 0 ? "-" : squareName(epSquare));
        sb.append("  HalfMove: ").append(ruleOf50);
        return sb.toString();
    }

    /** Algebraic name (e.g. "e3") for a board index. */
    private static String squareName(int sq) {
        char file = (char) ('a' + (sq % 8));
        char rank = (char) ('8' - (sq / 8)); // y=0 is rank 8
        return "" + file + rank;
    }
}
