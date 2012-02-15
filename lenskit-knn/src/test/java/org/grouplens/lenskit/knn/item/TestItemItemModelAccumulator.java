package org.grouplens.lenskit.knn.item;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import org.grouplens.lenskit.collections.ScoredLongList;
import org.grouplens.lenskit.collections.ScoredLongListIterator;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Michael Ekstrand
 */
public class TestItemItemModelAccumulator {
    LongSortedSet universe;
    /**
     * An accumulator with a size of 5 and universe of [1,10].
     */
    ItemItemModelAccumulator accum;

    @Before
    public void newAccumulator() {
        universe = new LongAVLTreeSet();
        for (long i = 1; i <= 10; i++) {
            universe.add(i);
        }
        accum = new ItemItemModelAccumulator(5, universe);
    }

    @Test
    public void testEmpty() {
        ItemItemModel model = accum.build();
        assertThat(model, notNullValue());
        assertThat(model.getItemUniverse(), equalTo(universe));
    }
    
    @Test
    public void testAccum() {
        accum.put(1, 2, Math.PI);
        accum.put(7, 3, Math.E);
        ItemItemModel model = accum.build();
        ScoredLongList nbrs = model.getNeighbors(1);
        assertThat(nbrs, hasSize(1));
        assertThat(nbrs.get(0), equalTo(2L));
        assertThat(nbrs.getScore(0), closeTo(Math.PI, 1.0e-6));
        nbrs = model.getNeighbors(7);
        assertThat(nbrs, hasSize(1));
        assertThat(nbrs.get(0), equalTo(3L));
        assertThat(nbrs.getScore(0), closeTo(Math.E, 1.0e-6));
    }

    @Test
    public void testTruncate() {
        for (long i = 1; i <= 10; i++) {
            for (long j = 1; j <= 10; j += (i % 3) + 1) {
                accum.put(i, j, Math.pow(Math.E, -i) * Math.pow(Math.PI, -j));
            }
        }
        ItemItemModel model = accum.build();
        ScoredLongList nbrs = model.getNeighbors(1);
        assertThat(nbrs, hasSize(5));
        nbrs = model.getNeighbors(4);
        assertThat(nbrs, hasSize(5));
        ScoredLongListIterator iter = nbrs.iterator();
        while (iter.hasNext()) {
            long j = iter.nextLong();
            double s = iter.getScore();
            assertThat(s, closeTo(Math.pow(Math.E, -4) * Math.pow(Math.PI, -j), 1.0e-6));
        }
    }
}
