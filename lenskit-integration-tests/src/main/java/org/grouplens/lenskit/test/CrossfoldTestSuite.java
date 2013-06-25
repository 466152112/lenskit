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
package org.grouplens.lenskit.test;

import org.grouplens.lenskit.core.LenskitRecommenderEngineFactory;
import org.grouplens.lenskit.eval.TaskExecutionException;
import org.grouplens.lenskit.eval.algorithm.LenskitAlgorithmInstanceBuilder;
import org.grouplens.lenskit.eval.config.EvalConfig;
import org.grouplens.lenskit.eval.data.GenericDataSource;
import org.grouplens.lenskit.eval.metrics.predict.CoveragePredictMetric;
import org.grouplens.lenskit.eval.metrics.predict.MAEPredictMetric;
import org.grouplens.lenskit.eval.metrics.predict.RMSEPredictMetric;
import org.grouplens.lenskit.eval.traintest.SimpleEvaluator;
import org.grouplens.lenskit.util.table.Table;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * A test suite that does cross-validation of an algorithm.
 */
public abstract class CrossfoldTestSuite extends ML100KTestSuite {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    protected abstract void configureAlgorithm(LenskitRecommenderEngineFactory factory);

    protected abstract void checkResults(Table table);

    @Test
    public void testAlgorithmAccuracy() throws TaskExecutionException, IOException {
        Properties props =  new Properties(System.getProperties());
        props.setProperty(EvalConfig.DATA_DIR_PROPERTY, workDir.newFolder("data").getAbsolutePath());
        EvalConfig config = new EvalConfig(props);
        SimpleEvaluator evalCommand = new SimpleEvaluator(config);
        LenskitAlgorithmInstanceBuilder algo = new LenskitAlgorithmInstanceBuilder();
        configureAlgorithm(algo.getFactory());
        evalCommand.addAlgorithm(algo);

        evalCommand.addDataset(new GenericDataSource("ml-100k", daoFactory), 5, 0.2);

        evalCommand.addMetric(new CoveragePredictMetric())
                   .addMetric(new RMSEPredictMetric())
                   .addMetric(new MAEPredictMetric());


        Table result = evalCommand.call();
        assertThat(result, notNullValue());
        checkResults(result);
    }
}
