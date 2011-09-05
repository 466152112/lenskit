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
package org.grouplens.lenskit.data.history;

import org.grouplens.lenskit.data.event.Event;
import org.grouplens.lenskit.data.vector.UserVector;

/**
 * Summarize user histories as real-valued vectors.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public interface HistorySummarizer {
    /**
     * Get the supertype of all events required by this summarizer.
     *
     * @return A type such that any type used by this summarizer is a subtype of
     *         it.
     */
    Class<? extends Event> eventTypeWanted();

    /**
     * Compute a vector summary of a user's history.
     *
     * @param history The history to summarize.
     * @return A vector summarizing the user's history.
     */
    UserVector summarize(UserHistory<? extends Event> history);
}
