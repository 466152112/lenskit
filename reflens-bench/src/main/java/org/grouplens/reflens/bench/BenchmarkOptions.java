/*
 * RefLens, a reference implementation of recommender algorithms.
 * Copyright 2010 Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but
 * you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package org.grouplens.reflens.bench;

import java.io.File;
import java.util.List;

import uk.co.flamingpenguin.jewel.cli.Option;
import uk.co.flamingpenguin.jewel.cli.Unparsed;

/**
 * Interface for JewelCLI declaring the command line options taken by
 * BenchmarkRunner.
 * 
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 * 
 */
interface BenchmarkOptions {
	/**
	 * @return The field separator in the data file.
	 */
	@Option(longName = "delimiter", shortName = "d", defaultValue = "\t")
	String getDelimiter();

	/**
	 * @return The number of folds to use (where 10 is 10-fold, 90/10 train/test
	 *         split).
	 */
	@Option(longName="num-folds", shortName="n", defaultValue="10")
	int getNumFolds();
	
	@Option(longName="holdout-fraction", defaultValue="0.3333333")
	double getHoldoutFraction();
	
	@Option(longName="input-file", shortName="i", defaultValue="ratings.dat")
	File getInputFile();
	
	@Option(longName="output-file", shortName="o", defaultValue="")
	File getOutputFile();
	
	@Unparsed(name="FILES")
	List<File> getRecommenderSpecs();

	@Option(helpRequest = true)
	boolean getHelp();
}