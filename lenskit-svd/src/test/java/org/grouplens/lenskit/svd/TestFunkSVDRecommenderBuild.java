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
package org.grouplens.lenskit.svd;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.grouplens.lenskit.ItemRecommender;
import org.grouplens.lenskit.LenskitRecommenderEngineFactory;
import org.grouplens.lenskit.RatingPredictor;
import org.grouplens.lenskit.Recommender;
import org.grouplens.lenskit.RecommenderEngine;
import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.baseline.UserMeanPredictor;
import org.grouplens.lenskit.data.dao.DAOFactory;
import org.grouplens.lenskit.data.dao.RatingCollectionDAO;
import org.grouplens.lenskit.data.dao.RatingDataAccessObject;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.event.SimpleRating;
import org.grouplens.lenskit.svd.params.IterationCount;
import org.junit.Before;
import org.junit.Test;

public class TestFunkSVDRecommenderBuild {
	private DAOFactory<? extends RatingDataAccessObject> manager;
	private RecommenderEngine engine;

	@Before
	public void setup() {
		List<Rating> rs = new ArrayList<Rating>();
		rs.add(new SimpleRating(1, 5, 2));
		rs.add(new SimpleRating(1, 7, 4));
		rs.add(new SimpleRating(8, 4, 5));
		rs.add(new SimpleRating(8, 5, 4));

		manager = new RatingCollectionDAO.Factory(rs);

		LenskitRecommenderEngineFactory factory = new LenskitRecommenderEngineFactory(manager);
		factory.setComponent(RatingPredictor.class, FunkSVDRatingPredictor.class);
		factory.setComponent(ItemRecommender.class, FunkSVDRecommender.class);
		factory.setComponent(BaselinePredictor.class, UserMeanPredictor.class);
		factory.set(IterationCount.class, 10);

		engine = factory.create();
	}

	@Test
	public void testFunkSVDRecommenderEngineCreate() {
		Recommender rec = engine.open();

		try {
			// These assert instanceof's are also assertNotNull's
			assertTrue(rec.getRatingPredictor() instanceof FunkSVDRatingPredictor);
			assertTrue(rec.getItemRecommender() instanceof FunkSVDRecommender);

			assertNull(rec.getDynamicItemRecommender());
			assertNull(rec.getDynamicRatingPredictor());

		} finally {
			rec.close();
		}
	}
}