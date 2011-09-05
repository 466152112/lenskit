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
package org.grouplens.lenskit.svd;

import it.unimi.dsi.fastutil.doubles.DoubleArrays;

import org.grouplens.lenskit.RecommenderComponentBuilder;
import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.data.pref.IndexedPreference;
import org.grouplens.lenskit.data.snapshot.RatingSnapshot;
import org.grouplens.lenskit.data.vector.MutableSparseVector;
import org.grouplens.lenskit.data.vector.UserVector;
import org.grouplens.lenskit.svd.params.ClampingFunction;
import org.grouplens.lenskit.svd.params.FeatureCount;
import org.grouplens.lenskit.svd.params.IterationCount;
import org.grouplens.lenskit.svd.params.LearningRate;
import org.grouplens.lenskit.svd.params.RegularizationTerm;
import org.grouplens.lenskit.svd.params.TrainingThreshold;
import org.grouplens.lenskit.util.DoubleFunction;
import org.grouplens.lenskit.util.FastCollection;
import org.grouplens.lenskit.util.TaskTimer;
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
public class FunkSVDModelBuilder extends RecommenderComponentBuilder<FunkSVDModel> {
    private static Logger logger = LoggerFactory.getLogger(FunkSVDModelBuilder.class);

    // The default value for feature values - isn't supposed to matter much
    private static final double DEFAULT_FEATURE_VALUE = 0.1;
    // Minimum number of epochs to run to train a feature
    private static final double MIN_EPOCHS = 50;

    private int featureCount;
    private double learningRate;
    private double trainingThreshold;
    private double trainingRegularization;
    private DoubleFunction clampingFunction;
    private int iterationCount;

    private BaselinePredictor baseline;

    @FeatureCount
    public void setFeatureCount(int count) {
        featureCount = count;
    }

    @LearningRate
    public void setLearningRate(double rate) {
        learningRate = rate;
    }

    @TrainingThreshold
    public void setTrainingThreshold(double threshold) {
        trainingThreshold = threshold;
    }

    @RegularizationTerm
    public void setGradientDescentRegularization(double regularization) {
        trainingRegularization = regularization;
    }

    @ClampingFunction
    public void setClampingFunction(DoubleFunction function) {
        clampingFunction = function;
    }

    @IterationCount
    public void setIterationCount(int count) {
        iterationCount = count;
    }

    public void setBaseline(BaselinePredictor baseline) {
        this.baseline = baseline;
    }

    /* (non-Javadoc)
     * @see org.grouplens.lenskit.RecommenderComponentBuilder#build(org.grouplens.lenskit.data.snapshot.RatingBuildContext)
     */
    @Override
    public FunkSVDModel build() {
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

        final int numUsers = snapshot.getUserIds().size();
        final int numItems = snapshot.getItemIds().size();
        double[][] userFeatures = new double[featureCount][numUsers];
        double[][] itemFeatures = new double[featureCount][numItems];
        for (int i = 0; i < featureCount; i++) {
            trainFeature(userFeatures, itemFeatures, estimates, ratings, i);
        }

        return new FunkSVDModel(featureCount, itemFeatures, userFeatures,
                                clampingFunction, snapshot.itemIndex(), snapshot.userIndex(), baseline);
    }

    private double[] initializeEstimates(RatingSnapshot snapshot,
                                                      BaselinePredictor baseline) {
        final int nusers = snapshot.userIndex().getObjectCount();
        final int nprefs = snapshot.getRatings().size();
        double[] estimates = new double[nprefs];
        for (int i = 0; i < nusers; i++) {
            final long uid = snapshot.userIndex().getId(i);
            // FIXME Use the snapshot's user rating vector support
            FastCollection<IndexedPreference> ratings = snapshot.getUserRatings(uid);
            UserVector user = UserVector.fromPreferences(uid, ratings);
            MutableSparseVector blpreds = baseline.predict(user, user.keySet());
            for (IndexedPreference p: ratings.fast()) {
                estimates[p.getIndex()] = blpreds.get(p.getItemId());
            }
        }
        return estimates;
    }

