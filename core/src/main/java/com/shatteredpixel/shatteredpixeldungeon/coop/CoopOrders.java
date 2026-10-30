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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//every order a player can give. the UI here uses them and so does the replay of the other players,
//same code for any hero. if you add an action and it's not an order... desync. pregunta cómo lo sé
public class CoopOrders {

	// *** called by the interface (always for the local hero) ***

	//a tap on the map with the default listener
	public static void handleCell( final int cell ){
		//friendly fire: tapping another hero asks whether to hit them or swap places as usual
		final Hero me = Dungeon.local();
		final Char ch = Actor.findChar(cell);
		if (me != null && CoopRules.friendlyTarget(me, ch) && ch.isAlive() && CoopSession.localInputAllowed()
				&& !GameScene.isSelectingCell()){
			GameScene.showUntracked(new WndOptions(Messages.get(CoopOrders.class, "friend_title", Party.name((Hero) ch)),
					Messages.get(CoopOrders.class, "friend_body"),
					Messages.get(CoopOrders.class, "friend_attack"),
					Messages.get(CoopOrders.class, "friend_swap")){
				@Override
				protected void onSelect( int index ) {
					if (index == 0) attackFriend(cell);
					else if (index == 1) tap(cell);
				}
			});
			return;
		}
		tap(cell);
	}

	private static void tap( final int cell ){
		Bundle o = CoopSession.order("H");
		o.put("c", cell);
		CoopSession.local(o, new Runnable() {
			@Override
			public void run() {
				Hero me = Dungeon.local();
				if (me.handle(cell)) me.next();
			}
		});
	}

	//friendly fire, after the player confirmed it: hit the hero on that cell
	public static void attackFriend( final int cell ){
		Bundle o = CoopSession.order("H");
		o.put("c", cell);
		o.put("fa", true);
		CoopSession.local(o, new Runnable() {
			@Override
			public void run() {
				Hero me = Dungeon.local();
				if (me.attackFriend(cell)) me.next();
			}
		});
	}

	//an item action, null action means the item's default action
	public static void execute( final Item item, final String action ){
		final Hero me = Dungeon.local();
		Bundle o = CoopSession.order("X");
		o.put("i", ItemRef.of(me, item));
		if (action != null) o.put("a", action);
		CoopSession.local(o, new Runnable() {
			@Override
			public void run() {
				if (action != null) item.execute(me, action);
				else                item.execute(me);
			}
		});
	}

	//a named order without arguments ("rest", "restFull", "search", "resume", "waitPick")
	public static void named( final String name ){
		Bundle o = CoopSession.order("M");
		o.put("m", name);
		CoopSession.local(o, new Runnable() {
			@Override
			public void run() {
				perform(Dungeon.local(), name);
			}
		});
	}

	//talent upgrades chosen while another hero has the turn: the order is sent when this player's
	//turn comes (an order must be played at the same moment on every device). Interface state of
	//this device only, game logic never reads it.
	private static final ArrayList<Talent> pendingTalents = new ArrayList<>();

	//returns false if the upgrade could neither be made now nor kept for this player's turn
	public static boolean upgradeTalent( final Talent talent, final Runnable visuals ){
		if (CoopSession.active() && Party.active() && !CoopSession.localInputAllowed()){
			Hero me = Dungeon.local();
			if (me == null || !me.isAlive() || me.coopDown) return false;
			synchronized (pendingTalents){
				pendingTalents.add(talent);
			}
			CoopLog.line("talent " + talent + " waits for this player's turn");
			if (visuals != null) visuals.run();
			return true;
		}
		Bundle o = CoopSession.order("T");
		o.put("n", talent.name());
		CoopSession.local(o, new Runnable() {
			@Override
			public void run() {
				Dungeon.local().upgradeTalent(talent);
				if (visuals != null) visuals.run();
			}
		});
		return true;
	}

	//render thread, at the start of the local hero's turn
	static void flushPendingTalents(){
		while (CoopSession.localInputAllowed()){
			Talent t;
			synchronized (pendingTalents){
				if (pendingTalents.isEmpty()) return;
				t = pendingTalents.remove(0);
			}
			Hero me = Dungeon.local();
			int tier = tierOf(me, t);
			if (tier < 1 || me.talentPointsAvailable(tier) <= 0 || me.pointsInTalent(t) >= t.maxPoints()){
				CoopLog.line("talent " + t + " can't be upgraded any more, dropped");
				continue;
			}
			CoopLog.line("talent " + t + " sent at the start of the turn");
			final Talent talent = t;
			Bundle o = CoopSession.order("T");
			o.put("n", talent.name());
			CoopSession.local(o, new Runnable() {
				@Override
				public void run() {
					Dungeon.local().upgradeTalent(talent);
				}
			});
		}
	}

	public static void clearPending(){
		synchronized (pendingTalents){
			pendingTalents.clear();
		}
	}

	private static int tierOf( Hero h, Talent t ){
		for (int i = 0; i < h.talents.size(); i++){
			if (h.talents.get(i).containsKey(t)) return i+1;
		}
		return -1;
	}

	//upgrades of this player still waiting for their turn
	public static int pendingPoints( Talent t ){
		int n = 0;
		synchronized (pendingTalents){
			for (Talent p : pendingTalents) if (p == t) n++;
		}
		return n;
	}

