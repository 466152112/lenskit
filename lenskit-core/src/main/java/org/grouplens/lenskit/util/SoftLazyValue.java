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
package org.grouplens.lenskit.util;

import com.google.common.base.Supplier;

import javax.annotation.Nonnull;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.concurrent.Callable;

/**
 * A thread-safe lazy value class using soft references. Like {@link LazyValue},
 * but it recomputes its value if it is garbage collected.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SoftLazyValue<T> implements Supplier<T> {
    private volatile Reference<T> value;
    private Supplier<T> supplier;

    /**
     * Create a lazy value whose value will be provided by a callable.
     *
     * @param f The callable responsible for providing the lazy value. The
     *          callable's {@link Callable#call()} method cannot return
     *          {@code null}.
     */
    public SoftLazyValue(@Nonnull Supplier<T> f) {
        supplier = f;
    }

    /**
     * Get the value, computing it if necessary.
     *
     * @return The value returned by the callable.
     */
    @Override
    public synchronized T get() {
        T val = (value == null) ? null : value.get();
        if (val == null) {
            val = supplier.get();
            value = new SoftReference<T>(val);
        }

        return val;
    }
}
