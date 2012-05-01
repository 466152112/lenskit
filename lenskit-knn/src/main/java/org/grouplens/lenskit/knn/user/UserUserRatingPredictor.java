/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2012 Regents of the University of Minnesota and contributors
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

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.grouplens.lenskit.RatingPredictor;
import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.collections.LongSortedArraySet;
import org.grouplens.lenskit.core.AbstractItemScorer;
import org.grouplens.lenskit.data.Event;
import org.grouplens.lenskit.data.UserHistory;
import org.grouplens.lenskit.data.dao.DataAccessObject;
import org.grouplens.lenskit.data.history.RatingVectorUserHistorySummarizer;
import org.grouplens.lenskit.data.history.UserVector;
import org.grouplens.lenskit.norm.UserVectorNormalizer;
import org.grouplens.lenskit.norm.VectorTransformation;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;

import static java.lang.Math.abs;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class UserUserRatingPredictor extends AbstractItemScorer implements RatingPredictor {
    private static final double MINIMUM_SIMILARITY = 0.001;
    private static final Logger logger = LoggerFactory.getLogger(UserUserRatingPredictor.class);
    protected final NeighborhoodFinder neighborhoodFinder;
    protected final UserVectorNormalizer normalizer;
    protected final BaselinePredictor baseline;

    @Inject
    public UserUserRatingPredictor(DataAccessObject dao, NeighborhoodFinder nbrf,
                                   UserVectorNormalizer norm,
                                   @Nullable BaselinePredictor baseline) {
        super(dao);
        neighborhoodFinder = nbrf;
        normalizer = norm;
        this.baseline = baseline;
        logger.debug("Built predictor with baseline {}", baseline);
    }

    /**
     * Normalize all neighbor rating vectors, taking care to normalize each one
     * only once.
     *
     * FIXME: MDE does not like this method.
     *
     * @param neighborhoods
     *
     */
    protected Reference2ObjectMap<UserVector, SparseVector> normalizeNeighborRatings(Collection<? extends Collection<Neighbor>> neighborhoods) {
        Reference2ObjectMap<UserVector, SparseVector> normedVectors =
            new Reference2ObjectOpenHashMap<UserVector, SparseVector>();
        for (Neighbor n: Iterables.concat(neighborhoods)) {
            if (!normedVectors.containsKey(n.user)) {
                normedVectors.put(n.user, normalizer.normalize(n.user, null));
            }
        }
        return normedVectors;
    }

    /**
     * Get predictions for a set of items.  Unlike the interface method, this
     * method can take a null <var>items</var> set, in which case it returns all
     * possible predictions.
     * @see RatingPredictor#score(long, Collection)
     */
    @Override
    public SparseVector score(UserHistory<? extends Event> history,
                              @Nullable Collection<Long> items) {
        logger.trace("Predicting for user {} with {} events",
                     history.getUserId(), history.size());
        LongSortedSet iset;
        if (items == null) {
            iset = null;
        } else if (items instanceof LongSortedSet) {
            iset = (LongSortedSet) items;
        } else {
            iset = new LongSortedArraySet(items);
        }
        Long2ObjectMap<? extends Collection<Neighbor>> neighborhoods =
            neighborhoodFinder.findNeighbors(history, iset);
        Reference2ObjectMap<UserVector, SparseVector> normedUsers =
            normalizeNeighborRatings(neighborhoods.values());
        
        MutableSparseVector preds = new MutableSparseVector(iset);
        LongArrayList missing = new LongArrayList();
        LongIterator iter = iset.iterator();
        while (iter.hasNext()) {
            final long item = iter.nextLong();
            double sum = 0;
            double weight = 0;
            Collection<Neighbor> nbrs = neighborhoods.get(item);
            if (nbrs != null) {
                for (final Neighbor n: neighborhoods.get(item)) {
                    weight += abs(n.similarity);
                    sum += n.similarity * normedUsers.get(n.user).get(item);
                }
            }

            if (weight >= MINIMUM_SIMILARITY) {
                logger.trace("Total neighbor weight for item {} is {}", item, weight);
                preds.set(item, sum / weight);
            } else {
                missing.add(item);
            }
        }

        // Denormalize and return the results
        UserVector urv = RatingVectorUserHistorySummarizer.makeRatingVector(history);
        VectorTransformation vo = normalizer.makeTransformation(urv);
        vo.unapply(preds);

        // Use the baseline
        if (baseline != null && missing.size() > 0) {
            logger.trace("Filling in {} missing predictions with baseline",
                         missing.size());
            MutableSparseVector basePreds = baseline.predict(urv, missing);
            preds.set(basePreds);
        }
        
        return preds;
    }
}
