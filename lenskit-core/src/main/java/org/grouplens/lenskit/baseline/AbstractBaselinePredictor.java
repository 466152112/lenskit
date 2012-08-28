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
package org.grouplens.lenskit.baseline;

import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;

import java.util.Collection;

/**
 * Abstract implementation of BaselinePredictor.
 *
 * @author Michael Ekstrand
 */
public abstract class AbstractBaselinePredictor implements BaselinePredictor {
    /**
     * {@inheritDoc}
     * <p>Implemented new-vector predict in terms of
     * {@link #predict(long, SparseVector, MutableSparseVector)}.
     */
    @Override
    @Deprecated
    public MutableSparseVector predict(long user, SparseVector ratings, Collection<Long> items) {
        MutableSparseVector v = new MutableSparseVector(items);
        predict(user, ratings, v);
        return v;
    }

    @Override
    public void predict(long user, SparseVector ratings, MutableSparseVector output) {
        predict(user, ratings, output, true);
    }
}
