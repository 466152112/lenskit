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
package org.grouplens.lenskit.knn.item;

import org.grouplens.lenskit.core.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Build an item-item CF model from rating data.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 */
@NotThreadSafe
public class ItemItemModelProvider implements Provider<ItemItemModel> {
    private static final Logger logger = LoggerFactory.getLogger(ItemItemModelProvider.class);

    private final ItemSimilarity itemSimilarity;
    private final ItemItemBuildContextFactory contextFactory;
    private final SimilarityMatrixAccumulatorFactory simMatrixAccumulatorFactory;


    @Inject
    public ItemItemModelProvider(ItemSimilarity similarity,
                                 @Transient ItemItemBuildContextFactory contextFactory,
                                 SimilarityMatrixAccumulatorFactory simMatrixAccumulatorFactory) {
        itemSimilarity = similarity;
        this.contextFactory = contextFactory;
        this.simMatrixAccumulatorFactory = simMatrixAccumulatorFactory;
    }

    @Override
    public SimilarityMatrixModel get() {
        logger.debug("building item-item model");

        ItemItemBuildContext buildContext = contextFactory.buildContext();
        SimilarityMatrixAccumulator accumulator = simMatrixAccumulatorFactory.create(buildContext.getItems());

        for (ItemItemBuildContext.ItemVecPair pair : buildContext.getItemPairs()) {
            if (itemSimilarity.isSymmetric() && pair.itemId1 >= pair.itemId2) {
                continue;
            }
            if (pair.itemId1 != pair.itemId2) {
                double sim = itemSimilarity.similarity(pair.itemId1, pair.vec1, pair.itemId2, pair.vec2);
                accumulator.put(pair.itemId1, pair.itemId2, sim);
                if (itemSimilarity.isSymmetric()) {
                    accumulator.put(pair.itemId2, pair.itemId1, sim);
                }
            }
        }

        return accumulator.build();
    }

}
