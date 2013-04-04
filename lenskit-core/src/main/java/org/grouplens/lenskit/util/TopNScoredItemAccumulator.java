/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2013 Regents of the University of Minnesota and contributors
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.grouplens.lenskit.util;

import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.doubles.DoubleHeapIndirectPriorityQueue;
import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import it.unimi.dsi.fastutil.longs.LongComparators;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.scored.ScoredIdBuilder;
import org.grouplens.lenskit.vectors.MutableSparseVector;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulate the top <i>N</i> scored IDs.  IDs are sorted by their associated
 * scores.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 */
final public class TopNScoredItemAccumulator implements ScoredItemAccumulator {
    private final int count;
    private double[] scores;
    private long[] items;
    private int slot;
    private int size;
    private DoubleHeapIndirectPriorityQueue heap;

    /**
     * Create a new accumulator to accumulate the top {@var n} IDs.
     *
     * @param n The number of IDs to retain.
     */
    public TopNScoredItemAccumulator(int n) {
        this.count = n;
        // arrays must have n+1 slots to hold extra item before removing smallest
        scores = new double[n + 1];
        items = new long[n + 1];
        slot = 0;
        size = 0;
        heap = new DoubleHeapIndirectPriorityQueue(scores);
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(long item, double score) {
        assert slot <= count;
        assert heap.size() == size;

        /*
         * Store the new item. The slot shows where the current item is, and
         * then we deal with it based on whether we're oversized.
         */
        items[slot] = item;
        scores[slot] = score;
        heap.enqueue(slot);

        if (size == count) {
            // already at capacity, so remove and reuse smallest item
            slot = heap.dequeue();
        } else {
            // we have free space, so increment the slot and size
            slot += 1;
            size += 1;
        }
    }

    @Override
    public List<ScoredId> finish() {
        assert size == heap.size();
        int[] indices = new int[size];
        // Copy backwards so the scored list is sorted.
        for (int i = size - 1; i >= 0; i--) {
            indices[i] = heap.dequeue();
        }
        ArrayList<ScoredId> l = new ArrayList<ScoredId>(size);
        ScoredIdBuilder idBuilder = new ScoredIdBuilder();
        for (int i : indices) {
            ScoredId id = idBuilder.setId(items[i]).setScore(scores[i]).build();
            l.add(id);
        }

        assert heap.isEmpty();

        size = 0;
        slot = 0;
        return l;
    }

    @Override
    public MutableSparseVector vectorFinish() {
        if (scores == null) {
            return new MutableSparseVector();
        }

        assert size == heap.size();
        int[] indices = new int[size];
        // Copy backwards so the scored list is sorted.
        for (int i = size - 1; i >= 0; i--) {
            indices[i] = heap.dequeue();
        }
        assert heap.isEmpty();

        long[] keys = new long[indices.length];
        double[] values = new double[indices.length];
        for (int i = 0; i < indices.length; i++) {
            keys[i] = items[indices[i]];
            values[i] = scores[indices[i]];
        }
        size = 0;
        slot = 0;

        IdComparator comparator = new IdComparator(keys);
        ParallelSwapper swapper = new ParallelSwapper(keys, values);
        Arrays.quickSort(0, keys.length, comparator, swapper);

        return MutableSparseVector.wrap(keys, values);
    }

    private static class IdComparator extends AbstractIntComparator {

        private long[] keys;

        public IdComparator(long[] keys) {
            this.keys = keys;
        }

        @Override
        public int compare(int i, int i2) {
            return LongComparators.NATURAL_COMPARATOR.compare(keys[i], keys[i2]);
        }
    }

    private static class ParallelSwapper implements Swapper {

        private long[] keys;
        private double[] values;

        public ParallelSwapper(long[] keys, double[] values) {
            this.keys = keys;
            this.values = values;
        }

        @Override
        public void swap(int i, int i2) {
            long lTemp = keys[i];
            keys[i] = keys[i2];
            keys[i2] = lTemp;

            double dTemp = values[i];
            values[i] = values[i2];
            values[i2] = dTemp;
        }
    }
}
