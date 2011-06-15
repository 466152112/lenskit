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
package org.grouplens.lenskit;

import org.grouplens.lenskit.data.dao.RatingDataAccessObject;
import org.picocontainer.PicoContainer;

/**
 * Recommender implementation built on LensKit containers.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class LenskitRecommender implements Recommender {
    private final PicoContainer container;
    private final RatingDataAccessObject dao;
    private final boolean shouldCloseDao;
    
    // An alternate to this LenskitRecommender where it asks for the components as needed
    // is to see if there is an actual Recommender that can be built from the container
    // and then delegate to that.  The wrapper recommender would still handle the closing
    // logic, this would give us a single configuration point if people chose to use it.
    public LenskitRecommender(PicoContainer container, RatingDataAccessObject dao, boolean shouldCloseDao) {
        this.container = container;
        this.dao = dao;
        this.shouldCloseDao = shouldCloseDao;
    }
    
    /**
     * Get a particular component from the recommender session.  Generally
     * you want to use one of the type-specific getters; this method only exists
     * for specialized applications which need deep access to the recommender
     * components.
     * @param <T>
     * @param cls
     * @return
     */
    public <T> T getComponent(Class<T> cls) {
        return container.getComponent(cls);
    }

    @Override
    public RatingPredictor getRatingPredictor() {
        return container.getComponent(RatingPredictor.class);
    }

    @Override
    public DynamicRatingPredictor getDynamicRatingPredictor() {
        return container.getComponent(DynamicRatingPredictor.class);
    }

    @Override
    public DynamicRatingItemRecommender getDynamicItemRecommender() {
        return container.getComponent(DynamicRatingItemRecommender.class);
    }

    @Override
    public BasketRecommender getBasketRecommender() {
        return container.getComponent(BasketRecommender.class);
    }

    @Override
    public void close() {
        if (shouldCloseDao)
            dao.close();
    }

    @Override
    public RatingDataAccessObject getRatingDataAccessObject() {
        return dao;
    }

	@Override
	public ItemRecommender getItemRecommender() {
		return container.getComponent(ItemRecommender.class);
	}
}