	private static int pendingInTier( Hero h, int tier ){
		if (h != Dungeon.local() || tier < 1 || tier > h.talents.size()) return 0;
		int n = 0;
		synchronized (pendingTalents){
			for (Talent p : pendingTalents) if (h.talents.get(tier-1).containsKey(p)) n++;
		}
		return n;
	}

	//what the talent screen shows: the hero's points with this player's waiting upgrades already counted
	public static int talentPointsAvailable( Hero h, int tier ){
		return h.talentPointsAvailable(tier) - pendingInTier(h, tier);
	}

	public static int talentPointsSpent( Hero h, int tier ){
		return h.talentPointsSpent(tier) + pendingInTier(h, tier);
	}

	public static int pointsInTalent( Hero h, Talent t ){
		return h.pointsInTalent(t) + (h == Dungeon.local() ? pendingPoints(t) : 0);
	}

	public static void doAction( final ActionIndicator.Action action ){
		Bundle o = CoopSession.order("Q");
		o.put("a", action.getClass().getName());
		CoopSession.local(o, new Runnable() {
			@Override
			public void run() {
				action.doAction();
			}
		});
	}

	//code shared by this device and the replay of the other player's orders
	static void perform( Hero h, String name ){
		switch (name){
			case "rest":
				h.rest(false);
				break;
			case "restFull":
				h.rest(true);
				break;
			case "search":
				h.search(true);
				break;
			case "resume":
				h.resume();
				break;
			case "waitPick":
				h.waitOrPickup = true;
				if ((Dungeon.level.heaps.get(h.pos) != null || h.canSelfTrample()) && h.handle(h.pos)){
					//trigger hold fast and patient strike here, even if the hero didn't specifically wait
					if (h.hasTalent(Talent.HOLD_FAST)){
						Buff.affect(h, HoldFast.class).pos = h.pos;
					}
					if (h.hasTalent(Talent.PATIENT_STRIKE)){
						Buff.affect(h, Talent.PatientStrikeTracker.class).pos = h.pos;
					}
					h.next();
				} else {
					h.rest(false);
				}
				break;
		}
	}

	// *** replaying the other player's orders ***

	static void apply( CoopSession session, Hero h, Bundle ev ){
		int idx = Party.indexOf(h);
		String kind = ev.getString("k");
		switch (kind){
			case "H": {
				int c = ev.getInt("c");
				if (ev.getBoolean("fa") ? h.attackFriend(c) : h.handle(c)) h.next();
				break;
			}
			case "X": {
				Item item = ItemRef.resolve(h, ev.getString("i"));
				if (item == null) {
					missing(ev);
					break;
				}
				if (ev.contains("a")) item.execute(h, ev.getString("a"));
				else                  item.execute(h);
				break;
			}
			case "M":
				perform(h, ev.getString("m"));
				break;
			case "T":
				try {
					Talent t = Talent.valueOf(ev.getString("n"));
					int tier = tierOf(h, t);
					//the sender checked this on the same state
					if (tier < 1 || h.talentPointsAvailable(tier) <= 0 || h.pointsInTalent(t) >= t.maxPoints()){
						missing(ev);
					} else {
						h.upgradeTalent(t);
					}
				} catch (IllegalArgumentException e){
					missing(ev);
				}
				break;
			case "Q": {
				ActionIndicator.Action a = findAction(h, ev.getString("a"));
				if (a != null) a.doAction();
				else missing(ev);
				break;
			}
			case "C": {
				Object p = session.takeRemotePrompt(idx, ev.getInt("q"));
				if (p instanceof CellSelector.Listener){
					int c = ev.getInt("c");
					((CellSelector.Listener) p).onSelect(c < 0 ? null : c);
				} else {
					missing(ev);
				}
				break;
			}
			case "I": {
				Object p = session.takeRemotePrompt(idx, ev.getInt("q"));
				if (p instanceof WndBag.ItemSelector){
					((WndBag.ItemSelector) p).onSelect(ItemRef.resolve(h, ev.getString("i")));
				} else {
					missing(ev);
				}
				break;
			}
			case "B": {
				Object p = session.peekRemotePrompt(idx, ev.getInt("q"));
				if (p instanceof Window){
					ArrayList<Button> buttons = CoopSession.buttonsOf((Window) p);
					int b = ev.getInt("b");
					if (b >= 0 && b < buttons.size()){
						buttons.get(b).coopClick();
					} else {
						missing(ev);
					}
				} else {
					missing(ev);
				}
				break;
			}
			case "AA": case "AX": case "AC": case "AE":
				CoopAlchemy.apply(h, ev);
				break;
			case "K": {
				Object p = session.peekRemotePrompt(idx, ev.getInt("q"));
				if (p instanceof Window){
					((Window) p).onBackPressed();
				} else {
					missing(ev);
				}
				break;
			}
		}
	}

	private static ActionIndicator.Action findAction( Hero h, String className ){
		Class<?> cls = Reflection.forName(className);
		if (cls == null) return null;
		for (Buff b : h.buffs()){
			if (b.getClass() == cls && b instanceof ActionIndicator.Action) return (ActionIndicator.Action) b;
		}
		for (Item i : h.belongings){
			if (i.getClass() == cls && i instanceof ActionIndicator.Action) return (ActionIndicator.Action) i;
		}
		return null;
	}

	//an order that cannot be matched means the two copies of the run already differ
	private static void missing( Bundle ev ){
		CoopSession.reportMismatch("order " + ev.getString("k") + " could not be replayed");
	}
}
