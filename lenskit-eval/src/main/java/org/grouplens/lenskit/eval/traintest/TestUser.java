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
package org.grouplens.lenskit.eval.traintest;

import org.grouplens.lenskit.collections.ScoredLongList;
import org.grouplens.lenskit.data.Event;
import org.grouplens.lenskit.data.UserHistory;
import org.grouplens.lenskit.data.history.RatingVectorUserHistorySummarizer;
import org.grouplens.lenskit.vectors.SparseVector;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * A user in a test set, with the results of their recommendations or predictions.
 *
 * @author Michael Ekstrand
 */
public class TestUser {
    private final long userId;
    private Supplier<UserHistory<Event>> historySupplier;
    private Supplier<UserHistory<Event>> testHistorySupplier;
    private Supplier<SparseVector> predSupplier;
    private Supplier<ScoredLongList> recSupplier;

    /**
     * Construct a new test user.
     *
     * @param id              The user ID.
     * @param hist            Access to the user's training history.
     * @param testHist        Access to the user's test history.
     * @param predictions     Supplier of predictions (will be memoized)
     * @param recommendations Supplier of recommendations (will be memoized)
     */
    public TestUser(long id, 
                    Supplier<UserHistory<Event>> hist,
                    Supplier<UserHistory<Event>> testHist,
                    Supplier<SparseVector> predictions,
                    Supplier<ScoredLongList> recommendations) {
        userId = id;
        historySupplier = Suppliers.memoize(hist);
        testHistorySupplier = Suppliers.memoize(testHist);
        predSupplier = Suppliers.memoize(predictions);
        recSupplier = Suppliers.memoize(recommendations);
    }

    /**
     * Get the ID of this user.
     *
     * @return The user's ID.
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Return this user's training history.
     *
     * @return The history of the user from the training/query set.
     * @deprecated use {@link #getTrainHistory()}
     */
    @Deprecated 
    public UserHistory<Event> getHistory() {
        return historySupplier.get();
    }
    
    /**
     * Return this user's training history.
     *
     * @return The history of the user from the training/query set.
     */
    public UserHistory<Event> getTrainHistory() {
        return historySupplier.get();
    }
  
    /**
     * Return this user's test history.
     * 
     * @return The history of the user from the test set.
     */
    public UserHistory<Event> getTestHistory() {
        return testHistorySupplier.get();
    }

    /**
     * Get the user's test ratings.
     * 
     * Summarizes the user's ratings from the history. 
     *
     * @return The user's ratings for the test items.
     */
    public SparseVector getTestRatings() {
        // Since the summarizer is memoized, it will only run once.
        return RatingVectorUserHistorySummarizer.makeRatingVector(getTestHistory());
    }

    /**
     * Get the user's predictions.
     *
     * @return The predictions of the user's preference for items.
     */
    public SparseVector getPredictions() {
        return predSupplier.get();
    }

    /**
     * Get the user's recommendations.
     *
     * @return Some recommendations for the user.
     */
    public ScoredLongList getRecommendations() {
        return recSupplier.get();
    }
}
