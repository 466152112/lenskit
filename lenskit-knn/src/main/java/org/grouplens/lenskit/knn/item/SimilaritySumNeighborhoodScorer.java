/*
 * LensKit, an open source recommender systems toolkit.
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

import org.grouplens.lenskit.collections.ScoredLongList;
import org.grouplens.lenskit.collections.ScoredLongListIterator;
import org.grouplens.lenskit.vectors.SparseVector;

/**
 * Neighborhood scorer that computes the sum of neighborhood similarities.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class SimilaritySumNeighborhoodScorer implements NeighborhoodScorer {
    @Override
    public double score(ScoredLongList neighbors, SparseVector scores) {
        double sum = 0;
        ScoredLongListIterator nIter = neighbors.iterator();
        while (nIter.hasNext()) {
            @SuppressWarnings("unused")
            long i = nIter.nextLong();
            
            double sim = nIter.getScore();
            sum += sim;
        }
        return sum;
    }
}
