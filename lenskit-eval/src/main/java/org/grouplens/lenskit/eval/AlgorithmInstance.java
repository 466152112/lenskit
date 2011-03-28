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
package org.grouplens.lenskit.eval;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.grouplens.lenskit.RatingPredictor;
import org.grouplens.lenskit.RecommenderCoreModule;
import org.grouplens.lenskit.RecommenderModuleComponent;
import org.grouplens.lenskit.data.dao.RatingDataAccessObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

/**
 * An instance of a recommender algorithm to be benchmarked.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class AlgorithmInstance {
    private static final Logger logger = LoggerFactory.getLogger(AlgorithmInstance.class);
    private @Nonnull String algoName;
    private @Nullable RecommenderModuleComponent module;
    private @Nonnull Map<String,String> attributes;

    public AlgorithmInstance() {
        attributes = new HashMap<String,String>();
    }

    /**
     * Helper method to extract the basename for a file with an expected extension.
     * @param f The file
     * @param xtn The extension (or NULL, if no extension is to be stripped).
     * @return The basename of the file, with <var>xtn</var> stripped if it is
     * the file's extension.
     */
    static String fileBaseName(File f, String xtn) {
        String name = f.getName();
        if (xtn != null && name.endsWith("." + xtn)) {
            name = name.substring(0, name.length() - xtn.length() - 1);
        }
        return name;
    }

    /**
     * Get the name of this algorithm.  This returns a short name which is
     * used to identify the algorithm or instance.
     * @return The algorithm's name
     */
    public String getName() {
        return algoName;
    }

    /**
     * Set the instance's name.
     * @param name The instance name
     */
    public void setName(String name) {
        algoName = name;
        if (module != null)
            module.setName(name);
    }

    public Map<String,String> getAttributes() {
        return attributes;
    }

    public RecommenderModuleComponent getModule() {
        return module;
    }

    public void setModule(RecommenderModuleComponent mod) {
        module = mod;
        mod.setName(getName());
    }

    public void setModule(Class<? extends RecommenderCoreModule> mod) throws InstantiationException, IllegalAccessException {
        setModule(mod.newInstance());
    }

    private static class DataModule extends AbstractModule {
        private RatingDataAccessObject dataSource;
        public DataModule(RatingDataAccessObject source) {
            dataSource = source;
        }

        @Override protected void configure() {
        }

        @SuppressWarnings("unused")
        @Provides public RatingDataAccessObject provideDataSource() {
            return dataSource;
        }
    }

    public Injector makeInjector(final RatingDataAccessObject input) {
        return Guice.createInjector(new DataModule(input), module);
    }

    public RatingPredictor getRecommenderService(final RatingDataAccessObject input) {
        Injector inj = makeInjector(input);
        return inj.getInstance(RatingPredictor.class);
    }
    
    public static AlgorithmInstance load(File f) throws InvalidRecommenderException {
        return load(f, null);
    }

    public static AlgorithmInstance load(File f, @Nullable ClassLoader classLoader) throws InvalidRecommenderException {
        logger.info("Loading recommender definition from {}", f);
        String xtn = fileExtension(f);
        logger.debug("Loading recommender from {} with extension {}", f, xtn);
        if (classLoader == null)
            classLoader = Thread.currentThread().getContextClassLoader();
        ScriptEngineManager mgr = new ScriptEngineManager(classLoader);
        ScriptEngine engine = mgr.getEngineByExtension(xtn);
        if (engine == null)
            throw new InvalidRecommenderException(f.toURI(), "Cannot find engine for extension " + xtn);
        ScriptEngineFactory factory = engine.getFactory();
        logger.debug("Using {} {}", factory.getEngineName(), factory.getEngineVersion());
        AlgorithmInstance algo = new AlgorithmInstance();
        mgr.put("rec", algo);
        try {
            Reader r = new FileReader(f);
            try {
                engine.eval(r);
                if (algo.getModule() != null)
                    return algo;
                else
                    throw new InvalidRecommenderException(f.toURI(), "No recommender configured");
            } finally {
                r.close();
            }
        } catch (ScriptException e) {
            throw new InvalidRecommenderException(f.toURI(), e);
        } catch (IOException e) {
            throw new InvalidRecommenderException(f.toURI(), e);
        }
    }

    static String fileExtension(File f) {
        return fileExtension(f.getName());
    }
    static String fileExtension(String fn) {
        int idx = fn.lastIndexOf('.');
        if (idx >= 0) {
            return fn.substring(idx+1);
        } else {
            return "";
        }
    }
}
