/*
 * RefLens, a reference implementation of recommender algorithms.
 * Copyright 2010 Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but
 * you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

/**
 * 
 */
package org.grouplens.reflens.baseline;

import it.unimi.dsi.fastutil.longs.LongSortedSet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;

import org.grouplens.reflens.RatingPredictor;
import org.grouplens.reflens.RatingPredictorBuilder;
import org.grouplens.reflens.data.RatingDataSource;
import org.grouplens.reflens.data.RatingVector;
import org.grouplens.reflens.data.ScoredId;
import org.grouplens.reflens.params.meta.Parameter;
import org.grouplens.reflens.util.CollectionUtils;

import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;

/**
 * Rating predictor that predicts a constant rating for all items.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class ConstantPredictor implements RatingPredictor {
	private final double value;
	
	/**
	 * Annotation for value parameters to the recommender.
	 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
	 *
	 */
	@BindingAnnotation
	@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@Parameter
	public static @interface Value {
		public static final double DEFAULT_VALUE = 0;
	}
	
	/**
	 * Construct a rating vector with the same rating for all items.
	 * @param items The items to include in the vector.
	 * @param value The rating/prediction to give.
	 * @return A rating vector mapping all items in <var>items</var> to
	 * <var>value</var>.
	 */
	public static RatingVector constantPredictions(Collection<Long> items, double value) {
		long[] keys = CollectionUtils.fastCollection(items).toLongArray();
		if (!(items instanceof LongSortedSet))
			Arrays.sort(keys);
		double[] preds = new double[keys.length];
		Arrays.fill(preds, value);
		return RatingVector.wrap(keys, preds);
	}
	
	/**
	 * Construct a new constant predictor.  This is exposed so other code
	 * can use it as a fallback.
	 * @param value
	 */
	public ConstantPredictor(double value) {
		this.value = value;
	}
	
	@Override
	public RatingVector predict(long user, RatingVector ratings, Collection<Long> items) {
		return constantPredictions(items, value);
	}

	/* (non-Javadoc)
	 * @see org.grouplens.reflens.RatingPredictor#predict(java.lang.Object, java.util.Map, java.lang.Object)
	 */
	@Override
	public ScoredId predict(long user, RatingVector profile, long item) {
		return new ScoredId(item, value);
	}

	/**
	 * Predictor builder for a constant rating predictor.
	 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
	 *
	 */
	public static class Builder implements RatingPredictorBuilder {
		private final double value;
		@Inject
		public Builder(@Value double value) {
			this.value = value;
		}
		@Override
		public RatingPredictor build(RatingDataSource data) {
			return new ConstantPredictor(value);
		}
		
		/**
		 * Return the value which will be returned for predictions.
		 * @return
		 */
		public double getValue() {
			return value;
		}
	}
}
