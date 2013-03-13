/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2013 Regents of the University of Minnesota and contributors
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
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
package org.grouplens.lenskit.slopeone;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import org.grouplens.lenskit.collections.LongSortedArraySet;
import org.grouplens.lenskit.core.ScoreBasedItemRecommender;
import org.grouplens.lenskit.data.Event;
import org.grouplens.lenskit.data.UserHistory;
import org.grouplens.lenskit.data.dao.DataAccessObject;
import org.grouplens.lenskit.data.event.Rating;

/**
 * A {@code RatingRecommender} that uses the Slope One algorithm.
 * @deprecated Just use {@link ScoreBasedItemRecommender}.
 */
@Deprecated
public class SlopeOneRecommender extends ScoreBasedItemRecommender {
    private SlopeOneRatingPredictor predictor;

    /**
     * Construct a new recommender from a scorer.
     *
     * @param predictor The predictor to use.
     */
    @Inject
    public SlopeOneRecommender(DataAccessObject dao, SlopeOneRatingPredictor predictor) {
        super(dao, predictor);
        this.predictor = predictor;
    }
}
