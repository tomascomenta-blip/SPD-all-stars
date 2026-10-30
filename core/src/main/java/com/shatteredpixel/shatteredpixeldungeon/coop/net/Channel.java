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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Iterator;

//Reliable, ordered messages on top of a Link that may drop frames or reconnect.
//Frames:  "D <seq> <ack> <more>\n<data>"   data (more=1 means the message continues in the next frame)
//         "A <ack> <sent>"                  acknowledgement, and the last frame sent (a receiver that
//                                           lacks frames up to it asks for them at once)
//         "N <lastReceived>"                gap detected, please resend after this
//         "H <session> <lastReceived>"      sent on every (re)connection
public class Channel implements Link.Listener {

	public interface Listener {
		void onMessage( String message );
		void onChannelState( boolean connected, String info );
		//the other side restarted (a new session id), everything it had is gone
		void onPeerRestarted();
	}

	private static final int CHUNK = 24*1024;

	private final Link link;
	private volatile Listener listener;
	private final String session;

	private long sendSeq = 0;
	private long recvSeq = 0;
	private String peerSession = null;
	private final ArrayDeque<long[]> outSeqs = new ArrayDeque<>();
	private final ArrayDeque<String> outFrames = new ArrayDeque<>();
	private final StringBuilder partial = new StringBuilder();
	private long lastNack = 0;
	private volatile long lastFrameTime = System.currentTimeMillis();
	private volatile boolean closed = false;
	private int unackedReceived = 0;
	private long lastKeepAlive = 0;
	//frames can be lost without anything arriving after them (e.g. the last piece of a level):
	//if the other side stops confirming, they are sent again, waiting longer each time
	private long lastAckProgress = System.currentTimeMillis();
	private long resendDelay = RESEND_START;
	private static final long RESEND_START = 1200;
	private static final long RESEND_MAX = 10000;
	//the wait before sending again follows the time confirmations take (measured on frames sent once),
	//so a lost frame costs little more than a round trip instead of a fixed second and more
	private double srtt = -1;
	private double rttVar = 0;
	private static final long RTO_MIN = 500;
	//received frames are confirmed at most this long after they arrive (if nothing goes back sooner)
	private static final long ACK_DELAY = 60;
	private long unackedSince = 0;
	//frames go out in a limited window, and lost ones are sent again from the first missing one
	//(go-back-N): sending everything at once floods public brokers, which then drop even more
	private static final int WINDOW_FRAMES = 24;
	private static final int WINDOW_CHARS = 160*1024;
	private long highestSent = 0;   //last frame transmitted (at least once)
	private long peerAcked = 0;     //the other side has everything up to here
	//frames that arrived after a gap wait here until the missing ones come (selective repeat):
	//with a lossy link, throwing them away would make everything be sent again and again
	private final TreeMap<Long, String> early = new TreeMap<>();
	private static final int EARLY_MAX = 512;
	//how many missing frames are sent again at once when the other side asks or stops confirming
	private static final int REPAIR = 4;

	//testing: -Dspd.coop.droprate=0.1 loses that fraction of the incoming data frames
	private static final double DROP_RATE = dropRate();
	private static double dropRate(){
		try {
			String p = System.getProperty("spd.coop.droprate");
			return p == null ? 0 : Double.parseDouble(p);
		} catch (Exception e){
			return 0;
		}
	}

	public Channel( Link link ){
		this.link = link;
		this.session = Long.toHexString(Double.doubleToLongBits(Math.random()) ^ System.nanoTime());
		link.setListener(this);
	}

	public void setListener( Listener listener ){
		this.listener = listener;
	}

	//for the test log
	private volatile String name = "";
	public void setName( String name ){
		this.name = " " + name;
	}

