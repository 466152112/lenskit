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
package org.grouplens.lenskit.eval.data.traintest;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import org.grouplens.lenskit.data.dao.DAOFactory;
import org.grouplens.lenskit.data.pref.PreferenceDomain;
import org.grouplens.lenskit.eval.AbstractEvalTask;
import org.grouplens.lenskit.eval.EvalTask;
import org.grouplens.lenskit.eval.data.DataSource;
import org.grouplens.lenskit.eval.data.GenericDataSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A train-test data set backed by a pair of factories.
 * 
 * @since 0.8
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 * 
 */
public class GenericTTDataSet extends AbstractEvalTask implements TTDataSet {
    private final @Nonnull DataSource trainData;
    private final @Nonnull DataSource testData;
    private final @Nullable PreferenceDomain preferenceDomain;
    
    public GenericTTDataSet(@Nonnull String name, Set<EvalTask> dependency, @Nonnull DataSource train, @Nonnull DataSource test,
                            @Nullable PreferenceDomain dom) {
        super(name, dependency);
        Preconditions.checkNotNull(train, "no training data");
        Preconditions.checkNotNull(test, "no test data");
        trainData = train;
        testData = test;
        preferenceDomain = dom;
    }

    /**
     * Create a new generic data set.
     * @param name The data set name.
     * @param train The training DAO factory.
     * @param test The test DAO factory.
     * @param domain The preference domain.
     */
    public GenericTTDataSet(@Nonnull String name, Set<EvalTask> dependency, @Nonnull DAOFactory train, @Nonnull DAOFactory test,
                            @Nullable PreferenceDomain domain) {
        super(name, dependency);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(train);
        Preconditions.checkNotNull(test);
        trainData = new GenericDataSource(name + ".train", null, train, domain);
        testData = new GenericDataSource(name + ".test", null, test, domain);
        preferenceDomain = domain;
    }

    @Override @Nonnull
    public String getName() {
        return name;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return Collections.<String,Object>singletonMap("DataSet", getName());
    }
    
    @Override
    public long lastUpdated() {
        return Math.max(trainData.lastUpdated(),
                        testData.lastUpdated());
    }                   
    
    public Void call() throws Exception{
        if(!dependency.isEmpty()) {
            for (EvalTask e : this.getDependency()) {
                e.call();
            }
        }
        return null;
    }

//    @Override
//    public void prepare(PreparationContext context) throws PreparationException {
//        context.prepare(trainData);
//        context.prepare(testData);
//    }


    @Override
    public void release() {
        /* no-op */
    }

    @Override @Nullable
    public PreferenceDomain getPreferenceDomain() {
        return preferenceDomain;
    }

    @Override
    public DAOFactory getTrainFactory() {
        return trainData.getDAOFactory();
    }

    @Override
    public DAOFactory getTestFactory() {
        return testData.getDAOFactory();
    }

    @Nonnull
    public DataSource getTestData() {
        return testData;
    }

    @Nonnull
    public DataSource getTrainData() {
        return trainData;
    }
    
    @Override
    public String toString() {
        return String.format("{TTDataSet %s}", name);
    }
}
