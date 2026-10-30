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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.DarkTaint;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.ColossusPresence;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalFinale;
import com.watabou.noosa.Game;

import java.util.ArrayList;
import java.util.Collections;

//testing only. -Dspd.test.awake=<biome>[layer] starts a run on the continent already awake, with
//seven drops; -Dspd.test.essence=N starts any test run over there with N drops (to kill yog's true
//form in the preview and see it wake); -Dspd.test.awakekit=N gives every hero of those runs level 30 and a
//greatsword and plate +N (and ten scrolls of teleportation); -Dspd.test.awakewalk=1 sends the hero (every
//bot in co-op) home on its own, up, through the passes and into the portal, hitting whatever boxes it in
public class AwakeTest {

	//Continent.testStart: true if it put the party somewhere itself
	public static boolean start(){
		int e = DevFlags.essence();
		if (e > 0) Continent.essence = Math.max(Continent.essence, Math.min(Continent.TOTAL, e));
		kit();
		String at = DevFlags.awake();
		if (at == null) return false;
		at = at.trim().toLowerCase();
		int layer = 1;
		if (!at.isEmpty() && Character.isDigit(at.charAt(at.length() - 1))){
			layer = Math.max(1, Math.min(3, at.charAt(at.length() - 1) - '0'));
			at = at.substring(0, at.length() - 1);
		}
		Biome b = Biome.ASH;
		for (Biome o : Biome.ALL) if (o.key.equals(at)) b = o;
		if (layer == 2 && !b.depths) layer = 3;
		Dungeon.branch = b.branch;
		Dungeon.depth = 20 + layer;
		ThalFinale.defeated = true;
		ThalFinale.called = true;
		Continent.essence = Math.max(Continent.essence, Continent.NEEDED);
		Awakening.reset();
		Awakening.wake();
		CoopLog.line("awake test: start in " + b.key + " layer " + layer + " with " + Continent.essence + " essence");
		return true;
	}

	private static ArrayList<Hero> party(){
		ArrayList<Hero> list = new ArrayList<>();
		if (Party.active()) list.addAll(Party.heroes());
		else if (Dungeon.hero != null) list.addAll(Collections.singletonList(Dungeon.hero));
		return list;
	}

	private static void kit(){
		int plus = DevFlags.awakeKit();
		if (plus <= 0) return;
		for (Hero h : party()){
			h.lvl = Math.max(h.lvl, 30);
			MeleeWeapon w = new Greatsword();
			w.identify();
			w.upgrade(plus);
			w.cursed = false;
			h.belongings.weapon = w;
			Armor a = new PlateArmor();
			a.identify();
			a.upgrade(plus);
			a.cursed = false;
			h.belongings.armor = a;
			a.activate(h);
			h.STR = Math.max(h.STR, 19);
			give(h, new ScrollOfTeleportation().quantity(10));
			h.updateHT(true);
			h.HP = h.HT;
		}
	}

	private static void give( final Hero h, final Item item ){
		item.identify();
		Party.withHero(h, new Runnable() {
			@Override
			public void run() {
				item.collect(h.belongings.backpack);
			}
		});
	}

	//Awakening.onArrive: as if they had a map of it, so the hero can be sent anywhere (a game
	//action, the same on every device)
	static void onArrive( Level level ){
		if (!DevFlags.awakeWalk()) return;
		for (int i = 0; i < level.length(); i++){
			if (!level.solid[i] || level.getTransition(i) != null) level.mapped[i] = true;
		}
	}

	// *** the walk home ***

	private static float timer = 0, logT = 0;
	private static int lastPos = -1, stuck = 0, lastFloor = -1, leapFrom = -1;

	//solo: every frame
	public static void tick( Hero h ){
		if (!DevFlags.awakeWalk() || h == null || !h.isAlive() || !h.ready) return;
		if (GameScene.topWindow() != null || GameScene.isSelectingCell()) return;
		if ((timer -= Game.elapsed) > 0) return;
		timer = 0.2f;
		act(h);
	}

