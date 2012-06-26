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
package org.grouplens.lenskit.svd;

import it.unimi.dsi.fastutil.doubles.DoubleArrays;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.time.StopWatch;
import org.grouplens.grapht.annotation.Transient;
import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.collections.CollectionUtils;
import org.grouplens.lenskit.collections.FastCollection;
import org.grouplens.lenskit.data.pref.IndexedPreference;
import org.grouplens.lenskit.data.snapshot.PreferenceSnapshot;
import org.grouplens.lenskit.svd.params.FeatureCount;
import org.grouplens.lenskit.svd.params.IterationCount;
import org.grouplens.lenskit.svd.params.LearningRate;
import org.grouplens.lenskit.svd.params.RegularizationTerm;
import org.grouplens.lenskit.svd.params.TrainingThreshold;
import org.grouplens.lenskit.transform.clamp.ClampingFunction;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SVD recommender builder using gradient descent (Funk SVD).
 *
 * This recommender builder constructs an SVD-based recommender using gradient
 * descent, as pioneered by Simon Funk.  It also incorporates the regularizations
 * Funk did. These are documented in
 * <a href="http://sifter.org/~simon/journal/20061211.html">Netflix Update: Try
 * This at Home</a>. This implementation is based in part on
 * <a href="http://www.timelydevelopment.com/demos/NetflixPrize.aspx">Timely
 * Development's sample code</a>.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class FunkSVDModelProvider implements Provider<FunkSVDModel> {
    private static Logger logger = LoggerFactory.getLogger(FunkSVDModelProvider.class);

    // The default value for feature values - isn't supposed to matter much
    private static final double DEFAULT_FEATURE_VALUE = 0.1;
    // Minimum number of epochs to run to train a feature
    private static final double MIN_EPOCHS = 50;

    private final int featureCount;
    private final double learningRate;
    private final double trainingThreshold;
    private final double trainingRegularization;
    private final ClampingFunction clampingFunction;
    private final int iterationCount;

    private final BaselinePredictor baseline;
    
    private final PreferenceSnapshot snapshot;
    
    
    @Inject
    public FunkSVDModelProvider(@Transient PreferenceSnapshot snapshot,
                               @FeatureCount int featureCount,
                               @LearningRate double learningRate,
                               @TrainingThreshold double threshold,
                               @RegularizationTerm double gradientDescent,
                               ClampingFunction clamp,
                               @IterationCount int iterCount,
                               BaselinePredictor baseline) {
        this.snapshot = snapshot;
        this.featureCount = featureCount;
        this.learningRate = learningRate;
        this.baseline = baseline;
        trainingThreshold = threshold;
        trainingRegularization = gradientDescent;
        clampingFunction = clamp;
        iterationCount = iterCount;
    }

    /* (non-Javadoc)
     * @see org.grouplens.lenskit.RecommenderComponentBuilder#build(org.grouplens.lenskit.data.snapshot.RatingBuildContext)
     */
    @Override
    public FunkSVDModel get() {
        logger.debug("Setting up to build SVD recommender with {} features", featureCount);
        logger.debug("Learning rate is {}", learningRate);
        logger.debug("Regularization term is {}", trainingRegularization);
        if (iterationCount > 0) {
            logger.debug("Training each epoch for {} iterations", iterationCount);
        } else {
            logger.debug("Error epsilon is {}", trainingThreshold);
        }

        double[] estimates = initializeEstimates(snapshot, baseline);
        FastCollection<IndexedPreference> ratings = snapshot.getRatings();

        logger.debug("Building SVD with {} features for {} ratings",
                featureCount, ratings.size());

        // Declaration and Initialization of the unique trainer object
        // We'll be using this one object throughout the whole building time
        // 		by reseting its internal values in the loop over featureCount
        UpdateRule trainer = new UpdateRule(learningRate, trainingThreshold, trainingRegularization,
												iterationCount, clampingFunction, MIN_EPOCHS);
        
        final int numUsers = snapshot.getUserIds().size();
        final int numItems = snapshot.getItemIds().size();
        double[][] userFeatures = new double[featureCount][numUsers];
        double[][] itemFeatures = new double[featureCount][numItems];
        for (int i = 0; i < featureCount; i++) {
        	trainer.reset();
            trainFeature(userFeatures, itemFeatures, estimates, ratings, i, trainer);
        }

        return new FunkSVDModel(featureCount, itemFeatures, userFeatures,
                                clampingFunction, snapshot.itemIndex(), snapshot.userIndex(), baseline);
    }

    private double[] initializeEstimates(PreferenceSnapshot snapshot,
                                                      BaselinePredictor baseline) {
        final int nusers = snapshot.userIndex().getObjectCount();
        final int nprefs = snapshot.getRatings().size();
        double[] estimates = new double[nprefs];
        for (int i = 0; i < nusers; i++) {
            final long uid = snapshot.userIndex().getId(i);
            FastCollection<IndexedPreference> ratings = snapshot.getUserRatings(uid);
            SparseVector rvector = snapshot.userRatingVector(uid);
            MutableSparseVector blpreds = baseline.predict(uid, rvector, rvector.keySet());
            for (IndexedPreference p: CollectionUtils.fast(ratings)) {
                estimates[p.getIndex()] = blpreds.get(p.getItemId());
            }
        }
        return estimates;
    }

    @SuppressWarnings("deprecation")
	private void trainFeature(double[][] ufvs, double[][] ifvs,
                              double[] estimates,
                              FastCollection<IndexedPreference> ratings,
                              int feature, UpdateRule trainer) {
    	
        logger.trace("Training feature {}", feature);

        // Fetch and initialize the arrays for this feature
        final double[] ufv = ufvs[feature];
        final double[] ifv = ifvs[feature];
        DoubleArrays.fill(ufv, DEFAULT_FEATURE_VALUE);
        DoubleArrays.fill(ifv, DEFAULT_FEATURE_VALUE);

        // Initialize our counters and error tracking
        StopWatch timer = new StopWatch();
        timer.start();
        
        while (trainer.nextEpochs()) {
            logger.trace("Running epoch {} of feature {}", trainer.getEpoch(), feature);
            trainFeatureIteration(ratings, ufv, ifv, estimates, trainer, feature);
            logger.trace("Epoch {} had RMSE of {}", trainer.getEpoch(), trainer.getRmse());
        }

        timer.stop();
        logger.debug("Finished feature {} in {} epochs (took {}), rmse={}",
                new Object[]{feature, trainer.getEpoch(), timer, trainer.getRmse()});

        // After training this feature, we need to update each rating's cached
        // value to accommodate it.
        for (IndexedPreference r: ratings.fast()) {
            final int idx = r.getIndex();
            final int uidx = r.getUserIndex();
            final int iidx = r.getItemIndex();
            double est = estimates[idx];
            est = clampingFunction.apply(r.getUserId(), r.getItemId(),
                                         est + ufv[uidx] * ifv[iidx]);
            estimates[idx] = est;
        }
    }


    @SuppressWarnings("deprecation")
	private final void trainFeatureIteration(FastCollection<IndexedPreference> ratings,
            double[] ufv, double[] ifv, double[] estimates, UpdateRule trainer, int feature) {
        for (IndexedPreference r: ratings.fast()) {
            trainRating(ufv, ifv, estimates, trainer, r, feature);
        }
    }

    
    private final void trainRating(double[] ufv, double[] ifv,
                                 double[] estimates, UpdateRule trainer,
                                 IndexedPreference r, int feature) {
        final long uid = r.getUserId();
        final long iid = r.getItemId();
        final int uidx = r.getUserIndex();
        final int iidx = r.getItemIndex();
        final double value = r.getValue();
        // Step 1: get the predicted value (based on preceding features
        // and the current feature values)
        final double estimate = estimates[r.getIndex()];
        
        // Step 2: Save the old feature values before computing the new ones 
        final double ouf = ufv[uidx];
        final double oif = ifv[iidx];
        
        // Step 3: Compute the error
        // We assume that all subsequent features have DEFAULT_FEATURE_VALUE
        // We can therefore precompute the "trailing" prediction value, as it
        // will be the same for all ratings for this feature.
        final int rFeatCount = featureCount - feature - 1;
        final double trailingValue = rFeatCount * DEFAULT_FEATURE_VALUE * DEFAULT_FEATURE_VALUE;
        trainer.compute(uid, iid, trailingValue, estimate, value, ouf, oif);

        // Step 4: Update feature values
        ufv[uidx] = trainer.getUserUpdate(ouf, oif);
        ifv[uidx] = trainer.getItemUpdate(ouf, oif);
    }
}
