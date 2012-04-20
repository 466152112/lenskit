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
package org.grouplens.lenskit.slopeone;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.grouplens.lenskit.data.dao.DataAccessObject;
import org.grouplens.lenskit.data.dao.EventCollectionDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.event.Ratings;
import org.grouplens.lenskit.data.pref.PreferenceDomain;
import org.grouplens.lenskit.data.snapshot.PackedPreferenceSnapshot;
import org.junit.Test;

public class TestSlopeOneRatingPredictor {

    private static final double EPSILON = 1.0e-6;
    
    private SlopeOneModel getModel(DataAccessObject dao) {
        PackedPreferenceSnapshot snapshot = new PackedPreferenceSnapshot.Provider(dao).get();
        SlopeOneModelProvider builder = new SlopeOneModelProvider(snapshot, null, null,
                                                                  new PreferenceDomain(1,5),
                                                                  0);
        SlopeOneModel model = builder.get();
        return model;
    }

    @Test
    public void testPredict1() {

        List<Rating> rs = new ArrayList<Rating>();
        rs.add(Ratings.make(1, 6, 4));
        rs.add(Ratings.make(2, 6, 2));
        rs.add(Ratings.make(1, 7, 3));
        rs.add(Ratings.make(2, 7, 2));
        rs.add(Ratings.make(3, 7, 5));
        rs.add(Ratings.make(4, 7, 2));
        rs.add(Ratings.make(1, 8, 3));
        rs.add(Ratings.make(2, 8, 4));
        rs.add(Ratings.make(3, 8, 3));
        rs.add(Ratings.make(4, 8, 2));
        rs.add(Ratings.make(5, 8, 3));
        rs.add(Ratings.make(6, 8, 2));
        rs.add(Ratings.make(1, 9, 3));
        rs.add(Ratings.make(3, 9, 4));
        
        EventCollectionDAO.Factory manager = new EventCollectionDAO.Factory(rs);
        DataAccessObject dao = manager.create();
        SlopeOneModel model = getModel(dao);
        SlopeOneRatingPredictor predictor = new SlopeOneRatingPredictor(dao, model);
        
        assertEquals(7/3.0, predictor.score(2, 9), EPSILON);
        assertEquals(13/3.0, predictor.score(3, 6), EPSILON);
        assertEquals(2, predictor.score(4, 6), EPSILON);
        assertEquals(2, predictor.score(4, 9), EPSILON);
        assertEquals(2.5, predictor.score(5, 6), EPSILON);
        assertEquals(3, predictor.score(5, 7), EPSILON);
        assertEquals(3.5, predictor.score(5, 9), EPSILON);
        assertEquals(1.5, predictor.score(6, 6), EPSILON);
        assertEquals(2, predictor.score(6, 7), EPSILON);
        assertEquals(2.5, predictor.score(6, 9), EPSILON);
    }

    @Test
    public void testPredict2() {
        List<Rating> rs = new ArrayList<Rating>();
        rs.add(Ratings.make(1, 4, 3.5));
        rs.add(Ratings.make(2, 4, 5));
        rs.add(Ratings.make(3, 5, 4.25));
        rs.add(Ratings.make(2, 6, 3));
        rs.add(Ratings.make(1, 7, 4));
        rs.add(Ratings.make(2, 7, 4));
        rs.add(Ratings.make(3, 7, 1.5));
        
        EventCollectionDAO.Factory manager = new EventCollectionDAO.Factory(rs);
        DataAccessObject dao = manager.create();
        SlopeOneModel model = getModel(dao);
        SlopeOneRatingPredictor predictor = new SlopeOneRatingPredictor(dao, model);
        
        assertEquals(5, predictor.score(1, 5), EPSILON);
        assertEquals(2.25, predictor.score(1, 6), EPSILON);
        assertEquals(5, predictor.score(2, 5), EPSILON);
        assertEquals(1.75, predictor.score(3, 4), EPSILON);
        assertEquals(1, predictor.score(3, 6), EPSILON);
    }
}