	//co-op: the bot asks before doing its own thing
	public static boolean act( Hero me ){
		if (!DevFlags.awakeWalk() || !Awakening.on() || Awakening.sealed() || me == null || !me.isAlive()) return false;
		Level l = Dungeon.level;
		if (!(l instanceof OpenWorld)) return false;
		int goal = Awakening.homeward(l);
		if (goal == -1) return false;

		if (Continent.floor() != lastFloor){
			lastFloor = Continent.floor();
			stuck = 0;
			CoopLog.line("awake walk: hero " + Party.indexOf(me) + " in " + Continent.place() + " at " + xy(l, me.pos)
					+ ", home is " + xy(l, goal));
		}
		if ((logT -= 1) <= 0){
			logT = 25;
			CoopLog.line("awake walk: hero " + Party.indexOf(me) + " at " + xy(l, me.pos) + " -> " + xy(l, goal)
					+ " d=" + l.distance(me.pos, goal) + " hunting=" + hunting(l) + " big=" + bigOnes(l, me) + " taint=" + DarkTaint.of(me)
					+ " hp=" + me.HP + "/" + me.HT + " t=" + (int) Actor.now());
		}

		stuck = me.pos == lastPos ? stuck + 1 : 0;
		lastPos = me.pos;
		if (leapFrom != -1){
			CoopLog.line("awake walk: teleport from " + xy(l, leapFrom) + " to " + xy(l, me.pos)
					+ " d=" + l.distance(leapFrom, me.pos) + " (at most " + Awakening.LEAP + ")");
			leapFrom = -1;
		}
		//boxed in for a while: a scroll of teleportation, which only leaps a few cells over there
		ScrollOfTeleportation scroll = me.belongings.getItem(ScrollOfTeleportation.class);
		if (stuck >= 8 && scroll != null){
			stuck = 0;
			leapFrom = me.pos;
			CoopOrders.execute(scroll, Scroll.AC_READ);
			return true;
		}
		if (stuck >= 2){
			Char foe = blocker(me);
			if (foe != null && stuck < 12){
				CoopOrders.handleCell(foe.pos);
				return true;
			}
			if (stuck >= 12){
				stuck = 0;
				int side = aside(l, me.pos);
				if (side != -1){
					CoopLog.line("awake walk: boxed in at " + xy(l, me.pos) + ", stepping aside");
					CoopOrders.handleCell(side);
					return true;
				}
			}
		}

		//the strip of a pass: a cell of it that can be stood on
		int cell = goal;
		LevelTransition t = l.getTransition(goal);
		if (t != null && !l.passable[goal]){
			int best = -1, bestD = Integer.MAX_VALUE;
			for (int i = 0; i < l.length(); i++){
				if (!t.inside(i) || !l.passable[i]) continue;
				int d = l.distance(i, goal);
				if (d < bestD){
					bestD = d;
					best = i;
				}
			}
			if (best != -1) cell = best;
		}
		CoopOrders.handleCell(cell);
		return true;
	}

	private static String xy( Level l, int cell ){
		return (cell % l.width()) + "," + (cell / l.width());
	}

	private static int hunting( Level l ){
		int n = 0;
		for (Mob m : l.mobs){
			if (m.alignment == Char.Alignment.ENEMY && m.state == m.HUNTING) n++;
		}
		return n;
	}

	//the big ones coming: how many and how far the nearest is
	private static String bigOnes( Level l, Hero me ){
		int n = 0, near = -1;
		for (Mob m : l.mobs){
			if (!(m instanceof Colossus) || ((Colossus) m).size < ColossusPresence.BIG || m.state != m.HUNTING) continue;
			n++;
			int d = ((Colossus) m).reach(me.pos);
			if (near == -1 || d < near) near = d;
		}
		return n + (n > 0 ? "(" + near + ")" : "");
	}

	//the enemy next to the hero that's in the way, the small ones first
	private static Char blocker( Hero me ){
		Char best = null;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment != Char.Alignment.ENEMY || !m.isAlive()) continue;
			boolean next = m instanceof Colossus ? ((Colossus) m).touches(me.pos) : Dungeon.level.adjacent(m.pos, me.pos);
			if (!next) continue;
			if (best == null || best instanceof Colossus && !(m instanceof Colossus)) best = m;
		}
		return best;
	}

	private static int aside( Level l, int from ){
		for (int r = 3; r <= 8; r++){
			for (int i = 0; i < l.length(); i++){
				if (l.distance(i, from) != r || !l.passable[i] || l.avoid[i] || Actor.findChar(i) != null) continue;
				if ((i + (int) Actor.now()) % 3 == 0) return i;
			}
		}
		return -1;
	}
}
