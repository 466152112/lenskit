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
package org.grouplens.lenskit.data.vector;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

import java.util.Arrays;



/**
 * Mutable sparse vector interface
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 * <p>This extends the sparse vector with support for imperative mutation
 * operations on their values, but
 * once created the set of keys remains immutable.  Addition and subtraction are
 * supported.  Mutation operations also operate in-place to reduce the
 * reallocation and copying required.  Therefore, a common pattern is:
 *
 * <pre>
 * MutableSparseVector normalized = MutableSparseVector.copy(vector);
 * normalized.subtract(normFactor);
 * </pre>
 *
 */
public class MutableSparseVector extends SparseVector {
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new empty vector.
     */
    public MutableSparseVector() {
        this(new long[0], new double[0]);
    }

    /**
     * Construct a new vector from the contents of a map.
     * @param ratings A map providing the values for the vector.
     */
    public MutableSparseVector(Long2DoubleMap ratings) {
        super(ratings);
    }

    /**
     * Construct a new zero vector with specified keys.
     * @param keySet The keys to include in the vector.
     */
    public MutableSparseVector(LongSet keySet) {
        this(keySet, 0);
    }

    /**
     * Construct a new vector with specified keys, setting all values to a constant
     * value.
     * @param keySet The keys to include in the vector.
     * @param value The value to assign for all keys.
     */
    public MutableSparseVector(LongSet keySet, double value) {
        super(normalizeKeys(keySet), new double[keySet.size()]);
        Arrays.fill(values, value);
    }

    /**
     * Construct a new vector from existing arrays.  It is assumed that the keys
     * are sorted and duplicate-free, and that the values is the same length.
     * @param keys
     * @param values
     */
    protected MutableSparseVector(long[] keys, double[] values) {
        super(keys, values);
    }

    /**
     * Construct a new vector from existing arrays.  It is assumed that the keys
     * are sorted and duplicate-free, and that the values is the same length.
     * @param keys
     * @param values
     * @param length Number of items to actually use.
     */
    protected MutableSparseVector(long[] keys, double[] values, int length) {
        super(keys, values, length);
    }

    static long[] normalizeKeys(LongSet set) {
        long[] keys = set.toLongArray();
        if (!(set instanceof LongSortedSet))
            Arrays.sort(keys);
        return keys;
    }

