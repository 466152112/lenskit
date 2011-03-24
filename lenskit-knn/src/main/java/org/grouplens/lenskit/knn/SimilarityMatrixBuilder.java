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
package org.grouplens.lenskit.knn;

/**
 * Interface for building similarity matrices for collaborative filtering.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public interface SimilarityMatrixBuilder {
    /**
     * Store the similarity between two entities. Calls to this method are
     * thread-safe with each other. Only one similary should be stored for each
     * ordered pair of entities, as the implementation may not de-duplicate the
     * matrix.  {@link #build()} cannot be called concurrently with this method.
     *
     * @param i1
     *            The index of the first entity.
     * @param i2
     *            The index of the second entity.
     * @param sim
     *            The similarity score.
     * @throws IndexOutOfBoundsException
     *             if an index is out of bounds (<0 or >{@link #size()}).
     */
    public void put(int i1, int i2, double sim);

    /**
     * Store a symmetric similarity.
     * @see #put(int, int, double)
     * @param i1 The index of the first entity
     * @param i2 The index of the second entity
     * @param sim The similarity score
     */
    void putSymmetric(int i1, int i2, double sim);

    /**
     * Finish building the matrix and return it.  The builder can no longer be
     * used after this.  This method cannot be called concurrently with
     * {@link #put(int, int, double)}.
     * @return The finalized similarity matrix.
     */
    public SimilarityMatrix build();

    /**
     * Query the number of rows in the underlying similarity matrix builder.
     * @return The number of valid rows (or -1 if this is unknown or unlimited).
     */
    public int size();
}
