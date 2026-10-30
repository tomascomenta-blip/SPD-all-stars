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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//the heroes of a co-op run.
//Dungeon.hero = hero "in context" (whose turn/buff/action is running), it only changes at
//deterministic points. the UI wants Dungeon.local() instead. mixing those two up = 90% of the bugs
public class Party {

	public static final int MAX_HEROES = 8;

	//index 0 is always the host's hero, and acts as the leader for things which need a single hero
	private static final ArrayList<Hero> heroes = new ArrayList<>();
	private static final ArrayList<String> names = new ArrayList<>();
	private static int localIndex = 0;
	private static boolean active = false;

	//which party index this device controls when a cooperative save is loaded
	public static int localIndexForLoad = 0;

	public static synchronized boolean active(){
		return active;
	}

	//returns to normal single player behaviour
	public static synchronized void reset(){
		heroes.clear();
		names.clear();
		localIndex = 0;
		active = false;
	}

	public static synchronized void setup(List<Hero> list, List<String> heroNames, int local){
		heroes.clear();
		heroes.addAll(list);
		names.clear();
		names.addAll(heroNames);
		while (names.size() < heroes.size()) names.add("P" + (names.size()+1));
		localIndex = local;
		active = heroes.size() > 1;
	}

	public static synchronized List<Hero> heroes(){
		return Collections.unmodifiableList(new ArrayList<>(heroes));
	}

	public static synchronized int size(){
		return heroes.size();
	}

	public static synchronized Hero get(int i){
		if (i < 0 || i >= heroes.size()) return null;
		return heroes.get(i);
	}

	public static synchronized void set(int i, Hero h){
		heroes.set(i, h);
	}

	public static synchronized int indexOf(Hero h){
		for (int i = 0; i < heroes.size(); i++){
			if (heroes.get(i) == h) return i;
		}
		return -1;
	}

	public static synchronized String name(int i){
		if (i < 0 || i >= names.size()) return "?";
		return names.get(i);
	}

	public static String name(Hero h){
		return name(indexOf(h));
	}

	public static synchronized List<String> names(){
		return new ArrayList<>(names);
	}

	public static synchronized int localIndex(){
		return localIndex;
	}

	public static synchronized Hero leader(){
		return heroes.isEmpty() ? Dungeon.hero : heroes.get(0);
	}

	public static synchronized Hero local(){
		if (!active || localIndex >= heroes.size()) return Dungeon.hero;
		return heroes.get(localIndex);
	}

	public static boolean isLocal(Char ch){
		return !active() || ch == local();
	}

	public static boolean isRemote(Char ch){
		return active() && ch instanceof Hero && indexOf((Hero) ch) >= 0 && ch != local();
	}

	//whether a buff or item belongs to the hero controlled on this device
	public static boolean ownedByLocal(Object o){
		if (!active()) return true;
		Hero me = local();
		if (o instanceof Buff) return ((Buff) o).target == me;
		if (o instanceof Item){
			for (Item i : me.belongings){
				if (i == o) return true;
			}
			return false;
		}
		return true;
	}

	public static boolean isPartyHero(Char ch){
		return ch instanceof Hero && (!active() || indexOf((Hero) ch) >= 0);
	}

	//heroes that are alive and in the current level
	public static synchronized ArrayList<Hero> living(){
		ArrayList<Hero> result = new ArrayList<>();
		if (!active){
			if (Dungeon.hero != null && Dungeon.hero.isAlive()) result.add(Dungeon.hero);
			return result;
		}
		for (Hero h : heroes){
			if (h.isAlive() && !h.coopDown) result.add(h);
		}
		return result;
	}

	public static boolean anyAlive(){
		if (!active()) return Dungeon.hero != null && Dungeon.hero.isAlive();
		return !living().isEmpty();
	}

	//closest living hero to a cell, ties go to the lower party index
	public static Hero nearestLiving(int pos){
		Hero best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Hero h : living()){
			int d = Dungeon.level == null ? 0 : Dungeon.level.distance(pos, h.pos);
			if (d < bestDist){
				best = h;
				bestDist = d;
			}
		}
		return best == null ? leader() : best;
	}

	//runs code with a specific hero in context
	public static void withHero(Hero h, Runnable r){
		Hero prev = Dungeon.hero;
		if (h != null) Dungeon.hero = h;
		try {
			r.run();
		} finally {
			Dungeon.hero = prev;
		}
	}

	//picks the hero in context for an actor that is about to act
	public static void contextFor(Actor a){
		if (!active()) return;
		Hero h = heroFor(a);
		if (h != null) Dungeon.hero = h;
	}

	public static Hero heroFor(Actor a){
		if (a instanceof Hero && indexOf((Hero) a) >= 0){
			return (Hero) a;
		} else if (a instanceof Buff){
			Char target = ((Buff) a).target;
			if (target instanceof Hero && indexOf((Hero) target) >= 0){
				return (Hero) target;
			} else if (target != null){
				return heroForChar(target);
			}
		} else if (a instanceof Char){
			return heroForChar((Char) a);
		}
		return nearestOrLeader();
	}

	private static Hero heroForChar(Char ch){
		if (ch instanceof Mob){
			Char enemy = ((Mob) ch).focusTarget();
			if (enemy instanceof Hero && indexOf((Hero) enemy) >= 0 && enemy.isAlive() && !((Hero) enemy).coopDown){
				return (Hero) enemy;
			}
		}
		return nearestLiving(ch.pos);
	}

	private static Hero nearestOrLeader(){
		ArrayList<Hero> alive = living();
		return alive.isEmpty() ? leader() : alive.get(0);
	}

}
