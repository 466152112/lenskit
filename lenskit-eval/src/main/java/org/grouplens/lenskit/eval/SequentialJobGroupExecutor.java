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
package org.grouplens.lenskit.eval;

import org.apache.commons.lang3.time.StopWatch;
import org.grouplens.lenskit.util.parallel.ExecHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Execute job groups sequentially. Used to implement
 * {@link IsolationLevel#JOB_GROUP}.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 * @since 0.8
 */
public class SequentialJobGroupExecutor implements JobGroupExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SequentialJobGroupExecutor.class);

    private final List<JobGroup<?>> groups;
    private final int threadCount;


    public SequentialJobGroupExecutor(int nthreads) {
        groups = new ArrayList<JobGroup<?>>();
        threadCount = nthreads;
    }

    @Override
    public void add(JobGroup<?> group) {
        groups.add(group);
    }

    @Override
    public void run() throws ExecutionException {
        ExecutorService svc = Executors.newFixedThreadPool(threadCount);
        try {
            for (JobGroup<?> group : groups) {
                StopWatch timer = new StopWatch();
                timer.start();

                logger.info("Running job group {}", group.getName());
                group.start();
                try {
                    ExecHelpers.parallelRun(svc, group.getJobs());
                } finally {
                    group.finish();
                }

                timer.stop();
                logger.info("Job group {} finished in {}",
                            group.getName(), timer);
            }
        } finally {
            svc.shutdownNow();
        }
    }
}
