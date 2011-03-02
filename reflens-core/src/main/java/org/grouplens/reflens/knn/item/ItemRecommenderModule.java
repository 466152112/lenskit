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

package org.grouplens.reflens.knn.item;

import org.grouplens.reflens.RatingPredictor;
import org.grouplens.reflens.RecommenderModule;
import org.grouplens.reflens.RecommenderService;
import org.grouplens.reflens.RecommenderServiceProvider;
import org.grouplens.reflens.data.RatingDataSource;
import org.grouplens.reflens.data.vector.SparseVector;
import org.grouplens.reflens.knn.NeighborhoodRecommenderModule;
import org.grouplens.reflens.knn.OptimizableVectorSimilarity;
import org.grouplens.reflens.knn.Similarity;
import org.grouplens.reflens.knn.SimilarityMatrixBuilderFactory;
import org.grouplens.reflens.knn.TruncatingSimilarityMatrixBuilder;
import org.grouplens.reflens.knn.params.ItemSimilarity;
import org.grouplens.reflens.params.BaselinePredictor;
import org.grouplens.reflens.util.SymmetricBinaryFunction;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

/**
 * TODO Extract NeighborhoodRecommenderModule
 * TODO Document this class
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class ItemRecommenderModule extends RecommenderModule {
	/**
	 * Neighborhood recommender parameters.
	 */
	public final NeighborhoodRecommenderModule knn;
	
	public ItemRecommenderModule() {
		knn = new NeighborhoodRecommenderModule();
	}
	
	@Override
	public void setName(String name) {
		super.setName(name);
		knn.setName(name);
	}
	
	@Override
	protected void configure() {
		super.configure();
		install(ThrowingProviderBinder.forModule(this));
		install(knn);
		configureSimilarityMatrix();
	}

	/**
	 * 
	 */
	protected void configureSimilarityMatrix() {
		bind(SimilarityMatrixBuilderFactory.class).toProvider(
				FactoryProvider.newFactory(SimilarityMatrixBuilderFactory.class,
						TruncatingSimilarityMatrixBuilder.class));
	}
	
	@CheckedProvides(RecommenderServiceProvider.class)
	@Singleton
	public RecommenderService provideRecommenderService(ItemItemRecommenderBuilder builder,
			RatingDataSource data, @BaselinePredictor RatingPredictor baseline) {
		return builder.build(data, baseline);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Provides
	protected SimilarityMatrixBuildStrategy buildStrategy(
			SimilarityMatrixBuilderFactory matrixFactory,
			@ItemSimilarity Similarity<? super SparseVector> similarity) {
		if (similarity instanceof OptimizableVectorSimilarity) {
			if (similarity instanceof SymmetricBinaryFunction)
				return new OptimizedSymmetricSimilarityMatrixBuildStrategy(matrixFactory,
						(OptimizableVectorSimilarity) similarity);
			else
				return new OptimizedSimilarityMatrixBuildStrategy(matrixFactory,
						(OptimizableVectorSimilarity) similarity);
		} else {
			if (similarity instanceof SymmetricBinaryFunction)
				return new SymmetricSimilarityMatrixBuildStrategy(matrixFactory, similarity);
			else
				return new SimpleSimilarityMatrixBuildStrategy(matrixFactory, similarity);
		}
	}
}
