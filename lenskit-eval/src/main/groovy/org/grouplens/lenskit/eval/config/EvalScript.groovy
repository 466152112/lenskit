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
package org.grouplens.lenskit.eval.config

import org.apache.commons.lang3.builder.Builder
import org.grouplens.lenskit.eval.EvalTask
import org.slf4j.LoggerFactory
import org.apache.tools.ant.DirectoryScanner

/**
 * Base class for evaluator configuration scripts. It contains the metaclass
 * machinery to set up evaluation taskMap.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 * @since 0.10
 */
class EvalScript extends Script {
    protected final def logger = LoggerFactory.getLogger(getClass())
    private EvalScriptEngine engine

    EvalScript() {
        engine = null
    }

    EvalScript(EvalScriptEngine eng) {
        engine = eng
    }

    EvalConfig getConfig() {
        return engine.config
    }

    EvalScriptEngine getEngine() {
        return engine
    }

    void setEngine(EvalScriptEngine ece) {
        engine = ece
    }

    /**
     * Evaluate another script.
     * @param file The script to evaluate.
     * @param args The arguments to the script.
     * @return The return value of the script (typically the return value of its last expression).
     */
    Object evalScript(File file, String... args) {
        return engine.execute(file, args)
    }

    /**
     * Evaluate another script.
     * @param fn The script to evaluate.
     * @param args The arguments to the script.
     * @return The return value of the script (typically the return value of its last expression).
     */
    Object evalScript(String fn, String... args) {
        return evalScript(new File(fn), args)
    }

    def methodMissing(String name, args) {
        logger.debug("searching for eval command {}", name)
        def method = ConfigHelpers.findExternalMethod(engine, name)
        if (method != null) {
            def obj = method(args)
            if (obj instanceof Builder) {
                return obj.build()
            } else if (obj instanceof EvalTask) {
                obj.setEvalConfig(config)
                return obj.call()
            } else {
                throw new RuntimeException("${obj} is not a configurable object")
            }
        } else {
            throw new MissingMethodException(name, this.class, args)
        }
    }

    /**
     * Performs a file search based upon the parameter glob pattern.
     * @param globPattern String in glob syntax giving the glob to expand.
     * @return A List<String> of paths from the working directory to
     *          matching file names.
     */
    def glob(String globPattern) {
        glob(globPattern, ".")
    }

    /**
     * Performs a file search based upon the parameter glob pattern.
     * @param globPattern String in glob syntax giving the glob to expand.
     * @param baseDir The base directory from which to search.
     * @return A List<String> of paths from the base directory
     *          matching the glob.
     */
    def glob(String globPattern, String baseDir) {
        def ds = new DirectoryScanner();
        ds.setIncludes([globPattern] as String[])
        ds.setBasedir(baseDir)
        ds.scan()
        return ds.getIncludedFiles()
    }

    def run() {
        throw new UnsupportedOperationException("script not implemented")
    }
}