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
package org.grouplens.lenskit.eval.cli;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.grouplens.lenskit.eval.*;
import org.grouplens.lenskit.eval.config.EvalConfigEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Main entry point to run the evaluator from the command line
 * 
 * @since 0.8
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class EvalCLI {
    private static final Logger logger = LoggerFactory.getLogger(EvalCLI.class);
    
    /**
     * Run the evaluator from the command line.
     * @param args The command line arguments to the evaluator.
     */
    public static void main(String[] args) {
        EvalOptions options = EvalOptions.parse(args);
        EvalCLI cli = new EvalCLI(options);
        cli.run();
    }
    
    EvalOptions options;
    
    public EvalCLI(EvalOptions opts) {
        options = opts;
    }
    
    public void run() {
        EvalConfigEngine config = new EvalConfigEngine();
        
        List<EvalRunner> runners = new LinkedList<EvalRunner>();
        for (File f: options.getConfigFiles()) {
            logger.info("loading evaluation from {}", f);
            List<Evaluation> evals;
            try {
                evals = config.load(f);
            } catch (EvaluatorConfigurationException e) {
                // we handle these specially
                System.err.format("%s: %s\n", f.getPath(), e.getMessage());
                StackTraceUtils.sanitize(e.getCause()).printStackTrace(System.err);
                System.exit(2);
                return;
            } catch (IOException e) {
                reportError(e, "%s: %s\n", f.getPath(), e.getMessage());
                return;
            }
            for (Evaluation e: evals) {
                runners.add(new EvalRunner(e));
            }
        }
        
        PreparationContext context = new PreparationContext();
        context.setUnconditional(options.forcePrepare());
        context.setCacheDirectory(options.getCacheDir());
        for (EvalRunner runner: runners) {
            runner.setIsolationLevel(options.getIsolation());
            runner.setThreadCount(options.getThreadCount());
            try {
                runner.prepare(context);
            } catch (PreparationException e) {
                reportError(e, "Preparation error: " + e.getMessage());
                return;
            }
            
            if (!options.isPrepareOnly()) {
                try {
                    runner.run();
                } catch (ExecutionException e) {
                    reportError(e, "Error running evaluation: %s", e.getMessage());
                    return;
                }
            }
        }
    }
    
    protected void reportError(Exception e, String msg, Object... args) {
        String text = String.format(msg, args);
        System.err.println(text);
        if (options.throwErrors()) {
            throw new RuntimeException(text, e);
        } else {
            if (e != null && options.printBacktraces()) {
                e.printStackTrace(System.err);
            }
            System.exit(2);
        }
    }
}
