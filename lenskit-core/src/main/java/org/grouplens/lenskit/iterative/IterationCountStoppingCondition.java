/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2013 Regents of the University of Minnesota and contributors
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
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
package org.grouplens.lenskit.iterative;

import org.grouplens.lenskit.core.Shareable;
import org.grouplens.lenskit.iterative.params.IterationCount;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Stop once the iteration count has reached a value.
 *
 * @author Michael Ekstrand
 */
@Immutable
@Shareable
public class IterationCountStoppingCondition implements StoppingCondition, Serializable {
    private static final long serialVersionUID = 1L;

    private final int iterCount;

    /**
     * Construct a new iteration count stopping condition
     *
     * @param niter The number of iterations to run.
     */
    @Inject
    public IterationCountStoppingCondition(@IterationCount int niter) {
        iterCount = niter;
    }

    /**
     * Get the number of iterations the stopper requires.
     *
     * @return The number of iterations.
     */
    public int getIterationCount() {
        return iterCount;
    }

    @Override
    public boolean isFinished(int n, double delta) {
        return n >= iterCount;
    }

    @Override
    public TrainingLoopController newLoop() {
        return new IterationCountTrainingLoopController();
    }

    /**
     * Iteration Count Training Loop controller for iterative updates
     */
    private class IterationCountTrainingLoopController implements TrainingLoopController {
        private int iterations = 0;

        @Override
        public boolean keepTraining(double error) {
            if (iterations >= iterCount) {
                return false;
            } else {
                ++iterations;
                return true;
            }
        }

        @Override
        public int getIterationCount() {
            return iterations;
        }
    }
}
