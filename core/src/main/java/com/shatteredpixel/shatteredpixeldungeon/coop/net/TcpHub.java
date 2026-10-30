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
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

//Host on the same network: listens and accepts any number of guests (each one a TcpLink.join).
//The first frame of every connection is the guest channel's "H <session> ..." greeting, which tells
//which guest it is, so a guest that reconnects gets its old link back.
public class TcpHub implements Hub {

	private static final int MAX_FRAME = 8*1024*1024;

	private final int port;
	private volatile Listener listener;
	private volatile boolean closed = false;
	private ServerSocket serverSocket;
	private final LinkedHashMap<String, HubLink> links = new LinkedHashMap<>();

	public TcpHub( int port ){
		this.port = port;
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
				acceptLoop();
			}
		});
		t.setName("SPD coop tcp hub");
		t.setDaemon(true);
		t.start();
	}

	private void acceptLoop(){
		try {
			serverSocket = new ServerSocket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(new InetSocketAddress(port));
			while (!closed){
				final Socket s = serverSocket.accept();
				Thread reader = new Thread(new Runnable() {
					@Override
					public void run() {
						serve(s);
					}
				});
				reader.setName("SPD coop tcp hub reader");
				reader.setDaemon(true);
				reader.start();
			}
		} catch (IOException e){
			if (!closed) CoopLog.line("tcp hub error " + e);
		}
	}

	private void serve( final Socket s ){
		HubLink link = null;
		HubLink.Sender sender = null;
		try {
			s.setTcpNoDelay(true);
			s.setKeepAlive(true);
			s.setSoTimeout(15000);
			DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
			final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
			String first = read(in);
			if (!first.startsWith("H ")) throw new IOException("no greeting");
			s.setSoTimeout(0);
			String key = first.substring(2).trim().split(" ")[0];
			boolean isNew = false;
			synchronized (links){
				link = links.get(key);
				if (link == null || link.isClosed()){
					final String k = key;
					link = new HubLink(key, new Runnable() {
						@Override
						public void run() {
							synchronized (links){
								links.remove(k);
							}
						}
					});
					links.put(key, link);
					isNew = true;
				}
			}
			if (isNew){
				Listener l = listener;
				if (l != null) l.onGuest(link);
			}
			sender = new HubLink.Sender() {
				@Override
				public boolean send( String frame ) {
					synchronized (out){
						try {
							byte[] data = frame.getBytes(StandardCharsets.UTF_8);
							out.writeInt(data.length);
							out.write(data);
							out.flush();
							return true;
						} catch (IOException e){
							try { s.close(); } catch (IOException ie) { /* ignore */ }
							return false;
						}
					}
				}

				@Override
				public void close() {
					try { s.close(); } catch (IOException e) { /* ignore */ }
				}
			};
			link.attach(sender, "connected");
			link.deliver(first);
			while (!closed && !link.isClosed()){
				link.deliver(read(in));
			}
		} catch (IOException e){
			//fall through
		}
		try { s.close(); } catch (IOException e) { /* ignore */ }
		if (link != null && sender != null) link.detach(sender, "listening");
	}

	private static String read( DataInputStream in ) throws IOException {
		int len = in.readInt();
		if (len < 0 || len > MAX_FRAME) throw new IOException("bad frame");
		byte[] data = new byte[len];
		in.readFully(data);
		return new String(data, StandardCharsets.UTF_8);
	}

	@Override
	public void close() {
		closed = true;
		HubLink[] all;
		synchronized (links){
			all = links.values().toArray(new HubLink[0]);
		}
		for (HubLink l : all) l.close();
		if (serverSocket != null){
			try { serverSocket.close(); } catch (IOException e) { /* ignore */ }
		}
	}
}
