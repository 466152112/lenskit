package org.grouplens.reflens.data;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleCollections;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.grouplens.reflens.util.LongSortedArraySet;

/**
 * Sparse vector representation
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 * 
 * <p>This vector class works a lot like a map, but it also caches some
 * commonly-used statistics.  The values are stored in parallel arrays sorted
 * by ID.  This allows fast lookup and sorted iteration.  All iterators access
 * the items in key ID.
 *
 */
public class SparseVector implements Iterable<Long2DoubleMap.Entry>, Serializable {
	private static final long serialVersionUID = 5097272716721395321L;
	protected final long[] keys;
	protected final double[] values;
	private transient Double norm;
	private transient Double sum;
	private transient Double mean;

	public SparseVector(Long2DoubleMap ratings) {
		keys = ratings.keySet().toLongArray();
		Arrays.sort(keys);
		assert keys.length == ratings.size();
		values = new double[keys.length];
		for (int i = 0; i < keys.length; i++) {
			values[i] = ratings.get(keys[i]);
		}
	}
	
	/**
	 * Construct a new sparse vector from existing arrays.  The arrays must
	 * be sorted by key; no checking is done.
	 * @param keys The array of keys.
	 * @param values The keys' values.
	 */
	protected SparseVector(long[] keys, double[] values) {
		this.keys = keys;
		this.values = values;
	}

	protected void clearCachedValues() {
		norm = null;
		sum = null;
		mean = null;
	}

	/**
	 * Get the rating for <var>id</var>.
	 * @param id the item or user ID for which the rating is desired
	 * @return the rating (or {@link Double.NaN} if no such rating exists)
	 */
	public double get(long id) {
		return get(id, Double.NaN);
	}

	/**
	 * Get the rating for <var>id</var>.
	 * @param id the item or user ID for which the rating is desired
	 * @param dft The rating to return if no such rating exists
	 * @return the rating (or <var>dft</var> if no such rating exists)
	 */
	public double get(long id, double dft) {
		int idx = Arrays.binarySearch(keys, id);
		if (idx >= 0)
			return values[idx];
		else
			return dft;
	}

	public boolean containsId(long id) {
		return Arrays.binarySearch(keys, id) >= 0;
	}

	/**
	 * Iterate over all entries.
	 * @return an iterator over all ID/Rating pairs.
	 */
	@Override
	public Iterator<Long2DoubleMap.Entry> iterator() {
		return new IterImpl();
	}

	/**
	 * Fast iterator over all entries (it can reuse entry objects).
	 * @see Long2DoubleMap.FastEntrySet#fastIterator()
	 * @return a fast iterator over all ID/Rating pairs
	 */
	public Iterator<Long2DoubleMap.Entry> fastIterator() {
		return new FastIterImpl();
	}

	public Iterable<Long2DoubleMap.Entry> fast() {
		return new Iterable<Long2DoubleMap.Entry>() {
			public Iterator<Long2DoubleMap.Entry> iterator() {
				return fastIterator();
			}
		};
	}

	public LongSortedSet idSet() {
		return new LongSortedArraySet(keys);
	}

	public DoubleCollection values() {
		return DoubleCollections.unmodifiable(new DoubleArrayList(values));
	}

	public int size() {
		return keys.length;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Compute and return the L2 norm (Euclidian length) of the vector.
	 * @return The L2 norm of the vector
	 */
	public double norm() {
		if (norm == null) {
			double ssq = 0;
			for (int i = 0; i < values.length; i++) {
				double v = values[i];
				ssq += v * v;
			}
			norm = Math.sqrt(ssq);
		}
		return norm;
	}

	/**
	 * Compute and return the L1 norm (sum) of the vector
	 * @return the sum of the vector's values
	 */
	public double sum() {
		if (sum == null) {
			double s = 0;
			for (int i = 0; i < values.length; i++) {
				s += values[i];
			}
			sum = s;
		}
		return sum;
	}

	/**
	 * Compute and return the mean of the vector's values
	 * @return the mean of the vector
	 */
	public double mean() {
		if (mean == null) {
			mean = keys.length > 0 ? sum() / keys.length : 0;
		}
		return mean;
	}

	/**
	 * Compute the dot product of two vectors.
	 * @param other The vector to dot-product with.
	 * @return The dot product of this vector and <var>other</var>.
	 */
	public double dot(SparseVector other) {
		double dot = 0;
		int i = 0;
		int j = 0;
		while (i < keys.length && j < other.keys.length) {
			if (keys[i] == other.keys[j]) {
				dot += values[i] * other.values[j];
				i++;
				j++;
			} else if (keys[i] < other.keys[j]) {
				i++;
			} else {
				j++;
			}
		}
		return dot;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o instanceof MutableSparseVector) {
			MutableSparseVector vo = (MutableSparseVector) o;
			return Arrays.equals(keys, vo.keys) && Arrays.equals(values, vo.values);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return keys.hashCode() ^ values.hashCode();
	}
	
	final class IterImpl implements Iterator<Long2DoubleMap.Entry> {
		int pos = 0;
		@Override
		public boolean hasNext() {
			return pos < keys.length;
		}
		@Override
		public Entry next() {
			if (hasNext())
				return new Entry(pos++);
			else
				throw new NoSuchElementException();
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	final class FastIterImpl implements Iterator<Long2DoubleMap.Entry> {
		Entry entry = new Entry(-1);
		@Override
		public boolean hasNext() {
			return entry.pos < keys.length - 1;
		}
		@Override
		public Entry next() {
			if (hasNext()) {
				entry.pos += 1;
				return entry;
			} else {
				throw new NoSuchElementException();
			}
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	private final class Entry implements Long2DoubleMap.Entry {
		int pos;
		public Entry(int p) {
			pos = p;
		}
		@Override
		public double getDoubleValue() {
			return values[pos];
		}
		@Override
		public long getLongKey() {
			return keys[pos];
		}
		@Override
		public double setValue(double value) {
			throw new UnsupportedOperationException();
		}
		@Override
		public Long getKey() {
			return getLongKey();
		}
		@Override
		public Double getValue() {
			return getDoubleValue();
		}
		@Override
		public Double setValue(Double value) {
			return setValue(value.doubleValue());
		}
	}

}