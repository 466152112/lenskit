/*
 * RefLens, a reference implementation of recommender algorithms.
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
/**
 *
 */
package org.grouplens.lenskit.data.dao;

import java.util.Collection;

import org.grouplens.common.cursors.Cursor;
import org.grouplens.common.cursors.Cursors;
import org.grouplens.lenskit.data.Rating;

/**
 * Data source backed by a collection of ratings.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class RatingCollectionDAO extends AbstractRatingDataAccessObject {
    private Collection<Rating> ratings;

    /**
     * Construct a new data source from a collection of ratings.
     * @param ratings The ratings to use.
     */
    public RatingCollectionDAO(Collection<Rating> ratings) {
        this.ratings = ratings;
    }

    /* (non-Javadoc)
     * @see org.grouplens.lenskit.data.dao.AbRatingDataAccessObjectAccessObject#getRatings()
     */
    @Override
    public Cursor<Rating> getRatings() {
        return Cursors.wrap(ratings);
    }

}
