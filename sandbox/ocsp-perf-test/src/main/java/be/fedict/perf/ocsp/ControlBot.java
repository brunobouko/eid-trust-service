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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;

public class ControlBot extends ListenerAdapter<PircBotX> {

	private String challenge;

	private final String secret;

	private final Set<String> usedNonces;

	private final Map<String, TestResult[]> testResults;

	private final PircBotX pircBotX;

	public ControlBot(String secret) throws Exception {
		this.secret = secret;
		this.usedNonces = new HashSet<String>();
		this.testResults = new HashMap<String, TestResult[]>();

		String name = "ctrl-" + UUID.randomUUID().toString();
		System.out.println("bot name: " + name);
		this.pircBotX = new PircBotX();
		this.pircBotX.setName(name);
		this.pircBotX.getListenerManager().addListener(this);
		this.pircBotX.connect(Main.IRC_SERVER);
		this.pircBotX.joinChannel(Main.IRC_CHANNEL);
	}

	@Override
	public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
		System.out.println("Connected to IRC");
	}

	@Override
	public void onJoin(JoinEvent<PircBotX> event) throws Exception {
		System.out.println("Joining: " + event.getUser().getNick());
	}

	@Override
	public void onQuit(QuitEvent<PircBotX> event) throws Exception {
		System.out.println("Quits: " + event.getUser().getNick());
	}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) {
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		System.out.println(sender + ": " + message);
	}

	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event)
			throws Exception {
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		System.out.println(sender + ": " + message);
		try {
			if (message.startsWith("HI ")) {
				Scanner scanner = new Scanner(message);
				scanner.useDelimiter(" ");
				scanner.next();
				String nonce = scanner.next();
				String signature = scanner.next();

				if (this.usedNonces.contains(nonce)) {
					throw new RuntimeException("nonce already used");
				}
				this.usedNonces.add(nonce);

				String toBeSigned = "HI " + this.challenge + nonce;
				Mac mac = Mac.getInstance("HmacSHA1");
				Key key = new SecretKeySpec(this.secret.getBytes(), 0,
						this.secret.getBytes().length, "HmacSHA1");
				mac.init(key);
				byte[] signatureData = mac.doFinal(toBeSigned.getBytes());
				String expectedSignature = new String(Hex.encode(signatureData));
				if (signature.equals(expectedSignature)) {
					System.out.println("Trusted bot: " + sender);
					this.testResults.put(sender, null);
					// we can accept test results from trusted senders
				} else {
					System.err.println("UNTRUSTED BOT: " + sender);
				}
			} else if (message.startsWith("RESULT ")) {
				Scanner scanner = new Scanner(message);
				scanner.useDelimiter(" ");
				scanner.next();
				int intervalCount = scanner.nextInt();
				int workerCount = scanner.nextInt();
				int currentRequestCount = scanner.nextInt();
				int currentRequestMillis = scanner.nextInt();
				TestResult[] botTestResults = this.testResults.get(sender);
				botTestResults[intervalCount] = new TestResult(workerCount,
						currentRequestCount, currentRequestMillis);
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public void listBots() {
		this.challenge = UUID.randomUUID().toString();
		this.testResults.clear();
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, "HELLO " + this.challenge);
	}

	public Map<String, TestResult[]> getTestResults() {
		return this.testResults;
	}

	public void runTest(int requestsPerSecond, int maxWorkers,
			long totalTimeMillis, boolean sameSerialNumber) throws Exception {
		// reset test results
		for (String trustedBot : this.testResults.keySet()) {
			TestResult[] botTestResults = new TestResult[(int) totalTimeMillis / 1000 + 1];
			this.testResults.put(trustedBot, botTestResults);
		}

		String nonce = UUID.randomUUID().toString();
		String message = "TEST " + requestsPerSecond + " " + maxWorkers + " "
				+ totalTimeMillis + " " + sameSerialNumber + " " + nonce;
		Mac mac = Mac.getInstance("HmacSHA1");
		Key key = new SecretKeySpec(this.secret.getBytes(), 0,
				this.secret.getBytes().length, "HmacSHA1");
		mac.init(key);
		byte[] signatureData = mac.doFinal(message.getBytes());
		String signature = new String(Hex.encode(signatureData));
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, message + " " + signature);
	}

	public void killAllBots() throws Exception {
		String nonce = UUID.randomUUID().toString();
		String toBeSigned = "KILL " + nonce;
		Mac mac = Mac.getInstance("HmacSHA1");
		Key key = new SecretKeySpec(this.secret.getBytes(), 0,
				this.secret.getBytes().length, "HmacSHA1");
		mac.init(key);
		byte[] signatureData = mac.doFinal(toBeSigned.getBytes());
		String signature = new String(Hex.encode(signatureData));
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, toBeSigned + " "
				+ signature);
	}
}
