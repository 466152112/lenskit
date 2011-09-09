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
package org.grouplens.lenskit.cursors;

import it.unimi.dsi.fastutil.longs.LongIterator;

class LongCursorIterator implements LongIterator {
    private final LongCursor cursor;

    public LongCursorIterator(LongCursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public long nextLong() {
        return cursor.nextLong();
    }

    @Override
    public int skip(int n) {
        int i = 0;
        while (i < n && cursor.hasNext()) {
            nextLong();
            i++;
        }
        return i;
    }

    @Override
    public boolean hasNext() {
        return cursor.hasNext();
    }

    @Override
    public Long next() {
        return cursor.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
