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
/**
 * 
 */
package org.grouplens.lenskit.eval.ant;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.codehaus.plexus.util.FileUtils;
import org.grouplens.lenskit.eval.InvalidRecommenderException;
import org.grouplens.lenskit.eval.traintest.EvaluationRecipe;
import org.grouplens.lenskit.eval.traintest.TrainTestPredictEvaluator;

import com.google.common.primitives.Longs;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class TrainTestTask extends Task {
	private String databaseDriver = "org.sqlite.JDBC";
	private FileSet databases;
	private File outFile;
	private File script;
	private int threadCount = 0;
	private File predictionOutput;
	private boolean useTimestamp = true;
	private Properties properties = new Properties();
	
	public void setDatabaseDriver(String driver) {
		databaseDriver = driver;
	}
	
	public void setOutput(File f) {
		outFile = f;
	}
	
	public void setScript(File s) {
	    script = s;
	}
	
	public void setThreadCount(int n) {
		threadCount = n;
	}
	
	public void setPredictions(File f) {
		predictionOutput = f;
	}
	
	public void setTimestamp(boolean ts) {
		useTimestamp = ts;
	}
	
	public void addConfiguredDatabases(FileSet dbs) {
		databases = dbs;
	}
	
	public void addConfiguredProperty(Property prop) {
	    properties.put(prop.getName(), prop.getValue());
	}
	
	public void execute() throws BuildException {
		if (databaseDriver != null) {
			try {
				Class.forName(databaseDriver);
			} catch (ClassNotFoundException e) {
				throw new BuildException("Database driver " + databaseDriver + " not found");
			}
		}
		EvaluationRecipe recipe;
		try {
		    log("Loading recommender from " + script.getPath());
		    recipe = EvaluationRecipe.load(script, properties, outFile);
		    if (predictionOutput != null) {
		        try {
		            recipe.setPredictionOutput(predictionOutput);
		        } catch (IOException e) {
		            handleErrorOutput("Cannot open prediction output");
		        }
		    }
		} catch (InvalidRecommenderException e) {
			throw new BuildException("Invalid recommender", e);
		}
		
		DirectoryScanner dbs = databases.getDirectoryScanner();
        dbs.scan();
        String[] dbNames = dbs.getIncludedFiles();
        File[] dbFiles = new File[dbNames.length];
        for (int i = 0; i < dbNames.length; i++) {
            dbFiles[i] = new File(dbs.getBasedir(), dbNames[i]);
        }
        Arrays.sort(dbFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Longs.compare(f1.length(), f2.length());
            }
        });
        for (int i = 0; i < dbFiles.length; i++) {
            File dbf = dbFiles[i];
            String name = FileUtils.basename(dbf.getName(), ".db");
            String dsn = "jdbc:sqlite:" + dbf.getPath();
            TrainTestPredictEvaluator eval = new TrainTestPredictEvaluator(dsn, "train", "test");
            eval.setTimestampEnabled(useTimestamp);
            eval.setThreadCount(threadCount);
            log(String.format("Running against %s with %d threads", name, eval.getThreadCount()));
            eval.evaluateAlgorithms(recipe.getAlgorithms(), recipe.makeAccumulator(name));
        }
	}
}
