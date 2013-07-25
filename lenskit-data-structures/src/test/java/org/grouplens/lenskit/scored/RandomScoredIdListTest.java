package org.grouplens.lenskit.scored;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.SerializationUtils;
import org.grouplens.lenskit.collections.CollectionUtils;
import org.grouplens.lenskit.symbols.Symbol;
import org.grouplens.lenskit.symbols.TypedSymbol;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * More tests for scored IDs, on several random tests.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
@RunWith(Parameterized.class)
public class RandomScoredIdListTest {
    private static Symbol VAL_SYM = Symbol.of("VALUE");
    private static TypedSymbol<String> STR_SYM = TypedSymbol.of(String.class, "STRING");

    private static final int TEST_COUNT = 10;
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Random rng = new Random();
        List<Object[]> idLists = Lists.newArrayListWithCapacity(TEST_COUNT);
        for (int test = 0; test < TEST_COUNT; test++) {
            int size = rng.nextInt(50);
            ImmutableList.Builder<ScoredId> ids = ImmutableList.builder();
            for (int i = 0; i < size; i++) {
                double v = rng.nextGaussian() + Math.PI;
                ScoredIdBuilder bld = new ScoredIdBuilder(i, v);
                if (rng.nextBoolean()) {
                    bld.addChannel(VAL_SYM, Math.log(v));
                }
                if (rng.nextBoolean()) {
                    bld.addChannel(STR_SYM, Double.toString(v));
                }
                ScoredId id = bld.build();
                ids.add(id);
            }
            idLists.add(new Object[]{ids.build()});
        }
        return idLists;
    }

    private final List<ScoredId> idList;
    private final int size;
    private ScoredIdListBuilder builder;

    public RandomScoredIdListTest(List<ScoredId> ids) {
        idList = ids;
        size = idList.size();
    }

    /**
     * Set up a builder containing all the IDs.
     */
    @Before
    public void initializeBuilder() {
        builder = ScoredIds.newListBuilder();
        for (ScoredId id: idList) {
            builder.add(id);
        }
    }

    @Test
    public void testIterateFast() {
        PackedScoredIdList list = builder.build();
        assertThat(list, hasSize(size));
        int i = 0;
        for (ScoredId id: CollectionUtils.fast(list)) {
            assertThat(id, equalTo(idList.get(i)));
            i++;
        }
    }

    @Test
    public void testSort() {
        PackedScoredIdList list = builder.sort(ScoredIds.scoreOrder().reverse()).build();
        List<ScoredId> sorted = ScoredIds.scoreOrder().reverse().sortedCopy(idList);
        assertThat(list, hasSize(size));
        for (int i = 0; i < size; i++) {
            assertThat(list.get(i), equalTo(sorted.get(i)));
        }
        // check equality for good measure
        assertThat(list, equalTo(sorted));
    }

    /**
     * Test sorting by channel, to make sure the plumbing to make channels available in IDs at
     * sort time works properly.
     */
    @Test
    public void testChannelSort() {
        PackedScoredIdList list = builder.sort(ScoredIds.channelOrder(VAL_SYM)
                                                        .compound(ScoredIds.scoreOrder())).build();
        List<ScoredId> sorted = ScoredIds.channelOrder(VAL_SYM)
                                         .compound(ScoredIds.scoreOrder())
                                         .sortedCopy(idList);
        assertThat(list, hasSize(size));
        for (int i = 0; i < size; i++) {
            assertThat(list.get(i), equalTo(sorted.get(i)));
        }
        // check equality for good measure
        assertThat(list, equalTo(sorted));
    }

    /**
     * Test sorting by typed channel, to make sure the plumbing to make channels available in IDs at
     * sort time works properly.
     */
    @Test
    public void testTypedChannelSort() {
        PackedScoredIdList list = builder.sort(ScoredIds.channelOrder(STR_SYM)
                                                        .nullsFirst()
                                                        .compound(ScoredIds.scoreOrder())).build();
        List<ScoredId> sorted = ScoredIds.channelOrder(STR_SYM)
                                         .nullsFirst()
                                         .compound(ScoredIds.scoreOrder())
                                         .sortedCopy(idList);
        assertThat(list, hasSize(size));
        for (int i = 0; i < size; i++) {
            assertThat(list.get(i), equalTo(sorted.get(i)));
        }
        // check equality for good measure
        assertThat(list, equalTo(sorted));
    }

    @Test
    public void testFinish() {
        PackedScoredIdList list = builder.finish();
        assertThat(list, hasSize(size));
        for (int i = 0; i < size; i++) {
            assertThat(list.get(i), equalTo(idList.get(i)));
        }
        // check equality for good measure
        assertThat(list, equalTo(idList));
    }

    @Test
    public void testSerialize() {
        PackedScoredIdList list = builder.finish();
        PackedScoredIdList l2 = SerializationUtils.clone(list);
        assertThat(l2, not(sameInstance(list)));
        assertThat(l2, equalTo(list));
    }
}
