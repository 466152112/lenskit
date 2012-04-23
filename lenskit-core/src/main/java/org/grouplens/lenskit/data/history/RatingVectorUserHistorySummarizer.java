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
package org.grouplens.lenskit.data.history;

import org.grouplens.lenskit.data.Event;
import org.grouplens.lenskit.data.UserHistory;
import org.grouplens.lenskit.data.event.Rating;

import com.google.common.base.Function;

/**
 * Summarize a history by extracting a rating vector.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public final class RatingVectorUserHistorySummarizer implements UserHistorySummarizer, Function<UserHistory<? extends Event>, UserVector> {
    private static final RatingVectorUserHistorySummarizer INSTANCE = new RatingVectorUserHistorySummarizer();

    @Override
    public Class<? extends Event> eventTypeWanted() {
        return Rating.class;
    }

    @Override
    public UserVector summarize(UserHistory<? extends Event> history) {
        return history.memoize(this);
    }

    @Override
    public UserVector apply(UserHistory<? extends Event> history) {
        return UserVector.fromRatings(history.getUserId(), history.filter(Rating.class));
    }

    public static UserVector makeRatingVector(UserHistory<? extends Event> history) {
        return INSTANCE.summarize(history);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * All rating vector summarizers are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        else return getClass().equals(o.getClass());
    }
}
