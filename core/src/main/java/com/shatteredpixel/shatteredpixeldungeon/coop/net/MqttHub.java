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

import java.util.ArrayList;
import java.util.LinkedHashMap;

//Host through a public MQTT broker: one broker connection for every guest. Guests write to the
//host's topic with their id in front of each frame; the host answers on each guest's own topic.
public class MqttHub implements Hub, Link.Listener {

	private final MqttLink mqtt;
	private final String guestTopicBase;
	private volatile Listener listener;
	private volatile boolean up = false;
	private final LinkedHashMap<String, HubLink> links = new LinkedHashMap<>();
	private final LinkedHashMap<String, HubLink.Sender> senders = new LinkedHashMap<>();

	public MqttHub( String broker, int port, boolean tls, String hostTopic, String guestTopicBase ){
		this.guestTopicBase = guestTopicBase;
		mqtt = new MqttLink(broker, port, tls, hostTopic, null);
		mqtt.setListener(this);
	}

	@Override
	public void setListener( Listener listener ) {
		this.listener = listener;
	}

	@Override
	public void start() {
		mqtt.start();
	}

	private static boolean validId( String id ){
		if (id.length() < 6 || id.length() > 16) return false;
		for (char c : id.toCharArray()){
			if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
		}
		return true;
	}

	@Override
	public void onFrame( String frame ) {
		int sp = frame.indexOf(' ');
		if (sp <= 0) return;
		final String id = frame.substring(0, sp);
		if (!validId(id)) return;
		HubLink link;
		HubLink.Sender sender = null;
		boolean isNew = false;
		synchronized (links){
			link = links.get(id);
			if (link == null){
				link = new HubLink(id, new Runnable() {
					@Override
					public void run() {
						synchronized (links){
							links.remove(id);
							senders.remove(id);
						}
					}
				});
				sender = new HubLink.Sender() {
					@Override
					public boolean send( String f ) {
						return mqtt.sendTo(guestTopicBase + id, f);
					}

					@Override
					public void close() {
						//the broker connection is shared with the other guests
					}
				};
				links.put(id, link);
				senders.put(id, sender);
				isNew = true;
			}
		}
		if (isNew){
			Listener l = listener;
			if (l != null) l.onGuest(link);
			if (up) link.attach(sender, "connected");
		}
		link.deliver(frame.substring(sp + 1));
	}

	@Override
	public void onLinkState( boolean connected, String info ) {
		up = connected;
		ArrayList<HubLink> all = new ArrayList<>();
		ArrayList<HubLink.Sender> snd = new ArrayList<>();
		synchronized (links){
			for (String id : links.keySet()){
				all.add(links.get(id));
				snd.add(senders.get(id));
			}
		}
		for (int i = 0; i < all.size(); i++){
			if (connected) all.get(i).attach(snd.get(i), info);
			else           all.get(i).detach(snd.get(i), info);
		}
	}

	public boolean connected(){
		return up;
	}

	@Override
	public void close() {
		HubLink[] all;
		synchronized (links){
			all = links.values().toArray(new HubLink[0]);
		}
		for (HubLink l : all) l.close();
		mqtt.close();
	}
}
