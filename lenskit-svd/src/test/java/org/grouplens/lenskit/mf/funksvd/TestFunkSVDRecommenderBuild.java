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
package org.grouplens.lenskit.mf.funksvd;

import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.RatingPredictor;
import org.grouplens.lenskit.Recommender;
import org.grouplens.lenskit.RecommenderBuildException;
import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.baseline.ItemUserMeanPredictor;
import org.grouplens.lenskit.baseline.UserMeanPredictor;
import org.grouplens.lenskit.basic.SimpleRatingPredictor;
import org.grouplens.lenskit.basic.TopNItemRecommender;
import org.grouplens.lenskit.core.LenskitRecommender;
import org.grouplens.lenskit.core.LenskitRecommenderEngine;
import org.grouplens.lenskit.core.LenskitRecommenderEngineFactory;
import org.grouplens.lenskit.data.dao.DAOFactory;
import org.grouplens.lenskit.data.dao.EventCollectionDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.event.Ratings;
import org.grouplens.lenskit.data.snapshot.PackedPreferenceSnapshot;
import org.grouplens.lenskit.data.snapshot.PreferenceSnapshot;
import org.grouplens.lenskit.iterative.IterationCount;
import org.grouplens.lenskit.iterative.IterationCountStoppingCondition;
import org.grouplens.lenskit.iterative.StoppingCondition;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class TestFunkSVDRecommenderBuild {
    private DAOFactory daoFactory;

    @Before
    public void setup() throws RecommenderBuildException {
        List<Rating> rs = new ArrayList<Rating>();
        rs.add(Ratings.make(1, 5, 2));
        rs.add(Ratings.make(1, 7, 4));
        rs.add(Ratings.make(8, 4, 5));
        rs.add(Ratings.make(8, 5, 4));

        daoFactory = new EventCollectionDAO.Factory(rs);
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private LenskitRecommenderEngine makeEngine() throws RecommenderBuildException {
        LenskitRecommenderEngineFactory factory = new LenskitRecommenderEngineFactory(daoFactory);
        factory.bind(PreferenceSnapshot.class)
               .to(PackedPreferenceSnapshot.class);
        factory.bind(ItemScorer.class)
               .to(FunkSVDItemScorer.class);
        factory.bind(BaselinePredictor.class)
               .to(UserMeanPredictor.class);
        factory.bind(StoppingCondition.class)
               .to(IterationCountStoppingCondition.class);
        factory.set(IterationCount.class)
               .to(10);
        factory.set(FeatureCount.class)
               .to(20);

        return factory.create();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testFunkSVDRecommenderEngineCreate() throws RecommenderBuildException {
        LenskitRecommenderEngine engine = makeEngine();
        Recommender rec = engine.open();

        try {
            assertThat(rec.getItemScorer(),
                       instanceOf(FunkSVDItemScorer.class));
            assertThat(rec.getItemRecommender(),
                       instanceOf(TopNItemRecommender.class));
            RatingPredictor pred = rec.getRatingPredictor();
            assertThat(pred, instanceOf(SimpleRatingPredictor.class));
            assertThat(((SimpleRatingPredictor) pred).getScorer(),
                       sameInstance(rec.getItemScorer()));
            assertThat(((FunkSVDItemScorer) rec.getItemScorer()).getUpdateRule(),
                       nullValue());
        } finally {
            rec.close();
        }
    }

    @Test
    public void testFeatureInfo() throws RecommenderBuildException {
        LenskitRecommenderEngine engine = makeEngine();
        LenskitRecommender rec = engine.open();

        try {
            FunkSVDModel model = rec.get(FunkSVDModel.class);
            assertThat(model, notNullValue());
            assertThat(model.getFeatureInfo().size(),
                       equalTo(20));
            for (FeatureInfo feat: model.getFeatureInfo()) {
                assertThat(feat.getIterCount(), equalTo(10));
                assertThat(feat.getLastDeltaRMSE(),
                           greaterThan(0.0));
            }
        } finally {
            rec.close();
        }
    }

    @Test
    public void testConfigSeparation() throws RecommenderBuildException {
        LenskitRecommenderEngine engine = makeEngine();
        LenskitRecommender rec1 = null;
        LenskitRecommender rec2 = null;
        try {
            rec1 = engine.open();
            rec2 = engine.open();

            assertThat(rec1.getItemScorer(),
                       not(sameInstance(rec2.getItemScorer())));
            assertThat(rec1.get(FunkSVDModel.class),
                       sameInstance(rec2.get(FunkSVDModel.class)));
        } finally {
            if (rec2 != null) {
                rec2.close();
            }
            if (rec1 != null) {
                rec1.close();
            }
        }
    }

    /**
     * Test whether we can build a recommender with predict-time updates.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testPredictUpdates() throws RecommenderBuildException {
        LenskitRecommenderEngineFactory factory = new LenskitRecommenderEngineFactory(daoFactory);
        factory.bind(ItemScorer.class)
               .to(FunkSVDItemScorer.class);
        factory.bind(BaselinePredictor.class)
               .to(ItemUserMeanPredictor.class);
        factory.set(IterationCount.class)
               .to(10);
        factory.bind(RuntimeUpdate.class, FunkSVDUpdateRule.class)
               .to(FunkSVDUpdateRule.class);

        LenskitRecommenderEngine engine = factory.create();

        LenskitRecommender rec = engine.open();
        try {
            ItemScorer scorer = rec.getItemScorer();
            assertThat(scorer, instanceOf(FunkSVDItemScorer.class));
            FunkSVDItemScorer fsvd = (FunkSVDItemScorer) scorer;
            assertThat(fsvd.getUpdateRule(),
                       not(nullValue()));
        } finally {
            rec.close();
        }
    }
}
