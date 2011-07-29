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
package org.grouplens.lenskit.slopeone;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

import java.util.Collection;

import org.grouplens.lenskit.AbstractItemScorer;
import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.data.dao.DataAccessObject;
import org.grouplens.lenskit.data.event.Event;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.data.vector.MutableSparseVector;
import org.grouplens.lenskit.data.vector.SparseVector;
import org.grouplens.lenskit.data.vector.UserRatingVector;
import org.grouplens.lenskit.util.LongSortedArraySet;

/**
 * A <tt>RatingPredictor<tt> that implements the Slope One algorithm.
 */
public class SlopeOneRatingPredictor extends AbstractItemScorer {

	protected SlopeOneModel model;

	public SlopeOneRatingPredictor(DataAccessObject dao, SlopeOneModel model) {
		super(dao);
		this.model = model;
	}

	@Override
	public SparseVector score(UserHistory<? extends Event> history, Collection<Long> items) {
	    UserRatingVector user = history.ratingVector();

		LongSortedSet iset;
		if (items instanceof LongSortedSet)
			iset = (LongSortedSet) items;
		else
			iset = new LongSortedArraySet(items);
		MutableSparseVector preds = new MutableSparseVector(iset, Double.NaN);
		LongArrayList unpreds = new LongArrayList();
		for (long predicteeItem : items) {
			if (!user.containsKey(predicteeItem)) {
				double total = 0;
				int nitems = 0;
				for (long currentItem : user.keySet()) {
					int nusers = model.getCoratings(predicteeItem, currentItem);
					if (nusers != 0) {
						double currentDev = model.getDeviation(predicteeItem, currentItem);
						total += currentDev + user.get(currentItem);
						nitems++;
					}
				}
				if (nitems == 0) unpreds.add(predicteeItem);
				else {
					double predValue = total/nitems;
					if (predValue > model.getMaxRating()) predValue = model.getMaxRating();
					else if (predValue < model.getMinRating()) predValue = model.getMinRating();
					preds.set(predicteeItem, predValue);
				}
			}
		}
		//Use Baseline Predictor if necessary
		final BaselinePredictor baseline = model.getBaselinePredictor();
		if (baseline != null && !unpreds.isEmpty()) {
			SparseVector basePreds = baseline.predict(user, unpreds);
			for (Long2DoubleMap.Entry e: basePreds.fast()) {
				assert Double.isNaN(preds.get(e.getLongKey()));
				preds.set(e.getLongKey(), e.getDoubleValue());
			}
			return preds;
		}
		else return preds.copy(true);
	}
	
	public SlopeOneModel getModel() {
		return model;
	}
}