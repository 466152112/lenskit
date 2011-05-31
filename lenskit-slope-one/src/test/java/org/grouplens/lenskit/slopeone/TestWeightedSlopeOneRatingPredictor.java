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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.grouplens.lenskit.data.Rating;
import org.grouplens.lenskit.data.SimpleRating;
import org.grouplens.lenskit.data.dao.RatingCollectionDAO;
import org.grouplens.lenskit.data.snapshot.PackedRatingSnapshot;
import org.grouplens.lenskit.slopeone.DeviationComputer;
import org.grouplens.lenskit.slopeone.SlopeOneModel;
import org.grouplens.lenskit.slopeone.SlopeOneModelBuilder;
import org.grouplens.lenskit.slopeone.WeightedSlopeOneRatingPredictor;
import org.junit.Test;

public class TestWeightedSlopeOneRatingPredictor {
	
	private static final double EPSILON = 1.0e-6;
	
	@Test
	public void testPredict1() {

		List<Rating> rs = new ArrayList<Rating>();
		rs.add(new SimpleRating(1, 6, 4));
		rs.add(new SimpleRating(2, 6, 2));
		rs.add(new SimpleRating(1, 7, 3));
		rs.add(new SimpleRating(2, 7, 2));
		rs.add(new SimpleRating(3, 7, 5));
		rs.add(new SimpleRating(4, 7, 2));
		rs.add(new SimpleRating(1, 8, 3));
		rs.add(new SimpleRating(2, 8, 4));
		rs.add(new SimpleRating(3, 8, 3));
		rs.add(new SimpleRating(4, 8, 2));
		rs.add(new SimpleRating(5, 8, 3));
		rs.add(new SimpleRating(6, 8, 2));
		rs.add(new SimpleRating(1, 9, 3));
		rs.add(new SimpleRating(3, 9, 4));
		RatingCollectionDAO.Manager manager = new RatingCollectionDAO.Manager(rs);
		RatingCollectionDAO dao = manager.open();
		PackedRatingSnapshot.Builder snapBuilder = new PackedRatingSnapshot.Builder(dao);
		PackedRatingSnapshot snapshot = snapBuilder.build();		
		SlopeOneModelBuilder builder = new SlopeOneModelBuilder();
		builder.setRatingSnapshot(snapshot);
		builder.setDeviationComputer(new DeviationComputer(0));
		builder.setMinRating(1);
		builder.setMaxRating(5);
		SlopeOneModel model = builder.build();
		WeightedSlopeOneRatingPredictor predictor = new WeightedSlopeOneRatingPredictor(dao, model);
		assertEquals(2.6, predictor.predict(2, 9).getScore(), EPSILON);
		assertEquals(4.2, predictor.predict(3, 6).getScore(), EPSILON);
		assertEquals(2, predictor.predict(4, 6).getScore(), EPSILON);
		assertEquals(2, predictor.predict(4, 9).getScore(), EPSILON);
		assertEquals(2.5, predictor.predict(5, 6).getScore(), EPSILON);
		assertEquals(3, predictor.predict(5, 7).getScore(), EPSILON);
		assertEquals(3.5, predictor.predict(5, 9).getScore(), EPSILON);
		assertEquals(1.5, predictor.predict(6, 6).getScore(), EPSILON);
		assertEquals(2, predictor.predict(6, 7).getScore(), EPSILON);
		assertEquals(2.5, predictor.predict(6, 9).getScore(), EPSILON);
	}
	
	@Test
	public void testPredict2() {
		List<Rating> rs = new ArrayList<Rating>();
		rs.add(new SimpleRating(1, 4, 3.5));
		rs.add(new SimpleRating(2, 4, 5));
		rs.add(new SimpleRating(3, 5, 4.25));
		rs.add(new SimpleRating(2, 6, 3));
		rs.add(new SimpleRating(1, 7, 4));
		rs.add(new SimpleRating(2, 7, 4));
		rs.add(new SimpleRating(3, 7, 1.5));
		RatingCollectionDAO.Manager manager = new RatingCollectionDAO.Manager(rs);
		RatingCollectionDAO dao = manager.open();
		PackedRatingSnapshot.Builder snapBuilder = new PackedRatingSnapshot.Builder(dao);
		PackedRatingSnapshot snap = snapBuilder.build();
		SlopeOneModelBuilder builder = new SlopeOneModelBuilder();
		builder.setRatingSnapshot(snap);
		builder.setDeviationComputer(new DeviationComputer(0));
		builder.setMinRating(1);
		builder.setMaxRating(5);
		SlopeOneModel model = builder.build();
		WeightedSlopeOneRatingPredictor predictor = new WeightedSlopeOneRatingPredictor(dao, model);
		assertEquals(5, predictor.predict(1, 5).getScore(), EPSILON);
		assertEquals(2.25, predictor.predict(1, 6).getScore(), EPSILON);
		assertEquals(5, predictor.predict(2, 5).getScore(), EPSILON);
		assertEquals(1.75, predictor.predict(3, 4).getScore(), EPSILON);
		assertEquals(1, predictor.predict(3, 6).getScore(), EPSILON);
	}
}