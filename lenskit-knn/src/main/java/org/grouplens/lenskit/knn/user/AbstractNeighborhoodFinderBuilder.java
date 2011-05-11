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
package org.grouplens.lenskit.knn.user;

import org.grouplens.lenskit.RecommenderComponentBuilder;
import org.grouplens.lenskit.data.vector.SparseVector;
import org.grouplens.lenskit.knn.Similarity;
import org.grouplens.lenskit.knn.params.NeighborhoodSize;
import org.grouplens.lenskit.knn.params.UserSimilarity;
import org.grouplens.lenskit.norm.UserRatingVectorNormalizer;
import org.grouplens.lenskit.params.Normalizer;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public abstract class AbstractNeighborhoodFinderBuilder<T extends NeighborhoodFinder>
        extends RecommenderComponentBuilder<T> {

    protected int neighborhoodSize;
    protected Similarity<? super SparseVector> similarity;
    protected UserRatingVectorNormalizer normalizer;

    @NeighborhoodSize
    public void setNeighborhoodSize(int neighborhood) {
        neighborhoodSize = neighborhood;
    }

    @UserSimilarity
    public void setSimilarity(Similarity<? super SparseVector> similarity) {
        this.similarity = similarity;
    }

    @Normalizer
    public void setNormalizer(UserRatingVectorNormalizer normalizer) {
        this.normalizer = normalizer;
    }
}
