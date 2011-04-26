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
package org.grouplens.lenskit.data;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.annotation.WillClose;

import org.grouplens.common.cursors.Cursor;
import org.grouplens.lenskit.data.vector.MutableSparseVector;
import org.grouplens.lenskit.data.vector.SparseVector;
import org.grouplens.lenskit.util.FastCollection;

/**
 * Utilities for working with ratings.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public final class Ratings {

    public static final Comparator<Rating> TIMESTAMP_COMPARATOR = new Comparator<Rating>() {
        @Override
        public int compare(Rating r1, Rating r2) {
            Long ts1 = r1.getTimestamp();
            Long ts2 = r2.getTimestamp();
            return ts1.compareTo(ts2);
        }
    };
    public static final Comparator<Rating> USER_COMPARATOR = new Comparator<Rating>() {
        @Override
        public int compare(Rating r1, Rating r2) {
            Long ts1 = r1.getUserId();
            Long ts2 = r2.getUserId();
            return ts1.compareTo(ts2);
        }
    };
    public static final Comparator<Rating> ITEM_COMPARATOR = new Comparator<Rating>() {
        @Override
        public int compare(Rating r1, Rating r2) {
            Long ts1 = r1.getItemId();
            Long ts2 = r2.getItemId();
            return ts1.compareTo(ts2);
        }
    };

    /**
     * Construct a rating vector that contains the ratings provided by each user.
     * If all ratings in <var>ratings</var> are for the same item, then this
     * will be a valid item rating vector.  If multiple ratings are by the same
     * user, the one with the highest timestamp is retained.  If two ratings
     * by the same user have identical timestamps, then the one that occurs last
     * when the collection is iterated is retained.
     *
     * @param ratings Some ratings (they should all be for the same item)
     * @return A sparse vector mapping user IDs to ratings.
     */
    public static MutableSparseVector itemRatingVector(Collection<? extends Rating> ratings) {
        Long2DoubleMap vect = new Long2DoubleOpenHashMap(ratings.size());
        Long2LongMap tsMap = new Long2LongOpenHashMap(ratings.size());
        tsMap.defaultReturnValue(Long.MIN_VALUE);
        for (Rating r: ratings) {
            long uid = r.getUserId();
            long ts = r.getTimestamp();
            if (ts >= tsMap.get(uid)) {
                vect.put(uid, r.getRating());
                tsMap.put(uid, ts);
            }
        }
        return new MutableSparseVector(vect);
    }

    /**
     * Construct a rating vector that contains the ratings provided for each item.
     * If all ratings in <var>ratings</var> are by the same user, then this will
     * be a valid user rating vector.  If multiple ratings are provided for the
     * same item, the one with the greatest timestamp is retained.  Ties are
     * broken by preferring ratings which come later when iterating through the
     * collection.
     *
     * @param ratings A collection of ratings (should all be by the same user)
     * @return A sparse vector mapping item IDs to ratings
     */
    public static MutableSparseVector userRatingVector(Collection<? extends Rating> ratings) {
        Long2DoubleMap vect = new Long2DoubleOpenHashMap(ratings.size());
        Long2LongMap tsMap = new Long2LongOpenHashMap(ratings.size());
        tsMap.defaultReturnValue(Long.MIN_VALUE);
        
        // Take advantage of fast iteration if possible
        Iterable<? extends Rating> riter = ratings;
        if (ratings instanceof FastCollection)
            riter = ((FastCollection<? extends Rating>) ratings).fast();
        for (Rating r: riter) {
            long iid = r.getItemId();
            long ts = r.getTimestamp();
            if (ts >= tsMap.get(iid)) {
                vect.put(r.getItemId(), r.getRating());
                tsMap.put(iid, ts);
            }
        }
        return new MutableSparseVector(vect);
    }
    
    public static MutableSparseVector userRatingVector(@WillClose Cursor<? extends Rating> ratings) {
        try {
            int initialSize = ratings.getRowCount();
            if (initialSize < 0) initialSize = 20;
            Long2DoubleMap vect = new Long2DoubleOpenHashMap(initialSize);
            Long2LongMap tsMap = new Long2LongOpenHashMap(initialSize);
            tsMap.defaultReturnValue(Long.MIN_VALUE);
            
            for (Rating r: ratings) {
                long iid = r.getItemId();
                long ts = r.getTimestamp();
                if (ts >= tsMap.get(iid)) {
                    vect.put(r.getItemId(), r.getRating());
                    tsMap.put(iid, ts);
                }
            }
            return new MutableSparseVector(vect);
        } finally {
            ratings.close();
        }
        
    }
    
    /**
     * Convert a sparse vector into a rating list
     */
    public static List<Rating> fromUserVector(long user, SparseVector v) {
        List<Rating> ratings = new ArrayList<Rating>(v.size());
        for (Long2DoubleMap.Entry e: v.fast()) {
            ratings.add(new SimpleRating(user, e.getLongKey(), e.getDoubleValue()));
        }
        return ratings;
    }
}
