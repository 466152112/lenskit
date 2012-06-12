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
package org.grouplens.lenskit.svd;

import java.io.Serializable;

import org.grouplens.grapht.annotation.DefaultProvider;
import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.transform.clamp.ClampingFunction;
import org.grouplens.lenskit.util.Index;

@DefaultProvider(FunkSVDModelProvider.class)
public class FunkSVDModel implements Serializable {
    private static final long serialVersionUID = -5797099617512506185L;

    public final int featureCount;
    public final double itemFeatures[][];
    public final double userFeatures[][];
    public final ClampingFunction clampingFunction;

    public final Index itemIndex;
    public final Index userIndex;
    public final BaselinePredictor baseline;

    //Added
    public final double[] averUserFeatures;
    public final double[] averItemFeatures;
    public final int numUser;
    public final int numItem;
    //End Adding
    
    public FunkSVDModel(int nfeatures, double[][] ifeats, double[][] ufeats,
                        ClampingFunction clamp, Index iidx, Index uidx,
                        BaselinePredictor baseline) {
        featureCount = nfeatures;
        itemFeatures = ifeats;
        userFeatures = ufeats;
        clampingFunction = clamp;
        itemIndex = iidx;
        userIndex = uidx;
        this.baseline = baseline; 
        
        //Added
        numItem = iidx.getIds().size();
        numUser = uidx.getIds().size();
        averItemFeatures = getAverageFeatureVector(ifeats, numItem, featureCount);
        averUserFeatures = getAverageFeatureVector(ufeats, numUser, featureCount);
        //End Adding
        
    }

    //Add
    
    private double[] getAverageFeatureVector(double[][] twoDimMatrix, int dimension, int feature){
    	double[] outputVector = new double[feature];
    	for (int i = 0; i < feature; i++){
    		for (int j = 0; j < dimension; j++){
    			outputVector[i] += twoDimMatrix[i][j];
    		}
    		outputVector[i] = outputVector[i] / dimension;
    	}
    	return outputVector;
    }
    
    public double getUserFeatureValue(int user, int feature){
    	return userFeatures[feature][user];
    }
    
    public int getUserIndex(long user){
    	return userIndex.getIndex(user);
    }
    //End Adding
    
    public double getItemFeatureValue(int item, int feature) {
        return itemFeatures[feature][item];
    }

    public int getItemIndex(long item) {
        return itemIndex.getIndex(item);
    }
}
