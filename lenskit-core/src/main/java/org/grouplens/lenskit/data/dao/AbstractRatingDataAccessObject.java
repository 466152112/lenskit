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
/**
 *
 */
package org.grouplens.lenskit.data.dao;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.grouplens.common.cursors.AbstractCursor;
import org.grouplens.common.cursors.Cursor;
import org.grouplens.common.cursors.Cursors;
import org.grouplens.lenskit.data.BasicUserRatingProfile;
import org.grouplens.lenskit.data.Cursors2;
import org.grouplens.lenskit.data.LongCursor;
import org.grouplens.lenskit.data.Rating;
import org.grouplens.lenskit.data.SortOrder;
import org.grouplens.lenskit.data.UserRatingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

/**
 * Abstract implementation of {@link RatingDataAccessObject}, delegating
 * to a few core methods.  It also handles thread-local session management,
 * requiring subclasses only to provide the {@link #openNewSession()} method.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 * @param <S> The type of session objects.
 */
public abstract class AbstractRatingDataAccessObject<S extends Closeable> implements RatingDataAccessObject {
    protected final Logger logger;
    
    protected AbstractRatingDataAccessObject() {
        logger = LoggerFactory.getLogger(getClass());
    }
    
    /**
     * Implement {@link RatingDataAccessObject#getRatings(SortOrder)} by sorting the
     * output of {@link #getRatings()}.
     */
    @Override
    public Cursor<Rating> getRatings(SortOrder order) {
        checkSession();
        
        Comparator<Rating> comp = null;

        switch (order) {
        case ANY:
            return getRatings();
        case TIMESTAMP:
            comp = new Comparator<Rating>() {
                public int compare(Rating r1, Rating r2) {
                    long ts1 = r1.getTimestamp();
                    long ts2 = r2.getTimestamp();
                    if (ts1 > ts2)
                        return 1;
                    else if (ts1 < ts2)
                        return -1;
                    else
                        return 0;
                }
            };
            break;
        case USER:
            comp = new Comparator<Rating>() {
                public int compare(Rating r1, Rating r2) {
                    long u1 = r1.getUserId();
                    long u2 = r2.getUserId();
                    if (u1 > u2)
                        return 1;
                    else if (u1 < u2)
                        return -1;
                    else
                        return 0;
                }
            };
            break;
        case ITEM:
            comp = new Comparator<Rating>() {
                public int compare(Rating r1, Rating r2) {
                    long i1 = r1.getItemId();
                    long i2 = r2.getItemId();
                    if (i1 > i2)
                        return 1;
                    else if (i1 < i2)
                        return -1;
                    else
                        return 0;
                }
            };
            break;
        default:
            assert false;
        }

        ArrayList<Rating> ratings = Cursors.makeList(getRatings());
        Collections.sort(ratings, comp);
        return Cursors.wrap(ratings.iterator());
    }

    /**
     * Implement {@link RatingDataAccessObject#getUserRatingProfiles()} by
     * processing the output of {@link #getRatings(SortOrder)} sorted by user ID.
     */
    @Override
    public Cursor<UserRatingProfile> getUserRatingProfiles() {
        return new UserProfileCursor(getRatings(SortOrder.USER));
    }

    /**
     * Implement {@link RatingDataAccessObject#getUserRatings(long)} by delegating to
     * {@link #getUserRatings(long, SortOrder)}.
     */
    @Override
    public Cursor<Rating> getUserRatings(long userId) {
        return getUserRatings(userId, SortOrder.ANY);
    }

    /**
     * Implement {@link RatingDataAccessObject#getUserRatings(long, SortOrder)} by
     * filtering the output of {@link #getRatings(SortOrder)}.
     */
    @Override
    public Cursor<Rating> getUserRatings(final long userId, SortOrder order) {
        Cursor<Rating> base = getRatings(order);
        return Cursors.filter(base, new Predicate<Rating>() {
            public boolean apply(Rating r) {
                return r.getUserId() == userId;
            }
        });
    }
    
    /**
     * Implement {@link RatingDataAccessObject#getItemRatings(long)} by delegating
     * to {@link #getItemRatings(long, SortOrder)}.
     */
    @Override
    public Cursor<Rating> getItemRatings(long itemId) {
        return getItemRatings(itemId, SortOrder.ANY);
    }
    
    /**
     * Implement {@link RatingDataAccessObject#getItemRatings(long, SortOrder)}
     * by filtering the output of {@link #getRatings(SortOrder)}.
     */
    @Override
    public Cursor<Rating> getItemRatings(final long itemId, SortOrder order) {
        return Cursors.filter(getRatings(order), new Predicate<Rating>() {
            public boolean apply(Rating r) {
                return r.getItemId() == itemId;
            }
        });
    }

