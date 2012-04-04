/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2012 Regents of the University of Minnesota and contributors
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
package org.grouplens.lenskit.eval.config

import org.junit.Before
import org.junit.Test
import static org.hamcrest.Matchers.*
import static org.junit.Assert.*
import org.junit.Ignore
import org.grouplens.lenskit.eval.traintest.TrainTestEvalCommand

/**
 * Test the eval config engine and make sure it can actually execute tests.
 * @author Michael Ekstrand
 */
class TestEvalConfigEngine {
    EvalConfigEngine engine;

    @Before
    void createEngine() {
        engine = new EvalConfigEngine()
    }

    private def script(name) {
        return new InputStreamReader(getClass().getResourceAsStream(name), "UTF-8")
    }

    @Test @Ignore
    void testSingleEmptyEval() {
        def result = engine.execute(script("emptyTrainTest.groovy"))
        assertThat(result, nullValue())
    }

    @Test @Ignore
    void testDefaultImports() {
        def result = engine.execute(script("import.groovy"))
        assertThat(result, equalTo(1))
    }

    @Test @Ignore
    void testMultiTasks() {
        def result = engine.execute(script("multiple.groovy"))
        def eval = env.defaultTask
        assertThat(eval, instanceOf(TrainTestEvalTask))
        def evals = env.tasks
        assertThat(evals.size(), equalTo(2))
        assertTrue(evals.contains(eval));
        assertTrue(evals.containsAll(eval.dependencies))
    }
}
