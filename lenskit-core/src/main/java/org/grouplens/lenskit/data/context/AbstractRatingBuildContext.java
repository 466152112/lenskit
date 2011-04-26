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
package org.grouplens.lenskit.data.context;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.grouplens.lenskit.norm.NormalizedRatingSnapshot;
import org.grouplens.lenskit.norm.UserRatingVectorNormalizer;

public abstract class AbstractRatingBuildContext implements RatingBuildContext {
    private final Map<Object, Object> cachedValues;
    private final IdentityHashMap<UserRatingVectorNormalizer, NormalizedRatingSnapshot>
        normalizedSnapshots;
    
    public AbstractRatingBuildContext() {
        cachedValues = new ConcurrentHashMap<Object, Object>();
        normalizedSnapshots = new IdentityHashMap<UserRatingVectorNormalizer, NormalizedRatingSnapshot>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Key<T> key) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        return (T) cachedValues.get(key);
    }

    @Override
    public <T> void put(Key<T> key, T value) {
        if (value == null || key == null)
            throw new NullPointerException("Key and value cannot be null");
        cachedValues.put(key, value);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void close() {
        // Help the garbage collector out and clear the cache now
        cachedValues.clear();
        synchronized (normalizedSnapshots) {
            normalizedSnapshots.clear();
        }
    }
}
