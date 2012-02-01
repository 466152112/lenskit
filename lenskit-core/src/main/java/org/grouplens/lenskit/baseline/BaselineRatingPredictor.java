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
package org.grouplens.lenskit.baseline;

import java.util.Collection;

import org.grouplens.lenskit.RatingPredictor;
import org.grouplens.lenskit.core.AbstractItemScorer;
import org.grouplens.lenskit.data.Event;
import org.grouplens.lenskit.data.UserHistory;
import org.grouplens.lenskit.data.dao.DataAccessObject;
import org.grouplens.lenskit.data.history.RatingVectorHistorySummarizer;
import org.grouplens.lenskit.data.history.UserVector;
import org.grouplens.lenskit.vectors.SparseVector;

/**
 * {@link RatingPredictor} that delegates to the baseline predictor. This allows
 * baseline predictors to be used as rating predictors in their own right.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 * @see BaselinePredictor
 */
public class BaselineRatingPredictor extends AbstractItemScorer implements RatingPredictor {
    private BaselinePredictor predictor;

    /**
     * Construct a new baseline rating predictor.
     * 
     * @param baseline The baseline predictor to use.
     * @param dao The DAO.
     */
    public BaselineRatingPredictor(BaselinePredictor baseline, DataAccessObject dao) {
        super(dao);
        predictor = baseline;
    }

    /**
     * Delegate to {@link BaselinePredictor#predict(UserVector, Collection)}.
     */
    @Override
    public SparseVector score(UserHistory<? extends Event> profile, Collection<Long> items) {
        UserVector ratings = RatingVectorHistorySummarizer.makeRatingVector(profile);
        return predictor.predict(ratings, items);
    }

    /**
     * Delegate to {@link BaselinePredictor#predict(long, Collection)}.
     */
	@Override
	public SparseVector globalScore(long queryItem, Collection<Long> items) {
		return predictor.predict(queryItem, items);
	}
}
