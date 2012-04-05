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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.Security;
import java.util.Date;
import java.util.Map;
import java.util.Timer;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Main implements WorkListener {

	private static final String IRC_SERVER = "irc.freenode.net";

	public static final String IRC_CHANNEL = "#ocsp-perf-test";

	private String secret;

	public Main(String[] args) throws Exception {
		System.out.println("OCSP Performance Test.");
		if (args.length >= 2) {
			this.secret = args[1];
			if ("bot".equals(args[0])) {
				bot();
			} else if ("control".equals(args[0])) {
				control();
			} else {
				standalone(args);
			}
		} else {
			usage();
		}
	}

	private void bot() throws Exception {
		System.out.println("Bot mode...");
		ClientBot clientBot = new ClientBot(this.secret, this);
		clientBot.connect(IRC_SERVER);
		clientBot.joinChannel(IRC_CHANNEL);
	}

	private void control() throws Exception {
		System.out.println("Control mode...");
		ControlBot controlBot = new ControlBot(this.secret);
		controlBot.connect(IRC_SERVER);
		controlBot.joinChannel(IRC_CHANNEL);
		char commandChar;
		do {
			showMenu();
			commandChar = getKeyboardChar();
			switch (commandChar) {
			case 'l':
				controlBot.listBots();
				break;
			case 'r':
				System.out.print("Requests per second: ");
				int requestsPerSecond = getKeyboardInt();
				System.out.print("Max workers: ");
				int maxWorkers = getKeyboardInt();
				System.out.print("Total Time in seconds: ");
				long totalTimeMillis = getKeyboardInt() * 1000;
				System.out.print("Always same serial number (Y/N): ");
				boolean sameSerialNumber = getKeyboardBoolean();
				controlBot.runTest(requestsPerSecond, maxWorkers,
						totalTimeMillis, sameSerialNumber);
				break;
			case 'p':
				System.out.println("Test results");
				Map<String, TestResult[]> testResults = controlBot
						.getTestResults();
				for (String trustedBot : testResults.keySet()) {
					// TODO
				}
				break;
			case 'k':
				System.out.println("Kill all bots");
				controlBot.killAllBots();
				break;
			}
		} while (commandChar != 'e');
		System.exit(0);
	}

	private static void showMenu() {
		System.out.println("Menu");
		System.out.println("l. List bots");
		System.out.println("r. Run test");
		System.out.println("p. Print test results");
		System.out.println("k. Kill all bots");
		System.out.println("e. Exit");
	}

	private char getKeyboardChar() throws Exception {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(System.in));
		String line;
		do {
			line = bufferedReader.readLine();
		} while (line.length() < 1);
		return line.charAt(0);
	}

	private int getKeyboardInt() throws Exception {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(System.in));
		String line;
		do {
			line = bufferedReader.readLine();
		} while (line.length() < 1);
		return Integer.parseInt(line);
	}

	private boolean getKeyboardBoolean() throws Exception {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(System.in));
		String line;
		do {
			line = bufferedReader.readLine();
		} while (line.length() < 1);
		char c = line.toUpperCase().charAt(0);
		if ('Y' == c) {
			return true;
		}
		return false;
	}

	private void standalone(String[] args) throws Exception {
		if (args.length < 4) {
			usage();
		}
		int requestsPerSecond = Integer.parseInt(args[0]);
		int maxWorkers = Integer.parseInt(args[1]);
		long totalTimeMillis = Integer.parseInt(args[2]) * 1000;
		boolean sameSerialNumber = Boolean.parseBoolean(args[3]);
		NetworkConfig networkConfig;
		if (args.length >= 6) {
			String proxyHost = args[4];
			int proxyPort = Integer.parseInt(args[5]);
			networkConfig = new NetworkConfig(proxyHost, proxyPort);
		} else {
			networkConfig = null;
		}

		System.out.println("Requests per second: " + requestsPerSecond);
		System.out.println("Maximum number of worker threads: " + maxWorkers);
		System.out
				.println("Total running time: " + totalTimeMillis + " millis");
		System.out
				.println("Always use same serial number: " + sameSerialNumber);

		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}

		CertificateRepository certificateRepository = new CertificateRepository();
		certificateRepository.init(sameSerialNumber);
		System.out.println("Certificate repository size: "
				+ certificateRepository.getSize());

		Runtime runtime = Runtime.getRuntime();
		System.out.println("Available processors: "
				+ runtime.availableProcessors());
		System.out.println("Free memory: " + runtime.freeMemory()
				/ (1024 * 1024) + " MiB");
		System.out.println("Total memory: " + runtime.totalMemory()
				/ (1024 * 1024) + " MiB");
		System.out.println("Max memory: " + runtime.maxMemory() / (1024 * 1024)
				+ " MiB");
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		long totalFreeMemory = runtime.maxMemory() - usedMemory;
		System.out.println("Used memory: " + usedMemory / (1024 * 1024)
				+ " MiB");
		System.out.println("Total free memory: " + totalFreeMemory
				/ (1024 * 1024) + " MiB");
		System.out.println("% free memory: " + (double) totalFreeMemory
				/ runtime.maxMemory() * 100 + " %");

		runTest(requestsPerSecond, maxWorkers, totalTimeMillis,
				certificateRepository, networkConfig, this);
	}

	public void runTest(int requestsPerSecond, int maxWorkers,
			long totalTimeMillis, CertificateRepository certificateRepository,
			NetworkConfig networkConfig, WorkListener workListener) {
		System.out.println("Starting tests at: " + new Date());
		Timer timer = new Timer("manager-timer-task");
		ManagerTimerTask managerTimerTask = new ManagerTimerTask(timer,
				requestsPerSecond, maxWorkers, totalTimeMillis,
				certificateRepository, networkConfig);
		managerTimerTask.registerWorkListener(workListener);
		timer.scheduleAtFixedRate(managerTimerTask, new Date(), 1000);
	}

	private void usage() {
		System.err
				.println("Usage: java <program> <req/sec> <max workers> <total time> <same serial number> [proxy host] [proxy port]");
		System.err
				.println("Example: 10 5 60 false => 10 per second, 5 workers at max, during 60 seconds, use full database");
		System.err
				.println("Bot usage: java <program> <'bot' or 'control'> <secret>");
		System.exit(1);
	}

	public static void main(String[] args) throws Exception {
		new Main(args);
	}

	@Override
	public void done() {
		System.out.println("All done.");
		System.exit(0);
	}

	@Override
	public void result(int intervalCounter, int workerCount,
			int currentRequestCount, int currentRequestMillis) {
	}
}
