package org.grouplens.lenskit.vectors;

import it.unimi.dsi.fastutil.longs.AbstractLong2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.*;
import org.grouplens.lenskit.collections.CollectionUtils;

import java.util.Iterator;

/**
 * Shim implementing {@link it.unimi.dsi.fastutil.longs.Long2ObjectMap} on top of a {@link SparseVector}.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class SparseVectorMap extends AbstractLong2ObjectMap<Double> {
    final SparseVector vector;

    /**
     * Create a new shim.
     * @param vec The vector to wrap.
     */
    SparseVectorMap(SparseVector vec) {
        vector = vec;
    }

    SparseVector getVector() {
        return vector;
    }

    @Override
    public ObjectSet<Entry<Double>> long2ObjectEntrySet() {
        return new EntrySetImpl();
    }

    @Override
    public LongSet keySet() {
        return vector.keySet();
    }

    @Override
    public ObjectCollection<Double> values() {
        return CollectionUtils.objectCollection(vector.values());
    }

    @Override
    public Double get(long key) {
        return vector.get(key, defaultReturnValue());
    }

    @Override
    public boolean containsKey(long key) {
        return vector.containsKey(key);
    }

    @Override
    public int size() {
        return vector.size();
    }

    /**
     * Implement the entry set.
     */
    class EntrySetImpl extends AbstractObjectSet<Entry<Double>> implements FastEntrySet<Double> {
        @Override
        public ObjectIterator<Entry<Double>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return vector.size();
        }

        @Override
        public ObjectIterator<Entry<Double>> fastIterator() {
            return new FastEntryIterator();
        }
    }

    /**
     * Implement a map entry iterator.
     */
    class EntryIterator extends AbstractObjectIterator<Entry<Double>> {
        Iterator<VectorEntry> delegate = vector.fastIterator();

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public Entry<Double> next() {
            VectorEntry e = delegate.next();
            return new BasicEntry<Double>(e.getKey(), (Double) e.getValue());
        }
    }

    /**
     * Implement a fast map iterator.
     */
    class FastEntryIterator extends AbstractObjectIterator<Entry<Double>> {
        Iterator<VectorEntry> delegate = vector.fastIterator();
        EntryShim entry = new EntryShim(null);

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public Entry<Double> next() {
            VectorEntry e = delegate.next();
            entry.setEntry(e);
            return entry;
        }
    }

    /**
     * Implement a simple entry shim on top of a vector entry.
     */
    static class EntryShim implements Entry<Double> {
        VectorEntry entry;

        public EntryShim(VectorEntry e) {
            entry = e;
        }

        void setEntry(VectorEntry e) {
            entry = e;
        }

        @Override
        public long getLongKey() {
            return entry.getKey();
        }

        @Override
        public Long getKey() {
            return entry.getKey();
        }

        @Override
        public Double getValue() {
            return entry.getValue();
        }

        @Override
        public Double setValue(Double value) {
            throw new UnsupportedOperationException("setValue not supported");
        }
    }
}
