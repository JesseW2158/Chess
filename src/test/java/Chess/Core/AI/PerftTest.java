package Chess.Core.AI;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import Chess.Core.AI.Util.MoveGen;
import Chess.Core.AI.Util.Position;

public class PerftTest {
    // Reference node counts are the standard Chess Programming Wiki perft results.
    private static void check(String fen, int depth, long expected) {
        assertEquals("perft(" + depth + ") for " + fen,
                expected, MoveGen.perft(Position.fromFen(fen), depth));
    }

    @Test public void startPosition() {
        String f = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        check(f, 1, 20);
        check(f, 2, 400);
        check(f, 3, 8902);
        check(f, 4, 197281);
        // depth 5 = 4865609 — enable once 1-4 pass; takes a few seconds.
        // check(f, 5, 4865609);
    }

    @Test public void kiwipete() { // exercises castling, en passant, promotions, pins
        String f = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        check(f, 1, 48);
        check(f, 2, 2039);
        check(f, 3, 97862);
    }

    @Test public void position3() { // tricky pawn/rook endgame, lots of ep and checks
        String f = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";
        check(f, 1, 14);
        check(f, 2, 191);
        check(f, 3, 2812);
        check(f, 4, 43238);
    }

    @Test public void position4() { // a-file promotions, pins, and one-sided castling rights
        String f = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
        check(f, 1, 6);
        check(f, 2, 264);
        check(f, 3, 9467);
        check(f, 4, 422333);
    }

    @Test public void position5() { // dense middlegame where under-promotions matter
        String f = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        check(f, 1, 44);
        check(f, 2, 1486);
        check(f, 3, 62379);
        // depth 4 = 2103487 — correct but ~a second; enable if you want deeper coverage.
        // check(f, 4, 2103487);
    }
}