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
 * 
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
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
package org.grouplens.reflens.util.parallel;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A task queue that returns (long) integer task IDs.
 * 
 * This task queue represents tasks as consecutive, increasing integer IDs,
 * starting with 0.  All active iterators are considered threads, and each one
 * returns the next global task ID.  This allows multiple threads to start up,
 * each one to start an iterator loop overthe task queue, and the work to get
 * done.  Updates are done atomically so the whole thing is lock-free on good
 * hardware.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class IntegerTaskQueue {
	private final long taskCount;
	private AtomicLong nextTask;
	
	public IntegerTaskQueue(long ntasks) {
		taskCount = ntasks;
		nextTask = new AtomicLong();
	}
	
	public long getTaskCount() {
		return taskCount;
	}
	
	public <W extends IntWorker> void run(WorkerFactory<W> factory, int nthreads) {
		Queue<Thread> threads = new LinkedList<Thread>();
		ThreadGroup group = new ThreadGroup(factory.getClass().getName());
		for (int i = 0; i < nthreads; i++) {
			String name = String.format("%s(%d)", factory.getClass().getName(), i);
			Thread t = new TaskThread<W>(group, name, factory);
			threads.add(t);
			t.start();
		}
		while (!threads.isEmpty()) {
			Thread t = threads.element();
			try {
				t.join();
				threads.remove();
			} catch (InterruptedException e) {
				/* no-op, try again */;
			}
		}
	}
	
	private class TaskThread<W extends IntWorker> extends Thread {
		private final WorkerFactory<W> factory;
		public TaskThread(ThreadGroup group, String name, WorkerFactory<W> factory) {
			super(group, name);
			this.factory = factory;
		}
		
		@Override
		public void run() {
			IntWorker worker = factory.create(this);
			for (long job = nextTask.getAndIncrement(); job < taskCount;
					  job = nextTask.getAndIncrement()) {
				worker.doJob(job);
			}
			worker.finish();
		}
	}
}
