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
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;
import org.jibble.pircbot.PircBot;

public class ControlBot extends PircBot {

	private final String challenge;

	private final String secret;
	
	public ControlBot(String secret) {
		this.secret = secret;

		String username = System.getProperty("user.name");
		InetAddress localhostInetAddress;
		try {
			localhostInetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		String hostname = localhostInetAddress.getHostName();
		String name = "control-" + hostname + "-" + username;
		System.out.println("bot name: " + name);
		setName(name);

		this.challenge = UUID.randomUUID().toString();
	}

	@Override
	protected void onMessage(String channel, String sender, String login,
			String hostname, String message) {
		System.out.println("message: " + message);
		try {
			if (message.startsWith("HI ")) {
				String signature = message.substring("HI ".length());
				Mac mac = Mac.getInstance("HmacSHA1");
				Key key = new SecretKeySpec(this.secret.getBytes(), 0,
						this.secret.getBytes().length, "HmacSHA1");
				mac.init(key);
				byte[] signatureData = mac.doFinal(this.challenge.getBytes());
				String expectedSignature = new String(Hex.encode(signatureData));
				if (signature.equals(expectedSignature)) {
					System.out.println("Trusted bot: " + sender);
				} else {
					System.err.println("UNTRUSTED BOT: " + sender);
				}
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public void listBots() {
		sendMessage(Main.IRC_CHANNEL, "HELLO " + this.challenge);
	}

	public void runTest(int requestsPerSecond, int maxWorkers,
			long totalTimeMillis) throws Exception {
		String nonce = UUID.randomUUID().toString();
		String message = "TEST " + requestsPerSecond + " " + maxWorkers + " "
				+ totalTimeMillis + " " + nonce;
		Mac mac = Mac.getInstance("HmacSHA1");
		Key key = new SecretKeySpec(this.secret.getBytes(), 0,
				this.secret.getBytes().length, "HmacSHA1");
		mac.init(key);
		byte[] signatureData = mac.doFinal(message.getBytes());
		String signature = new String(Hex.encode(signatureData));
		sendMessage(Main.IRC_CHANNEL, message + " " + signature);
	}
}
