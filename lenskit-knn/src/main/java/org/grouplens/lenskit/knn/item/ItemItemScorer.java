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
package org.grouplens.lenskit.knn.item;

import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.basic.AbstractItemScorer;
import org.grouplens.lenskit.data.Event;
import org.grouplens.lenskit.data.UserHistory;
import org.grouplens.lenskit.data.dao.DataAccessObject;
import org.grouplens.lenskit.data.history.UserHistorySummarizer;
import org.grouplens.lenskit.knn.item.model.ItemItemModel;
import org.grouplens.lenskit.symbols.Symbol;
import org.grouplens.lenskit.transform.normalize.UserVectorNormalizer;
import org.grouplens.lenskit.transform.normalize.VectorTransformation;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;

/**
 * Score items using an item-item CF model. User ratings are <b>not</b> supplied
 * as default preferences.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class ItemItemScorer extends AbstractItemScorer implements ItemScorer {
    private static final Logger logger = LoggerFactory.getLogger(ItemItemScorer.class);
    public static final Symbol NEIGHBORHOOD_SIZE_SYMBOL =
            Symbol.of("org.grouplens.lenskit.knn.item.neighborhoodSize");
    protected final ItemItemModel model;

    @Nonnull
    protected final UserVectorNormalizer normalizer;
    protected final UserHistorySummarizer summarizer;
    @Nonnull
    protected final NeighborhoodScorer scorer;
    @Nonnull
    protected final ItemScoreAlgorithm algorithm;
    @Nullable
    private final BaselinePredictor baseline;

    /**
     * Construct a new item-item scorer.
     *
     * @param dao    The DAO.
     * @param m      The model
     * @param sum    The history summarizer.
     * @param scorer The neighborhood scorer.
     * @param algo   The item scoring algorithm.  It converts neighborhoods to scores.
     * @param bl     The baseline scorer. If present, it will be used to supply scores that the
     *               item-item CF algorithm cannot.
     */
    @Inject
    public ItemItemScorer(DataAccessObject dao, ItemItemModel m,
                          UserHistorySummarizer sum,
                          NeighborhoodScorer scorer,
                          ItemScoreAlgorithm algo,
                          UserVectorNormalizer norm,
                          @Nullable BaselinePredictor bl) {
        super(dao);
        model = m;
        summarizer = sum;
        this.scorer = scorer;
        algorithm = algo;
        normalizer = norm;
        baseline = bl;
        logger.info("building item-item scorer with scorer {}", scorer);
    }

    @Nonnull
    public UserVectorNormalizer getNormalizer() {
        return normalizer;
    }

    /**
     * Score items by computing predicted ratings.
     *
     * @see ItemScoreAlgorithm#scoreItems(ItemItemModel, SparseVector, MutableSparseVector, NeighborhoodScorer)
     */
    @Override
    public void score(@Nonnull UserHistory<? extends Event> history,
                      @Nonnull MutableSparseVector scores) {
        final long uid = history.getUserId();

        SparseVector summary = summarizer.summarize(history);
        VectorTransformation transform = normalizer.makeTransformation(uid, summary);
        MutableSparseVector normed = summary.mutableCopy();
        transform.apply(normed);

        scores.clear();
        algorithm.scoreItems(model, normed, scores, scorer);

        // untransform the scores
        transform.unapply(scores);

        if (baseline != null) {
            baseline.predict(uid, summary, scores, false);
        }
    }
}