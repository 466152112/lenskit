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

package org.grouplens.reflens;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.grouplens.reflens.data.ScoredId;
import org.grouplens.reflens.data.vector.SparseVector;

/**
 * Recommender recommending items from ratings-based user profiles.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public interface RatingRecommender {
	/**
	 * Recommend all posible items for a user.
	 * @param user The user ID.
	 * @param ratings The user's rating vector.
	 * @return The sorted list of scored items.
	 * @see #recommend(long, SparseVector, int, Set)
	 */
	public List<ScoredId> recommend(long user, SparseVector ratings);
	
	/**
	 * Recommend all possible items for a user from a set of candidates.
	 * @param user The user ID.
	 * @param ratings The user's rating vector.
	 * @param candidates The candidate set (can be null to represent the
	 * universe).
	 * @return The sorted list of scored items.
	 * @see #recommend(long, SparseVector, int, Set)
	 */
	public List<ScoredId> recommend(long user, SparseVector ratings,
			@Nullable Set<Long> candidates);
	
	/**
	 * Produce a set of recommendations for the user.
	 * @param user The user's ID
	 * @param ratings The user's ratings
	 * @param n The number of ratings to return.  If negative, recommend all
	 * possible items.
	 * @param candidates A set of candidate items which can be recommended.  If
	 * <tt>null</tt>, the candidate set is considered to contain the universe.
	 * @return a list of scored recommendations, sorted in nondecreasing order
	 * of score.
	 */
	public List<ScoredId> recommend(long user, SparseVector ratings, int n,
			@Nullable Set<Long> candidates);
}