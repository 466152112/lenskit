/*
 * LensKit, an open source recommender systems toolkit.
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
package org.grouplens.lenskit.util;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * Additional function utilities to go with {@link Functions}.
 * 
 * @since 0.9
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 */
public class MoreFunctions {
    
    /**
     * Identity function casting its arguments to a particular type.
     * 
     * @param target The target type for arguments.
     * @return A function which, when applied to an object, casts it to type
     *         <var>target</var>.
     */
    public static <F,T> Function<F,T> cast(final Class<T> target) {
        return new Function<F,T>() {
            @Override
            public T apply(F obj) {
                return target.cast(obj);
            }
        };
    }
}
