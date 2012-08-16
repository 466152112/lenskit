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
package org.grouplens.lenskit.data.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.grouplens.lenskit.data.AbstractUserHistory;
import org.grouplens.lenskit.data.Event;
import org.grouplens.lenskit.data.UserHistory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Basic user rating profile backed by a collection of ratings. The event list
 * aspects of the profile are implemented as a read-only delegation to an
 * underlying list.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 * @param <E> The type of event in this history.
 */
public class BasicUserHistory<E extends Event> extends AbstractUserHistory<E> implements UserHistory<E> {
    private long user;
    private List<E> events;

    /**
     * Construct a new basic user profile.
     *
     * @param user The user ID.
     * @param events The list of events in this user's history. All events must
     *            be for the user.
     */
    public BasicUserHistory(long user, List<E> events) {
        this.user = user;
        this.events = events;
    }

    @Override
    public long getUserId() {
        return user;
    }

    @Override
    public E get(int i) {
        return events.get(i);
    }

    @Override
    public int size() {
        return events.size();
    }

    @Override
    public Iterator<E> iterator() {
        return events.iterator();
    }

    @Override
    public List<E> subList(int from, int to) {
        return Collections.unmodifiableList(events.subList(from, to));
    }

    @Override
    public Object[] toArray() {
        return events.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return events.toArray(a);
    }

    /**
     * {@inheritDoc}
     * <p>This implementation filters into a new {@link BasicUserHistory} backed
     * by an {@link ArrayList}.
     */
    @Override
    public <T extends Event> UserHistory<T> filter(Class<T> type) {
        List<T> events = Lists.newArrayList(Iterables.filter(this, type));
        return new BasicUserHistory<T>(getUserId(), events);
    }

    /**
     * {@inheritDoc}
     * <p>This implementation filters into a new {@link BasicUserHistory} backed
     * by an {@link ArrayList}.
     */
    @Override
    public UserHistory<E> filter(Predicate<? super E> pred) {
        List<E> events = Lists.newArrayList(Iterables.filter(this, pred));
        return new BasicUserHistory<E>(getUserId(), events);
    }
}
