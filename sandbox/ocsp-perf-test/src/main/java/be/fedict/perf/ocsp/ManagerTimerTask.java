/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2012 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.perf.ocsp;

import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

public class ManagerTimerTask extends TimerTask {

	private final int requestsPerSecond;

	private final int maxWorkers;

	private final long endTimeMillis;

	private int currentRequestCount;

	private int currentRequestMillis;

	private int workerCount;

	private final CertificateRepository certificateRepository;

	private final NetworkConfig networkConfig;

	private ThreadGroup workerThreadGroup;

	private List<WorkerThread> workerThreads;

	private boolean running;

	private List<WorkListener> workListeners;

	private int intervalCounter;

	public ManagerTimerTask(int requestsPerSecond, int maxWorkers,
			long totalTimeMillis, CertificateRepository certificateRepository,
			NetworkConfig networkConfig) {
		this.requestsPerSecond = requestsPerSecond;
		this.maxWorkers = maxWorkers;
		long beginTimeMillis = System.currentTimeMillis();
		this.endTimeMillis = beginTimeMillis + totalTimeMillis;
		this.certificateRepository = certificateRepository;
		this.networkConfig = networkConfig;
		System.out.println("INTERVAL, WORKER COUNT, REQUEST COUNT, AVERAGE DT");
		System.out.println("-------------------------------------------------");
		this.workerThreads = new LinkedList<WorkerThread>();
		this.running = true;
		this.workListeners = new LinkedList<WorkListener>();
		this.workerThreadGroup = new ThreadGroup("worker-thread-group");
	}

	public void addWorkListener(WorkListener workListener) {
		this.workListeners.add(workListener);
	}

	private void notifyWorkListenersDone() {
		for (WorkListener workListener : this.workListeners) {
			workListener.done();
		}
	}

	@Override
	public synchronized void run() {
		if (this.running) {
			System.out
					.println(this.intervalCounter
							+ ","
							+ this.workerCount
							+ ","
							+ this.currentRequestCount
							+ ","
							+ (this.currentRequestCount != 0 ? (double) this.currentRequestMillis
									/ this.currentRequestCount
									: 0));
			this.intervalCounter++;
		}

		long currentTimeMillis = System.currentTimeMillis();
		if (this.endTimeMillis <= currentTimeMillis) {
			if (this.running) {
				System.out.println("Ending tests...");
			}
			this.running = false;

			this.currentRequestCount = 0;
			this.currentRequestMillis = 0;
			notifyAll();

			boolean oneAlive = false;
			for (WorkerThread workerThread : this.workerThreads) {
				oneAlive |= workerThread.isAlive();
			}
			if (false == oneAlive) {
				notifyWorkListenersDone();
			}
			return;
		}

		if (this.currentRequestCount < this.requestsPerSecond) {
			if (this.workerCount < this.maxWorkers) {
				WorkerThread workerThread = new WorkerThread(
						this.workerThreadGroup, this.workerCount, this,
						this.certificateRepository, this.networkConfig);
				workerThread.start();
				this.workerThreads.add(workerThread);
				this.workerCount++;
			}
		}

		this.currentRequestCount = 0;
		this.currentRequestMillis = 0;
		notifyAll();
	}

	public synchronized boolean reportWork(long millis) {
		if (this.currentRequestCount >= this.requestsPerSecond) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException("wait error: " + e.getMessage(), e);
			}
		}
		this.currentRequestCount++;
		this.currentRequestMillis += millis;
		return this.running;
	}
}
