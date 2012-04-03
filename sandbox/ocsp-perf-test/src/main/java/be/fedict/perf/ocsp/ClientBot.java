/*
 * eID Trust Service Project.
 * Copyright (C) 2012 Frank Cornelis.
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.Security;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.jibble.pircbot.PircBot;

public class ClientBot extends PircBot {

	private final String secret;

	private final Set<String> usedNonces;

	private final Main main;

	public ClientBot(String secret, Main main) {
		this.secret = secret;
		this.main = main;
		this.usedNonces = new HashSet<String>();
		String username = System.getProperty("user.name");
		InetAddress localhostInetAddress;
		try {
			localhostInetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		String hostname = localhostInetAddress.getHostName();
		String name = "client-" + hostname + "-" + username;
		System.out.println("bot name: " + name);
		setName(name);
	}

	@Override
	protected void onMessage(String channel, String sender, String login,
			String hostname, String message) {
		try {
			System.out.println("message: " + message);
			if (message.startsWith("HELLO ")) {
				String challenge = message.substring("HELLO ".length());
				System.out.println("challenge: " + challenge);
				Mac mac = Mac.getInstance("HmacSHA1");
				Key key = new SecretKeySpec(this.secret.getBytes(), 0,
						this.secret.getBytes().length, "HmacSHA1");
				mac.init(key);
				byte[] signatureData = mac.doFinal(challenge.getBytes());
				String signature = new String(Hex.encode(signatureData));
				sendMessage(channel, "HI " + signature);
			} else if (message.startsWith("TEST ")) {
				Scanner scanner = new Scanner(message);
				scanner.useDelimiter(" ");
				scanner.next();
				int requestsPerSecond = scanner.nextInt();
				int maxWorkers = scanner.nextInt();
				int totalTimeMillis = scanner.nextInt();
				String nonce = scanner.next();
				String signature = scanner.next();
				System.out.println("Request for testing");
				System.out.println("Requests per second: " + requestsPerSecond);
				System.out.println("Max workers: " + maxWorkers);
				System.out.println("Total time millis: " + totalTimeMillis);
				if (this.usedNonces.contains(nonce)) {
					throw new RuntimeException("nonce already user");
				}
				String toBeSigned = "TEST " + requestsPerSecond + " "
						+ maxWorkers + " " + totalTimeMillis + " " + nonce;
				Mac mac = Mac.getInstance("HmacSHA1");
				Key key = new SecretKeySpec(this.secret.getBytes(), 0,
						this.secret.getBytes().length, "HmacSHA1");
				mac.init(key);
				byte[] signatureData = mac.doFinal(toBeSigned.getBytes());
				String expectedSignature = new String(Hex.encode(signatureData));
				if (false == signature.equals(expectedSignature)) {
					throw new RuntimeException("invalid request signature");
				} else {
					System.out.println("Ready to run test...");
					if (null == Security
							.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
						Security.addProvider(new BouncyCastleProvider());
					}
					CertificateRepository certificateRepository = new CertificateRepository(
							false);
					this.main.runTest(requestsPerSecond, maxWorkers,
							totalTimeMillis, certificateRepository, null);
				}
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
