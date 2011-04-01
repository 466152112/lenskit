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
package org.grouplens.lenskit.knn.user;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.Collection;
import java.util.PriorityQueue;

import org.grouplens.common.cursors.Cursor;
import org.grouplens.lenskit.data.UserRatingProfile;
import org.grouplens.lenskit.data.dao.RatingDataAccessObject;
import org.grouplens.lenskit.data.vector.SparseVector;
import org.grouplens.lenskit.knn.Similarity;
import org.grouplens.lenskit.knn.params.NeighborhoodSize;
import org.grouplens.lenskit.knn.params.UserSimilarity;

import com.google.inject.Inject;

/**
 * Neighborhood finder that does a fresh search over the data source ever time.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class SimpleNeighborhoodFinder implements NeighborhoodFinder {
    private final RatingDataAccessObject dataSource;
    private final int neighborhoodSize;
    private final Similarity<? super SparseVector> similarity;

    /**
     * Construct a new user-user recommender.
     * @param data The data source to scan.
     * @param nnbrs The number of neighbors to consider for each item.
     * @param similarity The similarity function to use.
     */
    @Inject
    SimpleNeighborhoodFinder(RatingDataAccessObject data,
            @NeighborhoodSize int nnbrs,
            @UserSimilarity Similarity<? super SparseVector> similarity) {
        dataSource = data;
        neighborhoodSize = nnbrs;
        this.similarity = similarity;
    }

    /**
     * Find the neighbors for a user with respect to a collection of items.
     * For each item, the <var>neighborhoodSize</var> users closest to the
     * provided user are returned.
     *
     * @param uid The user ID.
     * @param ratings The user's ratings vector.
     * @param items The items for which neighborhoods are requested.
     * @return A mapping of item IDs to neighborhoods.
     */
    @Override
    public Long2ObjectMap<? extends Collection<Neighbor>> findNeighbors(long uid, SparseVector ratings, LongSet items) {
        Long2ObjectMap<PriorityQueue<Neighbor>> heaps =
            new Long2ObjectOpenHashMap<PriorityQueue<Neighbor>>(items != null ? items.size() : 100);

        Cursor<UserRatingProfile> users = dataSource.getUserRatingProfiles();

        try {
            for (UserRatingProfile user: users) {
                if (user.getUser() == uid) continue;

                final SparseVector urv = user.getRatingVector();
                final double sim = similarity.similarity(ratings, urv);
                final Neighbor n = new Neighbor(user.getUser(), urv, sim);

                LongIterator iit = urv.keySet().iterator();
                ITEMS: while (iit.hasNext()) {
                    final long item = iit.nextLong();
                    if (items != null && !items.contains(item))
                        continue ITEMS;

                    PriorityQueue<Neighbor> heap = heaps.get(item);
                    if (heap == null) {
                        heap = new PriorityQueue<Neighbor>(neighborhoodSize + 1,
                                Neighbor.SIMILARITY_COMPARATOR);
                        heaps.put(item, heap);
                    }
                    heap.add(n);
                    if (heap.size() > neighborhoodSize) {
                        assert heap.size() == neighborhoodSize + 1;
                        heap.remove();
                    }
                }
            }
        } finally {
            users.close();
        }
        return heaps;
    }
}
