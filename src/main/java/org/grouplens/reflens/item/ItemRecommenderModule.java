/* RefLens, a reference implementation of recommender algorithms.
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
 */

package org.grouplens.reflens.item;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;

import org.grouplens.reflens.Normalizer;
import org.grouplens.reflens.RecommenderBuilder;
import org.grouplens.reflens.Similarity;
import org.grouplens.reflens.item.params.ItemSimilarity;
import org.grouplens.reflens.item.params.NeighborhoodSize;
import org.grouplens.reflens.item.params.RatingNormalization;
import org.grouplens.reflens.util.ObjectLoader;
import org.grouplens.reflens.util.SimilarityMatrixBuilderFactory;
import org.grouplens.reflens.util.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;
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
		configureSimilarityMatrix();
		
		configureUserNormalizer();
		configureNeighborhoodSize();

		configureItemSimilarity();
		configureRecommenderBuilder();
	}

	/**
	 * 
	 */
	private void configureRecommenderBuilder() {
		bind(new TypeLiteral<RecommenderBuilder<Integer, Integer>>() {}).to(new TypeLiteral<ItemItemRecommenderBuilder<Integer,Integer>>() {});
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

	/**
	 * 
	 */
	protected void configureUserNormalizer() {
		Key<Normalizer<Integer,Map<Integer,Float>>> rnKey =
			Key.get(new TypeLiteral<Normalizer<Integer,Map<Integer,Float>>>() {},
					RatingNormalization.class);
		bindClassFromProperty(rnKey, RatingNormalization.PROPERTY_NAME);
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
			target = ObjectLoader.getClass(rnorm);
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
		Key<Similarity<Map<Integer,Float>>> key =
			Key.get(new TypeLiteral<Similarity<Map<Integer,Float>>>() {},
					ItemSimilarity.class);
		bindClassFromProperty(key, ItemSimilarity.PROPERTY_NAME,
				CosineSimilarity.class);
	}
}
