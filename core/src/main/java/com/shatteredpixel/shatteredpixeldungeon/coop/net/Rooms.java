/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * All-Stars Pixel Dungeon
 * Copyright (C) 2026 tomascomenta
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.coop.net;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

//Room codes for play between different networks, and which public broker each one uses.
public class Rooms {

	//public brokers that accept anonymous connections. The first letter of a code picks one, the host
	//takes the quickest (see probeBroker). TLS is preferred: plain MQTT (1883) is filtered by some providers.
	//broker.emqx.io comes last: it drops everything above about 10 messages a second per connection
	//(measured 2026-10-14), which a party of 3 or 4 easily sends, and the game crawls.
	public static final String[] BROKERS =   {"broker.hivemq.com", "test.mosquitto.org", "broker.hivemq.com", "broker.emqx.io"};
	public static final int[] BROKER_PORTS = {8883,                8886,                 1883,                8883};
	public static final boolean[] BROKER_TLS = {true,              true,                 false,               true};

	private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ"; //no I or O
	public static final int CODE_LENGTH = 5;

	public static String newCode( int brokerIndex ){
		java.util.Random r = new java.util.Random();
		StringBuilder sb = new StringBuilder();
		char first;
		do {
			first = LETTERS.charAt(r.nextInt(LETTERS.length()));
		} while (LETTERS.indexOf(first) % BROKERS.length != brokerIndex);
		sb.append(first);
		for (int i = 1; i < CODE_LENGTH; i++){
			sb.append(LETTERS.charAt(r.nextInt(LETTERS.length())));
		}
		return sb.toString();
	}

	//upper case, letters only, I -> J and O -> Q so that easy mistakes still work
	public static String normalize( String code ){
		if (code == null) return "";
		StringBuilder sb = new StringBuilder();
		for (char c : code.toUpperCase(Locale.ROOT).toCharArray()){
			if (c == 'I') c = 'J';
			if (c == 'O' || c == '0') c = 'Q';
			if (LETTERS.indexOf(c) >= 0) sb.append(c);
		}
		return sb.toString();
	}

	public static boolean valid( String code ){
		return normalize(code).length() == CODE_LENGTH;
	}

	public static int brokerFor( String code ){
		String c = normalize(code);
		if (c.isEmpty()) return 0;
		return LETTERS.indexOf(c.charAt(0)) % BROKERS.length;
	}

	//topic names are hashed so the code itself is not visible on the public broker.
	//Every guest writes to the host's topic and reads its own one.
	public static String hostTopic( String code ){
		return "spdcoop/v2/" + hash(normalize(code)) + "/h";
	}

	public static String guestTopicBase( String code ){
		return "spdcoop/v2/" + hash(normalize(code)) + "/g/";
	}

	//every guest also reads this one: guests send their orders here too, so the other guests get them
	//straight away instead of through the host (which still passes them on, in case these get lost)
	public static String partyTopic( String code ){
		return "spdcoop/v2/" + hash(normalize(code)) + "/p";
	}

	//a guest's id on the broker, new for every game that joins
	public static String newGuestId(){
		return Long.toHexString(new java.util.Random().nextLong() | 0x1000000000000000L).substring(0, 12);
	}

	private static String hash( String text ){
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] d = md.digest(("spd-coop-room:" + text).getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 10; i++){
				sb.append(String.format(Locale.ROOT, "%02x", d[i] & 0xFF));
			}
			return sb.toString();
		} catch (Exception e){
			return text;
		}
	}

	//the quickest broker right now (all but the last are tried at the same time), -1 if none answers
	public static int probeBroker(){
		final int n = BROKERS.length - 1;
		final long[] time = new long[n];
		Thread[] probes = new Thread[n];
		for (int i = 0; i < n; i++){
			final int b = i;
			time[b] = -1;
			probes[b] = new Thread(new Runnable() {
				@Override
				public void run() {
					time[b] = MqttLink.echoTime(BROKERS[b], BROKER_PORTS[b], BROKER_TLS[b]);
				}
			});
			probes[b].setDaemon(true);
			probes[b].start();
		}
		long until = System.currentTimeMillis() + 9000;
		for (Thread t : probes){
			try { t.join(Math.max(1, until - System.currentTimeMillis())); } catch (InterruptedException e) { break; }
		}
		int best = -1;
		for (int i = 0; i < n; i++){
			CoopLog.line("broker " + BROKERS[i] + ":" + BROKER_PORTS[i] + " echo " + time[i] + " ms");
			//a little better is not worth leaving the preferred order
			if (time[i] > 0 && (best < 0 || time[i] < time[best] - 30)) best = i;
		}
		if (best >= 0) return best;
		//the last one only if nothing else works (it drops messages above ~10 a second)
		int last = BROKERS.length - 1;
		return MqttLink.handshakeWorks(BROKERS[last], BROKER_PORTS[last], BROKER_TLS[last]) ? last : -1;
	}

	//IPv4 addresses of this device on local networks, for joining by address
	public static ArrayList<String> localAddresses(){
		ArrayList<String> result = new ArrayList<>();
		try {
			for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())){
				if (!ni.isUp() || ni.isLoopback()) continue;
				for (InetAddress a : Collections.list(ni.getInetAddresses())){
					if (a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress()){
						result.add(a.getHostAddress());
					}
				}
			}
		} catch (Exception e){
			//no addresses then
		}
		return result;
	}
}
