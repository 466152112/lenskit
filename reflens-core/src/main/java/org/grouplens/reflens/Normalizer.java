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

package org.grouplens.reflens;

/**
 * Interface for normalization functions.  The normalizer takes an "owner" and
 * an object (such as a user and their rating vector) and returns the normalized
 * version.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 * @param <S> The type of owner objects.
 * @param <V> The type of to-be-normalized values.
 * 
 * TODO enhance this to support reversible normalizations (see issue #23).
 */
public interface Normalizer<S,V> {
	public V normalize(S owner, V src);
}
