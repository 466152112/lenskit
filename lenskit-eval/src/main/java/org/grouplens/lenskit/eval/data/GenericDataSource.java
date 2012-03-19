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
package org.grouplens.lenskit.eval.data;

import org.grouplens.lenskit.data.dao.DAOFactory;
import org.grouplens.lenskit.data.pref.PreferenceDomain;
import org.grouplens.lenskit.eval.AbstractEvalTask;
import org.grouplens.lenskit.eval.EvalTask;
import org.grouplens.lenskit.eval.PreparationContext;
import org.grouplens.lenskit.eval.PreparationException;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Ekstrand
 */
public class GenericDataSource extends AbstractEvalTask implements DataSource {
    private DAOFactory daoFactory;
    private PreferenceDomain domain;

    public GenericDataSource(String name, DAOFactory factory) {
        this(name, null, factory, null);
    }

    public GenericDataSource(String name, Set<EvalTask> dependency, DAOFactory factory, PreferenceDomain dom) {
        super(name,dependency);
        daoFactory = factory;
        domain = dom;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PreferenceDomain getPreferenceDomain() {
        return domain;
    }

    @Override
    public DAOFactory getDAOFactory() {
        return daoFactory;
    }

    @Override
    public long lastUpdated() {
        return 0;
    }

    @Override
    public Void call() {
        /* no-op */
        return null;
    }
//
//    @Override
//    public void prepare(PreparationContext context) throws PreparationException {
//        /* no-op */
//    }
}
