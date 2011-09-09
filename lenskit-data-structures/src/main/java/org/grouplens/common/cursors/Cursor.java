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
package org.grouplens.common.cursors;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;

/**
 * Cursors over data connections.  These are basically closable iterators which
 * also implement {@link Iterable} for convenience.  Cursors are not allowed to
 * contain null elements.
 * 
 * <p>
 * Note that the {@link #iterator()} method does <b>not</b> return a fresh
 * iterator but rather a wrapper of this cursor; it is only present to allow
 * for-each loops over cursors.  After it is exhausted, any iterator returned
 * will be null.
 * 
 * <p>
 * This class does not implement {@link Iterator} because the 'is-a' relationship
 * does not hold; cursors must be closed by their clients while iterators do
 * not have such a requirement.
 * 
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 * @param <T> The type of data returned by the cursor
 */
public interface Cursor<T> extends Iterable<T>, Closeable {
	/**
	 * Get an upper bound on the number of rows available from the cursor.
	 * @return the maximum number of rows which may be returned by {@link #next()},
	 * or -1 if that count is not available.
	 */
	int getRowCount();

	/**
	 * Query whether the cursor has any more items.  If the cursor or underlying
	 * source has been closed, this may return even if the end has not been
	 * reached.
	 * @return <tt>true</tt> if there remains another item to fetch.
	 */
	boolean hasNext();
	
	/**
	 * Fetch the next item from the cursor.
	 * @return The next item in the cursor.
	 * @throws NoSuchElementException if there are no more elements.
	 * @throws RuntimeException if the cursor or its data source have been
	 * closed (optional).
	 */
	@Nonnull T next();
	
	/**
     * Variant of {@link #next()} that may mutate and return the same object
     * avoid excess allocations. Since many loops don't do anything with the
     * object after the iteration in which it is retrieved, using this iteration
     * method can improve both speed and memory use.
     * @see Cursor#next()  
     */
	@Nonnull T fastNext();
	
	/**
	 * Convert the cursor to an iterable whose {@link Iterator#next()} method is
	 * implemented in terms of {@link #fastNext()}.
	 * @return An iterable for fast iteration.
	 */
	Iterable<T> fast();
	
	/**
	 * Close the cursor.  This invalidates the cursor; no more elements may be
	 * fetched after a call to <tt>close()</tt> (although implementations are
	 * not required to enforce this).  It is not an error to close a cursor
	 * multiple times.
	 */
	@Override
    void close();
}
