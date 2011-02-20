/*
 * RefLens, a reference implementation of recommender algorithms.
 * Copyright 2010 Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but
 * you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package org.grouplens.reflens.svd;

import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.Arrays;
import java.util.Collection;

import org.grouplens.reflens.BasketRecommender;
import org.grouplens.reflens.RatingPredictor;
import org.grouplens.reflens.RatingRecommender;
import org.grouplens.reflens.RecommenderService;
import org.grouplens.reflens.data.Index;
import org.grouplens.reflens.data.MutableSparseVector;
import org.grouplens.reflens.data.ScoredId;
import org.grouplens.reflens.data.SparseVector;
import org.grouplens.reflens.util.CollectionUtils;
import org.grouplens.reflens.util.DoubleFunction;

/**
 * Do recommendations and predictions based on SVD matrix factorization.
 * 
 * Recommendation is done based on folding-in.  The strategy is do a fold-in
 * operation as described in
 * <a href="http://www.grouplens.org/node/212">Sarwar et al., 2002</a> with the
 * user's ratings.
 * 
 * @todo Look at using the user's feature preferences in some cases.
 * 
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class SVDRecommenderService implements RecommenderService, RatingPredictor {
	
	private final Index itemIndex;
	private final RatingPredictor baseline;

	private final int numFeatures;
	private final double itemFeatures[][];
	private final double singularValues[];
	private final DoubleFunction clampingFunction;
	
	SVDRecommenderService(int nfeatures, Index itemIndexer,
			RatingPredictor baseline, double itemFeatures[][],
			double singularValues[],
			DoubleFunction clamp) { 		
		numFeatures = nfeatures;
		this.itemIndex = itemIndexer;
		this.baseline = baseline;
		this.itemFeatures = itemFeatures;
		this.singularValues = singularValues;
		clampingFunction = clamp;
		assert itemFeatures.length == numFeatures;
		assert singularValues.length == numFeatures;
	}
	
	/**
	 * Get the number of features used by the underlying factorization.
	 * @return the feature count (rank) of the factorization.
	 */
	public int getFeatureCount() {
		return numFeatures;
	}
	
	/**
	 * Fold in a user's ratings vector to produce a feature preference vector.
	 * A baseline vector is also provided; its values are subtracted from the
	 * rating vector prior to folding in.
	 * @param user The user ID.
	 * @param ratings The user's rating vector.
	 * @param base The user's baseline vector (e.g. baseline predictions).
	 * @return An array of feature preference values.  The length of this array
	 * will be the number of features.
	 * @see #getFeatureCount()
	 */
	protected double[] foldIn(long user, SparseVector ratings, SparseVector base) {
		double featurePrefs[] = new double[numFeatures];
		DoubleArrays.fill(featurePrefs, 0.0);
		
		for (Long2DoubleMap.Entry rating: ratings.fast()) {
			long iid = rating.getLongKey();
			int idx = itemIndex.getIndex(iid);
			if (idx < 0) continue;
			double r = rating.getValue() - base.get(iid);
			for (int f = 0; f < numFeatures; f++) {
				featurePrefs[f] += r * itemFeatures[f][idx] / singularValues[f];
			}
		}
		
		return featurePrefs;
	}

	/* (non-Javadoc)
	 * @see org.grouplens.reflens.RecommenderService#predict(org.grouplens.reflens.data.UserRatingProfile, java.lang.Object)
	 */
	@Override
	public ScoredId predict(long user, SparseVector ratings, long item) {
		LongArrayList items = new LongArrayList(1);
		items.add(item);
		SparseVector scores = predict(user, ratings, items);
		
		if (scores.containsId(item))
			return new ScoredId(item, scores.get(item));
		else
			return null;
	}
	
	@Override
	public MutableSparseVector predict(long user, SparseVector ratings, Collection<Long> items) {
		LongSet tgtids = new LongOpenHashSet(ratings.keySet());
		tgtids.addAll(items);
		SparseVector base = baseline.predict(user, ratings, tgtids);
		double uprefs[] = foldIn(user, ratings, base);
		
		long[] keys = CollectionUtils.fastCollection(items).toLongArray();
		Arrays.sort(keys);
		double[] values = new double[keys.length];
		for (int i = 0; i < keys.length; i++) {
			final long item = keys[i];
			final int idx = itemIndex.getIndex(item);
			if (idx < 0)
				continue;

			double score = base.get(item);
			for (int f = 0; f < numFeatures; f++) {
				score += uprefs[f] * singularValues[f] * itemFeatures[f][idx];
				score = clampingFunction.apply(score);
			}
			values[i] = score;
		}
		return MutableSparseVector.wrap(keys, values);
	}

	@Override
	public BasketRecommender getBasketRecommender() {
		return null;
	}

	@Override
	public RatingPredictor getRatingPredictor() {
		return this;
	}

	@Override
	public RatingRecommender getRatingRecommender() {
		return null;
	}
}
