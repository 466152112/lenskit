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
package org.grouplens.lenskit.data.snapshot;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import org.grouplens.lenskit.data.Ratings;
import org.grouplens.lenskit.data.vector.SparseVector;

public abstract class AbstractRatingSnapshot implements RatingSnapshot {

	protected volatile Long2ObjectMap<SparseVector> cache;
	
	public AbstractRatingSnapshot() {
		cache = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<SparseVector>());
	}
	
	@SuppressWarnings("deprecation")
    @Override
	public SparseVector userRatingVector(long userId) {
		SparseVector data = cache.get(userId);
		if (data != null) {
			return data;
		}
		else {
			data = Ratings.userRatingVector(this.getUserRatings(userId));
			cache.put(userId, data);
			return data;
		}
	}
	
	@Override
    @OverridingMethodsMustInvokeSuper
	public void close() {
		cache = null;
	}
}
