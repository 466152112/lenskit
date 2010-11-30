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

package org.grouplens.reflens.knn;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;

import java.lang.reflect.Type;
import java.util.Properties;

import javax.annotation.Nullable;

import org.grouplens.reflens.RatingPredictorBuilder;
import org.grouplens.reflens.RecommenderBuilder;
import org.grouplens.reflens.knn.params.BaselinePredictor;
import org.grouplens.reflens.knn.params.ItemSimilarity;
import org.grouplens.reflens.knn.params.NeighborhoodSize;
import org.grouplens.reflens.knn.params.SimilarityDamper;
import org.grouplens.reflens.knn.params.ThreadCount;
import org.grouplens.reflens.util.ObjectLoader;
import org.grouplens.reflens.util.SimilarityMatrixBuilderFactory;
import org.grouplens.reflens.util.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.util.Providers;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class ItemRecommenderModule extends AbstractModule {
	
	private static final Logger logger = LoggerFactory.getLogger(ItemRecommenderModule.class);
	
	private Properties properties;
	
	public ItemRecommenderModule() {
		this(System.getProperties());
	}

	public ItemRecommenderModule(Properties props) {
		this.properties = props;
	}

	/* (non-Javadoc)
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
		configureThreadCount();
		
		configureSimilarityMatrix();
		
		configureBaseline();
		
		configureParameters();

		configureItemSimilarity();
		configureRecommenderBuilder();
	}
	
	protected void configureThreadCount() {
		int count = Runtime.getRuntime().availableProcessors();
		String cfg = properties.getProperty(ThreadCount.PROPERTY_NAME);
		try {
			if (cfg != null)
				count = Integer.parseInt(cfg, 10);
		} catch (NumberFormatException e) {
			logger.warn("Invalid integer {}", cfg);
		}
		bind(int.class).annotatedWith(ThreadCount.class).toInstance(count);
	}
	
	protected void configureParameters() {
		configureNeighborhoodSize();
		configureSimilarityDamper();
	}

	/**
	 * 
	 */
	protected void configureRecommenderBuilder() {
		bind(new TypeLiteral<RecommenderBuilder>(){}).to(
				new TypeLiteral<ItemItemRecommenderBuilder>(){});
	}

	/**
	 * 
	 */
	protected void configureSimilarityMatrix() {
		bind(SimilarityMatrixBuilderFactory.class).toProvider(
				FactoryProvider.newFactory(SimilarityMatrixBuilderFactory.class,
						TruncatingSimilarityMatrixBuilder.class));
	}

	/**
	 * 
	 */
	protected void configureNeighborhoodSize() {
		bind(int.class).annotatedWith(NeighborhoodSize.class).toInstance(
				Integer.parseInt(properties.getProperty(NeighborhoodSize.PROPERTY_NAME, "100"), 10));
	}
	
	protected void configureSimilarityDamper() {
		bind(double.class).annotatedWith(SimilarityDamper.class).toInstance(
				Double.parseDouble(properties.getProperty(SimilarityDamper.PROPERTY_NAME, "100")));
	}

	@SuppressWarnings("unchecked")
	protected void configureBaseline() {
		String rnorm = properties.getProperty(BaselinePredictor.PROPERTY_NAME);
		Class target = null;
		if (rnorm != null) {
			try {
				target = ObjectLoader.getClass(rnorm);
			} catch (ClassNotFoundException e) {
				logger.error("Class {} not found", rnorm);
			}
		}
		if (target != null) {
			if (!RatingPredictorBuilder.class.isAssignableFrom(target)) {
				for (Class c: target.getClasses()) {
					if (!c.getEnclosingClass().equals(target)) continue;
					if (RatingPredictorBuilder.class.isAssignableFrom(c)) {
						target = c;
						break;
					}
				}
			}
		}
		LinkedBindingBuilder<RatingPredictorBuilder> binder = bind(RatingPredictorBuilder.class).annotatedWith(BaselinePredictor.class);
		
		if (target != null) {
			logger.debug("Using baseline {}", target.getName());
			binder.to(target);
		} else {
			binder.toProvider(Providers.of((RatingPredictorBuilder) null));
		}
	}
	
	protected <T> void bindClassFromProperty(Key<T> key, String propName) {
		bindClassFromProperty(key, propName, null);
	}

	/**
	 * Bind a dependency using a class read from a property.
	 * 
	 * @param key The Guice dependency key to bind.
	 * @param propName The name of the property containing the class name.
	 * @param dftClass The implementation to bind if the property is not set.
	 * If <tt>null</tt>, then null will be bound (and the dependency must have
	 * the {@link Nullable} annotation).  This parameter has a bare type to make
	 * it easier to use in the face of type erasure.
	 */
	@SuppressWarnings("unchecked")
	protected <T> void bindClassFromProperty(Key<T> key, String propName, Class dftClass) {
		String rnorm = properties.getProperty(propName);
		Class target = dftClass;
		if (rnorm != null) {
			try {
				target = ObjectLoader.getClass(rnorm);
			} catch (ClassNotFoundException e) {
				logger.error("Class {} (from {}) not found", rnorm, propName);
			}
		}
		
		if (target != null) {
			logger.debug("Binding {} to {}", key.toString(), target.getName());
			Type tgtType = TypeUtils.reifyType(key.getTypeLiteral().getType(), target);
			logger.debug("Reified {} to {}", target, tgtType);
			bind(key).to((Key) Key.get(tgtType));
		} else {
			logger.debug("Binding {} to null", key.toString());
			bind(key).toProvider(Providers.of((T) null));
		}
	}
	
	protected void configureItemSimilarity() {
		Key<Similarity<Long2DoubleMap>> key =
			Key.get(new TypeLiteral<Similarity<Long2DoubleMap>>() {},
					ItemSimilarity.class);
		bindClassFromProperty(key, ItemSimilarity.PROPERTY_NAME,
				CosineSimilarity.class);
	}
}
