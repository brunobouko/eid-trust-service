package be.fedict.perf.ocsp;

import java.security.Security;
import java.util.Date;
import java.util.Timer;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Main {

	public static void main(String[] args) throws Exception {
		System.out.println("OCSP Performance Test.");
		if (args.length < 4) {
			System.err
					.println("Usage: java <program> <req/sec> <max workers> <total time> <same serial number> [proxy host] [proxy port]");
			System.err
					.println("Example: 10 5 60 false => 10 per second, 5 workers at max, during 60 seconds, use full database");
			System.exit(1);
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
		System.out.println("Total running time: " + totalTimeMillis);
		System.out
				.println("Always use same serial number: " + sameSerialNumber);

		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}

		CertificateRepository certificateRepository = new CertificateRepository(
				sameSerialNumber);
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

		System.out.println("Starting tests at: " + new Date());

		Timer timer = new Timer("manager-timer-task");
		ManagerTimerTask managerTimerTask = new ManagerTimerTask(
				requestsPerSecond, maxWorkers, totalTimeMillis,
				certificateRepository, networkConfig);
		timer.scheduleAtFixedRate(managerTimerTask, new Date(), 1000);
	}
}