    private final void trainFeature(double[][] ufvs, double[][] ifvs,
                                    double[] estimates,
                                    FastCollection<IndexedPreference> ratings, int feature) {

        logger.trace("Training feature {}", feature);

        // Fetch and initialize the arrays for this feature
        final double[] ufv = ufvs[feature];
        final double[] ifv = ifvs[feature];
        DoubleArrays.fill(ufv, DEFAULT_FEATURE_VALUE);
        DoubleArrays.fill(ifv, DEFAULT_FEATURE_VALUE);

        // We assume that all subsequent features have DEFAULT_FEATURE_VALUE
        // We can therefore precompute the "trailing" prediction value, as it
        // will be the same for all ratings for this feature.
        final int rFeatCount = featureCount - feature - 1;
        final double trailingValue = rFeatCount * DEFAULT_FEATURE_VALUE * DEFAULT_FEATURE_VALUE;

        // Initialize our counters and error tracking
        double rmse = Double.MAX_VALUE, oldRmse = 0.0;
        int epoch;
        TaskTimer timer = new TaskTimer();

        for (epoch = 0; !isDone(epoch, rmse, oldRmse); epoch++) {
            logger.trace("Running epoch {} of feature {}", epoch, feature);
            // Save the old RMSE so that we can measure change in error
            oldRmse = rmse;
            // Run the iteration and save the error
            rmse = trainFeatureIteration(ratings, ufv, ifv, estimates, trailingValue);
            logger.trace("Epoch {} had RMSE of {}", epoch, rmse);
        }

        logger.debug("Finished feature {} in {} epochs (took {}), rmse={}",
                new Object[]{feature, epoch, timer.elapsedPretty(), rmse});

        // After training this feature, we need to update each rating's cached
        // value to accommodate it.
        for (IndexedPreference r: ratings.fast()) {
            final int idx = r.getIndex();
            final int uidx = r.getUserIndex();
            final int iidx = r.getItemIndex();
            double est = estimates[idx];
            est = clampingFunction.apply(est + ufv[uidx] * ifv[iidx]);
            estimates[idx] = est;
        }
    }

    /**
     * We have two potential terminating conditions: if iterationCount is
     * specified, we run for that many iterations irregardless of error.
     * Otherwise, we run until the change in error is less than the training
     * threshold.
     *
     * @param epoch
     * @param rmse
     * @param oldRmse
     * @return <tt>true</tt> if the feature is sufficiently trained
     */
    protected final boolean isDone(int epoch, double rmse, double oldRmse) {
        if (iterationCount > 0) {
            return epoch >= iterationCount;
        } else {
            return epoch >= MIN_EPOCHS && (oldRmse - rmse) < trainingThreshold;
        }
    }

    private final double trainFeatureIteration(FastCollection<IndexedPreference> ratings,
            double[] ufv, double[] ifv, double[] estimates, double trailingValue) {
        // We'll need to keep track of our sum of squares
        double ssq = 0;
        for (IndexedPreference r: ratings.fast()) {
            ssq += trainRating(ufv, ifv, estimates, trailingValue, r);
        }
        // We're done with this feature.  Compute the total error (RMSE)
        // and head off to the next iteration.
        return Math.sqrt(ssq / ratings.size());
    }

    private final double trainRating(double[] ufv, double[] ifv,
                                 double[] estimates,
                                 double trailingValue,
                                 IndexedPreference r) {
        final int uidx = r.getUserIndex();
        final int iidx = r.getItemIndex();
        final double value = r.getValue();
        // Step 1: get the predicted value (based on preceding features
        // and the current feature values)
        final double estimate = estimates[r.getIndex()];
        double pred = estimate + ufv[uidx] * ifv[iidx];
        pred = clampingFunction.apply(pred);

        // Step 1b: add the estimate from remaining trailing values
        // and clamp the result.
        pred = clampingFunction.apply(pred + trailingValue);

        // Step 2: compute the prediction error. We will follow this for
        // the gradient descent.
        final double err = value - pred;

        // Step 3: update the feature values.  We'll save the old values first.
        final double ouf = ufv[uidx];
        final double oif = ifv[iidx];
        // Then we'll update user feature preference
        final double udelta = err * oif - trainingRegularization * ouf;
        ufv[uidx] += udelta * learningRate;
        // And the item feature relevance.
        final double idelta = err * ouf - trainingRegularization * oif;
        ifv[iidx] += idelta * learningRate;

        // Finally, accumulate the squared error
        return err * err;
    }
}
