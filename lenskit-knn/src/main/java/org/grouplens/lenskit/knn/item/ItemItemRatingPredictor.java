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
package org.grouplens.lenskit.knn.item;

import static java.lang.Math.abs;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

import java.util.Collection;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.grouplens.lenskit.RatingPredictor;
import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.data.ScoredId;
import org.grouplens.lenskit.data.vector.MutableSparseVector;
import org.grouplens.lenskit.data.vector.SparseVector;
import org.grouplens.lenskit.norm.VectorTransformation;
import org.grouplens.lenskit.util.IndexedItemScore;
import org.grouplens.lenskit.util.LongSortedArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class ItemItemRatingPredictor implements RatingPredictor {
    private static final Logger logger = LoggerFactory.getLogger(ItemItemRatingPredictor.class);
    
    protected final ItemItemRecommender model;
    private BaselinePredictor baseline;
    private final double similarityThreshold;
    
    public ItemItemRatingPredictor(ItemItemRecommender model, double simThresh) {
        this.model = model;
        similarityThreshold = simThresh;
    }
    
    /**
     * Set the baseline predictor to use for this predictor.
     * @param baseline The baseline predictor
     */
    protected void setBaseline(BaselinePredictor baseline) {
        logger.debug("Using baseline {}", baseline);
        this.baseline = baseline;
    }
    
    /**
     * Get the baseline predictor.
     * @return The baseline predictor if one has been configured.
     */
    @Nullable @CheckForNull
    protected RatingPredictor getBaselinePredictor() {
        return baseline;
    }
    
    public ItemItemRecommender getRecommender() {
        return model;
    }
    
    public LongSet getPredictableItems(long user, SparseVector ratings) {
        if (getBaselinePredictor() != null) {
            return model.getItemUniverse();
        } else {
            LongSet items = new LongOpenHashSet();
            LongIterator iter = ratings.keySet().iterator();
            while (iter.hasNext()) {
                final long item = iter.nextLong();
                for (IndexedItemScore n: model.getNeighbors(item)) {
                    items.add(model.getItem(n.getIndex()));
                }
            }
            return items;
        }
    }
    
    @Override
    public ScoredId predict(long user, SparseVector ratings, long item) {
        VectorTransformation norm = model.normalizingTransformation(user, ratings);
        MutableSparseVector normed = ratings.mutableCopy();
        norm.apply(normed);
        double sum = 0;
        double totalWeight = 0;
        for (IndexedItemScore score: model.getNeighbors(item)) {
            long other = model.getItem(score.getIndex());
            double s = score.getScore();
            if (normed.containsKey(other)) {
                // FIXME this goes wacky with negative similarities
                double rating = normed.get(other);
                sum += rating * s;
                totalWeight += abs(s);
            }
        }
        
        RatingPredictor baseline;
        
        if (totalWeight >= similarityThreshold) {
            // denormalize
            long[] keys = {item};
            double[] preds = {sum / totalWeight};
            MutableSparseVector v = MutableSparseVector.wrap(keys, preds);
            norm.unapply(v);
            return new ScoredId(item, preds[0]);
        } else if ((baseline = getBaselinePredictor()) != null) {
            // fall back to baseline
            return baseline.predict(user, ratings, item);
        } else {
            return null;
        }
    }

    @Override
    public SparseVector predict(long user, SparseVector ratings, Collection<Long> items) {
        VectorTransformation norm = model.normalizingTransformation(user, ratings);
        MutableSparseVector normed = ratings.mutableCopy();
        norm.apply(normed);

        LongSortedSet iset;
        if (items instanceof LongSortedSet)
            iset = (LongSortedSet) items;
        else
            iset = new LongSortedArraySet(items);

        MutableSparseVector sums = new MutableSparseVector(iset);
        MutableSparseVector weights = new MutableSparseVector(iset);
        for (Long2DoubleMap.Entry rating: normed.fast()) {
            final double r = rating.getDoubleValue();
            for (IndexedItemScore score: model.getNeighbors(rating.getLongKey())) {
                final double s = score.getScore();
                final int idx = score.getIndex();
                final long iid = model.getItem(idx);
                weights.add(iid, abs(s));
                sums.add(iid, s*r);
            }
        }

        // create lists to accumulate the predictable items
        LongArrayList predItems = new LongArrayList(sums.size());
        DoubleArrayList predValues = new DoubleArrayList(sums.size());
        LongArrayList unpredItems = new LongArrayList();
        
        // Divide by weight into accumulators.
        LongIterator iter = sums.keySet().iterator();
        while (iter.hasNext()) {
            final long iid = iter.next();
            final double w = weights.get(iid);
            if (w >= similarityThreshold) {
                predItems.add(iid);
                predValues.add(sums.get(iid) / w);
            } else {
                unpredItems.add(iid);
            }
        }
        
        // Create a vector for the predictions and normalize it
        MutableSparseVector preds = MutableSparseVector.wrap(predItems, predValues);
        norm.unapply(preds);
        
        final RatingPredictor baseline = getBaselinePredictor();
        if (baseline != null) {
            SparseVector basePreds = baseline.predict(user, ratings, unpredItems);
            // Re-use the sums vector to merge predictions with baseline
            for (Long2DoubleMap.Entry e: preds.fast()) {
                final long iid = e.getLongKey();
                assert !basePreds.containsKey(iid);
                sums.set(iid, e.getDoubleValue());
            }
            for (Long2DoubleMap.Entry e: basePreds.fast()) {
                final long iid = e.getLongKey();
                assert !preds.containsKey(iid);
                sums.set(iid, e.getDoubleValue());
            }
            return sums;
        } else {
            return preds;
        }
    }

}
