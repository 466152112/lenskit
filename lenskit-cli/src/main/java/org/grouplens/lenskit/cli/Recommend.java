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
package org.grouplens.lenskit.cli;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.grouplens.lenskit.ItemRecommender;
import org.grouplens.lenskit.RecommenderBuildException;
import org.grouplens.lenskit.config.ConfigurationLoader;
import org.grouplens.lenskit.core.*;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.util.io.LKFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Generate Top-N recommendations for users.
 *
 * @since 2.1
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
@CommandSpec(name="recommend", help="generate recommendations for users")
public class Recommend implements Command {
    private final Logger logger = LoggerFactory.getLogger(Recommend.class);
    private final Namespace options;
    private final InputData input;
    private final ScriptEnvironment environment;

    public Recommend(Namespace opts) {
        options = opts;
        input = new InputData(opts);
        environment = new ScriptEnvironment(opts);
    }

    @Override
    public void execute() throws IOException, RecommenderBuildException {
        LenskitRecommenderEngine engine = loadEngine();

        List<Long> users = options.get("users");
        final int n = options.getInt("num_recs");

        LenskitRecommender rec = engine.createRecommender();
        ItemRecommender irec = rec.getItemRecommender();
        if (irec == null) {
            logger.error("recommender has no item recommender");
            throw new UnsupportedOperationException("no item recommender");
        }

        logger.info("recommending for {} users", users.size());
        Stopwatch timer = new Stopwatch();
        timer.start();
        for (long user: users) {
            List<ScoredId> recs = irec.recommend(user, n);
            System.out.format("recommendations for user %d:\n", user);
            for (ScoredId item: recs) {
                System.out.format("  %d: %.3f\n", item.getId(), item.getScore());
            }
        }
        timer.stop();
        logger.info("recommended for {} users in {}", users.size(), timer);
    }

    private LenskitRecommenderEngine loadEngine() throws RecommenderBuildException, IOException {
        File modelFile = options.get("model_file");
        if (modelFile == null) {
            logger.info("creating fresh recommender");
            LenskitRecommenderEngineBuilder builder = LenskitRecommenderEngine.newBuilder();
            for (LenskitConfiguration config: loadConfigurations()) {
                builder.addConfiguration(config);
            }
            builder.addConfiguration(input.getConfiguration());
            Stopwatch timer = new Stopwatch();
            timer.start();
            LenskitRecommenderEngine engine = builder.build();
            timer.stop();
            logger.info("built recommender in {}", timer);
            return engine;
        } else {
            logger.info("loading recommender from {}", modelFile);
            LenskitRecommenderEngineLoader loader = LenskitRecommenderEngine.newLoader();
            for (LenskitConfiguration config: loadConfigurations()) {
                loader.addConfiguration(config);
            }
            loader.addConfiguration(input.getConfiguration());
            Stopwatch timer = new Stopwatch();
            timer.start();
            LenskitRecommenderEngine engine;
            InputStream input = new FileInputStream(modelFile);
            try {
                if (LKFileUtils.isCompressed(modelFile)) {
                    input = new GZIPInputStream(input);
                }
                engine = loader.load(input);
            } finally {
                input.close();
            }
            timer.stop();
            logger.info("loaded recommender in {}", timer);
            return engine;
        }
    }

    private List<LenskitConfiguration> loadConfigurations() throws IOException, RecommenderConfigurationException {
        List<File> files = options.getList("config_file");
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        ConfigurationLoader loader = new ConfigurationLoader(environment.getClassLoader());
        // FIXME Make properties available

        List<LenskitConfiguration> configs = Lists.newArrayListWithCapacity(files.size());
        for (File file: files) {
            configs.add(loader.load(file));
        }

        return configs;
    }

    public static void configureArguments(ArgumentParser parser) {
        InputData.configureArguments(parser);
        ScriptEnvironment.configureArguments(parser);
        parser.addArgument("-n", "--num-recs")
              .type(Integer.class)
              .setDefault(10)
              .metavar("N")
              .help("generate up to N recommendations per user");
        parser.addArgument("-c", "--config-file")
              .type(File.class)
              .action(Arguments.append())
              .metavar("FILE")
              .help("use configuration from FILE");
        parser.addArgument("-m", "--model-file")
              .type(File.class)
              .metavar("FILE")
              .help("load model from FILE");
        parser.addArgument("users")
              .type(Long.class)
              .nargs("+")
              .metavar("USER")
              .help("recommend for USERS");
    }
}
