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

package org.grouplens.reflens.data.generic;

import java.util.Map;

/**
 * Interface for factories that build maps.  We use this to allow more efficient
 * map implementations to be introduced in certain places.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 * @param <K>
 * @param <V>
 */
public interface MapFactory<K, V> {
	public Map<K,V> create();
	public Map<K,V> copy(Map<K,V> map);
}
