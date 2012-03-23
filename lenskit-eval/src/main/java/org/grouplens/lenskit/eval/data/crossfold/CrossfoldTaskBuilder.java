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
package org.grouplens.lenskit.eval.data.crossfold;

import com.google.common.base.Function;
import org.apache.commons.lang3.builder.Builder;
import org.grouplens.lenskit.data.dao.DAOFactory;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.eval.AbstractEvalTaskBuilder;
import org.grouplens.lenskit.eval.config.BuilderFactory;
import org.grouplens.lenskit.eval.data.DataSource;
import org.kohsuke.MetaInfServices;

import javax.annotation.Nonnull;

/**
 * Builder for crossfold data sources (used to do cross-validation).
 * @author Michael Ekstrand
 * @since 0.10
 */
public class CrossfoldTaskBuilder extends AbstractEvalTaskBuilder implements Builder<CrossfoldTask> {
    private int folds = 5;
    private Order<Rating> order = new RandomOrder<Rating>();
    private PartitionAlgorithm<Rating> partition = new CountPartition<Rating>(10);
    private DataSource source;
    private String name;
    private Function<DAOFactory,DAOFactory> wrapper;
    private String fileName;

    public CrossfoldTaskBuilder() {
        super();
    }

    public CrossfoldTaskBuilder(String name) {
        this.name = name;
    }

    /**
     * Set the number of partitions to generate.
     * @param n The number of partitions to generate.
     * @return The builder (for chaining)
     */
    public CrossfoldTaskBuilder setPartitions(int n) {
        folds = n;
        return this;
    }
    
    public CrossfoldTaskBuilder setfileName(String name) {
        fileName = name;
        return this;
    }

    /**
     * Set the order for the train-test splitting. To split a user's ratings, the ratings are
     * first ordered by this order, and then partitioned.
     * @param o The sort order.
     * @return The builder (for chaining)
     * @see RandomOrder
     * @see TimestampOrder
     * @see #setHoldout(double)
     * @see #setHoldout(int)
     */
    public CrossfoldTaskBuilder setOrder(Order<Rating> o) {
        order = o;
        return this;
    }

    /**
     * Set holdout to a fixed number of items per user.
     * @param n The number of items to hold out from each user's profile.
     * @return The builder (for chaining)
     */
    public CrossfoldTaskBuilder setHoldout(int n) {
        partition = new CountPartition<Rating>(n);
        return this;
    }

    /**
     * Set holdout to a fraction of each user's profile.
     * @param f The fraction of a user's ratings to hold out.
     * @return The builder (for chaining)
     */
    public CrossfoldTaskBuilder setHoldout(double f) {
        partition = new FractionPartition<Rating>(f);
        return this;
    }

    /**
     * Set the input data source.
     * @param source The data source to use.
     * @return The builder (for chaining)
     */
    public CrossfoldTaskBuilder setSource(DataSource source) {
        this.source = source;
        return this;
    }
 

    /**
     * Specify a function that will wrap the DAO factories created by the crossfold. Used
     * to e.g. associate a text index with the split rating data. Note that the
     * Groovy configuration subsystem allows you to use a closure for the function,
     * so you can do this:
     * {@code
     * wrapper {
     *     return new WrappedDao(it)
     * }
     * }
     * @param f The function to wrap DAOs.
     * @return The builder (for chaining)
     */
    public CrossfoldTaskBuilder setWrapper(Function<DAOFactory,DAOFactory> f) {
        wrapper = f;
        return this;
    }

    public CrossfoldTask build() {
        CrossfoldTask task = new CrossfoldTask(name, dependencies, source, folds, new Holdout(order, partition),
                fileName, wrapper);
        return task;
    }

    @MetaInfServices
    public static class Factory implements BuilderFactory<CrossfoldTask> {
        @Override
        public String getName() {
            return "crossfold";
        }

        @Nonnull @Override
        public CrossfoldTaskBuilder newBuilder(String arg) {
            return new CrossfoldTaskBuilder(arg);
        }
    }
}
