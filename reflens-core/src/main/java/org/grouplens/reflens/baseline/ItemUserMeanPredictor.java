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

package org.grouplens.reflens.baseline;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.grouplens.reflens.data.RatingDataSource;
import org.grouplens.reflens.data.ScoredId;
import org.grouplens.reflens.data.vector.MutableSparseVector;
import org.grouplens.reflens.data.vector.SparseVector;
import org.grouplens.reflens.params.MeanDamping;
import org.grouplens.reflens.util.CollectionUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Predictor that returns the user's mean offset from item mean rating for all
 * predictions.
 * 
 * This implements the baseline predictor <i>p<sub>u,i</sub> = µ + b<sub>i</sub> +
 * b<sub>u</sub></i>, where <i>b<sub>i</sub></i> is the item's average rating (less the global
 * mean <i>µ</i>), and <i>b<sub>u</sub></i> is the user's average offset (the average
 * difference between their ratings and the item-mean baseline).
 * 
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class ItemUserMeanPredictor extends ItemMeanPredictor {
	protected final double damping;
	
	@Inject
	public ItemUserMeanPredictor(Provider<RatingDataSource> ratingProvider,
			@MeanDamping double damping) {
		this(ratingProvider.get(), damping);
	}
	
	public ItemUserMeanPredictor(RatingDataSource ratings) {
		this(ratings, 0);
	}

	public ItemUserMeanPredictor(RatingDataSource ratings, double damping) {
		super(ratings, damping);
		this.damping = damping;
	}
	
	/**
	 * Compute the mean offset in user rating from item mean rating.
	 * @param ratings the user's rating profile
	 * @return the mean offset from item mean rating.
	 */
	double computeUserAverage(SparseVector ratings) {
		if (ratings.isEmpty()) return 0;
		
		Collection<Double> values = ratings.values();
		double total = 0;
		
		Iterator<Long2DoubleMap.Entry> iter = ratings.fastIterator();
		while (iter.hasNext()) {
			Long2DoubleMap.Entry rating = iter.next();
			double r = rating.getDoubleValue();
			long iid = rating.getLongKey();
			total += r - getItemMean(iid);
		}
		return total / (values.size() + damping);
	}
	
	/* (non-Javadoc)
	 * @see org.grouplens.reflens.RatingPredictor#predict(long, java.util.Map, java.util.Collection)
	 */
	@Override
	public MutableSparseVector predict(long user, SparseVector ratings,
			Collection<Long> items) {
		double meanOffset = computeUserAverage(ratings);
		long[] keys = CollectionUtils.fastCollection(items).toLongArray();
		if (!(items instanceof LongSortedSet))
			Arrays.sort(keys);
		double[] preds = new double[keys.length];
		for (int i = 0; i < keys.length; i++) {
			preds[i] = meanOffset + getItemMean(keys[i]);
		}
		return MutableSparseVector.wrap(keys, preds);
	}

	/* (non-Javadoc)
	 * @see org.grouplens.reflens.RatingPredictor#predict(long, java.util.Map, long)
	 */
	@Override
	public ScoredId predict(long user, SparseVector ratings, long item) {
		return new ScoredId(item, computeUserAverage(ratings) + getItemMean(item));
	}
}
