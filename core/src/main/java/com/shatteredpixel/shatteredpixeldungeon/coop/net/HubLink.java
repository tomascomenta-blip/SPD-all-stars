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

//One guest's connection inside a Hub. The hub plugs the real transport in and out of it.
public class HubLink implements Link {

	//whatever currently carries frames to this guest
	interface Sender {
		boolean send( String frame );
		void close();
	}

	public final String key;
	private volatile Listener listener;
	private volatile Sender sender = null;
	private volatile boolean closed = false;
	private final Runnable onClose;

	HubLink( String key, Runnable onClose ){
		this.key = key;
		this.onClose = onClose;
	}

	@Override
	public void setListener( Listener listener ) {
		this.listener = listener;
	}

	@Override
	public void start() {
		//the hub runs the transport
	}

	@Override
	public boolean send( String frame ) {
		Sender s = sender;
		return s != null && !closed && s.send(frame);
	}

	@Override
	public boolean connected() {
		return sender != null && !closed;
	}

	@Override
	public void close() {
		closed = true;
		Sender s = sender;
		sender = null;
		if (s != null) s.close();
		if (onClose != null) onClose.run();
	}

	boolean isClosed(){
		return closed;
	}

	//a (new) transport for this guest: replaces any older one
	void attach( Sender s, String info ){
		if (closed){
			s.close();
			return;
		}
		Sender old;
		synchronized (this){
			old = sender;
			sender = s;
		}
		if (old != null && old != s) old.close();
		Listener l = listener;
		if (l != null) l.onLinkState(true, info);
	}

	void detach( Sender s, String info ){
		synchronized (this){
			if (sender != s) return;
			sender = null;
		}
		Listener l = listener;
		if (l != null && !closed) l.onLinkState(false, info);
	}

	void deliver( String frame ){
		Listener l = listener;
		if (l != null && !closed) l.onFrame(frame);
	}
}