	public void start(){
		link.start();
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!closed){
					try { Thread.sleep(25); } catch (InterruptedException e) { return; }
					tick();
				}
			}
		});
		t.setName("SPD coop channel");
		t.setDaemon(true);
		t.start();
	}

	public boolean connected(){
		return link.connected();
	}

	public long millisSinceLastFrame(){
		return System.currentTimeMillis() - lastFrameTime;
	}

	public void send( String message ){
		synchronized (this){
			if (outSeqs.isEmpty()){
				lastAckProgress = System.currentTimeMillis();
				resendDelay = rto();
			}
			int pos = 0;
			do {
				int end = Math.min(message.length(), pos + CHUNK);
				boolean more = end < message.length();
				long seq = ++sendSeq;
				String frame = "D " + seq + " " + recvSeq + " " + (more ? 1 : 0) + "\n" + message.substring(pos, end);
				outSeqs.add(new long[]{seq, 0, 0});
				outFrames.add(frame);
				pos = end;
			} while (pos < message.length());
			unackedReceived = 0;
			pumpOut();
		}
	}

	//transmits the frames not sent yet, as long as the window allows
	private void pumpOut(){
		int inFlight = 0;
		int chars = 0;
		Iterator<long[]> seqs = outSeqs.iterator();
		Iterator<String> frames = outFrames.iterator();
		while (seqs.hasNext()){
			long[] e = seqs.next();
			long s = e[0];
			String f = frames.next();
			if (inFlight >= WINDOW_FRAMES || (chars >= WINDOW_CHARS && inFlight > 0)) break;
			inFlight++;
			chars += f.length();
			if (s > highestSent){
				link.send(f);
				highestSent = s;
				if (e[1] == 0) e[1] = System.currentTimeMillis();
				else           e[2] = 1;
			}
		}
	}

	private synchronized void tick(){
		long now = System.currentTimeMillis();
		//acknowledgements soon after frames arrive, so the other side can forget them (and knows
		//quickly when one got lost), unless something we send carries it first
		if (unackedReceived > 0 && link.connected() && now - unackedSince >= ACK_DELAY){
			link.send("A " + recvSeq + " " + highestSent);
			unackedReceived = 0;
		}
		//keep the connection warm (the other side's 'last heard' time stays fresh)
		if (link.connected() && now - lastKeepAlive > 5000){
			lastKeepAlive = now;
			link.send("A " + recvSeq + " " + highestSent);
		}
		if (!outSeqs.isEmpty() && link.connected() && now - lastAckProgress > resendDelay){
			lastAckProgress = now;
			resendDelay = Math.min(resendDelay*2, RESEND_MAX);
			CoopLog.line("channel" + name + ": no confirmation, sending again from frame "
					+ (peerAcked+1) + " (" + outSeqs.size() + " waiting)");
			resendMissing(peerAcked);
		} else {
			pumpOut();
		}
	}

	@Override
	public void onFrame( String frame ) {
		lastFrameTime = System.currentTimeMillis();
		Listener l = listener;
		ArrayList<String> deliver = new ArrayList<>();
		boolean restarted = false;
		synchronized (this){
			char kind = frame.isEmpty() ? '?' : frame.charAt(0);
			if (kind == 'D' && DROP_RATE > 0 && Math.random() < DROP_RATE){
				CoopLog.line("channel: test drop");
				return;
			}
			if (kind == 'D'){
				int nl = frame.indexOf('\n');
				if (nl < 0) return;
				String[] head = frame.substring(2, nl).split(" ");
				long seq = Long.parseLong(head[0]);
				long ack = Long.parseLong(head[1]);
				acked(ack);
				if (seq == recvSeq + 1){
					accept(frame, deliver);
					//frames that came early can follow now
					while (!early.isEmpty() && early.firstKey() == recvSeq + 1){
						accept(early.pollFirstEntry().getValue(), deliver);
					}
					while (!early.isEmpty() && early.firstKey() <= recvSeq){
						early.pollFirstEntry();
					}
				} else if (seq > recvSeq + 1){
					//something got lost on the way: keep this one and ask for the missing ones (not too often)
					if (early.size() < EARLY_MAX) early.put(seq, frame);
					long now = System.currentTimeMillis();
					if (now - lastNack > 150){
						lastNack = now;
						CoopLog.line("channel" + name + ": gap, have " + recvSeq + " got " + seq);
						link.send("N " + recvSeq);
					}
				}
				//seq <= recvSeq is a duplicate, ignore it
			} else if (kind == 'A'){
				String[] parts = frame.substring(2).trim().split(" ");
				acked(Long.parseLong(parts[0]));
				//the other side sent frames that never arrived (the last ones of a burst are lost
				//without any later frame showing the gap): ask for them now
				if (parts.length > 1 && Long.parseLong(parts[1]) > recvSeq){
					long now = System.currentTimeMillis();
					if (now - lastNack > 150){
						lastNack = now;
						link.send("N " + recvSeq);
					}
				}
			} else if (kind == 'N'){
				long last = Long.parseLong(frame.substring(2).trim());
				acked(last);
				resendMissing(last);
			} else if (kind == 'F'){
				//a message sent to everyone outside of any channel (see Rooms.partyTopic): it may get
				//lost or come twice, whoever receives it sorts that out
				deliver.add(frame.substring(2));
			} else if (kind == 'H'){
				String[] parts = frame.substring(2).trim().split(" ");
				String theirSession = parts[0];
				long theirLast = Long.parseLong(parts[1]);
				if (peerSession != null && !peerSession.equals(theirSession)){
					//the other side started over: so do we, old messages mean nothing to it
					restarted = true;
					recvSeq = 0;
					sendSeq = 0;
					highestSent = 0;
					peerAcked = 0;
					outSeqs.clear();
					outFrames.clear();
					partial.setLength(0);
					early.clear();
				}
				peerSession = theirSession;
				acked(theirLast);
				//after a reconnection anything sent meanwhile may be lost: send the window again
				resendAfter(theirLast);
			}
		}
		if (restarted && l != null) l.onPeerRestarted();
		if (l != null){
			for (String m : deliver) l.onMessage(m);
		}
	}

	//the next frame in order: adds its data, and a finished message to the delivery list
	private void accept( String frame, ArrayList<String> deliver ){
		int nl = frame.indexOf('\n');
		String[] head = frame.substring(2, nl).split(" ");
		recvSeq = Long.parseLong(head[0]);
		if (unackedReceived++ == 0) unackedSince = System.currentTimeMillis();
		partial.append(frame, nl+1, frame.length());
		if (!head[2].equals("1")){
			deliver.add(partial.toString());
			partial.setLength(0);
		}
	}

	//sends again the first frames the other side is missing (it has everything up to 'last')
	private void resendMissing( long last ){
		int sent = 0;
		Iterator<long[]> seqs = outSeqs.iterator();
		Iterator<String> frames = outFrames.iterator();
		while (seqs.hasNext() && sent < REPAIR){
			long[] e = seqs.next();
			long s = e[0];
			String f = frames.next();
			if (s <= last) continue;
			if (s > highestSent) break;
			link.send(f);
			e[2] = 1;
			sent++;
		}
		pumpOut();
	}

	private void acked( long ack ){
		Iterator<long[]> seqs = outSeqs.iterator();
		Iterator<String> frames = outFrames.iterator();
		long now = System.currentTimeMillis();
		long sample = -1;
		while (seqs.hasNext()){
			long[] e = seqs.next();
			frames.next();
			if (e[0] <= ack){
				seqs.remove();
				frames.remove();
				//only frames sent once tell how long a confirmation takes
				if (e[1] > 0 && e[2] == 0) sample = now - e[1];
				lastAckProgress = now;
				resendDelay = -1;
			} else {
				break;
			}
		}
		if (sample >= 0){
			if (srtt < 0){
				srtt = sample;
				rttVar = sample / 2.0;
			} else {
				rttVar = 0.75*rttVar + 0.25*Math.abs(srtt - sample);
				srtt = 0.875*srtt + 0.125*sample;
			}
		}
		if (resendDelay < 0) resendDelay = rto();
		if (ack > peerAcked){
			peerAcked = ack;
			pumpOut();
		}
	}

	//how long to wait for a confirmation before sending again
	private long rto(){
		if (srtt < 0) return RESEND_START;
		return Math.max(RTO_MIN, Math.min(RESEND_START, (long)(srtt + Math.max(4*rttVar, 200))));
	}

	private void resendAfter( long last ){
		//go back: everything after 'last' counts as not sent, and the window goes out again
		highestSent = Math.max(Math.min(highestSent, last), peerAcked);
		pumpOut();
	}

	@Override
	public void onLinkState( boolean connected, String info ) {
		if (connected){
			synchronized (this){
				link.send("H " + session + " " + recvSeq);
			}
		}
		Listener l = listener;
		if (l != null) l.onChannelState(connected, info);
	}

	public void close(){
		closed = true;
		link.close();
	}
}
