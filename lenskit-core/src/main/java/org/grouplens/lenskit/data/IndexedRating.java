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
package org.grouplens.lenskit.data;

import org.grouplens.lenskit.data.context.BuildContext;

/**
 * Rating that also knows the indexes for its user and item.
 * @see BuildContext
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public interface IndexedRating extends Rating {
	/**
	 * Return the index for the user.  Indexes are 0-based and consecutive, so
	 * they can be used for indexing into arrays.
	 * @return The user index.
	 */
	int getUserIndex();
	
	/**
	 * Return the index for the item.  Indexes are 0-based and consecutive, so
	 * they can be used for indexing into arrays.
	 * @return The item index.
	 */
	int getItemIndex();
}
