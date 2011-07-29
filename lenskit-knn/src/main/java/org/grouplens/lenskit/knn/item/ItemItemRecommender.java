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

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import org.grouplens.lenskit.PredictorBasedItemRecommender;
import org.grouplens.lenskit.data.UserHistory;
import org.grouplens.lenskit.data.dao.DataAccessObject;
import org.grouplens.lenskit.data.event.Event;
import org.grouplens.lenskit.data.event.Rating;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class ItemItemRecommender extends PredictorBasedItemRecommender {
    protected final ItemItemRatingPredictor predictor;
    
    /**
     * Construct a new recommender from a scorer.
     * @param scorer The scorer to use.
     */
    public ItemItemRecommender(DataAccessObject dao, ItemItemRatingPredictor scorer) {
        super(dao, scorer);
        this.predictor = scorer;
    }
    
    /**
     * Compute the predictable items from the neighborhood.
     */
    @Override
    public LongSet getPredictableItems(UserHistory<? extends Event> user) {
        // FIXME This method incorrectly assumes the model is symmetric
        ItemItemModel model = predictor.getModel();
    	if (predictor.getBaseline() != null) {
            return model.getItemUniverse();
        } else {
            LongSet items = new LongOpenHashSet();
            LongSet userItems = user.filter(Rating.class).itemSet();
            LongIterator iter = userItems.iterator();
            while (iter.hasNext()) {
                final long item = iter.nextLong();
                items.addAll(model.getNeighbors(item));
            }
            return items;
        }
    }
}