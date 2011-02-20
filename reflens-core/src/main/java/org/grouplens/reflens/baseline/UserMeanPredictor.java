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

import java.util.Collection;

import org.grouplens.reflens.RatingPredictor;
import org.grouplens.reflens.RatingPredictorBuilder;
import org.grouplens.reflens.data.RatingDataSource;
import org.grouplens.reflens.data.MutableSparseVector;
import org.grouplens.reflens.data.ScoredId;
import org.grouplens.reflens.data.SparseVector;

import com.google.inject.Inject;

/**
 * Rating predictor that returns the user's average rating for all predictions.
 * 
 * If the user has no ratings, the global mean is returned.  This is done by
 * actually computing the average offset from the global mean and adding back
 * the global mean for the returned prediction.
 * 
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class UserMeanPredictor implements RatingPredictor {
	private final double globalMean;

	UserMeanPredictor(double mean) {
		globalMean = mean;
	}
	
	static double average(SparseVector ratings, double offset) {
		if (ratings.isEmpty()) return 0;
		
		double total = ratings.sum();
		total -= ratings.size() * offset;
		return total / ratings.size();
	}

	/* (non-Javadoc)
	 * @see org.grouplens.reflens.RatingPredictor#predict(long, java.util.Map, java.util.Collection)
	 */
	@Override
	public MutableSparseVector predict(long user, SparseVector ratings,
			Collection<Long> items) {
		double mean = average(ratings, globalMean) + globalMean;
		return ConstantPredictor.constantPredictions(items, mean);
	}

	/* (non-Javadoc)
	 * @see org.grouplens.reflens.RatingPredictor#predict(long, java.util.Map, long)
	 */
	@Override
	public ScoredId predict(long user, SparseVector ratings, long item) {
		return new ScoredId(item, average(ratings, globalMean) + globalMean);
	}
	
	/**
	 * Predictor builder for the user mean predictor.
	 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
	 *
	 */
	public static class Builder implements RatingPredictorBuilder {
		@Inject
		public Builder() {
		}
		@Override
		public RatingPredictor build(RatingDataSource data) {
			double avg = GlobalMeanPredictor.computeMeanRating(data.getRatings());
			return new UserMeanPredictor(avg);
		}
	}
}
