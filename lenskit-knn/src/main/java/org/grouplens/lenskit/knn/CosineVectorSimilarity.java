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
package org.grouplens.lenskit.knn;

import java.io.Serializable;

import javax.inject.Inject;

import org.grouplens.lenskit.core.Shareable;
import org.grouplens.lenskit.params.Damping;
import org.grouplens.lenskit.vectors.SparseVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Similarity function using cosine similarity.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
@Shareable
public class CosineVectorSimilarity implements VectorSimilarity, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(CosineVectorSimilarity.class);

    private final double dampingFactor;

    public CosineVectorSimilarity() {
        this(0.0);
    }

    @Inject
    public CosineVectorSimilarity(@Damping double dampingFactor) {
        this.dampingFactor = dampingFactor;
        logger.debug("Using smoothing factor {}", dampingFactor);
    }

    /* (non-Javadoc)
     * @see org.grouplens.lenskit.Similarity#similarity(java.lang.Object, java.lang.Object)
     */
    @Override
    public double similarity(SparseVector vec1, SparseVector vec2) {
        final double dot = vec1.dot(vec2);
        final double denom = vec1.norm() * vec2.norm() + dampingFactor;
        if (denom == 0)
            return 0;

        return dot / denom;
    }

    @Override
    public boolean isSparse() {
        return true;
    }

    @Override
    public boolean isSymmetric() {
        return true;
    }
}
