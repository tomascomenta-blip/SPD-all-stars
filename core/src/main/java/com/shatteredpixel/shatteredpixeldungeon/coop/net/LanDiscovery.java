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

import com.watabou.noosa.Game;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

//Finds games on the same Wi-Fi: the host broadcasts a small UDP message every second.
public class LanDiscovery {

	public static final int PORT = 22051;
	private static final String TAG = "SPDCOOP1";

	public interface Listener {
		void found( String address, int port, String hostName );
	}

	private static volatile Thread beacon;
	private static volatile Thread listener;

	public static synchronized void startBeacon( final String hostName, final int tcpPort ){
		stopBeacon();
		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				DatagramSocket socket = null;
				try {
					socket = new DatagramSocket();
					socket.setBroadcast(true);
					byte[] data = (TAG + "|" + tcpPort + "|" + hostName.replace("|", "/")).getBytes(StandardCharsets.UTF_8);
					while (beacon == Thread.currentThread()){
						for (InetAddress target : broadcastAddresses()){
							try {
								socket.send(new DatagramPacket(data, data.length, target, PORT));
							} catch (Exception e){
								//some interfaces refuse broadcasts, that's fine
							}
						}
						Thread.sleep(1000);
					}
				} catch (Exception e){
					//stop quietly
				} finally {
					if (socket != null) socket.close();
				}
			}
		});
		t.setName("SPD coop beacon");
		t.setDaemon(true);
		beacon = t;
		t.start();
	}

	public static synchronized void stopBeacon(){
		beacon = null;
	}

	public static synchronized void startListening( final Listener l ){
		stopListening();
		if (Game.platform != null) Game.platform.setLocalDiscovery(true);
		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				DatagramSocket socket = null;
				try {
					socket = new DatagramSocket(null);
					socket.setReuseAddress(true);
					socket.setBroadcast(true);
					socket.bind(new java.net.InetSocketAddress(PORT));
					socket.setSoTimeout(1000);
					byte[] buf = new byte[512];
					while (listener == Thread.currentThread()){
						DatagramPacket p = new DatagramPacket(buf, buf.length);
						try {
							socket.receive(p);
						} catch (SocketTimeoutException e){
							continue;
						}
						String msg = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
						String[] parts = msg.split("\\|", 3);
						if (parts.length == 3 && parts[0].equals(TAG)){
							try {
								l.found(p.getAddress().getHostAddress(), Integer.parseInt(parts[1]), parts[2]);
							} catch (NumberFormatException e){
								//ignore malformed beacons
							}
						}
					}
				} catch (Exception e){
					//stop quietly
				} finally {
					if (socket != null) socket.close();
				}
			}
		});
		t.setName("SPD coop discovery");
		t.setDaemon(true);
		listener = t;
		t.start();
	}

	public static synchronized void stopListening(){
		listener = null;
		if (Game.platform != null) Game.platform.setLocalDiscovery(false);
	}

	private static ArrayList<InetAddress> broadcastAddresses(){
		ArrayList<InetAddress> result = new ArrayList<>();
		try {
			result.add(InetAddress.getByName("255.255.255.255"));
			for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())){
				if (!ni.isUp() || ni.isLoopback()) continue;
				for (InterfaceAddress ia : ni.getInterfaceAddresses()){
					if (ia.getBroadcast() != null && !result.contains(ia.getBroadcast())){
						result.add(ia.getBroadcast());
					}
				}
			}
			//so two copies of the game on the same computer can find each other
			result.add(InetAddress.getByName("127.0.0.1"));
		} catch (Exception e){
			//use what we have
		}
		return result;
	}
}
