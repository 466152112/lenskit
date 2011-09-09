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

import static java.lang.Math.abs;

import org.grouplens.lenskit.collections.ScoredLongList;
import org.grouplens.lenskit.collections.ScoredLongListIterator;
import org.grouplens.lenskit.vector.SparseVector;

/**
 * Neighborhood scorer that computes the weighted average of neighbor scores.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class WeightedAverageNeighborhoodScorer implements NeighborhoodScorer {
    @Override
    public double score(ScoredLongList neighbors, SparseVector scores) {
        double sum = 0;
        double weight = 0;
        ScoredLongListIterator nIter = neighbors.iterator();
        while (nIter.hasNext()) {
            long oi = nIter.nextLong();
            double sim = nIter.getScore();
            weight += abs(sim);
            sum += sim * scores.get(oi);
        }
        if (weight > 0) {
            return sum / weight;
        } else {
            return Double.NaN;
        }
    }
}
