package org.grouplens.lenskit.svd;

import javax.inject.Inject;

import org.grouplens.lenskit.svd.params.IterationCount;
import org.grouplens.lenskit.svd.params.LearningRate;
import org.grouplens.lenskit.svd.params.RegularizationTerm;
import org.grouplens.lenskit.svd.params.TrainingThreshold;
import org.grouplens.lenskit.transform.clamp.ClampingFunction;

public final class UpdateRule {
	private int epoch;
	private int ratingCount;
	private double err;
	private double ssq;
	private double oldRmse;
	private double rmse;
	
	private final double MIN_EPOCHS;
	private final double iterationCount;
    private final double learningRate;
    private final double trainingThreshold;
    private final double trainingRegularization;
    private final ClampingFunction clampingFunction;
    
	@Inject
	public UpdateRule(@LearningRate double learningRate, @TrainingThreshold double threshold,
            	@RegularizationTerm double gradientDescent, ClampingFunction clamp,
            	@IterationCount int iterCount) {
		epoch = 0;
		ratingCount = 0;
		err = 0.0;
		ssq = 0.0;
		oldRmse = 0.0;
		rmse = Double.MAX_VALUE;
		
		MIN_EPOCHS = 50;
		this.learningRate = learningRate;
		trainingThreshold = threshold;
		trainingRegularization = gradientDescent;
		clampingFunction = clamp;
		iterationCount = iterCount;
		
	}
	
	public void compute(long uid, long iid, double trailingValue, 
			double estimate, double rating, double ufv, double ifv) {
		
		// Compute prediction
		double pred = estimate + ufv * ifv;
		
		// Clamp the prediction first
		pred = clampingFunction.apply(uid, iid, pred);
		
		// Add the trailing value, then clamp the result again
		pred = clampingFunction.apply(uid, iid, pred + trailingValue);
		
		// Compute the err and store this value
		err = rating - pred;
		
		// Update properties
		ssq += err * err;
		
		// Keep track of how many ratings have been gone through
		ratingCount += 1;
	}
	
	public double getUserUpdate(double ufv, double ifv) {
		double delta = err * ifv - trainingRegularization * ufv;
		return ufv + delta * learningRate;
	}
	
	public double getItemUpdate(double ufv, double ifv) {
		double delta = err * ufv - trainingRegularization * ifv;
		return ifv + delta * learningRate;
	}
	
	public int getEpoch() {
		return epoch;
	}
	
	public double getLastRMSE() {
		return rmse;
	}
	
	public double getIterationCount() {
		return iterationCount;
	}
	
	public double getLearningRate() {
		return learningRate;
	}
	
	public double getTrainingThreshold() {
		return trainingThreshold;
	}
	
	public double getTrainingRegularization() {
		return trainingRegularization;
	}
	
	public ClampingFunction getClampingFunction() {
		return clampingFunction;
	}
	
	public void reset() {
		epoch = 0;
		err = 0.0;
		ssq = 0.0;
	}
	
	private boolean isDone(int epoch, double rmse, double oldRmse) {
        if (iterationCount > 0) {
            return epoch >= iterationCount;
        } else {
            return epoch >= MIN_EPOCHS && (oldRmse - rmse) < trainingThreshold;
        }
    }
	
	public boolean nextEpoch() {
		if (!isDone(epoch, rmse, oldRmse)) {
			oldRmse = rmse;
			rmse = Math.sqrt(ssq / ratingCount);
			epoch += 1;
			ratingCount = 0;
			return true;
		} 
		
		return false;
	}
}
