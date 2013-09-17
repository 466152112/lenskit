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
package org.grouplens.lenskit.eval.algorithm;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.builder.Builder;
import org.grouplens.lenskit.core.LenskitConfiguration;
import org.grouplens.lenskit.eval.script.ConfigDelegate;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to get a algorithm instances.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
@ConfigDelegate(AlgorithmInstanceBuilderDelegate.class)
public class LenskitAlgorithmInstanceBuilder implements Builder<LenskitAlgorithmInstance> {
    private final LenskitConfiguration config;
    private String name;
    private Map<String, Object> attributes = new HashMap<String, Object>();
    private boolean preload;

    public LenskitAlgorithmInstanceBuilder() {
        this("Unnamed Algorithm");
    }

    public LenskitAlgorithmInstanceBuilder(String name) {
        this.name = name;
        config = new LenskitConfiguration();
    }

    /**
     * Set the algorithm name.
     *
     * @param n The name for this algorithm instance.
     * @return The command for chaining.
     */
    public LenskitAlgorithmInstanceBuilder setName(String n) {
        name = n;
        return this;
    }

    /**
     * Get the algorithm name.
     *
     * @return The algortihm name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get whether this algorithm will require ratings to be pre-loaded.
     *
     * @return {@code true} if the algorithm should have ratings pre-loaded into memory.
     */
    public Boolean getPreload() {
        return preload;
    }

    /**
     * Set whether the algorithm wants ratings pre-loaded. Use this for algorithms that
     * are too slow reading on a CSV file if you have enough memory to load them all.
     *
     * @param pl {@code true} to pre-load input data when running this algorithm.
     * @return The command for chaining.
     */
    public LenskitAlgorithmInstanceBuilder setPreload(boolean pl) {
        preload = pl;
        return this;
    }

    /**
     * Set an attribute for this algorithm instance. Used for distinguishing similar
     * instances in an algorithm family.
     *
     * @param attr  The attribute name.
     * @param value The attribute value.
     * @return The command for chaining.
     */
    public LenskitAlgorithmInstanceBuilder setAttribute(@Nonnull String attr, @Nonnull Object value) {
        Preconditions.checkNotNull(attr, "attribute names cannot be null");
        Preconditions.checkNotNull(value, "attribute values cannot be null");
        attributes.put(attr, value);
        return this;
    }

    /**
     * Get the attributes of this algorithm instance.
     *
     * @return A map of user-defined attributes for this algorithm instance.
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public LenskitConfiguration getConfig() {
        return config;
    }

    @Override
    public LenskitAlgorithmInstance build() {
        return new LenskitAlgorithmInstance(getName(), config, attributes, preload);
    }

}
