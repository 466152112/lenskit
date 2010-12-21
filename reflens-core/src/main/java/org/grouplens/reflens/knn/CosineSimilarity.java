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

package org.grouplens.reflens.knn;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;

import java.util.Iterator;

import org.grouplens.reflens.data.RatingVector;
import org.grouplens.reflens.knn.params.SimilarityDamper;
import org.grouplens.reflens.util.SymmetricBinaryFunction;

import com.google.inject.Inject;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class CosineSimilarity
	implements OptimizableVectorSimilarity<RatingVector>, SymmetricBinaryFunction {
	
	private final double dampingFactor;
	
	public CosineSimilarity() {
		this(0.0);
	}
	
	@Inject
	public CosineSimilarity(@SimilarityDamper double dampingFactor) {
		this.dampingFactor = dampingFactor;
	}

	/* (non-Javadoc)
	 * @see org.grouplens.reflens.Similarity#similarity(java.lang.Object, java.lang.Object)
	 */
	@Override
	public double similarity(RatingVector vec1, RatingVector vec2) {
		double dot = 0.0f;
		
		Iterator<Long2DoubleMap.Entry> v1iter = vec1.fastIterator();
		while (v1iter.hasNext()) {
			Long2DoubleMap.Entry e = v1iter.next();
			long k = e.getLongKey();
			double v = e.getDoubleValue();
			if (vec2.containsId(k)) {
				dot += v * vec2.get(k);
			}
		}
		
		double denom = vec1.norm() * vec2.norm() + dampingFactor;
		
		if (denom == 0.0f) {
			return Double.NaN;
		} else { 
			return dot / (double) denom;
		}
	}
}
