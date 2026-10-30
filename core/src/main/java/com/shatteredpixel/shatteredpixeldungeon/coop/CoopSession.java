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

package com.shatteredpixel.shatteredpixeldungeon.coop;

import com.badlogic.gdx.utils.Base64Coder;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.Channel;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.Hub;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.LanDiscovery;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.Link;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.MqttHub;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.MqttLink;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.Rooms;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.TcpHub;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.TcpLink;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Scene;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.FileUtils;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;

//co-op, 2 to 8 devices. star shaped: guests only talk to the host and the host relays stuff.
//everyone simulates the whole run (lockstep), only orders go over the wire. new floor / new game /
//copies disagree -> host sends its save and everybody reloads it. brute force but it works
//(si algo se desincroniza, empieza a mirar por aquí)
public class CoopSession {

	//bump this on ANY change that can desync. old devices just refuse to join, which is the point
	public static final int PROTOCOL = 37;

	//silent this long = afk, host makes their hero rest
	private static final long AWAY_AFTER = 10000;

	public enum Role { HOST, GUEST }
	public enum Net { LAN, ONLINE }

	public static volatile CoopSession current;

	public final Role role;
	public final Net net;
	public final String code;       //room code (online) or host address (lan)

	//host: where guests connect; guest: null
	private final Hub hub;
	//guest: the connection to the host; host: null
	private final Peer hostPeer;

	public final String myName;
	public final HeroClass myClass;
	public volatile boolean started = false;
	//save slot of this run: the one the host picked (CoopSaves), the guest copy on a guest
	private volatile int slot = CoopSaves.GUEST_SLOT;
	//host, resumed run: party indexes of the players who weren't in it and get a new hero
	private final ArrayList<Integer> joiners = new ArrayList<>();
	//host: names thrown out of this room, they can't come back to it
	private final HashSet<String> kicked = new HashSet<>();
	//guest: the room password typed by the player, and whether the host asked for one
	private volatile String password = "";
	public volatile boolean passwordWanted = false;
	public volatile String problem = null;
	//guest: the host closed the run (a "bye" arrived during a game)
	public volatile boolean hostLeft = false;

	//host: every guest connection, and the player of each party index (0 is the host, always null)
	private final ArrayList<Peer> peers = new ArrayList<>();
	private final Peer[] seats = new Peer[Party.MAX_HEROES];
	//host: heroes whose player is away; the host plays them (they rest) until their player is back
	private final boolean[] hostControls = new boolean[Party.MAX_HEROES];
	//everyone: the players in the room by party index, and who is away, for the interface
	private volatile String[] roomNames = new String[0];
	private volatile String[] roomClasses = new String[0];
	private volatile boolean[] roomAway = new boolean[Party.MAX_HEROES];
	//guest: party index of this device's hero
	private volatile int myIdx = 1;
	//short messages for the game screen ("X left", "X is back")
	private final ArrayList<String> notices = new ArrayList<>();
	//one lock for relaying + sending, otherwise guests get orders in a different order. ask me how I know
	private final Object sendLock = new Object();

	private volatile int epoch = 0;
	private final ArrayList<ArrayDeque<Bundle>> inbox = new ArrayList<>();
	//guest: events of the host's next epoch that arrive before its state finished loading
	private final ArrayList<Bundle> early = new ArrayList<>();

	//deadlock watch (sendStatus). saved my sanity more than once
	private long lastStatusSent = 0;
	private long waitingSince = 0;
	private Hero waitingSinceFor = null;
	private long lastRsq = 0;
	private long awaitingSince = 0;
	//answer arrived before its window exists (actor thread windows show up a frame late). yes really
	private long promptWaitStart = 0;
	private volatile Bundle pendingSync = null;
	private volatile boolean awaitingSync = false;
	private volatile boolean resyncWanted = false;
	private volatile boolean pausedForResync = false;
	public volatile int desyncs = 0;

	//prompts numbered per hero, creation order is the same everywhere so the numbers match
	private final int[] promptSeq = new int[Party.MAX_HEROES];
	private final IdentityHashMap<Object, Integer> ownPrompts = new IdentityHashMap<>();
	private final ArrayList<HashMap<Integer, Object>> remotePrompts = new ArrayList<>();

	private volatile boolean localCancel = false;
	private volatile Hero waitingFor = null;

	//playing ahead (see announceTurn). By party index, all under the inbox lock:
	//the turns each hero started in this epoch (the same count on every device),
	private final int[] turnNo = new int[Party.MAX_HEROES];
	//the last turn of each hero its device promised not to stop it before (it may be played without
	//waiting for that device),
	private final int[] granted = new int[Party.MAX_HEROES];
	//and this device's fingerprints of turns it played ahead, checked when the owner's one arrives
	private final ArrayList<HashMap<Integer, Long>> aheadSums = new ArrayList<>();

	//online guests also shout their events on the broker topic. numbered per epoch so dupes get dropped
	private volatile MqttLink partyLink = null;
	private volatile String partyTopic = null;
	private int mySeq = 0;
	private final int[] nextSeq = new int[Party.MAX_HEROES];
	private final ArrayList<java.util.TreeMap<Integer, Bundle>> heldEvents = new ArrayList<>();

	//tap while it's not your turn -> queued, runs when it is
	private volatile int queuedCell = -1;
	private volatile Mob queuedTarget = null;
	private Image queuedMark = null;
	private Scene queuedMarkScene = null;
	//hero whose prompts/orders the render thread is currently handling
	private Hero renderOwner = null;

	//one connection: on the host one per guest, on a guest the one to the host
	private class Peer implements Channel.Listener {
		final Channel channel;
		volatile int seat = -1;
		volatile String name = null;
		volatile String cls = null;
		volatile boolean present = false;
		volatile boolean linkUp = false;
		//host: came back after being away, gets the state at the next resync
		volatile boolean rejoining = false;
		//host: the guest's game is in the background (another app): it is away until it says it's back
		volatile boolean paused = false;
		//what this guest says it waits for (deadlock watch)
		volatile int epoch = -1;
		volatile int waitsFor = -1;
		volatile boolean awaitsSync = false;
		volatile long statusTime = 0;
		int waitWhat = -2;
		long waitSince = 0;

		Peer( Link link ){
			channel = new Channel(link);
			channel.setListener(this);
		}

		@Override
		public void onMessage( String message ) {
			CoopSession.this.onMessage(this, message);
		}

		@Override
		public void onChannelState( boolean connected, String info ) {
			linkUp = connected;
			CoopSession.this.onChannelState(this, connected, info);
		}

		@Override
		public void onPeerRestarted() {
			CoopSession.this.onPeerRestarted(this);
		}

		long silence(){
			return channel.millisSinceLastFrame();
		}

		void send( Bundle b ){
			channel.send(b.toString());
		}

		String label(){
			return (name == null ? "?" : name) + "#" + seat;
		}
	}

	private CoopSession( Role role, Net net, String code, Hub hub, Link hostLink, String name, HeroClass cls ){
		this.role = role;
		this.net = net;
		this.code = code;
		this.myName = name;
		this.myClass = cls;
		for (int i = 0; i < Party.MAX_HEROES; i++){
			inbox.add(new ArrayDeque<Bundle>());
			remotePrompts.add(new HashMap<Integer, Object>());
			heldEvents.add(new java.util.TreeMap<Integer, Bundle>());
			nextSeq[i] = 1;
			aheadSums.add(new java.util.LinkedHashMap<Integer, Long>(){
				@Override
				protected boolean removeEldestEntry( Map.Entry<Integer, Long> eldest ) {
					return size() > 256;
				}
			});
		}
		this.hub = hub;
		this.hostPeer = hostLink == null ? null : new Peer(hostLink);
		if (hub != null){
			hub.setListener(new Hub.Listener() {
				@Override
				public void onGuest( Link link ) {
					Peer p = new Peer(link);
					synchronized (peers){
						peers.add(p);
					}
					p.channel.start();
					CoopLog.line("guest connection " + peers.size());
				}
			});
		}
	}

	// --- session ---

	public static CoopSession hostLan( String name, HeroClass cls ){
		end();
		CoopSession s = new CoopSession(Role.HOST, Net.LAN, "", new TcpHub(TcpLink.DEFAULT_PORT), null, name, cls);
		LanDiscovery.startBeacon(name, TcpLink.DEFAULT_PORT);
		return s.begin();
	}

	public static CoopSession joinLan( String address, String name, HeroClass cls ){
		end();
		CoopSession s = new CoopSession(Role.GUEST, Net.LAN, address, null, TcpLink.join(address, TcpLink.DEFAULT_PORT), name, cls);
		return s.begin();
	}

	//brokerIndex: from Rooms.probeBroker(), done off the render thread by the caller
	public static CoopSession hostOnline( int brokerIndex, String name, HeroClass cls ){
		end();
		String code = Rooms.newCode(brokerIndex);
		Hub hub = new MqttHub(Rooms.BROKERS[brokerIndex], Rooms.BROKER_PORTS[brokerIndex], Rooms.BROKER_TLS[brokerIndex],
				Rooms.hostTopic(code), Rooms.guestTopicBase(code));
		CoopSession s = new CoopSession(Role.HOST, Net.ONLINE, code, hub, null, name, cls);
		return s.begin();
	}

