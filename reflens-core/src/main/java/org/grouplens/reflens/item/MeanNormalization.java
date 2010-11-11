/* RefLens, a reference implementation of recommender algorithms.
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
 */

package org.grouplens.reflens.item;

import java.util.ArrayList;
import java.util.Collection;

import org.grouplens.reflens.Normalizer;
import org.grouplens.reflens.data.Rating;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class MeanNormalization implements Normalizer<Long,Collection<Rating>> {
	/**
	 * Computes the mean of the vector.
	 * @param vector
	 * @return
	 */
	private double computeMean(Collection<Rating> values) {
		if (values.isEmpty())
			return 0.0;
		
		double sum = 0.0f;
		
		for (Rating r: values) {
			sum += r.getRating();
		}
		return sum / values.size();
	}

	/* (non-Javadoc)
	 * @see org.grouplens.reflens.Normalization#normalize(java.lang.Object)
	 */
	@Override
	public Collection<Rating> normalize(Long owner, Collection<Rating> ratings) {
		Collection<Rating> normed = new ArrayList<Rating>(ratings.size());
		double mean = computeMean(ratings);
		for (Rating r: ratings) {
			normed.add(new Rating(r.getUserId(), r.getItemId(), r.getRating() - mean, r.getTimestamp()));
		}
		return normed;
	}
}
