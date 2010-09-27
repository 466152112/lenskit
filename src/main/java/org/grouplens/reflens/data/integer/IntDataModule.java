package org.grouplens.reflens.data.integer;

import org.grouplens.reflens.data.Indexer;
import org.grouplens.reflens.data.RatingVectorFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class IntDataModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(new TypeLiteral<RatingVectorFactory<Integer,Integer>>() {}).to(new TypeLiteral<IntRatingVectorFactory<Integer>>() {});
		bind(new TypeLiteral<Indexer<Integer>>() {}).to(IntIndexer.class);
	}

}
