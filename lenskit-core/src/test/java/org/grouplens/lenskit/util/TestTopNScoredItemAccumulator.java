/*
 * LensKit, an open source recommender systems toolkit.
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
package org.grouplens.lenskit.util;

import org.grouplens.lenskit.collections.ScoredLongList;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author Michael Ekstrand
 */
public class TestTopNScoredItemAccumulator {
    ScoredItemAccumulator accum;

    @Before
    public void createAccumulator() {
        accum = new TopNScoredItemAccumulator(3);
    }

    @Test
    public void testEmpty() {
        ScoredLongList out = accum.finish();
        assertThat(out, Matchers.<Long>empty());
    }

    @Test
    public void testAccum() {
        accum.put(5, 4.2);
        accum.put(3, 2.9);
        accum.put(2, 9.8);
        ScoredLongList out = accum.finish();
        assertThat(out, hasSize(3));
        assertThat(out.get(0), equalTo(2L));
        assertThat(out.getScore(0), equalTo(9.8));
        assertThat(out.get(1), equalTo(5L));
        assertThat(out.getScore(1), equalTo(4.2));
        assertThat(out.get(2), equalTo(3L));
        assertThat(out.getScore(2), equalTo(2.9));
    }

    @Test
    public void testAccumLimit() {
        accum.put(7, 1.0);
        accum.put(5, 4.2);
        accum.put(3, 2.9);
        accum.put(2, 9.8);
        accum.put(8, 2.1);
        ScoredLongList out = accum.finish();
        assertThat(out, hasSize(3));
        assertThat(out.get(0), equalTo(2L));
        assertThat(out.getScore(0), equalTo(9.8));
        assertThat(out.get(1), equalTo(5L));
        assertThat(out.getScore(1), equalTo(4.2));
        assertThat(out.get(2), equalTo(3L));
        assertThat(out.getScore(2), equalTo(2.9));
    }
}
