/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2011 Regents of the University of Minnesota
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.grouplens.lenskit.util.TaskTimer;
import org.grouplens.lenskit.util.parallel.ExecHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Execute job groups sequentially. Used to implement
 * {@link IsolationLevel#JOB_GROUP}.
 * 
 * @since 0.8
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 * 
 */
public class SequentialJobGroupExecutor implements JobGroupExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SequentialJobGroupExecutor.class);
    
    private List<JobGroup> groups;
    private int threadCount;

    private EvalListenerManager listeners;
    
    public SequentialJobGroupExecutor(int nthreads, EvalListenerManager lm) {
        groups = new ArrayList<JobGroup>();
        threadCount = nthreads;
        listeners = lm;
    }
    
    @Override
    public void add(JobGroup group) {
        groups.add(group);
    }
    
    class JobWrapper implements Function<Job, Runnable> {
        @Override
        public Runnable apply(final Job job) {
            return new Runnable() {
                @Override
                public void run() {
                    listeners.jobStarting(job);
                    try {
                        job.run();
                        listeners.jobFinished(job, null);
                    } catch (RuntimeException e) {
                        listeners.jobFinished(job, e);
                    }
                }
            };
        }
    }
    
    @Override
    public void run() throws ExecutionException {
        ExecutorService svc = Executors.newFixedThreadPool(threadCount);
        try {
            listeners.evaluationStarting();
            for (JobGroup group: groups) {
                TaskTimer timer = new TaskTimer();
                logger.info("Running job group {}", group.getName());
                listeners.jobGroupStarting(group);
                group.start();
                try {
                    ExecHelpers.parallelRun(svc, Lists.transform(group.getJobs(), new JobWrapper()));
                } finally {
                    listeners.jobGroupFinished(group);
                    group.finish();
                }
                logger.info("Job group {} finished in {}",
                            group.getName(), timer.elapsedPretty());
            }
            listeners.evaluationFinished(null);
        } catch (ExecutionException err) {
            listeners.evaluationFinished(err);
            throw err;
        } catch (RuntimeException err) {
            listeners.evaluationFinished(err);
            throw err;
        } finally {
            svc.shutdownNow();
        }
    }
}
