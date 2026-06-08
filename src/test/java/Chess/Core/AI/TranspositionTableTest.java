package Chess.Core.AI;

import static org.junit.Assert.*;
import org.junit.Test;

import Chess.Core.AI.Util.Move;

public class TranspositionTableTest {

    @Test
    public void storeThenProbeReturnsEntry() {
        TranspositionTable tt = new TranspositionTable(10); // 1024 entries
        tt.newSearch();
        Move m = Move.normal(52, 36);
        tt.store(0xABCDEF12345L, 5, 42, TranspositionTable.EXACT, m);

        TranspositionTable.Entry e = tt.probe(0xABCDEF12345L);
        assertNotNull(e);
        assertEquals(5, e.depth());
        assertEquals(42, e.score());
        assertEquals(TranspositionTable.EXACT, e.flag());
        assertEquals(m, e.best());
    }

    @Test
    public void probeMissReturnsNull() {
        TranspositionTable tt = new TranspositionTable(10);
        tt.newSearch();
        assertNull(tt.probe(123L));
    }

    /** A deeper entry from the current search is not overwritten by a shallower one. */
    @Test
    public void deeperEntrySurvivesShallowerStore() {
        TranspositionTable tt = new TranspositionTable(10);
        tt.newSearch();
        long key = 777L;
        tt.store(key, 8, 100, TranspositionTable.EXACT, null);
        tt.store(key, 3, 999, TranspositionTable.EXACT, null);
        assertEquals(8, tt.probe(key).depth());
        assertEquals(100, tt.probe(key).score());
    }

    /** Equal-or-greater depth replaces. */
    @Test
    public void equalDepthReplaces() {
        TranspositionTable tt = new TranspositionTable(10);
        tt.newSearch();
        long key = 555L;
        tt.store(key, 4, 10, TranspositionTable.EXACT, null);
        tt.store(key, 4, 20, TranspositionTable.LOWER, null);
        assertEquals(20, tt.probe(key).score());
        assertEquals(TranspositionTable.LOWER, tt.probe(key).flag());
    }
}
