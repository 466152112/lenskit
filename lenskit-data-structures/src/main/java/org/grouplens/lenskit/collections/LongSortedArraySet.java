/*
 * LensKit, a reference implementation of recommender algorithms.
 * Copyright 2010-2011 Regents of the University of Minnesota
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
package org.grouplens.lenskit.collections;

import it.unimi.dsi.fastutil.longs.AbstractLongBidirectionalIterator;
import it.unimi.dsi.fastutil.longs.AbstractLongSortedSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongIterators;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A sorted set of longs implemented using a sorted array.  It's much faster
 * than {@link LongArraySet} as it is able to use binary searches.  The set
 * is also immutable.
 *
 * <p>No orders are supported other than the natural ordering.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public final class LongSortedArraySet extends AbstractLongSortedSet implements Serializable {
    private static final long serialVersionUID = 885774794586510968L;

    private final long[] data;
    private final int start, end;
    private final @Nullable BitSet mask;

    public LongSortedArraySet(@Nonnull Collection<Long> items) {
        this(items instanceof LongCollection ? ((LongCollection) items).toLongArray()
                : LongIterators.unwrap(LongIterators.asLongIterator(items.iterator())));
    }

    /**
     * Create a new set from an existing array.
     * @param items An array of items. The array will be sorted and used as the
     * backing store for the set. If this array is changed after creating the
     * set, behavior is undefined.
     * @see #LongSortedArraySet(long[], int, int)
     */
    public LongSortedArraySet(@Nonnull long[] items) {
        this(items, 0, items.length);
    }

    /**
     * Create a new set from a range of an existing array.
     * @param items An array of items. The array will be sorted and used as the
     * backing store for the set. If this array is changed after creating the
     * set, behavior is undefined.
     * 
     * @param fromIndex The index of the first item in the array to use.
     * @param toIndex The end of the array to use (last index + 1).
     * set, behavior is undefined.
     * @throws IndexOutOfBoundsException if <var>start</var> or <var>end</var>
     * is out of range.
     */
    public LongSortedArraySet(@Nonnull long[] items, int fromIndex, int toIndex) {
        this(items, fromIndex, toIndex, false, null);
    }

    /**
     * Create a new set from a range of an existing array.
     * 
     * @param items An array of items. The array will be sorted and used as the
     *        backing store for the set. If this array is changed after creating
     *        the
     * @param fromIndex The index of the first item in the array to use.
     * @param toIndex The end of the array to use (last index + 1). set,
     *        behavior is undefined.
     * @param clean Assume the array is sorted and has no duplicates.
     * @param used A mask of indices into <var>items</var> indicating which ones
     *        are actually used. Indices are with respect to the underlying
     *        array, <b>not</b> <var>fromIndex</var>.
     * @throws IndexOutOfBoundsException if <var>start</var> or <var>end</var>
     *         is out of range.
     */
    private LongSortedArraySet(@Nonnull long[] items, int fromIndex, int toIndex,
                               boolean clean, @Nullable BitSet used) {
        data = items;
        start = fromIndex;
        if (fromIndex < 0 || toIndex > data.length)
            throw new IndexOutOfBoundsException();

        if (!clean) {
            // check for sortedness first to avoid the actual sort
            if (!isSorted(data, start, toIndex))
                Arrays.sort(data, start, toIndex);
            end = deduplicate(data, start, toIndex);
        } else {
            end = toIndex;
        }
        mask = used;
    }

    /**
     * Check that the array is sorted.
     * @return <code>true</code> iff the array is sorted.
     */
    static boolean isSorted(final long[] data, final int start, final int end) {
        for (int i = start; i < end - 1; i++) {
            if (data[i] > data[i+1]) return false;
        }
        return true;
    }

    /**
     * Remove duplicate elements in the backing store. The array should be
     * unsorted.
     * @return the new end index of the array
     */
    static int deduplicate(final long[] data, final int start, final int end) {
        if (start == end) return end;   // special-case empty arrays

        // Since we have a non-empty array, the nextPos will always be where the
        // end is if we find no more unique elements.
        int pos = start + 1;
        for (int i = pos; i < end; i++) {
            if (data[i] != data[i-1]) { // we have a non-duplicate item
                if (i != pos)           // indices out of alignment, must copy
                    data[pos] = data[i];
                pos++;                  // increment nextPos since we have a new non-dup
            }
            // if data[i] is a duplicate, then i steps forward and nextPos doesn't,
            // thereby arranging for data[i] to be elided.
        }
        return pos;
    }

    /**
     * Find the index for a key.
     * @see Arrays#binarySearch(long[], int, int, long)
     * @param key
     * @return The index at which <var>key</var> is stored.
     */
    private int findIndex(long key) {
        return Arrays.binarySearch(data, start, end, key);
    }

    /**
     * Find the index where <var>key</var> would appear if it exists.
     * @param key The search key.
     * @return The index in the array of the key, if it exists; otherwise, the
     * index of the first element greater than <var>key</var> (or the end of the
     * array).
     */
    private int findIndexAlways(long key) {
        int i = findIndex(key);
        if (i < 0) {
            i = -(i+1);
        }
        return i;
    }

    @Override
    public LongComparator comparator() {
        return null;
    }

    @Override
    public long firstLong() {
        if (end - start > 0) {
            if (mask == null) {
                return data[start];
            } else {
                int first = mask.nextSetBit(start);
                if (first >= 0) {
                    return data[first];
                }
            }
        }
        
        throw new NoSuchElementException();
    }

    @Override
    public long lastLong() {
        if (end - start > 0) {
            if (mask == null) {
                return data[end-1];
            } else {
                for (int i = end - 1; i >= start; i--) {
                    if (mask.get(i)) {
                        return data[i];
                    }
                }
            }
        }
        
        throw new NoSuchElementException();
    }
    
    @Override
    public LongBidirectionalIterator iterator(long key) {
        int index = findIndexAlways(key);
        if (index < end && data[index] == key) {
            index++;
        }
        if (mask == null) {
            return new IterImpl(index);
        } else {
            // FIXME Support reversing masked iterators
            return new MaskedIterImpl(index);
        }
    }

    @Override
    public LongSortedSet subSet(long startKey, long endKey) {
        return new LongSortedArraySet(data, findIndexAlways(startKey), findIndexAlways(endKey), true, mask);
    }

    @Override
    public LongSortedSet headSet(long key) {
        int nend = findIndexAlways(key);
        return new LongSortedArraySet(data, start, nend, true, mask);
    }

    @Override
    public LongSortedSet tailSet(long key) {
        return new LongSortedArraySet(data, findIndexAlways(key), end, true, mask);
    }

    @Override
    public LongBidirectionalIterator iterator() {
        if (mask == null) {
            return new IterImpl(start);
        } else {
            return new MaskedIterImpl(start);
        }
    }

    @Override
    public int size() {
        if (mask == null) {
            return end - start;
        } else if (start == 0 && end >= mask.size()) {
            return mask.cardinality();
        } else {
            BitSet bits = new BitSet(end);
            bits.set(start, end);
            bits.and(mask);
            return bits.cardinality();
        }
    }

    @Override
    public boolean contains(long key) {
        int idx = findIndex(key);
        return idx >= 0 && (mask == null || mask.get(idx));
    }

    /**
     * Unsupported remove operation.
     */
    @Override
    public boolean rem(long k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long[] toLongArray(long[] a) {
        if (mask == null) {
            final int sz = size();
            if (a == null || a.length < sz) {
                a = new long[sz];
            }
            System.arraycopy(data, start, a, 0, sz);
            return a;
        } else {
            return super.toLongArray(a);
        }
    }

    /**
     * Compute the set difference of two sets.
     */
    public static LongSortedSet setDifference(LongSet items, LongSet exclude) {
        long[] data = new long[items.size()];
        LongIterator iter = items.iterator();
        int i = 0;
        while (iter.hasNext()) {
            final long x = iter.nextLong();
            if (!exclude.contains(x)) {
                data[i++] = x;
            }
        }
        if (!(items instanceof LongSortedSet))
            Arrays.sort(data, 0, i);
        // trim the array
        if (data.length * 2 > i * 3)
            data = Arrays.copyOf(data, i);
        return new LongSortedArraySet(data, 0, i, true, null);
    }

    /**
     * Convert a {@link LongArrayList} to a sorted array set. The array list's
     * internal storage will be sorted and re-used.
     */
    public static LongSortedSet ofList(LongArrayList list) {
        return new LongSortedArraySet(list.elements(), 0, list.size());
    }
    
    /**
     * Wrap an array set around an array of values. The values must already be
     * sorted and de-duplicated.
     * 
     * @param data The array of data.
     * @param size The number of elements from the beginning of the array to
     *        use.
     * @return A set backed by the array.
     */
    public static LongSortedArraySet wrap(@Nonnull long[] data, int size) {
        return wrap(data, size, null);
    }
    
    /**
     * Wrap an existing array without change.
     * @see #wrap(long[],int)
     */
    public static LongSortedArraySet wrap(@Nonnull long[] data) {
        return wrap(data, data.length);
    }
    
    /**
     * Wrap an existing array with a mask.
     * @see #wrap(long[], int, BitSet)
     */
    public static LongSortedArraySet wrap(@Nonnull long[] data, @Nullable BitSet used) {
        return wrap(data, data.length, used);
    }
    
    /**
     * Wrap an array set around an array of values with a mask. The values must
     * already be sorted and de-duplicated.
     * 
     * @param data The array of data.
     * @param size The number of elements from the beginning of the array to
     *        use.
     * @param used The bitset of indices in <var>data</var> actually in use.
     * @return A set backed by the array.
     */
    public static LongSortedArraySet wrap(@Nonnull long[] data, int size,
                                          @Nullable BitSet used) {
        assert isSorted(data, 0, size);
        return new LongSortedArraySet(data, 0, size, true, used);
    }

    private final class IterImpl extends AbstractLongBidirectionalIterator {
        private int pos;
        public IterImpl(int start) {
            pos = start;
        }

        @Override
        public boolean hasNext() {
            return pos < end;
        }

        @Override
        public boolean hasPrevious() {
            return pos > start;
        }

        @Override
        public long nextLong() {
            if (hasNext())
                return data[pos++];
            else
                throw new NoSuchElementException();
        }

        @Override
        public long previousLong() {
            if (hasPrevious())
                return data[--pos];
            else
                throw new NoSuchElementException();
        }
    }
    
    private final class MaskedIterImpl extends AbstractLongBidirectionalIterator {
        BitSetIterator iter;
        public MaskedIterImpl(int spos) {
            iter = new BitSetIterator(mask, spos, end);
        }
        
        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }
        
        @Override
        public boolean hasPrevious() {
            return iter.hasPrevious();
        }
        
        @Override
        public long nextLong() {
            int idx = iter.nextInt();
            assert idx >= start && idx < end;
            return data[idx];
        }
        
        @Override
        public long previousLong() {
            int idx = iter.previousInt();
            assert idx >= start && idx < end;
            return data[idx];
        }
    }
}
