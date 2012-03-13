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

/**
 * A strategy for computing similarity matrices.
 *
 * {@link ItemItemModelBuilder} uses the Strategy pattern to optimize its
 * build algorithm based on what kind of similarity function is in use.  This is
 * the interface which makes that possible.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public interface ItemItemModelBuildStrategy {
    /**
     * Query whether this strategy requires the build state to have easy access
     * to the sets of items rated by each user.
     * @return {@code true} if the strategy requires the item sets.
     */
    boolean needsUserItemSets();

    /**
     * Build the item-item matrix.
     * @param context The data to be used to compute similarities.
     * @param accum The accumulator into which similarities should be stored.
     */
    void buildMatrix(ItemItemBuildContext context, SimilarityMatrixAccumulator accum);
}