    private LongSet getItemSet() {
        LongSet items = null;
        
        Cursor<Rating> ratings = getRatings();
        try {
            items = new LongOpenHashSet();
            for (Rating r: ratings) {
                items.add(r.getItemId());
            }
        } finally {
            ratings.close();
        }

        return items;
    }

    /**
     * Implement {@link RatingDataAccessObject#getItems()} by processing the output
     * of {@link #getRatings()}.
     */
    @Override
    public LongCursor getItems() {
        return Cursors2.wrap(getItemSet());
    }

    @Override
    public int getItemCount() {
        return getItemSet().size();
    }

    private LongSet getUserSet() {
        LongSet users;
        
        Cursor<Rating> ratings = getRatings();
        try {
            users = new LongOpenHashSet();
            for (Rating r: ratings) {
                users.add(r.getUserId());
            }
        } finally {
            ratings.close();
        }

        return users;
    }

    /* (non-Javadoc)
     * @see org.grouplens.lenskit.data.dao.DataSource#getUsers()
     */
    @Override
    public LongCursor getUsers() {
        return Cursors2.wrap(getUserSet());
    }

    @Override
    public int getUserCount() {
        return getUserSet().size();
    }

    static class UserProfileCursor extends AbstractCursor<UserRatingProfile> {
        private Cursor<Rating> cursor;
        private Rating lastRating;

        public UserProfileCursor(Cursor<Rating> cursor) {
            this.cursor = cursor;
            lastRating = null;
        }

        @Override
        public void close() {
            if (cursor != null)
                cursor.close();
            cursor = null;
            lastRating = null;
        }

        @Override
        public boolean hasNext() {
            return cursor != null && (lastRating != null || cursor.hasNext());
        }

        @Override
        public UserRatingProfile next() {
            if (cursor == null) throw new NoSuchElementException();
            long uid;
            List<Rating> ratings = new ArrayList<Rating>();
            if (lastRating == null)
                lastRating = cursor.next();
            uid = lastRating.getUserId();
            do {
                ratings.add(lastRating);
                if (cursor.hasNext())
                    lastRating = cursor.next();
                else
                    lastRating = null;
            } while (lastRating != null && lastRating.getUserId() == uid);

            return new BasicUserRatingProfile(uid, ratings);
        }
    }
    
    private ThreadLocal<S> activeSession = new ThreadLocal<S>();
    
    /**
     * Verify that no session is already open.
     */
    @Override
    public void openSession() {
        if (activeSession.get() != null)
            throw new IllegalStateException("Session already open");
        else
            activeSession.set(openNewSession());
    }
    
    /**
     * Explicitly open a new session object (for additional session-opening
     * methods).
     * @param session
     */
    protected void openSession(S session) {
        if (isSessionOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing new session for already-open thread", e);
            }
            throw new IllegalStateException("Session already open");
        }
        activeSession.set(session);
    }
    
    /**
     * Close the session.
     * @review Is logging and swallowing errors the appropriate approach?
     */
    @Override
    public void closeSession() {
        Closeable session = activeSession.get();
        if (session == null) {
            throw new NoSessionException();
        } else {
            activeSession.set(null);
            try {
                session.close();
            } catch (IOException e) {
                logger.error("Error closing session: " + e.getMessage(), e);
            }
        }
    }
    
    @Override
    public boolean isSessionOpen() {
        return activeSession.get() != null;
    }
    
    /**
     * Get the session object.
     * @throws NoSessionException if there is not an open session.
     */
    protected @Nonnull S getSession() {
        return getSession(true);
    }
    
    /**
     * Require an open session.
     * @throws NoSessionException if there is not an open session.
     */
    protected void checkSession() {
        getSession();
    }
    
    /**
     * Get the session object without failing.
     * @param require If <tt>true</tt>, fail if no session is available.
     * @throws NoSessionException if no session is available and <var>require</var>
     * is <tt>true</tt>.
     */
    protected @Nullable S getSession(boolean require) {
        S session = activeSession.get();
        if (session == null && require)
            throw new NoSessionException();
        else
            return session;
    }
    
    /**
     * Open and return a new session object.  Errors closing the returned object
     * are logged and swallowed.
     * @return
     */
    protected abstract S openNewSession();
    
    /**
     * Dummy session implementation for DAOs without sessions.
     * @author Michael Ekstrand <ekstrand@cs.umn.edu>
     *
     */
    protected static class DummySession implements Closeable {
        public void close() {
            /* no-op */
        }
    }
}
