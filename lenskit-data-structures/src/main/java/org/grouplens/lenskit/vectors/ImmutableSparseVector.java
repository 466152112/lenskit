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
package org.grouplens.lenskit.vectors;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import org.grouplens.lenskit.symbols.Symbol;
import org.grouplens.lenskit.symbols.TypedSymbol;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * Immutable sparse vectors. These vectors cannot be changed, even by other
 * code, and are therefore safe to store and are thread-safe.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 * @compat Public
 */
@Immutable
public final class ImmutableSparseVector extends SparseVector implements Serializable {
    private static final long serialVersionUID = -2L;

    @SuppressFBWarnings("SE_BAD_FIELD")
    private final Map<Symbol, ImmutableSparseVector> channelMap;
    private final Map<TypedSymbol<?>,ImmutableTypedSideChannel<?>> typedChannelMap;

    private transient volatile Double norm = null;
    private transient volatile Double sum = null;
    private transient volatile Double mean = null;
    private transient volatile Boolean fullySet = null;
    
    /**
     * Create a new, empty immutable sparse vector.
     */
    public ImmutableSparseVector() {
        this(new long[0], new double[0]);
    }

    /**
     * Create a new immutable sparse vector from a map of ratings.
     *
     * @param ratings The ratings to make a vector from. Its key set is used as
     *                the vector's key domain.
     */
    public ImmutableSparseVector(Long2DoubleMap ratings) {
        super(ratings);
        channelMap = Collections.emptyMap();
        typedChannelMap = Collections.emptyMap();
    }

    /**
     * Construct a new vector from existing arrays.  It is assumed that the keys
     * are sorted and duplicate-free, and that the values is the same length. The
     * key array is the key domain, and all keys are considered used.
     * No new keys can be added to this vector.  Clients should call
     * the wrap() method rather than directly calling this constructor.
     *
     * @param ks The array of keys backing this vector. They must be sorted.
     * @param vs The array of values backing this vector.
     */
    protected ImmutableSparseVector(long[] ks, double[] vs) {
        this(ks, vs, ks.length);
    }

    /**
     * Construct a new sparse vector from pre-existing arrays. These arrays must
     * be sorted in key order and cannot contain duplicate keys; this condition
     * is not checked.
     *
     * @param ks The key array (will be the key domain).
     * @param vs The value array.
     * @param sz The length to actually use.
     */
    protected ImmutableSparseVector(long[] ks, double[] vs, int sz) {
        super(ks, vs, sz);
        channelMap = Collections.emptyMap();
        typedChannelMap = Collections.emptyMap();
    }

    /**
     * Construct a new sparse vector from pre-existing arrays. These arrays must
     * be sorted in key order and cannot contain duplicate keys; this condition
     * is not checked.  The new vector will have a copy of the
     * channels that are passed into it.
     *
     * @param ks            the key array (will be the key domain).
     * @param vs            the value array.
     * @param sz            the length to actually use.
     * @param used          the keys that actually have values currently.
     * @param channels      The side channel values.
     * @param typedChannels The typed side channel values.
     */
    ImmutableSparseVector(long[] ks, double[] vs, int sz, BitSet used,
                          Map<Symbol, ImmutableSparseVector> channels,
                          Map<TypedSymbol<?>,ImmutableTypedSideChannel<?>> typedChannels) {
        super(ks, vs, sz, used);
        channelMap = ImmutableMap.copyOf(channels);
        typedChannelMap = ImmutableMap.copyOf(typedChannels);
    }

    @Override
    boolean isFullySet() {
        if (fullySet == null) {
            fullySet = usedKeys.cardinality() == domainSize;
        }
        return fullySet;
    }

    @Override
    public ImmutableSparseVector immutable() {
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public MutableSparseVector mutableCopy() {
        MutableSparseVector result = new MutableSparseVector(keys, Arrays.copyOf(values, domainSize),
                                                             domainSize, (BitSet) usedKeys.clone());
        for (Map.Entry<Symbol, ImmutableSparseVector> entry : channelMap.entrySet()) {
            result.addChannel(entry.getKey(), entry.getValue().mutableCopy());
        }
        for (Entry<TypedSymbol<?>, ImmutableTypedSideChannel<?>> entry : typedChannelMap.entrySet()) {
            TypedSymbol ts = entry.getKey();
            ImmutableTypedSideChannel val = entry.getValue();
            result.addChannel(ts,val.mutableCopy());
        }
        
        return result;
    }

    @Override
    public boolean hasChannel(Symbol channelSymbol) {
        return channelMap.containsKey(channelSymbol);
    }
    @Override
    public boolean hasChannel(TypedSymbol<?> channelSymbol) {
        return typedChannelMap.containsKey(channelSymbol);
    }

    @Override
    public ImmutableSparseVector channel(Symbol channelSymbol) {
        if (hasChannel(channelSymbol)) {
            return channelMap.get(channelSymbol);
        }
        throw new IllegalArgumentException("No existing channel under name " +
                                                   channelSymbol.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K> ImmutableTypedSideChannel<K> channel(TypedSymbol<K> channelSymbol) {
        if (hasChannel(channelSymbol)) {
            return (ImmutableTypedSideChannel<K>) typedChannelMap.get(channelSymbol);
        }
        throw new IllegalArgumentException("No existing channel under name " +
                                                   channelSymbol.getName() +
                                                   " of type " + 
                                                   channelSymbol.getType().getSimpleName());
    }

    @Override
    public Set<Symbol> getChannels() {
        return channelMap.keySet();
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public Set<TypedSymbol<?>> getTypedChannels() {
        return typedChannelMap.keySet();
    }

    // We override these three functions in the case that this vector is Immutable,
    // so we can avoid computing them more than once.
    @Override
    public double norm() {
        if (norm == null) {
            norm = super.norm();
        }
        return norm;
    }

    @Override
    public double sum() {
        if (sum == null) {
            sum = super.sum();
        }
        return sum;
    }

    @Override
    public double mean() {
        if (mean == null) {
            mean = super.mean();
        }
        return mean;
    }

}