    /**
     * Set a value in the vector
     * @param key The key of the value to set.
     * @param value The value to set.
     * @return The original value, or {@link Double#NaN} if there was no key
     * (or if the original value was {@link Double#NaN}).
     */
    public final double set(long key, double value) {
        checkValid();
        final int idx = Arrays.binarySearch(keys, key);
        if (idx >= 0) {
            double v = values[idx];
            values[idx] = value;
            clearCachedValues();
            return v;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Add a value to the specified entry.
     * @param key The key whose value should be added.
     * @param value The value to increase it by.
     * @return The new value (or {@link Double#NaN} if no such key existed).
     */
    public final double add(long key, double value) {
        checkValid();
        final int idx = Arrays.binarySearch(keys, key);
        if (idx >= 0) {
            clearCachedValues();
            return values[idx] += value;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Add a value to the specified entry, replacing {@link Double#NaN}.  If the
     * current value for <var>key</var> is {@link Double#NaN}, then the value is
     * replaced by <var>value</var>; otherwise, it is increased by <var>value</var>.
     * @param key The key whose value should be added.
     * @param value The value to increase it by.
     * @return The new value (or {@link Double#NaN} if no such key existed).
     */
    public final double addOrReplace(long key, double value) {
        checkValid();
        final int idx = Arrays.binarySearch(keys, key);
        if (idx >= 0) {
            clearCachedValues();
            if (Double.isNaN(values[idx]))
                return values[idx] = value;
            else
                return values[idx] += value;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Subtract another rating vector from this one.
     *
     * <p>After calling this method, every element of this vector has been
     * decreased by the corresponding element in <var>other</var>.  Elements
     * with no corresponding element are unchanged.
     * @param other The vector to subtract.
     */
    public final void subtract(final SparseVector other) {
        checkValid();
        other.checkValid();
        int i = 0;
        int j = 0;
        while (i < keys.length && j < other.keys.length) {
            if (keys[i] == other.keys[j]) {
                values[i] -= other.values[j];
                i++;
                j++;
            } else if (keys[i] < other.keys[j]) {
                i++;
            } else {
                j++;
            }
        }
        clearCachedValues();
    }

    /**
     * Add another rating vector to this one.
     *
     * <p>After calling this method, every element of this vector has been
     * increased by the corresponding element in <var>other</var>.  Elements
     * with no corresponding element are unchanged.
     * @param other The vector to add.
     */
    public final void add(final SparseVector other) {
        checkValid();
        other.checkValid();
        final int len = keys.length;
        final int olen = other.keys.length;
        int i = 0;
        int j = 0;
        while (i < len && j < olen) {
            if (keys[i] == other.keys[j]) {
                values[i] += other.values[j];
                i++;
                j++;
            } else if (keys[i] < other.keys[j]) {
                i++;
            } else {
                j++;
            }
        }
        clearCachedValues();
    }

    /**
     * Set the values in this SparseVector to equal the values in
     * <var>other</var> for each key that is present in both vectors.
     *
     * <p>After calling this method, every element in this vector that has a key
     * in <var>other</var> has its value set to the corresponding value in
     * <var>other</var>. Elements with no corresponding key are unchanged, and
     * elements in <var>other</var> that are not in this vector are not
     * inserted.
     *
     * @param other The vector to blit its values into this vector
     */
    public final void set(final SparseVector other) {
        checkValid();
        other.checkValid();

        final int len = keys.length;
        final int olen = other.keys.length;
        int i = 0;
        int j = 0;
        while (i < len && j < olen) {
            if (keys[i] == other.keys[j]) {
                values[i] = other.values[j];
                i++;
                j++;
            } else if (keys[i] < other.keys[j]) {
                i++;
            } else {
                j++;
            }
        }
        clearCachedValues();
    }

    /**
     * Copy the rating vector.
     * @return A new rating vector which is a copy of this one.
     */
    public final MutableSparseVector copy() {
        checkValid();
        return mutableCopy();
    }

    /**
     * Copy the rating vector, optionally removing NaN values.
     * @return A new rating vector which is a copy of this one.
     */
    public final MutableSparseVector copy(boolean removeNaN) {
        checkValid();
        if (removeNaN && !isComplete()) {
            long[] k2 = Arrays.copyOf(keys, size);
            double[] v2 = Arrays.copyOf(values, size);
            return wrap(k2, v2, true);
        } else {
            return copy();
        }
    }

    /**
     * Create a mutable copy of a sparse vector.
     * @param vector The base vector.
     * @return A mutable copy of <var>vector</var>.
     * @deprecated Use {@link #mutableCopy()} instead.
     */
    @Deprecated
    public static MutableSparseVector copy(SparseVector vector) {
        return vector.mutableCopy();
    }

    @Override
    public MutableSparseVector clone() {
        return (MutableSparseVector) super.clone();
    }

    /**
     * Construct an immutable sparse vector from this vector's data,
     * invalidating this vector in the process. Any subsequent use of this
     * vector is invalid; all access must be through the returned immutable
     * vector.
     *
     * @return An immutable vector built from this vector's data.
     */
    public ImmutableSparseVector freeze() {
        checkValid();
        ImmutableSparseVector isv = new ImmutableSparseVector(keys, values, size);
        invalidate();
        return isv;
    }

    /**
     * Freeze the vector, optionally removing NaN values. The mutable vector is
     * invalidated.
     *
     * @param removeNaN If <tt>true</tt>, remove all keys with NaN values from
     *        the final vector, resulting in a complete vector.
     * @return An immutable vector containing this vector's data, with NaN
     *         values possibly removed.
     * @see #freeze()
     * @see #copy(boolean)
     */
    public ImmutableSparseVector freeze(boolean removeNaN) {
        checkValid();
        if (!removeNaN || isComplete()) return freeze();

        // we need to remove NaN values
        long[] k2 = Arrays.copyOf(keys, size);
        double[] v2 = Arrays.copyOf(values, size);
        return wrap(k2, v2, true).freeze();
    }

    /**
     * Wrap key and value arrays in a sparse vector.
     *
     * <p>This method allows a new vector to be constructed from
     * pre-created arrays.  After wrapping arrays in a rating vector, client
     * code should not modify them (particularly the <var>items</var> array).
     *
     * @param keys Array of entry keys. This array must be in sorted order and
     * be duplicate-free.
     * @param values The values for the vector.
     * @return A sparse vector backed by the provided arrays.
     * @throws IllegalArgumentException if there is a problem with the provided
     * arrays (length mismatch, <var>keys</var> not sorted, etc.).
     */
    public static MutableSparseVector wrap(long[] keys, double[] values) {
        return wrap(keys, values, keys.length);
    }

    /**
     * Wrap key and value arrays in a sparse vector.
     *
     * <p>
     * This method allows a new vector to be constructed from pre-created
     * arrays. After wrapping arrays in a rating vector, client code should not
     * modify them (particularly the <var>items</var> array).
     *
     * @param keys Array of entry keys. This array must be in sorted order and
     *            be duplicate-free.
     * @param values The values for the vector.
     * @param size The size of the vector; only the first <var>size</var>
     *            entries from each array are actually used.
     * @return A sparse vector backed by the provided arrays.
     * @throws IllegalArgumentException if there is a problem with the provided
     *             arrays (length mismatch, <var>keys</var> not sorted, etc.).
     */
    public static MutableSparseVector wrap(long[] keys, double[] values, int size) {
        if (values.length < size)
            throw new IllegalArgumentException("value array too short");
        if (!isSorted(keys, size))
            throw new IllegalArgumentException("item array not sorted");
        return new MutableSparseVector(keys, values, size);
    }

    /**
     * Wrap key and value array lists in a mutable sparse vector. Don't modify
     * the original lists once this has been called!
     */
    public static MutableSparseVector wrap(LongArrayList keyList, DoubleArrayList valueList) {
        if (valueList.size() < keyList.size())
            throw new IllegalArgumentException("Value list too short");

        long[] keys = keyList.elements();
        double[] values = valueList.elements();

        if (!isSorted(keys, keyList.size()))
            throw new IllegalArgumentException("key array not sorted");

        return new MutableSparseVector(keys, values, keyList.size());
    }

    /**
     * Wrap key and value arrays in a sparse vector.
     *
     * <p>
     * This method allows a new vector to be constructed from pre-created
     * arrays. After wrapping arrays in a rating vector, client code should not
     * modify them (particularly the <var>items</var> array).
     *
     * <p>
     * The arrays may be modified, particularly to remove NaN values. The client
     * should not depend on them exhibiting any particular behavior after
     * calling this method.
     *
     * @param keys Array of entry keys. This array must be in sorted order and
     *            be duplicate-free.
     * @param values The values for the vector.
     * @param removeNaN If true, remove NaN values from the arrays.
     * @return A sparse vector backed by the provided arrays.
     * @throws IllegalArgumentException if there is a problem with the provided
     *             arrays (length mismatch, <var>keys</var> not sorted, etc.).
     * @see #wrap(long[], double[])
     */
    public static MutableSparseVector wrap(long[] keys, double[] values, boolean removeNaN) {
        if (values.length < keys.length)
            throw new IllegalArgumentException("key/value length mismatch");

        int length = keys.length;
        if (removeNaN) {
            length = 0;
            for (int i = 0; i < keys.length; i++) {
                if (!Double.isNaN(values[i])) {
                    if (i != length) {
                        keys[length] = keys[i];
                        values[length] = values[i];
                    }
                    length++;
                }
            }
        }
        return wrap(keys, values, length);
    }
}