	public static CoopSession joinOnline( String code, String name, HeroClass cls ){
		end();
		code = Rooms.normalize(code);
		int broker = Rooms.brokerFor(code);
		String id = Rooms.newGuestId();
		MqttLink link = new MqttLink(Rooms.BROKERS[broker], Rooms.BROKER_PORTS[broker], Rooms.BROKER_TLS[broker],
				Rooms.guestTopicBase(code) + id, Rooms.hostTopic(code));
		link.setPrefix(id + " ");
		link.setExtraTopic(Rooms.partyTopic(code));
		CoopSession s = new CoopSession(Role.GUEST, Net.ONLINE, code, null, link, name, cls);
		s.partyLink = link;
		s.partyTopic = Rooms.partyTopic(code);
		return s.begin();
	}

	private CoopSession begin(){
		current = this;
		CoopLog.installRandomTrace();
		installHooks();
		if (hub != null) hub.start();
		if (hostPeer != null) hostPeer.channel.start();
		if (isHost()) updateRoom();
		return this;
	}

	//after a run ends: everyone goes back to the co-op room, still connected
	public void backToLobby(){
		started = false;
		awaitingSync = false;
		resetTurnState();
		synchronized (sendLock){
			for (int i = 0; i < hostControls.length; i++) hostControls[i] = false;
		}
		roomAway = new boolean[Party.MAX_HEROES];
		CoopOrders.clearPending();
		Party.reset();
		Random.setCosmeticThread(null);
	}

	public static boolean active(){
		CoopSession s = current;
		return s != null && s.started;
	}

	public boolean isHost(){
		return role == Role.HOST;
	}

	public int myIndex(){
		return isHost() ? 0 : myIdx;
	}

	public static void end(){
		CoopSession s = current;
		current = null;
		if (s != null){
			try {
				Bundle bye = msg("bye");
				if (s.hostPeer != null) s.hostPeer.send(bye);
				for (Peer p : s.allPeers()) p.send(bye);
			} catch (Exception e) { /* ignore */ }
			if (s.hostPeer != null) s.hostPeer.channel.close();
			if (s.hub != null) s.hub.close();
		}
		CoopOrders.clearPending();
		LanDiscovery.stopBeacon();
		removeHooks();
		Party.reset();
	}

	/* messages */

	private static Bundle msg( String type ){
		Bundle b = new Bundle();
		b.put("t", type);
		return b;
	}

	private ArrayList<Peer> allPeers(){
		synchronized (peers){
			return new ArrayList<>(peers);
		}
	}

	//guests that take part in the game right now (seated, here, not replaced by the host)
	private ArrayList<Peer> activePeers(){
		ArrayList<Peer> result = new ArrayList<>();
		synchronized (seats){
			for (int i = 1; i < seats.length; i++){
				Peer p = seats[i];
				if (p != null && p.present && !(started && hostControls[i])) result.add(p);
			}
		}
		return result;
	}

	//host: to every guest taking part; guest: to the host
	private void sendAll( Bundle b ){
		synchronized (sendLock){
			if (!isHost()){
				boolean ev = "ev".equals(b.getString("t"));
				if (ev){
					synchronized (inbox){
						b.put("s", ++mySeq);
					}
				}
				hostPeer.send(b);
				MqttLink party = partyLink;
				if (ev && party != null){
					b.put("f", true);
					party.publish(partyTopic, "F " + b.toString());
				}
			} else {
				String text = b.toString();
				for (Peer p : activePeers()) p.channel.send(text);
			}
		}
	}

	private void onMessage( Peer from, String text ) {
		Bundle b = Bundle.fromString(text);
		if (b == null) return;
		String t = b.getString("t");
		if (!t.equals("ev") && !t.equals("st")) CoopLog.line("msg in " + t + (isHost() ? " from " + from.label() : ""));
		if (isHost()) hostMessage(from, t, b, text);
		else          guestMessage(t, b);
	}

	private void hostMessage( Peer from, String t, Bundle b, String text ){
		switch (t){
			case "hello":
				if (b.getInt("v") != PROTOCOL || b.getInt("gv") != Game.versionCode){
					refuse(from, "version");
					return;
				}
				from.name = b.getString("name");
				from.cls = b.getString("cls");
				from.channel.setName(from.name);
				if (from.name != null && kicked.contains(from.name)){
					refuse(from, "kicked");
					return;
				}
				if (!passwordOk(b.getString("pw"))){
					refuse(from, "password");
					return;
				}
				if (!seatGuest(from)){
					refuse(from, "full");
					return;
				}
				from.present = true;
				updateRoom();
				if (started){
					//the guest came back (or restarted the game): send them the run as it is now
					Bundle st = msg("start");
					st.put("idx", from.seat);
					from.send(st);
					from.rejoining = true;
					resyncWanted = true;
				}
				break;
			case "ev":
				relayEvent(from, b, text);
				break;
			case "rsq":
				//a request of an older epoch is already answered by the state on its way
				if (!b.contains("e") || b.getInt("e") >= epoch) resyncWanted = true;
				break;
			case "st":
				from.epoch = b.getInt("e");
				from.waitsFor = b.getInt("w");
				from.awaitsSync = b.getBoolean("s");
				from.statusTime = System.currentTimeMillis();
				break;
			case "say": {
				String line = b.getString("txt");
				String who = from.name == null ? "?" : from.name;
				showChat(who, line);
				Bundle r = msg("say");
				r.put("txt", line);
				r.put("who", who);
				synchronized (sendLock){
					for (Peer p : activePeers()) if (p != from) p.send(r);
				}
				break;
			}
			case "pg": {
				//a mark on the map: show it and pass it on
				CoopPings.show(b.getInt("c"), b.getInt("k"), from.name, b.getInt("d"));
				Bundle r = msg("pg");
				r.put("c", b.getInt("c"));
				r.put("k", b.getInt("k"));
				r.put("d", b.getInt("d"));
				r.put("who", from.name == null ? "?" : from.name);
				synchronized (sendLock){
					for (Peer p : activePeers()) if (p != from) p.send(r);
				}
				break;
			}
			case "pause":
				from.paused = b.getBoolean("p");
				CoopLog.line("guest " + from.label() + (from.paused ? " went to the background" : " is back from the background"));
				break;
			case "bye":
				from.present = false;
				if (!started){
					unseat(from);
					updateRoom();
				}
				//during a game the presence check hands their hero to the host
				break;
		}
	}

	private void guestMessage( String t, Bundle b ){
		switch (t){
			case "lobby":
				roomNames = b.getStringArray("names");
				roomClasses = b.getStringArray("classes");
				if (b.contains("you") && !started) myIdx = b.getInt("you");
				hostPeer.present = true;
				break;
			case "refuse": {
				String why = b.getString("why");
				if ("password".equals(why)){
					problem = Messages.get(CoopSession.class, password.isEmpty() ? "refused_password" : "refused_wrong_password");
					passwordWanted = true;
				} else if ("full".equals(why) || "kicked".equals(why)){
					problem = Messages.get(CoopSession.class, "refused_" + why);
				} else {
					problem = Messages.get(CoopSession.class, "refused_version");
				}
				break;
			}
			case "start":
				myIdx = b.getInt("idx");
				guestStart();
				break;
			case "sync":
				receiveSync(b);
				break;
			case "ev":
				queueEvent(b);
				break;
			case "away": {
				boolean[] away = b.getBooleanArray("a");
				boolean[] before = roomAway;
				if (away != null){
					roomAway = away;
					for (int i = 0; i < away.length && i < before.length; i++){
						if (away[i] != before[i] && i != myIdx) notice(i, away[i]);
					}
				}
				break;
			}
			case "say":
				showChat(b.getString("who"), b.getString("txt"));
				break;
			case "pg":
				CoopPings.show(b.getInt("c"), b.getInt("k"), b.getString("who"), b.getInt("d"));
				break;
			case "bye":
				hostPeer.present = false;
				if (started) hostLeft = true;
				break;
		}
	}

	private void refuse( Peer p, String why ){
		Bundle r = msg("refuse");
		r.put("why", why);
		p.send(r);
		CoopLog.line("refused " + p.label() + ": " + why);
	}

