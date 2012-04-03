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

	public ManagerTimerTask(int requestsPerSecond, int maxWorkers,
			long totalTimeMillis, CertificateRepository certificateRepository,
			NetworkConfig networkConfig) {
		this.requestsPerSecond = requestsPerSecond;
		this.maxWorkers = maxWorkers;
		long beginTimeMillis = System.currentTimeMillis();
		this.endTimeMillis = beginTimeMillis + totalTimeMillis;
		this.certificateRepository = certificateRepository;
		this.networkConfig = networkConfig;
		System.out.println("WORKER COUNT, REQUEST COUNT, AVERAGE DT");
		System.out.println("---------------------------------------");
	}

	@Override
	public synchronized void run() {
		System.out
				.println(this.workerCount
						+ ","
						+ this.currentRequestCount
						+ ","
						+ (this.currentRequestCount != 0 ? (double) this.currentRequestMillis
								/ this.currentRequestCount
								: 0));

		long currentTimeMillis = System.currentTimeMillis();
		if (this.endTimeMillis <= currentTimeMillis) {
			System.exit(0);
		}

		if (this.currentRequestCount < this.requestsPerSecond) {
			if (this.workerCount < this.maxWorkers) {
				WorkerThread workerThread = new WorkerThread(this.workerCount,
						this, this.certificateRepository, this.networkConfig);
				workerThread.start();
				this.workerCount++;
			}
		}

		this.currentRequestCount = 0;
		this.currentRequestMillis = 0;
		notifyAll();
	}

	public synchronized void reportWork(long millis) {
		if (this.currentRequestCount >= this.requestsPerSecond) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException("wait error: " + e.getMessage(), e);
			}
		}
		this.currentRequestCount++;
		this.currentRequestMillis += millis;
	}
}
