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

import java.security.Key;
import java.security.Security;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.MessageEvent;

public class ClientBot extends ListenerAdapter<PircBotX> implements
		WorkListener {

	private final String secret;

	private final Set<String> usedNonces;

	private final Main main;

	private final PircBotX pircBotX;

	private final CertificateRepository certificateRepository;

	private User controlUser;

	public ClientBot(String secret, Main main) throws Exception {
		this.secret = secret;
		this.main = main;
		this.usedNonces = new HashSet<String>();
		String name = "bt-" + UUID.randomUUID().toString();
		System.out.println("bot name: " + name);
		this.pircBotX = new PircBotX();
		this.pircBotX.setName(name);
		this.pircBotX.connect(Main.IRC_SERVER);
		this.pircBotX.joinChannel(Main.IRC_CHANNEL);
		this.pircBotX.getListenerManager().addListener(this);

		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}
		this.certificateRepository = new CertificateRepository();
	}

	@Override
	public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
		System.out.println("Connected to IRC");
	}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) {
		try {
			String message = event.getMessage();
			String sender = event.getUser().getNick();
			Channel channel = event.getChannel();
			System.out.println(sender + ": " + message);
			if (message.startsWith("HELLO ")) {
				String challenge = message.substring("HELLO ".length());
				System.out.println("challenge: " + challenge);

				if (this.usedNonces.contains(challenge)) {
					throw new RuntimeException("nonce already user");
				}
				this.usedNonces.add(challenge);

				Mac mac = Mac.getInstance("HmacSHA1");
				Key key = new SecretKeySpec(this.secret.getBytes(), 0,
						this.secret.getBytes().length, "HmacSHA1");
				mac.init(key);
				String nonce = UUID.randomUUID().toString();
				String toBeSigned = "HI " + challenge + nonce;
				byte[] signatureData = mac.doFinal(toBeSigned.getBytes());
				String signature = new String(Hex.encode(signatureData));
				this.pircBotX.sendMessage(event.getUser(), "HI " + nonce + " "
						+ signature);
			} else if (message.startsWith("TEST ")) {
				Scanner scanner = new Scanner(message);
				scanner.useDelimiter(" ");
				scanner.next();
				int requestsPerSecond = scanner.nextInt();
				int maxWorkers = scanner.nextInt();
				int totalTimeMillis = scanner.nextInt();
				boolean sameSerialNumber = scanner.nextBoolean();
				String nonce = scanner.next();
				String signature = scanner.next();
				System.out.println("Request for testing");
				System.out.println("Requests per second: " + requestsPerSecond);
				System.out.println("Max workers: " + maxWorkers);
				System.out.println("Total time millis: " + totalTimeMillis);
				System.out.println("Same serial number: " + sameSerialNumber);

				if (this.usedNonces.contains(nonce)) {
					throw new RuntimeException("nonce already user");
				}
				this.usedNonces.add(nonce);

				String toBeSigned = "TEST " + requestsPerSecond + " "
						+ maxWorkers + " " + totalTimeMillis + " "
						+ sameSerialNumber + " " + nonce;
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
					this.controlUser = event.getUser();
					this.pircBotX.sendMessage(channel, "STARTING");
					this.certificateRepository.init(sameSerialNumber);
					this.main.runTest(requestsPerSecond, maxWorkers,
							totalTimeMillis, this.certificateRepository, null,
							this);
				}
			} else if (message.startsWith("KILL ")) {
				Scanner scanner = new Scanner(message);
				scanner.useDelimiter(" ");
				scanner.next();
				String nonce = scanner.next();
				if (this.usedNonces.contains(nonce)) {
					throw new RuntimeException("nonce already user");
				}
				this.usedNonces.add(nonce);
				String signature = scanner.next();
				String toBeSigned = "KILL " + nonce;
				Mac mac = Mac.getInstance("HmacSHA1");
				Key key = new SecretKeySpec(this.secret.getBytes(), 0,
						this.secret.getBytes().length, "HmacSHA1");
				mac.init(key);
				byte[] signatureData = mac.doFinal(toBeSigned.getBytes());
				String expectedSignature = new String(Hex.encode(signatureData));
				if (false == signature.equals(expectedSignature)) {
					throw new RuntimeException("invalid request signature");
				} else {
					System.out.println("VALID SUICIDE MESSAGE RECEIVED!");
					this.pircBotX.sendMessage(channel, "SUICIDE");
					System.exit(1);
				}
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public void done() {
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, "DONE");
	}

	@Override
	public void result(int intervalCounter, int workerCount,
			int currentRequestCount, int currentRequestMillis) {
		String message = "RESULT " + intervalCounter + " " + workerCount + " "
				+ currentRequestCount + " " + currentRequestMillis;
		// pircbot is queing, so minimal impact on timer here
		this.pircBotX.sendMessage(this.controlUser, message);
	}
}
