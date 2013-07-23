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
package org.grouplens.lenskit.data.dao;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.data.event.Event;

import javax.inject.Inject;

/**
 * Item DAO that streams the events to get item information.  The item set is fetched and memorized
 * once for each instance of this class.
 *
 * @since 2.0
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public final class PrefetchingItemDAO implements ItemDAO {
    private final EventDAO eventDAO;
    private transient volatile LongSet items;

    @Inject
    public PrefetchingItemDAO(EventDAO events) {
        eventDAO = events;
    }

    @Override
    public LongSet getItemIds() {
        if (items == null) {
            synchronized (this) {
                if (items == null) {
                    LongSet us = new LongOpenHashSet();
                    Cursor<Event> events = eventDAO.streamEvents();
                    try {
                        for (Event e: events) {
                            us.add(e.getItemId());
                        }
                    } finally {
                        events.close();
                    }
                    items = us;
                }
            }
        }

        return items;
    }
}
