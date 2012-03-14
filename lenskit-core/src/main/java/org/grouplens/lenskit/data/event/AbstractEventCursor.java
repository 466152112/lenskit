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
package org.grouplens.lenskit.data.event;

import org.grouplens.lenskit.cursors.AbstractPollingCursor;
import org.grouplens.lenskit.data.Event;

/**
 * Polling-based cursor implementation for events with fast poll methods. This class
 * assumes that {@link #poll()} reuses objects, and overrides {@link #copy(E)} to copy
 * them using {@link Event#clone()}.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public abstract class AbstractEventCursor<E extends Event> extends AbstractPollingCursor<E> {
    public AbstractEventCursor() {
        super();
    }

    public AbstractEventCursor(int rowCount) {
        super(rowCount);
    }

    /**
     * Copy an event using {@link Event#clone()}.
     * @param event The event to copy.
     * @return A copy of {@code event}.
     */
    @SuppressWarnings("unchecked")
    protected E copy(E event) {
        return (E) event.clone();
    }
}
