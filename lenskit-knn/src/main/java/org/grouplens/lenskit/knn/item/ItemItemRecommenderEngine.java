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

import it.unimi.dsi.fastutil.longs.LongSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectCollections;

import org.grouplens.lenskit.BasketRecommender;
import org.grouplens.lenskit.RecommenderEngine;
import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.data.Index;
import org.grouplens.lenskit.data.dao.RatingDataAccessObject;
import org.grouplens.lenskit.data.vector.SparseVector;
import org.grouplens.lenskit.knn.SimilarityMatrix;
import org.grouplens.lenskit.norm.UserRatingVectorNormalizer;
import org.grouplens.lenskit.norm.VectorTransformation;
import org.grouplens.lenskit.util.IndexedItemScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RecommenderEngine implementation that uses an item-item similarity matrix to create
 * predictions and recommendations. It is built with an
 * {@link ItemItemRecommenderEngineBuilder}.
 * 
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 * @author Michael Ludwig <mludwig@cs.umn.edu
 */
public class ItemItemRecommenderEngine implements RecommenderEngine {
	private static final Logger logger = LoggerFactory.getLogger(ItemItemRecommenderEngine.class);
    private ItemItemRatingPredictor predictor;
    private ItemItemRatingRecommender recommender;
    
    private final Index itemIndexer;
    private final SimilarityMatrix matrix;
    private final UserRatingVectorNormalizer normalizer;
    private final BaselinePredictor baseline;
    private final LongSortedSet itemUniverse;
    private final RatingDataAccessObject dao;
    
    ItemItemRecommenderEngine(Index indexer, SimilarityMatrix matrix, 
                        UserRatingVectorNormalizer norm, BaselinePredictor baseline,
                        LongSortedSet items, RatingDataAccessObject dao) {
        this.itemIndexer = indexer;
        this.normalizer = norm;
        this.baseline = baseline;
        this.matrix = matrix;
        this.itemUniverse = items;
        this.dao = dao;
    }
    
    protected void setRatingPredictor(ItemItemRatingPredictor predictor) {
        this.predictor = predictor;
    }
    
    protected void setRatingRecommender(ItemItemRatingRecommender recommender) {
        this.recommender = recommender;
    }
    
    @Override
    public ItemItemRatingPredictor getRatingPredictor() {
        return predictor;
    }
    
    @Override
    public ItemItemRatingPredictor getDynamicRatingPredictor() {
        return predictor;
    }

    @Override
    public ItemItemRatingRecommender getRatingRecommender() {
        return recommender;
    }

    @Override
    public BasketRecommender getBasketRecommender() {
        // TODO not implemented
        return null;
    }
    
    public Iterable<IndexedItemScore> getNeighbors(long item) {
        int idx = itemIndexer.getIndex(item);
        if (idx >= 0) {
            return matrix.getNeighbors(idx);
        } else {
            return new ObjectCollections.EmptyCollection<IndexedItemScore>() {};
        }
    }

    public int getItemIndex(long id) {
        return itemIndexer.getIndex(id);
    }

    public long getItem(int idx) {
        return itemIndexer.getId(idx);
    }
    
    public RatingDataAccessObject getDAO() {
        return dao;
    }

    public LongSortedSet getItemUniverse() {
        return itemUniverse;
    }
    
    public BaselinePredictor getBaselinePredictor() {
        return baseline;
    }
    
    public VectorTransformation normalizingTransformation(long uid, SparseVector ratings) {
        return normalizer.makeTransformation(uid, ratings);
    }
}
