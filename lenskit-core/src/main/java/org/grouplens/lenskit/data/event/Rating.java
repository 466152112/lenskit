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
package org.grouplens.lenskit.data.event;

import javax.annotation.Nullable;

import org.grouplens.lenskit.data.pref.Preference;

/**
 * A rating is an expression of preference for an item by a user.
 *
 * <p>Like all events, ratings must be effectively immutable.  In the contexts
 * where a rating is allowed to be mutable, the preference object returned by
 * {@link #getPreference()} may be tied to the particular rating object and
 * mutate along with it.  Use {@link Preference#clone()} or {@link #clone()} to
 * obtain isolated references.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public interface Rating extends Event {
    /**
     * Get the expressed preference. If this is an "unrate" event, the
     * preference will be <tt>null</tt>.
     *
     * @return The expressed preference.
     */
    @Nullable Preference getPreference();

    /**
     * Get the rating's preference value.
     *
     * @return The value of the expressed preference (or {@link Double#NaN} if
     *         the preference is unset).
     * @deprecated Use {@link Preference#getValue()} on the output of
     *             {@link #getPreference()} instead.
     */
    @Deprecated
    double getRating();

    @Override
    Rating clone();
}
