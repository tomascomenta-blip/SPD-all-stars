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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

//Direct connection on the same network. The host listens, the guest connects.
//Frames are sent as a 4 byte length followed by UTF-8 text.
public class TcpLink implements Link {

	public static final int DEFAULT_PORT = Integer.getInteger("spd.coop.port", 22050); //testing: several pairs on one computer
	private static final int MAX_FRAME = 8*1024*1024;

	private final boolean server;
	private final String address;
	private final int port;

	private volatile Listener listener;
	private volatile boolean closed = false;

	private ServerSocket serverSocket;
	private volatile Socket socket;
	private volatile DataOutputStream out;
	private final Object writeLock = new Object();

	private TcpLink( boolean server, String address, int port ){
		this.server = server;
		this.address = address;
		this.port = port;
	}

	public static TcpLink host( int port ){
		return new TcpLink( true, null, port );
	}

	public static TcpLink join( String address, int port ){
		return new TcpLink( false, address, port );
	}

	@Override
	public void setListener( Listener listener ) {
		this.listener = listener;
	}

	@Override
	public void start() {
		Thread t = new Thread(server ? new Runnable() {
			@Override
			public void run() {
				acceptLoop();
			}
		} : new Runnable() {
			@Override
			public void run() {
				connectLoop();
			}
		});
		t.setName("SPD coop tcp");
		t.setDaemon(true);
		t.start();
	}

	private void acceptLoop(){
		try {
			serverSocket = new ServerSocket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(new InetSocketAddress(port));
			state(false, "listening");
			while (!closed){
				Socket s = serverSocket.accept();
				//a new connection replaces the old one (e.g. the guest reconnected)
				dropSocket();
				useSocket(s);
			}
		} catch (IOException e){
			if (!closed) state(false, "error: " + e.getMessage());
		}
	}

	private void connectLoop(){
		int delay = 500;
		while (!closed){
			try {
				Socket s = new Socket();
				s.connect(new InetSocketAddress(address, port), 4000);
				delay = 500;
				useSocket(s);
				//wait here until this socket dies, then try again
				while (!closed && socket == s){
					try { Thread.sleep(250); } catch (InterruptedException e) { return; }
				}
			} catch (IOException e){
				state(false, "connecting");
				try { Thread.sleep(delay); } catch (InterruptedException ie) { return; }
				delay = Math.min(delay*2, 4000);
			}
		}
	}

	private void useSocket( final Socket s ) throws IOException {
		s.setTcpNoDelay(true);
		s.setKeepAlive(true);
		synchronized (writeLock) {
			socket = s;
			out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
		}
		Thread reader = new Thread(new Runnable() {
			@Override
			public void run() {
				readLoop(s);
			}
		});
		reader.setName("SPD coop tcp reader");
		reader.setDaemon(true);
		reader.start();
		state(true, "connected");
	}

	private void readLoop( Socket s ){
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
			while (!closed && socket == s){
				int len = in.readInt();
				if (len < 0 || len > MAX_FRAME) throw new IOException("bad frame");
				byte[] data = new byte[len];
				in.readFully(data);
				Listener l = listener;
				if (l != null) l.onFrame(new String(data, StandardCharsets.UTF_8));
			}
		} catch (IOException e){
			//fall through
		}
		synchronized (writeLock) {
			if (socket == s) {
				socket = null;
				out = null;
			}
		}
		try { s.close(); } catch (IOException e) { /* ignore */ }
		if (!closed) state(false, server ? "listening" : "connecting");
	}

	private void dropSocket(){
		synchronized (writeLock){
			if (socket != null){
				try { socket.close(); } catch (IOException e) { /* ignore */ }
			}
			socket = null;
			out = null;
		}
	}

	@Override
	public boolean send( String frame ) {
		synchronized (writeLock){
			if (out == null) return false;
			try {
				byte[] data = frame.getBytes(StandardCharsets.UTF_8);
				out.writeInt(data.length);
				out.write(data);
				out.flush();
				return true;
			} catch (IOException e){
				dropSocket();
				return false;
			}
		}
	}

	@Override
	public boolean connected() {
		return out != null;
	}

	@Override
	public void close() {
		closed = true;
		dropSocket();
		if (serverSocket != null){
			try { serverSocket.close(); } catch (IOException e) { /* ignore */ }
		}
	}

	private void state( boolean connected, String info ){
		Listener l = listener;
		if (l != null) l.onLinkState(connected, info);
	}
}
