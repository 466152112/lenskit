package org.grouplens.reflens.knn;


import static org.junit.Assert.assertEquals;
import it.unimi.dsi.fastutil.longs.Long2DoubleMaps;

import org.grouplens.reflens.data.vector.SparseVector;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class TestPearsonCorrelation {
	public static final double EPSILON = 1.0e-6;
	
	Similarity<SparseVector> sim;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() {
		sim = new PearsonCorrelation();
	}
	
	@Test
	public void testEmptyVector() {
		SparseVector v = new SparseVector(Long2DoubleMaps.EMPTY_MAP);
		assertEquals(0, sim.similarity(v, v), EPSILON);
	}

	@Test
	public void testSelfSimilarity() {
		long keys[] = {1, 5, 7};
		double values[] = { 1.5, 2.5, 2 };
		SparseVector v = SparseVector.wrap(keys, values);
		assertEquals(1, sim.similarity(v, v), EPSILON);
		assertEquals(1, sim.similarity(v, v.clone()), EPSILON);
	}
	
	@Test
	public void testDisjointSimilarity() {
		long keys[] = {1, 5, 7};
		double values[] = {1.5, 2.5, 2};
		long keys2[] = {2, 4, 8};
		SparseVector v1 = SparseVector.wrap(keys, values);
		SparseVector v2 = SparseVector.wrap(keys2, values);
		assertEquals(0, sim.similarity(v1, v2), EPSILON);
	}
	
	@Test
	public void testSimilarity() {
		long k1[] = {1, 5, 7};
		double val1[] = {1.5, 2.5, 2};
		long k2[] = {1, 5, 6};
		double val2[] = {2, 2.5, 1.7};
		SparseVector v1 = SparseVector.wrap(k1, val1);
		SparseVector v2 = SparseVector.wrap(k2, val2);
		assertEquals(.806404996, sim.similarity(v1, v2), EPSILON);
	}
}
