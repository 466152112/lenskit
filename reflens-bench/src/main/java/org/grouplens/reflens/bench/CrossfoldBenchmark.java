package org.grouplens.reflens.bench;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.grouplens.reflens.RatingPredictor;
import org.grouplens.reflens.RecommenderEngine;
import org.grouplens.reflens.bench.crossfold.CrossfoldManager;
import org.grouplens.reflens.data.Rating;
import org.grouplens.reflens.data.RatingDataSource;
import org.grouplens.reflens.data.RatingVector;
import org.grouplens.reflens.data.ScoredId;
import org.grouplens.reflens.data.UserRatingProfile;
import org.grouplens.reflens.tablewriter.CSVWriterBuilder;
import org.grouplens.reflens.tablewriter.TableWriter;
import org.grouplens.reflens.tablewriter.TableWriterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run a crossfold benchmark.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class CrossfoldBenchmark implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(CrossfoldBenchmark.class);
	private final CrossfoldManager manager;
	private final int numFolds;
	private final double holdoutFraction;
	private final List<AlgorithmInstance> algorithms;
	
	private TableWriter writer;
	private int colTestSize, colTrainSize, colAlgo, colMAE, colRMSE;
	private int colNTry, colNGood, colCoverage;
	
	public CrossfoldBenchmark(RatingDataSource ratings, CrossfoldOptions options,
			List<AlgorithmInstance> algorithms, Writer output) throws IOException {
		numFolds = options.getNumFolds();
		holdoutFraction = options.getHoldoutFraction();
		manager = new CrossfoldManager(numFolds, ratings);
		this.algorithms = algorithms;
		writer = makeWriter(output);
	}
	
	public void run() {
		for (int i = 0; i < numFolds; i++) {
			RatingDataSource train = manager.trainingSet(i);
			try {
				Collection<UserRatingProfile> test = manager.testSet(i);
				int nusers = train.getUserCount();
				logger.info(String.format("Running benchmark %d with %d training and %d test users",
						i+1, nusers, test.size()));
				for (AlgorithmInstance algo: algorithms) {
					writer.setValue(colTrainSize, nusers);
					writer.setValue(colTestSize, test.size());
					writer.setValue(colAlgo, algo.getName());
					benchmarkAlgorithm(algo, train, test);
					writer.finishRow();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				train.close();
			}
		}
		try {
			writer.finish();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Print the header for the data file
	 * @param algorithms
	 * @throws IOException 
	 */
	private TableWriter makeWriter(Writer output) throws IOException {
		TableWriterBuilder bld = new CSVWriterBuilder();
		colTrainSize = bld.addColumn("TrainSize");
		colTestSize = bld.addColumn("TestSize");
		colAlgo = bld.addColumn("Algortihm");
		colMAE = bld.addColumn("MAE");
		colRMSE = bld.addColumn("RMSE");
		colNTry = bld.addColumn("NTried");
		colNGood = bld.addColumn("NGood");
		colCoverage = bld.addColumn("Coverage");
		
		return bld.makeWriter(output);
	}
	
	private void benchmarkAlgorithm(AlgorithmInstance algo, RatingDataSource train, Collection<UserRatingProfile> test) {
		TaskTimer timer = new TaskTimer();
		logger.debug("Benchmarking {}", algo.getName());
		RecommenderEngine engine;
		logger.debug("Building recommender");
		engine = algo.getBuilder().build(train);
		RatingPredictor rec = engine.getRatingPredictor();
		logger.debug("Built model {} model in {}",
				algo.getName(), timer.elapsedPretty());
		
		logger.debug("Testing recommender");
		double accumErr = 0.0f;		// accmulated error
		double accumSqErr = 0.0f;	// accumluated squared error
		int nitems = 0;				// total ratings
		int ngood = 0;				// total predictable ratings
		for (UserRatingProfile user: test) {
			List<Rating> ratings = new ArrayList<Rating>(user.getRatings());
			int midpt = (int) Math.round(ratings.size() * (1.0 - holdoutFraction));
			Collections.shuffle(ratings);
			RatingVector queryRatings = new RatingVector();
			for (int i = 0; i < midpt; i++) {
				Rating rating = ratings.get(i);
				queryRatings.put(rating.getItemId(), rating.getRating());
			}
			for (int i = midpt; i < ratings.size(); i++) {
				long iid = ratings.get(i).getItemId();
				ScoredId prediction = rec.predict(user.getUser(), queryRatings, iid);
				nitems++;
				if (prediction != null) {
					double err = prediction.getScore() - user.getRating(iid);
					ngood++;
					accumErr += Math.abs(err);
					accumSqErr += err * err;
				}
			}
		}
		double mae = accumErr / ngood;
		double rmse = accumSqErr / ngood;
		double cov = (double) nitems / ngood;
		logger.info(String.format("Recommender %s finished in %s (mae=%f, rmse=%f)",
				algo.getName(), timer.elapsedPretty(), mae, rmse));
		writer.setValue(colMAE, mae);
		writer.setValue(colRMSE, rmse);
		writer.setValue(colNTry, nitems);
		writer.setValue(colNGood, ngood);
		writer.setValue(colCoverage, cov);
	}
}
