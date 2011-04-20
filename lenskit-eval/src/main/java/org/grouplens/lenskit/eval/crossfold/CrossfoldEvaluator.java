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
package org.grouplens.lenskit.eval.crossfold;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.grouplens.lenskit.data.dao.RatingDataAccessObject;
import org.grouplens.lenskit.data.dao.RatingDataSession;
import org.grouplens.lenskit.eval.AlgorithmInstance;
import org.grouplens.lenskit.eval.CrossfoldOptions;
import org.grouplens.lenskit.eval.holdout.TrainTestPredictEvaluator;
import org.grouplens.lenskit.eval.predict.PredictionEvaluator;
import org.grouplens.lenskit.eval.results.MultiRunTableResultManager;
import org.grouplens.lenskit.eval.results.ResultAccumulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run a crossfold benchmark.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class CrossfoldEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(CrossfoldEvaluator.class);
    private final CrossfoldManager manager;
    private final int numFolds;
    private final List<AlgorithmInstance> algorithms;
    private final File predictionFile;
    
    public CrossfoldEvaluator(RatingDataAccessObject ratings,
            List<AlgorithmInstance> algorithms,
            int numFolds,
            UserRatingProfileSplitter splitter,
            @Nullable File predFile) throws IOException {
        this.numFolds = numFolds;
        this.algorithms = algorithms;
        manager = new CrossfoldManager(numFolds, ratings, splitter);
        predictionFile = predFile;
    }

    // TODO Clean this up. Majorly.
    public CrossfoldEvaluator(RatingDataAccessObject ratings,
            CrossfoldOptions options,
            List<AlgorithmInstance> algorithms) throws IOException {
        this(ratings, algorithms, options.getNumFolds(),
                (options.timeSplit() ? new TimestampUserRatingProfileSplitter(options.getHoldoutFraction())
                    : new RandomUserRatingProfileSplitter(options.getHoldoutFraction())),
                    options.predictionFile().isEmpty() ? null : new File(options.predictionFile()));
    }

    public void run(List<PredictionEvaluator> evaluators, File output) {
        MultiRunTableResultManager mgr = new MultiRunTableResultManager(algorithms, evaluators, output);
        try {
			mgr.setPredictionOutput(predictionFile);
		} catch (IOException e) {
			logger.error("Could not set up prediction file", e);
		}
        for (int i = 0; i < numFolds; i++) {
            RatingDataSession train = null;
            RatingDataSession test = null;
            try {
                train = manager.trainingSet(i);
                test = manager.testSet(i);
                int nusers = train.getUserCount();
                logger.info(String.format("Running benchmark %d with %d training and %d test users",
                                          i+1, nusers, test.getUserCount()));
                TrainTestPredictEvaluator eval = new TrainTestPredictEvaluator(train, test);
                ResultAccumulator accum = mgr.makeAccumulator(i);
                eval.evaluateAlgorithms(algorithms, accum);
            } finally {
                if (test != null)
                    test.release();
                if (train != null)
                    train.release();
            }
        }
        mgr.finish();
    }
}
