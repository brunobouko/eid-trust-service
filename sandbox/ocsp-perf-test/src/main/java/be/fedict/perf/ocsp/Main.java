package be.fedict.perf.ocsp;

import java.security.Security;
import java.util.Date;
import java.util.Timer;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Main {

	public static void main(String[] args) throws Exception {
		System.out.println("OCSP Performance Test.");
		if (args.length < 3) {
			System.err
					.println("Usage: java <program> <req/sec> <max workers> <total time> [proxy host] [proxy port]");
			System.exit(1);
		}
		int requestsPerSecond = Integer.parseInt(args[0]);
		int maxWorkers = Integer.parseInt(args[1]);
		long totalTimeMillis = Integer.parseInt(args[2]) * 1000;
		NetworkConfig networkConfig;
		if (args.length >= 5) {
			String proxyHost = args[3];
			int proxyPort = Integer.parseInt(args[4]);
			networkConfig = new NetworkConfig(proxyHost, proxyPort);
		} else {
			networkConfig = null;
		}

		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}

		CertificateRepository certificateRepository = new CertificateRepository();
		System.out.println("Certificate repository size: "
				+ certificateRepository.getSize());

		Timer timer = new Timer();
		ManagerTimerTask managerTimerTask = new ManagerTimerTask(
				requestsPerSecond, maxWorkers, totalTimeMillis,
				certificateRepository, networkConfig);
		timer.scheduleAtFixedRate(managerTimerTask, new Date(), 1000);
	}
}
