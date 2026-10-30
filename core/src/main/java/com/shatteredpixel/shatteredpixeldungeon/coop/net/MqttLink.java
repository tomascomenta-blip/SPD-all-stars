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
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

//Connection between different networks through a public MQTT broker (no account needed).
//Each room has a topic towards the host (guests put their id in front of every frame) and one topic
//per guest (see MqttHub). This is a minimal MQTT 3.1.1 client (QoS 0),
//lost frames are recovered by the Channel on top of it.
public class MqttLink implements Link {

	private static final int KEEPALIVE = 30;

	private final String broker;
	private final int port;
	private final boolean tls;
	private final String inTopic;
	private final String outTopic;
	private final String clientId;
	//a second topic to read (guests: the party topic), null if none
	private volatile String extraTopic = null;

	private volatile Listener listener;
	private volatile boolean closed = false;
	private volatile Socket socket;
	private volatile OutputStream out;
	private volatile boolean ready = false;
	private final Object writeLock = new Object();
	private volatile long lastSend = 0;
	private volatile long lastReceive = 0;
	//guests: their id, put in front of every frame so the host knows who sent it
	private volatile String prefix = "";

	public MqttLink( String broker, int port, boolean tls, String inTopic, String outTopic ){
		this.broker = broker;
		this.port = port;
		this.tls = tls;
		this.inTopic = inTopic;
		this.outTopic = outTopic;
		this.clientId = "spdcoop-" + Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 10);
	}

	@Override
	public void setListener( Listener listener ) {
		this.listener = listener;
	}

	@Override
	public void start() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				loop();
			}
		});
		t.setName("SPD coop mqtt");
		t.setDaemon(true);
		t.start();

		Thread pinger = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!closed){
					try { Thread.sleep(5000); } catch (InterruptedException e) { return; }
					long now = System.currentTimeMillis();
					//ping when quiet in either direction, so the broker answers and the read never times out
					if (ready && (now - lastSend > 10000L || now - lastReceive > 10000L)){
						write(new byte[]{(byte)0xC0, 0});
					}
				}
			}
		});
		pinger.setName("SPD coop mqtt ping");
		pinger.setDaemon(true);
		pinger.start();
	}

	private void loop(){
		int delay = 1000;
		while (!closed){
			Socket s = null;
			try {
				state(false, "connecting");
				s = open(broker, port, tls, 6000);
				s.setTcpNoDelay(true);
				s.setSoTimeout((KEEPALIVE*3/2)*1000);
				synchronized (writeLock){
					socket = s;
					out = s.getOutputStream();
				}
				DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));

				write(connectPacket());
				Packet ack = readPacket(in);
				if (ack.type != 2 || ack.body.length < 2 || ack.body[1] != 0){
					throw new IOException("broker refused the connection");
				}
				write(subscribePacket());
				delay = 1000;

				while (!closed){
					Packet p = readPacket(in);
					lastReceive = System.currentTimeMillis();
					if (p.type == 9){
						//SUBACK, from here on frames can flow
						ready = true;
						state(true, "connected");
					} else if (p.type == 3){
						handlePublish(p);
					}
					//PINGRESP and others need no handling
				}
			} catch (IOException e){
				CoopLog.line("mqtt error " + e);
			}
			ready = false;
			synchronized (writeLock){
				socket = null;
				out = null;
			}
			try { if (s != null) s.close(); } catch (IOException e) { /* ignore */ }
			if (closed) break;
			state(false, "connecting");
			try { Thread.sleep(delay); } catch (InterruptedException e) { return; }
			delay = Math.min(delay*2, 8000);
		}
	}

	//plain or TLS socket to a broker, with certificate and (where supported) host name checks
	static Socket open( String host, int port, boolean tls, int timeout ) throws IOException {
		Socket plain = new Socket();
		plain.connect(new InetSocketAddress(host, port), timeout);
		if (!tls) return plain;
		javax.net.ssl.SSLSocketFactory f = (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
		javax.net.ssl.SSLSocket s = (javax.net.ssl.SSLSocket) f.createSocket(plain, host, port, true);
		boolean endpointChecked;
		try {
			javax.net.ssl.SSLParameters params = s.getSSLParameters();
			params.setEndpointIdentificationAlgorithm("HTTPS");
			s.setSSLParameters(params);
			endpointChecked = true;
		} catch (Throwable t){
			//old Android versions: checked below instead
			endpointChecked = false;
		}
		s.setSoTimeout(timeout);
		s.startHandshake();
		if (!endpointChecked
				&& !javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier().verify(host, s.getSession())){
			s.close();
			throw new IOException("certificate does not match " + host);
		}
		return s;
	}

	//true if the broker answers an MQTT connection
	public static boolean handshakeWorks( String host, int port, boolean tls ){
		Socket s = null;
		try {
			s = open(host, port, tls, 4000);
			s.setSoTimeout(4000);
			MqttLink probe = new MqttLink(host, port, tls, "spdcoop/probe", "spdcoop/probe");
			OutputStream out = s.getOutputStream();
			out.write(probe.connectPacket());
			out.flush();
			Packet ack = readPacket(new DataInputStream(new BufferedInputStream(s.getInputStream())));
			return ack.type == 2 && ack.body.length >= 2 && ack.body[1] == 0;
		} catch (Exception e){
			return false;
		} finally {
			try { if (s != null) s.close(); } catch (IOException e) { /* ignore */ }
		}
	}

	//how long a message takes through the broker and back (the second slowest of a few), -1 if the
	//broker doesn't work: rooms go to the quickest broker, which depends on where the player is
	//and on how busy each broker is that day
	public static long echoTime( String host, int port, boolean tls ){
		Socket s = null;
		try {
			s = open(host, port, tls, 4000);
			s.setSoTimeout(3000);
			String topic = "spdcoop/probe/" + Long.toHexString(Double.doubleToLongBits(Math.random()) ^ System.nanoTime());
			MqttLink probe = new MqttLink(host, port, tls, topic, topic);
			OutputStream out = s.getOutputStream();
			DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
			out.write(probe.connectPacket());
			out.flush();
			Packet ack = readPacket(in);
			if (ack.type != 2 || ack.body.length < 2 || ack.body[1] != 0) return -1;
			out.write(probe.subscribePacket());
			out.flush();
			while (readPacket(in).type != 9);
			long[] times = new long[5];
			for (int i = 0; i < times.length; i++){
				long sent = System.nanoTime();
				out.write(probe.publishPacket(topic, "p" + i));
				out.flush();
				while (readPacket(in).type != 3);
				times[i] = (System.nanoTime() - sent) / 1000000L;
			}
			java.util.Arrays.sort(times);
			return Math.max(1, times[times.length - 2]);
		} catch (Exception e){
			return -1;
		} finally {
			try { if (s != null) s.close(); } catch (IOException e) { /* ignore */ }
		}
	}

	private void handlePublish( Packet p ){
		int topicLen = ((p.body[0] & 0xFF) << 8) | (p.body[1] & 0xFF);
		int offset = 2 + topicLen;
		int qos = (p.flags >> 1) & 3;
		if (qos > 0) offset += 2;
		if (offset > p.body.length) return;
		String text = new String(p.body, offset, p.body.length - offset, StandardCharsets.UTF_8);
		Listener l = listener;
		if (l != null) l.onFrame(text);
	}

	private static class Packet {
		int type;
		int flags;
		byte[] body;
	}

	private static Packet readPacket( DataInputStream in ) throws IOException {
		int b = in.readUnsignedByte();
		Packet p = new Packet();
		p.type = b >> 4;
		p.flags = b & 0x0F;
		int len = 0, mult = 1, digit;
		do {
			digit = in.readUnsignedByte();
			len += (digit & 127) * mult;
			mult *= 128;
			if (mult > 128*128*128*128) throw new IOException("bad length");
		} while ((digit & 128) != 0);
		p.body = new byte[len];
		in.readFully(p.body);
		return p;
	}

	private byte[] connectPacket(){
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		writeString(body, "MQTT");
		body.write(4);              //protocol level 3.1.1
		body.write(0x02);           //clean session
		body.write(KEEPALIVE >> 8);
		body.write(KEEPALIVE & 0xFF);
		writeString(body, clientId);
		return packet(0x10, body.toByteArray());
	}

	private byte[] subscribePacket(){
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		body.write(0);
		body.write(1);              //packet id
		writeString(body, inTopic);
		body.write(0);              //QoS 0
		if (extraTopic != null){
			writeString(body, extraTopic);
			body.write(0);
		}
		return packet(0x82, body.toByteArray());
	}

	private byte[] publishPacket( String topic, String text ){
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		writeString(body, topic);
		byte[] data = text.getBytes(StandardCharsets.UTF_8);
		body.write(data, 0, data.length);
		return packet(0x30, body.toByteArray());
	}

	private static void writeString( ByteArrayOutputStream out, String s ){
		byte[] data = s.getBytes(StandardCharsets.UTF_8);
		out.write(data.length >> 8);
		out.write(data.length & 0xFF);
		out.write(data, 0, data.length);
	}

	private static byte[] packet( int header, byte[] body ){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(header);
		int len = body.length;
		do {
			int digit = len % 128;
			len /= 128;
			if (len > 0) digit |= 128;
			out.write(digit);
		} while (len > 0);
		out.write(body, 0, body.length);
		return out.toByteArray();
	}

	private boolean write( byte[] data ){
		synchronized (writeLock){
			if (out == null) return false;
			try {
				out.write(data);
				out.flush();
				lastSend = System.currentTimeMillis();
				return true;
			} catch (IOException e){
				try { if (socket != null) socket.close(); } catch (IOException ie) { /* ignore */ }
				out = null;
				return false;
			}
		}
	}

	public void setExtraTopic( String topic ){
		extraTopic = topic;
	}

	//a frame to any topic, as it is (no prefix)
	public boolean publish( String topic, String frame ){
		if (!ready) return false;
		return write(publishPacket(topic, frame));
	}

	public void setPrefix( String prefix ){
		this.prefix = prefix == null ? "" : prefix;
	}

	@Override
	public boolean send( String frame ) {
		if (!ready || outTopic == null) return false;
		return write(publishPacket(outTopic, prefix + frame));
	}

	//host: a frame for one guest's topic
	public boolean sendTo( String topic, String frame ) {
		if (!ready) return false;
		return write(publishPacket(topic, frame));
	}

	@Override
	public boolean connected() {
		return ready;
	}

	@Override
	public void close() {
		closed = true;
		write(new byte[]{(byte)0xE0, 0});
		synchronized (writeLock){
			try { if (socket != null) socket.close(); } catch (IOException e) { /* ignore */ }
			socket = null;
			out = null;
		}
		ready = false;
	}

	private void state( boolean connected, String info ){
		CoopLog.line("mqtt " + broker + " " + info + " in=" + inTopic);
		Listener l = listener;
		if (l != null) l.onLinkState(connected, info);
	}
}