	private void showChat( final String who, final String line ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GLog.p(who + ": " + line);
			}
		});
	}

	private void onChannelState( Peer p, boolean connected, String info ) {
		CoopLog.line("channel " + (connected ? "up" : "down") + " " + info + (isHost() ? " " + p.label() : ""));
		if (connected && !isHost()) sendHello();
	}

	private void onPeerRestarted( Peer p ) {
		if (!isHost()){
			//the host started over: the greeting just sent went to the old one
			sendHello();
		} else if (started){
			resyncWanted = true;
		}
	}

	private void sendHello(){
		Bundle h = msg("hello");
		h.put("v", PROTOCOL);
		h.put("gv", Game.versionCode);
		h.put("name", myName);
		h.put("cls", myClass.name());
		if (!password.isEmpty()) h.put("pw", passwordHash(password));
		hostPeer.send(h);
	}

	//guest: greets the host again with the password the player typed
	public void retryWithPassword( String pw ){
		password = pw == null ? "" : pw.trim();
		passwordWanted = false;
		problem = null;
		sendHello();
	}

	//host: the room password (WndCoopOptions, empty = open room). only a hash goes over the wire
	private boolean passwordOk( String pw ){
		String mine = WndCoopOptions.password();
		return mine.isEmpty() || passwordHash(mine).equals(pw);
	}

	private static String passwordHash( String pw ){
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] d = md.digest(("spdcoop:" + pw).getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte x : d) sb.append(String.format("%02x", x));
			return sb.toString();
		} catch (Exception e){
			return pw;
		}
	}

	//host, room only: that guest leaves and can't join this room again with the same name
	public void kick( int seat ){
		if (started) return;
		Peer p;
		synchronized (seats){
			p = seat > 0 && seat < seats.length ? seats[seat] : null;
		}
		if (p == null) return;
		if (p.name != null) kicked.add(p.name);
		refuse(p, "kicked");
		p.present = false;
		unseat(p);
		updateRoom();
		CoopLog.line("kicked " + p.label());
	}

	//a mark on the map (CoopPings): only a picture, it doesn't go through the turns
	public void ping( int cell, int kind, int floor ){
		Bundle b = msg("pg");
		b.put("c", cell);
		b.put("k", kind);
		b.put("d", floor);
		b.put("who", myName);
		sendAll(b);
	}

	public void say( String text ){
		Bundle b = msg("say");
		b.put("txt", text);
		b.put("who", myName);
		sendAll(b);
		GLog.i(myName + ": " + text);
	}

	// the room / lobby stuff

	//host: find a seat. lobby = first free one, mid game = someone afk (same name first, so rejoining works)
	private boolean seatGuest( Peer p ){
		Peer replaced = null;
		synchronized (seats){
			if (p.seat > 0 && seats[p.seat] == p) return true;
			int found = -1;
			if (!started){
				for (int i = 1; i < seats.length && found < 0; i++){
					if (seats[i] == null) found = i;
				}
			} else {
				int size = Party.size();
				for (int pass = 0; pass < 2 && found < 0; pass++){
					for (int i = 1; i < size && i < seats.length && found < 0; i++){
						Peer old = seats[i];
						boolean free = hostControls[i] || old == null || !old.present
								|| (old.silence() > 3000 && p.name != null && p.name.equals(old.name));
						if (free && (pass == 1 || old == null || (p.name != null && p.name.equals(old.name)))){
							found = i;
						}
					}
				}
			}
			if (found < 0) return false;
			replaced = seats[found];
			seats[found] = p;
			p.seat = found;
		}
		if (replaced != null && replaced != p){
			replaced.present = false;
			replaced.seat = -1;
			synchronized (peers){
				peers.remove(replaced);
			}
			replaced.channel.close();
		}
		CoopLog.line("seat " + p.seat + " for " + p.name);
		return true;
	}

	private void unseat( Peer p ){
		synchronized (seats){
			if (p.seat > 0 && seats[p.seat] == p) seats[p.seat] = null;
			p.seat = -1;
		}
	}

	//host: the list of players, to everyone in the room
	private void updateRoom(){
		String[] names;
		String[] classes;
		synchronized (seats){
			int n = 1;
			for (int i = 1; i < seats.length; i++) if (seats[i] != null) n = i + 1;
			names = new String[n];
			classes = new String[n];
			names[0] = myName;
			classes[0] = myClass.name();
			for (int i = 1; i < n; i++){
				names[i] = seats[i] == null ? "" : seats[i].name;
				classes[i] = seats[i] == null ? "" : seats[i].cls;
			}
		}
		roomNames = names;
		roomClasses = classes;
		synchronized (sendLock){
			for (Peer p : allPeers()){
				if (!p.present || p.seat < 0) continue;
				Bundle l = msg("lobby");
				l.put("names", names);
				l.put("classes", classes);
				l.put("you", p.seat);
				p.send(l);
			}
		}
	}

	//host: guests in the room right now
	public int guestCount(){
		int n = 0;
		synchronized (seats){
			for (int i = 1; i < seats.length; i++){
				if (seats[i] != null && seats[i].present) n++;
			}
		}
		return n;
	}

	//players in the room for the lobby screen: name and class, by party index
	public String[] roomNames(){
		return roomNames;
	}

	public HeroClass roomClass( int i ){
		String[] c = roomClasses;
		if (i < 0 || i >= c.length || c[i] == null || c[i].isEmpty()) return null;
		try {
			return HeroClass.valueOf(c[i]);
		} catch (IllegalArgumentException e){
			return null;
		}
	}

	public boolean connectedToHost(){
		return hostPeer != null && hostPeer.linkUp;
	}

	public boolean hostPresent(){
		return hostPeer != null && hostPeer.present;
	}

	//lobby: gone quiet = lose the seat, sorry
	public static void lobbyTick(){
		CoopSession s = current;
		if (s == null || s.started || !s.isHost()) return;
		boolean changed = false;
		synchronized (s.seats){
			for (int i = 1; i < s.seats.length; i++){
				Peer p = s.seats[i];
				if (p != null && (!p.present || p.silence() > AWAY_AFTER)){
					p.present = false;
					p.seat = -1;
					s.seats[i] = null;
					changed = true;
				}
			}
		}
		if (changed) s.updateRoom();
	}

	//host: who is away right now (by party index), to everyone
	private void sendAway(){
		boolean[] a = new boolean[Party.MAX_HEROES];
		synchronized (sendLock){
			System.arraycopy(hostControls, 0, a, 0, a.length);
		}
		roomAway = a;
		Bundle b = msg("away");
		b.put("a", a);
		sendAll(b);
	}

	private void notice( int idx, boolean away ){
		String who = Party.name(idx);
		final String text = Messages.get(CoopSession.class, away ? "player_away" : "player_back", who);
		synchronized (notices){
			notices.add(text);
		}
	}

	//messages waiting to be shown in the game log (render thread)
	public static ArrayList<String> takeNotices(){
		CoopSession s = current;
		ArrayList<String> result = new ArrayList<>();
		if (s == null) return result;
		synchronized (s.notices){
			result.addAll(s.notices);
			s.notices.clear();
		}
		return result;
	}

	//app went to background (phone call, whatsapp, whatever). host babysits the hero (it rests)
	//and the player gets a fresh state at the next resync
	public static void appPaused( boolean paused ){
		CoopSession s = current;
		if (s == null || !s.started || s.isHost() || s.hostPeer == null) return;
		Bundle b = msg("pause");
		b.put("p", paused);
		s.hostPeer.send(b);
		CoopLog.line(paused ? "game in the background: the host plays our hero" : "game back from the background");
	}

	//whether the player of a party hero is away (their hero rests)
	public static boolean heroAway( int idx ){
		CoopSession s = current;
		if (s == null || !s.started || idx <= 0) return false;
		if (s.isHost()) return idx < s.hostControls.length && s.hostControls[idx];
		boolean[] a = s.roomAway;
		return idx < a.length && a[idx];
	}

	//host, render thread during a game: players who went silent are replaced by the host, and
	//players who are back get the state at the next resync
	private void checkPresence(){
		int size = Math.min(Party.size(), Party.MAX_HEROES);
		boolean changed = false;
		for (int i = 1; i < size; i++){
			Peer p;
			synchronized (seats){
				p = seats[i];
			}
			boolean gone = p == null || !p.present || p.paused || p.silence() > AWAY_AFTER;
			if (gone && !hostControls[i]){
				synchronized (sendLock){
					hostControls[i] = true;
				}
				CoopLog.line("player " + i + " is away: the host plays their hero (rest)");
				notice(i, true);
				changed = true;
			} else if (!gone && hostControls[i] && p.silence() < 3000 && !p.rejoining){
				p.rejoining = true;
				resyncWanted = true;
				CoopLog.line("player " + i + " is back: sending the state");
			}
		}
		if (changed) sendAway();
	}

	// *** starting the game ***  (the scary part)

	//host, render thread: creates a new cooperative run in that slot (or loads the run saved there)
	//and sends it to the guests
	public void hostStart( int saveSlot, boolean resume ){
		slot = saveSlot;
		if (resume) seatsForResume();
		else        seatsForNewGame();
		started = true;
		skipTutorial();
		for (Peer p : activePeers()){
			Bundle st = msg("start");
			st.put("idx", p.seat);
			p.send(st);
		}
		updateRoom();
		LanDiscovery.stopBeacon();
		Random.setCosmeticThread(Game.renderThread);
		GamesInProgress.curSlot = slot;
		Party.localIndexForLoad = 0;
		if (resume){
			InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
		} else {
			Dungeon.deleteGame(slot, true);
			GamesInProgress.selectedClass = myClass;
			Dungeon.hero = null;
			Dungeon.daily = Dungeon.dailyReplay = false;
			Dungeon.initSeed();
			InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		}
		Game.switchScene(InterlevelScene.class);
	}

	//new run: the guests in the room, in order, without gaps
	private void seatsForNewGame(){
		synchronized (seats){
			ArrayList<Peer> here = new ArrayList<>();
			for (int i = 1; i < seats.length; i++){
				if (seats[i] != null && seats[i].present) here.add(seats[i]);
				else if (seats[i] != null) seats[i].seat = -1;
				seats[i] = null;
			}
			for (int i = 0; i < here.size(); i++){
				seats[i+1] = here.get(i);
				here.get(i).seat = i+1;
			}
		}
		synchronized (sendLock){
			for (int i = 0; i < hostControls.length; i++) hostControls[i] = false;
		}
	}

	//saved run: you get your hero back by NAME. don't rename yourself between sessions lol
	private void seatsForResume(){
		String[] saved = null;
		try {
			saved = FileUtils.bundleFromFile(GamesInProgress.gameFile(slot)).getStringArray("coop_names");
		} catch (IOException e){
			//plays like a new party of the same size
		}
		int size = saved == null ? 2 : Math.min(saved.length, Party.MAX_HEROES);
		ArrayList<Peer> extra = new ArrayList<>();
		synchronized (seats){
			ArrayList<Peer> here = new ArrayList<>();
			for (int i = 1; i < seats.length; i++){
				if (seats[i] != null && seats[i].present) here.add(seats[i]);
				seats[i] = null;
			}
			for (int i = 1; i < size && saved != null; i++){
				for (Peer p : here){
					if (saved[i] != null && saved[i].equals(p.name)){
						seats[i] = p;
						here.remove(p);
						break;
					}
				}
			}
			for (int i = 1; i < size; i++){
				if (seats[i] == null && !here.isEmpty()) seats[i] = here.remove(0);
			}
			//players who weren't in this run: a new hero each, after the saved ones (addJoiners)
			joiners.clear();
			for (int i = size; i < seats.length && !here.isEmpty(); i++){
				seats[i] = here.remove(0);
				joiners.add(i);
			}
			extra.addAll(here);
			for (int i = 1; i < seats.length; i++){
				if (seats[i] != null) seats[i].seat = i;
			}
		}
		for (Peer p : extra){
			p.seat = -1;
			refuse(p, "full");
		}
		synchronized (sendLock){
			for (int i = 0; i < hostControls.length; i++) hostControls[i] = i > 0 && i < size && seats[i] == null;
		}
		roomAway = hostControls.clone();
	}

	private void guestStart(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				started = true;
				skipTutorial();
				Random.setCosmeticThread(Game.renderThread);
				GamesInProgress.curSlot = slot;
				Party.localIndexForLoad = myIdx;
				awaitingSync = true;
				InterlevelScene.mode = InterlevelScene.Mode.COOP_SYNC;
				Game.switchScene(InterlevelScene.class);
			}
		});
	}

	//tutorial mode changes the rules on that device only (hunger, hidden doors...) = instant desync.
	//took me a whole evening to find this one
	private static void skipTutorial(){
		if (SPDSettings.intro()){
			SPDSettings.intro(false);
		}
	}

	//host, loading thread of a new run: adds the guests' heroes to the party
	public void setupNewParty(){
		ArrayList<Hero> heroes = new ArrayList<>();
		heroes.add(Dungeon.hero);
		ArrayList<String> names = new ArrayList<>();
		names.add(myName);
		//initHero fills the quick slots, but these belong to this device's hero
		Item[] mySlots = new Item[QuickSlot.SIZE];
		for (int i = 0; i < mySlots.length; i++) mySlots[i] = Dungeon.quickslot.getItem(i);
		for (int i = 1; i < Party.MAX_HEROES; i++){
			Peer p;
			synchronized (seats){
				p = seats[i];
			}
			if (p == null) break;
			final Hero partner = new Hero();
			partner.live();
			HeroClass c;
			try {
				c = HeroClass.valueOf(p.cls);
			} catch (Exception e){
				c = HeroClass.WARRIOR;
			}
			final HeroClass cls = c;
			//with the partner in context, so its starting items go to its own backpack
			Party.withHero(partner, new Runnable() {
				@Override
				public void run() {
					cls.initHero(partner);
				}
			});
			heroes.add(partner);
			names.add(p.name != null ? p.name : "?");
		}
		for (int i = 0; i < mySlots.length; i++){
			if (mySlots[i] != null) Dungeon.quickslot.setSlot(i, mySlots[i]);
			else                    Dungeon.quickslot.clearSlot(i);
		}
		Party.setup(heroes, names, 0);
		CoopAuto.prepareTestHeroes(heroes);
		CoopAuto.prepareTestDepth();
		CoopAuto.prepareTestDoom(heroes);
		//pure random is shared from here on, so seed it. ojo
		Random.setBaseState(Random.Long(false));
	}

	//host, loading thread of a resumed run: friendly fire as the room says now, and a new hero for
	//every player who wasn't in the run (they come after the saved heroes, see seatsForResume)
	public void prepareResumed(){
		Dungeon.friendlyFire = SPDSettings.coopFriendlyFire();
		if (joiners.isEmpty()) return;
		ArrayList<Hero> heroes = new ArrayList<>(Party.heroes());
		ArrayList<String> names = new ArrayList<>(Party.names());
		ArrayList<Hero> saved = new ArrayList<>(heroes);
		Item[] mySlots = new Item[QuickSlot.SIZE];
		for (int i = 0; i < mySlots.length; i++) mySlots[i] = Dungeon.quickslot.getItem(i);
		for (int idx : joiners){
			Peer p;
			synchronized (seats){
				p = seats[idx];
			}
			if (p == null || heroes.size() != idx) continue;
			final Hero partner = new Hero();
			partner.live();
			HeroClass c;
			try {
				c = HeroClass.valueOf(p.cls);
			} catch (Exception e){
				c = HeroClass.WARRIOR;
			}
			final HeroClass cls = c;
			Party.withHero(partner, new Runnable() {
				@Override
				public void run() {
					cls.initHero(partner);
				}
			});
			CoopRewards.catchUp(partner, saved);
			heroes.add(partner);
			names.add(p.name != null ? p.name : "?");
			CoopLog.line("new hero " + idx + " for " + p.name + ": " + cls + " lvl " + partner.lvl);
		}
		for (int i = 0; i < mySlots.length; i++){
			if (mySlots[i] != null) Dungeon.quickslot.setSlot(i, mySlots[i]);
			else                    Dungeon.quickslot.clearSlot(i);
		}
		Party.setup(heroes, names, 0);
		joiners.clear();
	}

	// sharing the state

	//host: after a floor change/resync, send the save around and reload it ourselves too
	//(yes the host reloads its own save, it's simpler than proving it didn't drift)
	public void hostShareState() throws IOException {
		Dungeon.saveAll();
		Bundle s = msg("sync");
		s.put("depth", Dungeon.depth);
		s.put("branch", Dungeon.branch);
		s.put("mode", InterlevelScene.mode == null ? "COOP_SYNC" : InterlevelScene.mode.name());
		s.put("game", encode(GamesInProgress.gameFile(slot)));
		s.put("level", encode(GamesInProgress.depthFile(slot, Dungeon.depth, Dungeon.branch)));
		boolean backNow = false;
		synchronized (sendLock){
			epoch++;
			s.put("e", epoch);
			//players who came back take their hero again from this state on
			for (int i = 1; i < hostControls.length; i++){
				Peer p;
				synchronized (seats){
					p = seats[i];
				}
				if (p != null && p.present && !p.paused && p.silence() < AWAY_AFTER){
					if (hostControls[i]){
						hostControls[i] = false;
						CoopLog.line("player " + i + " is back: plays their hero again");
						notice(i, false);
						backNow = true;
					}
					p.rejoining = false;
				}
			}
			resetTurnState();
			String text = s.toString();
			for (Peer p : activePeers()) p.channel.send(text);
		}
		if (backNow) sendAway();
		CoopLog.line("sync out e=" + epoch + " depth=" + Dungeon.depth + " guests=" + activePeers().size());
		restoreFromSlot();
	}

	private void receiveSync( Bundle b ){
		pendingSync = b;
		awaitingSync = true;
		//mid game: drop everything and load the host's state. if a loading screen is already up it
		//picks it up, otherwise pump() opens one. fiddly, don't touch unless you have to
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (!(Game.scene() instanceof InterlevelScene)){
					InterlevelScene.mode = InterlevelScene.Mode.COOP_SYNC;
					Game.switchScene(InterlevelScene.class);
				}
			}
		});
	}

	//guest, loading thread: waits for the host's state and loads it
	public void guestLoadState() throws IOException {
		long start = System.currentTimeMillis();
		long lastAsk = start;
		while (pendingSync == null){
			if (current != this) throw new IOException("session closed");
			try { Thread.sleep(50); } catch (InterruptedException e) { throw new IOException("interrupted"); }
			long now = System.currentTimeMillis();
			sendStatus(-1, true);
			//the host may not know we wait (e.g. the copies disagreed about a floor change):
			//ask for its state now and then
			if (now - lastAsk > 8000){
				lastAsk = now;
				Bundle r = msg("rsq");
				r.put("e", epoch);
				sendAll(r);
				CoopLog.line("still waiting for the host's state, asking again");
			}
			if (now - start > 10*60*1000) throw new IOException("no state from host");
			//not even keep-alives. host is dead jim
			if (now - start > 30000 && hostPeer.silence() > 30000){
				problem = Messages.get(CoopSession.class, "host_gone");
				throw new IOException("host not responding");
			}
		}
		Bundle s = pendingSync;
		pendingSync = null;
		int depth = s.getInt("depth");
		int branch = s.getInt("branch");
		decode(GamesInProgress.gameFile(slot), s.getString("game"));
		decode(GamesInProgress.depthFile(slot, depth, branch), s.getString("level"));
		GamesInProgress.setUnknown(slot);
		synchronized (inbox){
			epoch = s.getInt("e");
			resetTurnState();
			//events of this epoch which arrived while the state was on its way
			for (Bundle ev : early){
				if (ev.getInt("e") == epoch) acceptEvent(ev);
			}
			early.clear();
		}
		awaitingSync = pendingSync != null;
		CoopLog.line("sync in e=" + epoch + " depth=" + depth);
		restoreFromSlot();
		//arrive the same way the host did (descending, returning...), for messages and effects
		try {
			InterlevelScene.mode = InterlevelScene.Mode.valueOf(s.getString("mode"));
		} catch (Exception e){
			InterlevelScene.mode = InterlevelScene.Mode.COOP_SYNC;
		}
	}

	private void restoreFromSlot() throws IOException {
		String[] mySlots = CoopQuickslots.capture();
		Mob.clearHeldAllies();
		Party.localIndexForLoad = myIndex();
		Dungeon.loadGame(slot);
		Level level = Dungeon.loadLevel(slot);
		Dungeon.switchLevel(level, Dungeon.hero.pos);
		CoopQuickslots.apply(mySlots);
		if (CoopLog.enabled()){
			for (Hero h : Party.heroes()) CoopLog.line("hero " + Party.indexOf(h) + " " + h.heroClass + " lvl=" + h.lvl
					+ " down=" + h.coopDown + " talents=" + talentSummary(h));
		}
	}

	private void resetTurnState(){
		synchronized (inbox){
			for (ArrayDeque<Bundle> q : inbox) q.clear();
			for (int i = 0; i < turnNo.length; i++){
				turnNo[i] = 0;
				granted[i] = 0;
				aheadSums.get(i).clear();
				nextSeq[i] = 1;
				heldEvents.get(i).clear();
			}
			mySeq = 0;
		}
		queuedCell = -1;
		queuedTarget = null;
		synchronized (ownPrompts) {
			for (int i = 0; i < promptSeq.length; i++) promptSeq[i] = 0;
			ownPrompts.clear();
			for (HashMap<Integer, Object> m : remotePrompts) m.clear();
		}
		localCancel = false;
		waitingFor = null;
		resyncWanted = false;
		pausedForResync = false;
	}

	private static String encode( String file ) throws IOException {
		byte[] data = FileUtils.getFileHandle(file).readBytes();
		return new String(Base64Coder.encode(data));
	}

	private static void decode( String file, String data ){
		FileUtils.getFileHandle(file).writeBytes(Base64Coder.decode(data), false);
	}

	// *** turns ***

	//Called first thing in Hero.act(). Returns true if the hero must wait (act returns false).
	public static boolean holdHeroTurn( Hero h ){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active()) return false;
		return s.heroTurn(h);
	}

	private boolean heroTurn( Hero h ){
		int idx = Party.indexOf(h);
		if (idx < 0) return false;
		if (Party.isLocal(h)){
			ownTurn(h, idx);
			return false;
		}
		if (awaitingSync) {
			waitingFor = h;
			return true;
		}
		Bundle head;
		Bundle start = null;
		boolean ahead = false;
		int n;
		ArrayList<Bundle> stale = new ArrayList<>();
		synchronized (inbox){
			ArrayDeque<Bundle> q = inbox.get(idx);
			n = turnNo[idx] + 1;
			//turn starts of turns already played ahead: only their fingerprint is left to check
			while ((head = q.peek()) != null && isTurnStart(head) && head.getInt("n") < n){
				stale.add(q.poll());
			}
			if (head != null && isTurnStart(head) && head.getInt("n") == n){
				start = q.poll();
			} else if (n <= granted[idx]){
				//its device promised not to stop the hero before this turn (it may not even send its
				//start): no need to wait for it. Orders already here are for the first moment the hero
				//waits for one, as on that device (a walk can end in the middle of a turn whose start
				//was not sent).
				ahead = true;
			} else if (isHost() && hostControls[idx]){
				//player away: what they sent before leaving went to everyone and is played first,
				//after that the host decides for their hero
				q.clear();
			}
			if (start != null || ahead) turnNo[idx] = n;
		}
		for (Bundle a : stale) checkAhead(idx, a);
		if (start == null && !ahead){
			if (isHost() && hostControls[idx]){
				synchronized (inbox){
					turnNo[idx] = n;
				}
				announceTurn(h, idx, n, false);
				return false;
			}
			if (head != null && isTurnStart(head) && head.getInt("n") > n){
				//a later turn than the next one: some turn start was lost, the copies can't agree any more
				CoopLog.line("turn start " + head.getInt("n") + " of hero " + idx + " while expecting " + n);
				reportMismatch("turn gap");
			}
			waitingFor = h;
			return true;
		}
		long mine = checksum();
		if (ahead){
			synchronized (inbox){
				aheadSums.get(idx).put(n, mine);
			}
			if (CoopLog.enabled()) CoopLog.line("A ahd h=" + idx + " n=" + n + " t=" + Actor.now() + " mine=" + mine);
			return false;
		}
		if (start.getBoolean("x")){
			h.curAction = null;
			h.resting = false;
		}
		CoopLog.line("A in  h=" + idx + " t=" + Actor.now() + " n=" + n + " c=" + start.getLong("c") + " mine=" + mine
				+ (start.getLong("c") != mine ? " DESYNC" : ""));
		if (CoopLog.enabled()) CoopLog.line("  " + checksumDetail());
		if (start.getLong("c") != mine){
			desyncDetected();
		}
		return false;
	}

	private static boolean isTurnStart( Bundle b ){
		return "A".equals(b.getString("k"));
	}

	//the owner's turn start of a turn this device already played ahead: same fingerprint?
	private void checkAhead( int idx, Bundle a ){
		Long mine;
		synchronized (inbox){
			mine = aheadSums.get(idx).remove(a.getInt("n"));
		}
		if (mine == null) return;
		long theirs = a.getLong("c");
		if (CoopLog.enabled() || theirs != mine){
			CoopLog.line("A in  h=" + idx + " n=" + a.getInt("n") + " (played ahead) c=" + theirs + " mine=" + mine
					+ (theirs != mine ? " DESYNC" : ""));
		}
		if (theirs != mine) desyncDetected();
	}

	//turns of its own hero (or of a player away, on the host) that a device lets the others play
	//without waiting for it. 0 makes every device wait for every turn start.
	private int lookahead(){
		return Integer.getInteger("spd.coop.ahead", net == Net.LAN ? 3 : 6);
	}

	//actor thread, the start of a turn of this device's hero
	private void ownTurn( Hero h, int idx ){
		//the hero has nothing to stop any more (it got where it was going): the stop is moot
		boolean idle = h.curAction == null && !h.resting;
		if (idle) localCancel = false;
		int n;
		boolean cancel;
		synchronized (inbox){
			n = ++turnNo[idx];
			//a stop can only happen on a turn the others weren't told to play ahead
			cancel = localCancel && n > granted[idx];
		}
		if (cancel){
			localCancel = false;
			h.curAction = null;
			h.resting = false;
		}
		announceTurn(h, idx, n, cancel);
	}

	//the start of turn n of a hero this device decides for, with the fingerprint of the state and how far
	//the others may play the hero without waiting for this device: up to there it won't be stopped.
	//While the hero goes on with what it does (walking, resting), turn starts are only sent now and then.
	private void announceTurn( Hero h, int idx, int n, boolean cancel ){
		int k = lookahead();
		int g;
		boolean send;
		synchronized (inbox){
			boolean stopWaits = idx == myIndex() && localCancel;
			g = stopWaits ? granted[idx] : Math.max(granted[idx], n + k);
			boolean goesOn = h.curAction != null || h.resting;
			send = cancel || !goesOn || k == 0 || g - granted[idx] >= 2 || n > granted[idx];
			if (send) granted[idx] = g;
		}
		long c = checksum();
		if (send){
			Bundle a = event("A", idx);
			a.put("n", n);
			a.put("g", g);
			a.put("c", c);
			if (cancel) a.put("x", true);
			sendAll(a);
		}
		CoopLog.line("A out h=" + idx + " t=" + Actor.now() + " n=" + n + " g=" + g + " c=" + c + (cancel ? " x" : "") + (send ? "" : " (not sent)"));
		if (CoopLog.enabled()) CoopLog.line("  " + checksumDetail());
	}

	//a turn start that arrived: how far its device lets us play its hero
	private void noteGrant( Bundle b ){
		if (!isTurnStart(b) || !b.contains("g")) return;
		int h = b.getInt("h");
		if (h >= 0 && h < granted.length) granted[h] = Math.max(granted[h], b.getInt("g"));
	}

	private Bundle event( String kind, int heroIdx ){
		Bundle b = msg("ev");
		b.put("e", epoch);
		b.put("h", heroIdx);
		b.put("k", kind);
		return b;
	}

	private void queueEvent( Bundle b ){
		int h = b.getInt("h");
		if (h < 0 || h >= inbox.size() || h == myIndex()) return;
		//straight from another guest (see Rooms.partyTopic): not for a hero the host plays meanwhile
		if (b.getBoolean("f") && heroAway(h)) return;
		synchronized (inbox){
			int e = b.getInt("e");
			if (e == epoch){
				acceptEvent(b);
			} else if (e > epoch && !isHost()){
				//the host already plays on the state it just sent, which is still loading here
				early.add(b);
			}
		}
	}

	//inbox lock held: an event of this epoch goes to its hero's inbox. A guest's events come twice to the
	//other guests (straight and through the host), numbered: each one is taken once, in order.
	private void acceptEvent( Bundle b ){
		int h = b.getInt("h");
		if (!b.contains("s")){
			inbox.get(h).add(b);
			noteGrant(b);
			return;
		}
		int sq = b.getInt("s");
		if (sq < nextSeq[h]) return;
		if (sq > nextSeq[h]){
			//one before it is still on its way
			heldEvents.get(h).put(sq, b);
			return;
		}
		inbox.get(h).add(b);
		noteGrant(b);
		nextSeq[h]++;
		Bundle next;
		while ((next = heldEvents.get(h).remove(nextSeq[h])) != null){
			inbox.get(h).add(next);
			noteGrant(next);
			nextSeq[h]++;
		}
		while (!heldEvents.get(h).isEmpty() && heldEvents.get(h).firstKey() < nextSeq[h]){
			heldEvents.get(h).pollFirstEntry();
		}
	}

	//host: an order of a guest. Played here and passed on to the other guests, unless the guest
	//sends for a hero that isn't theirs, or the host already plays that hero in their absence.
	private void relayEvent( Peer from, Bundle b, String text ){
		int h = b.getInt("h");
		if (!started || from.seat <= 0 || h != from.seat) return;
		synchronized (sendLock){
			if (hostControls[h] || b.getInt("e") != epoch) return;
			queueEvent(b);
			for (Peer p : activePeers()){
				if (p != from) p.channel.send(text);
			}
		}
	}

	//guests tell the host what they wait for, so a situation where devices wait for each other
	//(the copies disagree about whose turn it is, or about a floor change) can be noticed and fixed
	private void sendStatus( int waitsFor, boolean awaiting ){
		if (isHost()) return;
		long now = System.currentTimeMillis();
		if (now - lastStatusSent < 2000) return;
		lastStatusSent = now;
		Bundle st = msg("st");
		st.put("e", epoch);
		st.put("w", waitsFor);
		st.put("s", awaiting);
		sendAll(st);
	}

	//host, render thread: if devices wait for each other, send the state again
	private void checkDeadlock(){
		Actor cur = Actor.current();
		Hero waited = null;
		if (cur instanceof Hero && Party.isRemote((Hero) cur) && Actor.idle() && (waitingFor == cur || ((Hero) cur).ready)){
			int idx = Party.indexOf((Hero) cur);
			if (!hostControls[idx]){
				synchronized (inbox){
					if (inbox.get(idx).isEmpty()) waited = (Hero) cur;
				}
			}
		}
		long now = System.currentTimeMillis();
		if (waited != waitingSinceFor){
			waitingSinceFor = waited;
			waitingSince = now;
		}
		if (waited != null && now - waitingSince >= 6000){
			int idx = Party.indexOf(waited);
			Peer p;
			synchronized (seats){
				p = seats[idx];
			}
			//the player of the hero we wait for waits for something else: the copies disagree
			boolean stuck = p != null && now - p.statusTime < 5000 && p.epoch == epoch
					&& (p.awaitsSync || (p.waitsFor >= 0 && p.waitsFor != idx));
			if (stuck && !resyncWanted){
				CoopLog.line("devices wait for each other (" + p.label() + " waits for " + p.waitsFor
						+ (p.awaitsSync ? ", state" : "") + "): sending the state again");
				resyncWanted = true;
				waitingSince = now;
			}
		}
		//a guest that waits for the same hero for long while the host is past that turn is stuck too
		int curIdx = cur instanceof Hero ? Party.indexOf((Hero) cur) : -1;
		for (Peer p : activePeers()){
			int what = p.waitsFor;
			if (p.epoch != epoch || what < 0 || now - p.statusTime > 5000 || what == curIdx){
				p.waitWhat = -2;
				continue;
			}
			if (what != p.waitWhat){
				p.waitWhat = what;
				p.waitSince = now;
			} else if (now - p.waitSince > 20000 && !resyncWanted){
				CoopLog.line(p.label() + " waits for hero " + what + " for too long: sending the state again");
				resyncWanted = true;
				p.waitSince = now;
			}
		}
	}

	//TODO the full resync after a desync is heavy (whole save). resending just the floor would do, someday
	private void desyncDetected(){
		CoopLog.dumpRandomTrace();
		//CoopLog.line("desync dbg epoch=" + epoch + " mine=" + myIndex());
		desyncs++;
		CoopLog.line("desync #" + desyncs + (problem != null ? " (" + problem + ")" : ""));
		if (isHost()){
			resyncWanted = true;
		} else if (!awaitingSync){
			awaitingSync = true;
			sendAll(msg("rsq"));
		}
	}

	//readable version of the fingerprint, for the test log
	public static String checksumDetail(){
		StringBuilder sb = new StringBuilder();
		sb.append("now=").append(Actor.now()).append(" rng=").append(Random.baseState())
				.append(" stack=").append(Random.stackSize())
				.append(" gold=").append(Dungeon.gold).append(" depth=").append(Dungeon.depth);
		for (Hero h : Party.heroes()){
			sb.append(" H[").append(h.pos).append(",").append(h.HP).append("/").append(h.HT)
					.append(",x").append(h.exp).append(",l").append(h.lvl).append(",t").append(talentHash(h)).append("]");
		}
		if (Dungeon.level != null){
			sb.append(" mobs:");
			for (Mob m : Dungeon.level.mobs){
				sb.append(" ").append(m.getClass().getSimpleName()).append("#").append(m.id())
						.append("@").append(m.pos).append(":").append(m.HP);
			}
			sb.append(" heaps=").append(Dungeon.level.heaps.size);
			//test log only: things that are not in the fingerprint but steer the game (vision, paths)
			sb.append(" fov=").append(java.util.Arrays.hashCode(Dungeon.level.heroFOV))
					.append(" vis=").append(java.util.Arrays.hashCode(Dungeon.level.visited))
					.append(" map=").append(java.util.Arrays.hashCode(Dungeon.level.mapped));
			for (Hero h : Party.heroes()){
				sb.append(" path").append(Party.indexOf(h)).append("=").append(h.debugPath());
			}
		}
		return sb.toString();
	}

	//talents and armor ability of a hero, in the fingerprint
	private static int talentHash( Hero h ){
		int x = h.armorAbility == null ? 0 : h.armorAbility.getClass().getSimpleName().hashCode();
		for (java.util.LinkedHashMap<Talent, Integer> tier : h.talents){
			for (Talent t : tier.keySet()){
				x = x*31 + t.ordinal()*7 + tier.get(t);
			}
		}
		return x;
	}

	static String talentSummary( Hero h ){
		StringBuilder sb = new StringBuilder();
		int tierNo = 1;
		for (java.util.LinkedHashMap<Talent, Integer> tier : h.talents){
			for (Talent t : tier.keySet()){
				if (tier.get(t) > 0) sb.append(tierNo).append(":").append(t.name()).append("=").append(tier.get(t)).append(" ");
			}
			tierNo++;
		}
		if (h.armorAbility != null) sb.append("ability=").append(h.armorAbility.getClass().getSimpleName());
		return sb.toString().trim();
	}

	//compact fingerprint of the state, compared at every hero turn
	public static long checksum(){
		long x = Float.floatToIntBits(Actor.now());
		x = x*31 + Random.baseState();
		x = x*31 + Dungeon.gold;
		x = x*31 + Dungeon.depth;
		for (Hero h : Party.heroes()){
			x = x*31 + h.pos;
			x = x*31 + h.HP;
			x = x*31 + h.HT;
			x = x*31 + h.exp;
			x = x*31 + h.lvl;
			x = x*31 + talentHash(h);
		}
		if (Dungeon.level != null){
			for (Mob m : Dungeon.level.mobs){
				x = x*31 + m.pos;
				x = x*31 + m.HP;
			}
			x = x*31 + Dungeon.level.heaps.size;
		}
		return x;
	}

	//render thread, every frame of the game scene
	public static void update(){
		CoopSession s = current;
		if (s == null || !s.started) return;
		s.pump();
	}

	private void pump(){
		if (Game.switchingScene()) return;
		//guest: a state arrived after the last loading screen had already loaded the previous one
		if (!isHost() && pendingSync != null && !(Game.scene() instanceof InterlevelScene)){
			awaitingSync = true;
			CoopLog.line("state arrived after loading, loading it");
			InterlevelScene.mode = InterlevelScene.Mode.COOP_SYNC;
			Game.switchScene(InterlevelScene.class);
			return;
		}
		if (isHost()) checkPresence();
		if (isHost() && resyncWanted && !pausedForResync && Actor.idle()){
			Actor cur = Actor.current();
			if (cur instanceof Hero && (((Hero) cur).ready || waitingFor == cur)){
				resyncWanted = false;
				pausedForResync = true;
			}
		}
		if (pausedForResync){
			pausedForResync = false;
			InterlevelScene.mode = InterlevelScene.Mode.COOP_SYNC;
			Game.switchScene(InterlevelScene.class);
			return;
		}
		if (awaitingSync){
			sendStatus(-1, true);
			long now = System.currentTimeMillis();
			if (awaitingSince == 0) awaitingSince = now;
			//a request for the state can get lost with the link, or arrive while the host was busy
			if (!isHost() && now - awaitingSince > 8000 && now - lastRsq > 8000){
				lastRsq = now;
				Bundle r = msg("rsq");
				r.put("e", epoch);
				sendAll(r);
				CoopLog.line("still waiting for the host's state, asking again");
			}
			return;
		}
		awaitingSince = 0;
		Actor cur = Actor.current();
		int waits = -1;
		if (cur instanceof Hero && Party.isRemote((Hero) cur) && Actor.idle()) waits = Party.indexOf((Hero) cur);
		sendStatus(waits, false);
		if (isHost()) checkDeadlock();
		//talent upgrades chosen while another hero had the turn
		if (localInputAllowed()) CoopOrders.flushPendingTalents();
		//a tap made while another hero had the turn
		playQueuedTap();
		for (Hero h : Party.heroes()){
			if (Party.isLocal(h)) continue;
			int idx = Party.indexOf(h);
			for (int guard = 0; guard < 64; guard++){
				Bundle head;
				Bundle stale = null;
				boolean nextKnown;
				synchronized (inbox){
					head = inbox.get(idx).peek();
					if (head != null && isTurnStart(head) && head.getInt("n") <= turnNo[idx]){
						stale = inbox.get(idx).poll();
					}
					nextKnown = turnNo[idx] + 1 <= granted[idx] || (head != null && isTurnStart(head));
				}
				if (stale != null){
					//a turn already played ahead here
					checkAhead(idx, stale);
					continue;
				}
				boolean idleHere = Actor.current() == h && Actor.idle();
				//the actor thread waits for the hero's next turn, which can be played now
				if (idleHere && waitingFor == h && nextKnown){
					waitingFor = null;
					h.next();
					break;
				}
				if (head == null) break;
				if (isTurnStart(head)){
					//the hero's next turn has started on its device while it waits for orders here:
					//that device restarted the turn (e.g. it came back from the alchemy screen), so here too
					if (idleHere && h.ready){
						h.next();
					}
					break;
				}
				if (idleHere && h.ready){
					//answers to a window or cell selection: the game may still be about to open it
					//(prompts from the turn thread are posted to the next frame)
					if (head.contains("q") && !promptExists(idx, head.getInt("q"))){
						long now = System.currentTimeMillis();
						if (promptWaitStart == 0) promptWaitStart = now;
						if (now - promptWaitStart < 3000) break;
					}
					promptWaitStart = 0;
					synchronized (inbox){
						inbox.get(idx).poll();
					}
					applyRemote(h, head);
				} else {
					break;
				}
			}
			if (isHost() && hostControls[idx]) actForAway(h, idx);
		}
	}

	//host: the turn of a hero whose player is away. It rests, decided here and sent to everyone
	//as a normal order, so every device does the same.
	private void actForAway( final Hero h, int idx ){
		if (Actor.current() != h || !Actor.idle()) return;
		synchronized (inbox){
			if (!inbox.get(idx).isEmpty()) return;
		}
		if (waitingFor == h){
			//it was waiting for the player's turn start: the host starts the turn instead
			waitingFor = null;
			h.next();
		} else if (h.ready){
			Bundle o = order("M");
			o.put("m", "rest");
			o.put("e", epoch);
			o.put("h", idx);
			sendAll(o);
			runAs(h, new Runnable() {
				@Override
				public void run() {
					CoopOrders.perform(h, "rest");
				}
			});
		}
	}

	private boolean promptExists( int heroIdx, int seq ){
		synchronized (ownPrompts){
			return remotePrompts.get(heroIdx).containsKey(seq);
		}
	}

	//the actor thread asks before each actor acts; returning false pauses processing
	private boolean beforeAct( Actor acting ){
		if (isHost() && resyncWanted && acting instanceof Hero && !pausedForResync){
			resyncWanted = false;
			pausedForResync = true;
			return false;
		}
		return !pausedForResync;
	}

	// *** orders from this device ***

	//true when the local player may give orders to their hero
	public static boolean localInputAllowed(){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active()) return true;
		Hero me = Dungeon.local();
		return !s.awaitingSync && !s.pausedForResync && Actor.current() == me && Actor.idle() && me.ready;
	}

	//runs an order of the local player and sends it to the other device.
	//kind/fields describe the order, action performs it locally.
	public static void local( Bundle order, Runnable action ){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active()){
			action.run();
			return;
		}
		if (!localInputAllowed()) return;
		s.sendOrder(order);
		s.runAs(Dungeon.local(), action);
	}

	//like local(), for screens that only open during the local hero's turn (alchemy),
	//where the turn processing is paused and the usual check can't be made
	public static void localUnchecked( Bundle order, Runnable action ){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active()){
			action.run();
			return;
		}
		s.sendOrder(order);
		s.runAs(Dungeon.local(), action);
	}

	//an order of the local player, fields are added by the caller
	public static Bundle order( String kind ){
		Bundle b = msg("ev");
		b.put("k", kind);
		return b;
	}

	private void sendOrder( Bundle order ){
		order.put("e", epoch);
		order.put("h", myIndex());
		CoopLog.line("order " + order.toString());
		sendAll(order);
	}

	private void runAs( Hero h, Runnable action ){
		Hero prevOwner = renderOwner;
		Hero prevHero = Dungeon.hero;
		renderOwner = h;
		Dungeon.hero = h;
		Random.beginGameplay();
		try {
			action.run();
		} finally {
			Random.endGameplay();
			Dungeon.hero = prevHero;
			renderOwner = prevOwner;
		}
	}

	//async cancel from the interface (e.g. tapping while walking): applied at the next turn start
	public static boolean deferCancel(){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active()) return false;
		Hero me = Dungeon.local();
		if (me.curAction != null || me.resting){
			s.localCancel = true;
			return true;
		}
		return false;
	}

	// *** a tap while another hero has the turn ***

	//render thread: a tap on the map that can't be played now because another hero has the turn.
	//It is kept (the last one counts) and played as soon as this player's turn starts, so players
	//decide while the others play instead of one after the other. A tapped enemy is followed if it moves.
	public static boolean queueTap( int cell ){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active() || s.awaitingSync || cell < 0) return false;
		if (localInputAllowed() || GameScene.isSelectingCell() || GameScene.interfaceBlockingHero()) return false;
		Hero me = Dungeon.local();
		//a walking or resting hero is stopped by a tap instead, as usual
		if (me == null || !me.isAlive() || me.coopDown || me.curAction != null || me.resting) return false;
		if (Dungeon.level == null || cell >= Dungeon.level.length()) return false;
		Mob target = null;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.pos == cell && m.alignment == Char.Alignment.ENEMY) target = m;
		}
		s.queuedTarget = target;
		s.queuedCell = cell;
		CoopLog.line("tap kept for this player's turn: " + cell + (target != null ? " (enemy)" : ""));
		return true;
	}

	//whether a tap waits for this player's turn (the interface may show it)
	public static boolean tapQueued(){
		CoopSession s = current;
		return s != null && s.queuedCell >= 0;
	}

	public static void dropQueuedTap(){
		CoopSession s = current;
		if (s != null) s.queuedCell = -1;
	}

	//render thread, every frame: plays the kept tap when the turn comes, and shows where it goes meanwhile
	private void playQueuedTap(){
		int cell = queuedCell;
		Hero me = Dungeon.local();
		if (cell >= 0 && (me == null || !me.isAlive() || me.coopDown || me.curAction != null || me.resting
				|| GameScene.isSelectingCell() || Dungeon.level == null || cell >= Dungeon.level.length())){
			//the hero is busy with something else now, or the floor changed
			queuedCell = cell = -1;
		}
		Mob target = queuedTarget;
		if (cell >= 0 && target != null && target.isAlive() && Dungeon.level.heroFOV[target.pos]){
			cell = target.pos;
		}
		if (cell >= 0 && localInputAllowed() && !GameScene.interfaceBlockingHero()){
			queuedCell = -1;
			queuedTarget = null;
			CoopLog.line("kept tap played: " + cell);
			CoopOrders.handleCell(cell);
			GameScene.ready();
			cell = -1;
		}
		showQueuedMark(cell);
	}

	private void showQueuedMark( int cell ){
		if (cell < 0){
			if (queuedMark != null){
				if (queuedMarkScene == Game.scene()) queuedMark.killAndErase();
				queuedMark = null;
			}
			return;
		}
		if (queuedMark == null || queuedMark.parent == null || queuedMarkScene != Game.scene()){
			queuedMark = new Image(Icons.get(
					Icons.TARGET));
			queuedMark.hardlight(0x44DDFF);
			queuedMarkScene = Game.scene();
			GameScene.effectOverFog(queuedMark);
		}
		PointF p = DungeonTilemap.tileToWorld(cell);
		queuedMark.x = p.x + (DungeonTilemap.SIZE - queuedMark.width) / 2f;
		queuedMark.y = p.y + (DungeonTilemap.SIZE - queuedMark.height) / 2f;
		queuedMark.alpha(0.55f + 0.35f * (float) Math.sin(System.currentTimeMillis() / 160.0));
	}

	// *** orders from the other device ***

	private void applyRemote( final Hero h, final Bundle ev ){
		CoopLog.line("apply " + ev.toString());
		runAs(h, new Runnable() {
			@Override
			public void run() {
				CoopOrders.apply(CoopSession.this, h, ev);
			}
		});
	}

	// *** prompts ***

	//who a newly created prompt belongs to, null if it is plain interface (not game logic)
	private Hero promptOwner(){
		if (Thread.currentThread() != Game.renderThread){
			//only the turn thread and floor loading run game logic, not e.g. the network threads
			if (Actor.onProcessThread() || InterlevelScene.onLoadingThread()) return Dungeon.hero;
			return null;
		}
		if (renderOwner != null) return renderOwner;
		if (Random.inGameplay()) return Dungeon.hero;
		return null;
	}

	//hero the running game logic is about, null for plain interface code
	public static Hero contextHero(){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active()) return null;
		return s.promptOwner();
	}

	//true when game logic runs for another player's hero on this device
	public static boolean remoteContext(){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active()) return false;
		Hero o = s.promptOwner();
		return o != null && Party.isRemote(o);
	}

	//returns true if the prompt belongs to the other player and must not be shown here
	public static boolean promptCreated( Object prompt ){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active()) return false;
		Hero owner = s.promptOwner();
		if (owner == null) return false;
		int idx = Party.indexOf(owner);
		if (idx < 0) return false;
		synchronized (s.ownPrompts) {
			int seq = ++s.promptSeq[idx];
			if (Party.isLocal(owner)) {
				s.ownPrompts.put(prompt, seq);
				return false;
			} else {
				s.remotePrompts.get(idx).put(seq, prompt);
				return true;
			}
		}
	}

	Object peekRemotePrompt( int heroIdx, int seq ){
		synchronized (ownPrompts) {
			return remotePrompts.get(heroIdx).get(seq);
		}
	}

	//a replayed order did not match this device's state: the copies differ
	public static void reportMismatch( String why ){
		CoopSession s = current;
		if (s != null && s.started){
			s.problem = why;
			s.desyncDetected();
		}
	}

	public static void stopResting(){
		CoopSession s = current;
		Hero me = Dungeon.local();
		if (s == null || !s.started || !Party.active()){
			if (me != null) me.resting = false;
			return;
		}
		if (me != null && me.resting) s.localCancel = true;
	}

	//an item selection of the local player
	public static void select( final WndBag.ItemSelector selector, final Item item ){
		itemAnswered(selector, item, new Runnable() {
			@Override
			public void run() {
				selector.onSelect(item);
			}
		});
	}

	Object takeRemotePrompt( int heroIdx, int seq ){
		synchronized (ownPrompts) {
			return remotePrompts.get(heroIdx).remove(seq);
		}
	}

	private Integer takeOwnPrompt( Object prompt ){
		synchronized (ownPrompts){
			return ownPrompts.remove(prompt);
		}
	}

	private Integer ownPrompt( Object prompt ){
		synchronized (ownPrompts){
			return ownPrompts.get(prompt);
		}
	}

	//the local player answered a cell prompt. Returns false if the answer must be ignored for now.
	public static void cellAnswered( CellSelector.Listener listener, Integer cell, Runnable doIt ){
		CoopSession s = current;
		Integer seq = s == null ? null : s.takeOwnPrompt(listener);
		if (seq == null){
			doIt.run();
			return;
		}
		Bundle o = order("C");
		o.put("q", seq);
		o.put("c", cell == null ? -1 : cell);
		s.sendOrder(o);
		s.runAs(Dungeon.local(), doIt);
	}

	public static void itemAnswered( WndBag.ItemSelector selector, Item item, Runnable doIt ){
		CoopSession s = current;
		Integer seq = s == null ? null : s.takeOwnPrompt(selector);
		if (seq == null){
			doIt.run();
			return;
		}
		Bundle o = order("I");
		o.put("q", seq);
		o.put("i", ItemRef.of(Dungeon.local(), item));
		s.sendOrder(o);
		s.runAs(Dungeon.local(), doIt);
	}

	//button inside a prompt window. Returns true if the click was handled (or blocked) here.
	public static boolean buttonClicked( Button b ){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active()) return false;
		Window w = windowOf(b);
		if (w == null) return false;
		final Integer seq = s.ownPrompt(w);
		if (seq == null) return false;
		if (!localInputAllowed() || !w.clickAllowed()) return true;
		int index = buttonsOf(w).indexOf(b);
		if (index < 0) return false;
		Bundle o = order("B");
		o.put("q", seq);
		o.put("b", index);
		s.sendOrder(o);
		final Button btn = b;
		s.runAs(Dungeon.local(), new Runnable() {
			@Override
			public void run() {
				btn.coopClick();
			}
		});
		return true;
	}

	public static boolean windowBack( final Window w ){
		CoopSession s = current;
		if (s == null || !s.started || !Party.active()) return false;
		Integer seq = s.ownPrompt(w);
		if (seq == null) return false;
		if (!localInputAllowed()) return true;
		Bundle o = order("K");
		o.put("q", seq);
		s.sendOrder(o);
		s.runAs(Dungeon.local(), new Runnable() {
			@Override
			public void run() {
				w.onBackPressed();
			}
		});
		return true;
	}

	static Window windowOf( Gizmo g ){
		Group p = g.parent;
		while (p != null){
			if (p instanceof Window) return (Window) p;
			p = p.parent;
		}
		return null;
	}

	//shows a window created by game logic outside the game scene (e.g. in the alchemy screen):
	//windows for the other player's hero are only kept to replay their choices
	public static void showPrompt( Scene scene, Window w ){
		if (promptCreated(w)){
			w.coopDetach();
			return;
		}
		if (scene != null) scene.addToFront(w);
	}

	public static boolean ownsWindow( Window w ){
		CoopSession s = current;
		return s != null && s.ownPrompt(w) != null;
	}

	public static ArrayList<Button> buttonsOfWindow( Window w ){
		return buttonsOf(w);
	}

	static ArrayList<Button> buttonsOf( Group g ){
		ArrayList<Button> result = new ArrayList<>();
		collectButtons(g, result);
		return result;
	}

	private static void collectButtons( Group g, ArrayList<Button> out ){
		for (Gizmo m : g.membersList()){
			if (m == null) continue;
			if (m instanceof Button) out.add((Button) m);
			if (m instanceof Group) collectButtons((Group) m, out);
		}
	}

	// *** hooks ***

	private static void installHooks(){
		Actor.coopHook = new Actor.CoopHook() {
			@Override
			public boolean beforeAct( Actor acting ) {
				CoopSession s = current;
				return s == null || !s.started || s.beforeAct(acting);
			}
		};
		Game.postHook = new Game.PostHook() {
			@Override
			public Callback wrap( final Callback c ) {
				final CoopSession s = current;
				if (s == null || !s.started) return c;
				final Hero owner = s.promptOwner();
				if (owner == null) return c;
				return new Callback() {
					@Override
					public void call() {
						Hero prev = s.renderOwner;
						s.renderOwner = owner;
						try {
							c.call();
						} finally {
							s.renderOwner = prev;
						}
					}
				};
			}
		};
	}

	private static void removeHooks(){
		Actor.coopHook = null;
		Game.postHook = null;
		Random.setCosmeticThread(null);
	}

	// *** information for the interface ***

	//name of the player the game is waiting for, null if it waits for nobody
	public static String waitingForName(){
		CoopSession s = current;
		if (s == null || !s.started) return null;
		if (s.awaitingSync) return Messages.get(CoopSession.class, "syncing");
		Actor cur = Actor.current();
		if (cur instanceof Hero && Party.isRemote((Hero) cur) && Actor.idle()){
			int idx = Party.indexOf((Hero) cur);
			if (s.isHost() && s.hostControls[idx]) return null;
			return Party.name((Hero) cur);
		}
		return null;
	}

	public static String debugState(){
		CoopSession s = current;
		if (s == null) return "no session";
		StringBuilder sb = new StringBuilder();
		sb.append("awaitingSync=").append(s.awaitingSync).append(" paused=").append(s.pausedForResync)
				.append(" waitingFor=").append(s.waitingFor == null ? "-" : Party.name(s.waitingFor));
		synchronized (s.inbox){
			for (int i = 0; i < s.inbox.size(); i++){
				Bundle head = s.inbox.get(i).peek();
				sb.append(" inbox").append(i).append("=").append(s.inbox.get(i).size())
						.append(head == null ? "" : "(" + head.getString("k") + ")");
			}
		}
		synchronized (s.ownPrompts){
			sb.append(" ownPrompts=").append(s.ownPrompts.size());
			for (int i = 0; i < s.remotePrompts.size(); i++){
				sb.append(" remote").append(i).append("=").append(s.remotePrompts.get(i).keySet());
			}
		}
		if (s.isHost()){
			sb.append(" hostControls=").append(java.util.Arrays.toString(s.hostControls));
		}
		return sb.toString();
	}

	//guest: the host has not been heard of for a while
	public static boolean peerSilent(){
		CoopSession s = current;
		return s != null && s.started && s.hostPeer != null && (!s.hostPeer.linkUp || s.hostPeer.silence() > 15000);
	}

	public static void reportProblem( String text ){
		CoopSession s = current;
		if (s != null) s.problem = text;
		ShatteredPixelDungeon.reportException(new RuntimeException("coop: " + text));
	}
}
