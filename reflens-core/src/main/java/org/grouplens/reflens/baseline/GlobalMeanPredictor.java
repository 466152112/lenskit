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

import javax.annotation.WillClose;

import org.grouplens.reflens.data.Cursor;
import org.grouplens.reflens.data.Rating;
import org.grouplens.reflens.data.RatingDataSource;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Rating predictor that predicts the global mean rating for all items.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class GlobalMeanPredictor extends ConstantPredictor {

	/**
	 * Construct a new global mean predictor from a data source.
	 * @param ratings A data source of ratings.
	 */
	public GlobalMeanPredictor(RatingDataSource ratings) {
		super(computeMeanRating(ratings.getRatings()));
	}
	/**
	 * Injectable constructor.  Takes a provider so the resulting predictor can
	 * outlive the data source.
	 * @param ratingProvider A provider to get rating data sources.
	 */
	@Inject
	public GlobalMeanPredictor(Provider<RatingDataSource> ratingProvider) {
		this(ratingProvider.get());
	}
	
	/**
	 * Helper method to compute the mean of all ratings in a cursor.
	 * The cursor is closed after the ratings are computed.
	 * @param ratings A cursor of ratings to average.
	 * @return The arithmetic mean of all ratings.
	 */
	public static double computeMeanRating(@WillClose Cursor<Rating> ratings) {
		double total = 0;
		long count = 0;
		try {
			for (Rating r: ratings) {
				total += r.getRating();
				count += 1;
			}
		} finally {
			ratings.close();
		}
		double avg = 0;
		if (count > 0)
			avg = total / count;
		return avg;
	}
}
