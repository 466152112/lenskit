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
package org.grouplens.lenskit.eval.cli;

import org.codehaus.groovy.runtime.StackTraceUtils;
import org.grouplens.lenskit.eval.CommandException;
import org.grouplens.lenskit.eval.config.EvalScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Main entry point to run the evaluator from the command line
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 * @since 0.8
 */
public class EvalCLI {
    private static final Logger logger = LoggerFactory.getLogger(EvalCLI.class);

    /**
     * Run the evaluator from the command line.
     *
     * @param args The command line arguments to the evaluator.
     */
    public static void main(String[] args) {
        EvalCLIOptions options = EvalCLIOptions.parse(args);
        EvalCLI cli = new EvalCLI(options);
        cli.run();
    }

    private final EvalCLIOptions options;

    public EvalCLI(EvalCLIOptions opts) {
        options = opts;
    }

    public void run() {
        ClassLoader loader = options.getClassLoader();
        EvalScriptEngine engine = new EvalScriptEngine(loader, options.getProperties());

        File f = options.getScriptFile();
        logger.info("loading evaluation from {}", f);
        try {
            engine.execute(f, options.getArgs());
        } catch (CommandException e) {
            // we handle these specially
            reportError(e.getCause(), "%s: %s", f.getPath(), e.getMessage());
            return;
        } catch (IOException e) {
            reportError(e, "%s: %s", f.getPath(), e.getMessage());
            return;
        }
    }

    protected void reportError(@Nullable Throwable e, String msg, Object... args) {
        String text = String.format(msg, args);
        System.err.println(text);
        if (e instanceof ExecutionException) {
            e = e.getCause();
        }
        if (options.throwErrors()) {
            throw new RuntimeException(text, e);
        } else {
            if (e != null) {
                //noinspection ThrowableResultOfMethodCallIgnored
                StackTraceUtils.sanitize(e).printStackTrace(System.err);
            }
            System.exit(2);
        }
    }
}
