/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2013 Regents of the University of Minnesota and contributors
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
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
package org.grouplens.lenskit.scored;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import org.grouplens.lenskit.symbols.Symbol;
import org.grouplens.lenskit.symbols.TypedSymbol;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;

import java.util.Set;

/**
 * Scored ID implementation backed by a sparse vector.
 *
 * @since 1.1
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class VectorEntryScoredId extends AbstractScoredId {

    private final SparseVector vector;
    private VectorEntry ent;

    /**
     * Construct a new vector entry scored ID.
     * @param v The vector whose entries will back this scored ID.
     */
    public VectorEntryScoredId(SparseVector v) {
        vector = v;
    }

    @Override
    public long getId() {
        return ent.getKey();
    }

    @Override
    public double getScore() {
        return ent.getValue();
    }

    @Override
    public Set<Symbol> getChannels() {
        ReferenceArraySet<Symbol> res = new ReferenceArraySet<Symbol>();
        for (Symbol s: vector.getChannels()) {
            // FIXME Make this O(1)
            if (vector.channel(s).containsKey(ent.getKey())) {
                res.add(s);
            }
        }
        return res;
    }

    @Override
    public Set<TypedSymbol<?>> getTypedChannels() {
        ReferenceArraySet<TypedSymbol<?>> res = new ReferenceArraySet<TypedSymbol<?>>();
        for (TypedSymbol<?> s: vector.getTypedChannels()) {
            // FIXME Make this O(1)
            if (vector.channel(s).containsKey(ent.getKey())) {
                res.add(s);
            }
        }
        return res;
    }

    @Override
    public double channel(Symbol s) {
        return vector.channel(s).get(ent);
    }

    @Override
    public <T> T channel(TypedSymbol<T> s) {
        return vector.channel(s).get(ent.getKey());
    }

    @Override
    public boolean hasChannel(Symbol s) {
        return vector.hasChannel(s) && vector.channel(s).containsKey(ent.getKey());
    }

    @Override
    public boolean hasChannel(TypedSymbol s) {
        return vector.hasChannel(s) && vector.channel(s).containsKey(ent.getKey());
    }

    /**
     * Set the entry backing this scored ID.
     * @param e The entry backing the scored ID.
     */
    public void setEntry(VectorEntry e) {
        Preconditions.checkArgument(e.getVector() == vector, "entry must be associated with vector");
        ent = e;
    }
}
