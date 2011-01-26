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
package org.grouplens.reflens.bench;

import static org.grouplens.reflens.bench.AlgorithmInstance.fileExtension;

import java.io.File;

import org.junit.Test;
import static org.junit.Assert.assertEquals;


/**
 * Tests for the {@link AlgorithmInstance} class.  Not very extensive, but they
 * help us sanity-check a few things.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class TestAlgorithmInstance {
	@Test
	public void testBasename() {
		assertEquals("foo", AlgorithmInstance.fileBaseName(new File("foo"), null));
		assertEquals("foo", AlgorithmInstance.fileBaseName(new File("foo"), "bar"));
		assertEquals("foo.bar", AlgorithmInstance.fileBaseName(new File("foo.bar"), null));
		assertEquals("foo", AlgorithmInstance.fileBaseName(new File("foo.bar"), "bar"));
		assertEquals("foo.bar", AlgorithmInstance.fileBaseName(new File("foo.bar"), "properties"));
		assertEquals("foo", AlgorithmInstance.fileBaseName(new File("foo.properties"), "properties"));
		assertEquals("whizbang", AlgorithmInstance.fileBaseName(new File("whizbang.properties"), "properties"));
	}
	
	@Test
	public void testFileExtensionNone() {
		assertEquals("", fileExtension(""));
		assertEquals("", fileExtension("foo"));
	}
	
	@Test
	public void testFileExtensionSimple() {
		assertEquals("txt", fileExtension("foo.txt"));
	}
	
	@Test
	public void testFileExtensionMultiple() {
		assertEquals("txt", fileExtension("foo.exe.txt"));
	}
	
	@Test
	public void testFileExtensionFile() {
		assertEquals("txt", fileExtension(new File("foo.txt")));
		assertEquals("", fileExtension(new File("foo")));
	}
}
