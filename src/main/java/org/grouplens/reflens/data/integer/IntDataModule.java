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

package org.grouplens.reflens.data.integer;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

import java.util.Map;

import org.grouplens.reflens.data.Indexer;
import org.grouplens.reflens.data.RatingVectorFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class IntDataModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(new TypeLiteral<RatingVectorFactory<Integer,Integer>>() {}).to(new TypeLiteral<IntRatingVectorFactory<Integer>>() {});
		bind(new TypeLiteral<Indexer<Integer>>() {}).to(IntIndexer.class);
		bind(new TypeLiteral<Map<Integer,Float>>(){}).to(Int2FloatOpenHashMap.class);
	}

}
