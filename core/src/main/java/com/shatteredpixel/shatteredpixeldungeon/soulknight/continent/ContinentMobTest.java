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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.AshWalker;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.ObsidianColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.Salamander;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.SmokeVulture;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.FaunaTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.BurningFist;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogEye;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogTongue;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//testing only, all of it behind -Dspd.test.cmob=eye,larva...|all (eye*3 for three of them).
//turn(): game side, one kind at a time 3-6 cells from the party, the next one when it's cleared.
//act()/solo(): a dumb bot that dodges the marks and fights them
public class ContinentMobTest {

	private static final int GIVE_UP = 90;

	private static final String[] KINDS = {"eye", "larva", "fist", "tongue", "walker", "salamander", "vulture", "colossus"};

	private static int depth = -1;
	private static ArrayList<String> queue = new ArrayList<>();
	private static String current = null;
	private static int wait = 0;
	private static boolean kitDone = false;

	//numbers of the group that's out
	private static int start, rounds, lost, worst, taint0, falls;
	private static int[] lastHp = new int[0];

	private static Mob make( String kind ){
		switch (kind){
			case "eye":        return new YogEye();
			case "larva":      return new YogLarva();
			case "fist":       return new BurningFist();
			case "tongue":     return new YogTongue();
			case "walker":     return new AshWalker();
			case "salamander": return new Salamander();
			case "vulture":    return new SmokeVulture();
			case "colossus":   return new ObsidianColossus();
		}
		//the beasts of the open plains (jackal, bison, rex... see AshFauna.KINDS)
		return AshFauna.make(kind);
	}

	private static ArrayList<String> parse( String which ){
		ArrayList<String> out = new ArrayList<>();
		for (String s : which.split(",")){
			s = s.trim().toLowerCase();
			if (s.equals("all")) out.addAll(Arrays.asList(KINDS));
			else if (make(s.contains("*") ? s.substring(0, s.indexOf('*')) : s) != null) out.add(s);
			else CoopLog.line("cmob test: no such monster " + s);
		}
		return out;
	}

	public static boolean ours( Char ch ){
		return ch instanceof ContinentMob || ch instanceof AshWalker.Heap;
	}

	//start of every hero turn. only the first living hero does anything, so it happens once a round
	//and the same way on every device
	public static void turn( Hero hero, String which ){
		ArrayList<Hero> living = Party.living();
		if (living.isEmpty() || living.get(0) != hero) return;
		if (Dungeon.depth != depth){
			depth = Dungeon.depth;
			queue = parse(which);
			current = null;
			wait = 3;
		}
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment != Char.Alignment.ENEMY || ours(m)) continue;
			if (m.properties().contains(Char.Property.IMMOVABLE)) continue;
			m.EXP = 0;
			m.destroy();
			if (m.sprite != null) m.sprite.killAndErase();
		}
		boolean alive = false;
		for (Mob m : Dungeon.level.mobs){
			if (ours(m) && m.isAlive() && m.alignment == Char.Alignment.ENEMY) alive = true;
		}
		if (current != null) track();
		if (alive && current != null && (int) Actor.now() - start > GIVE_UP){
			//something's out of reach or the bot can't cope: note it and move on
			CoopLog.line("cmob test: gave up on " + current + " " + who());
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (!ours(m)) continue;
				m.EXP = 0;
				m.destroy();
				if (m.sprite != null) m.sprite.killAndErase();
			}
			alive = false;
		}
		if (alive){
			if (++rounds % 10 == 0) CoopLog.line("cmob test: " + current + " " + who() + " t=" + (int) Actor.now());
			return;
		}
		if (current != null){
			CoopLog.line("cmob test: done " + current + " turns=" + ((int) Actor.now() - start)
					+ " heroLoss=" + lost + " lowest=" + worst + "% taint+" + (taint() - taint0) + " falls=" + falls);
			current = null;
			wait = 4;
		}
		if (queue.isEmpty() || --wait > 0) return;
		if (!kitDone){
			kitDone = true;
			if (DevFlags.cmobKit() > 0) giveKit(DevFlags.cmobKit());
		}
		current = queue.remove(0);
		int n = 1;
		String kind = current;
		if (kind.contains("*")){
			try {
				n = Integer.parseInt(kind.substring(kind.indexOf('*') + 1));
			} catch (NumberFormatException e){
				n = 1;
			}
			kind = kind.substring(0, kind.indexOf('*'));
		}
		int spawned = 0;
		for (int i = 0; i < n; i++){
			if (spawn(kind, hero)) spawned++;
		}
		if (spawned == 0){
			//boxed in a corridor: try again in a bit
			CoopLog.line("cmob test: can't place " + current + " yet");
			queue.add(0, current);
			current = null;
			wait = 3;
			return;
		}
		start = (int) Actor.now();
		rounds = 0;
		lost = 0;
		worst = 100;
		falls = 0;
		taint0 = taint();
		lastHp = new int[heroes().size()];
		for (int i = 0; i < lastHp.length; i++) lastHp[i] = heroes().get(i).HP;
	}

	private static List<Hero> heroes(){
		if (Party.active()) return Party.heroes();
		ArrayList<Hero> solo = new ArrayList<>();
		if (Dungeon.hero != null) solo.add(Dungeon.hero);
		return solo;
	}

	private static int taint(){
		int t = 0;
		for (Hero h : heroes()) t += DarkTaint.of(h);
		return t;
	}

	private static void track(){
		List<Hero> all = heroes();
		for (int i = 0; i < all.size() && i < lastHp.length; i++){
			Hero h = all.get(i);
			if (h.HP < lastHp[i]) lost += lastHp[i] - h.HP;
			if (h.isAlive()) worst = Math.min(worst, h.HP * 100 / Math.max(1, h.HT));
			else if (lastHp[i] > 0) falls++;
			lastHp[i] = h.isAlive() ? h.HP : 0;
		}
	}

	private static String who(){
		StringBuilder s = new StringBuilder();
		for (Mob m : Dungeon.level.mobs){
			if (ours(m) && m.isAlive()) s.append(m.getClass().getSimpleName()).append(' ').append(m.HP).append('/').append(m.HT)
					.append(" @").append(m.pos).append(m.state == m.HUNTING ? " H" : m.state == m.WANDERING ? " W" : " -")
					.append(m.invisible > 0 ? " hidden " : " ");
		}
		for (Hero h : heroes()){
			s.append("| hero ").append(h.HP).append('/').append(h.HT).append(" @").append(h.pos).append(" taint ").append(DarkTaint.of(h)).append(' ');
		}
		return s.toString();
	}

	//free cells at that distance and a short walk away, the ones in sight first
	private static ArrayList<Integer> freeNear( int center, int minD, int maxD ){
		PathFinder.buildDistanceMap(center, Dungeon.level.passable, maxD * 2);
		ArrayList<Integer> seen = new ArrayList<>();
		ArrayList<Integer> unseen = new ArrayList<>();
		for (int d = minD; d <= maxD; d++){
			for (int c : SKCombat.area(center, d)){
				if (Dungeon.level.distance(c, center) != d) continue;
				if (!Dungeon.level.passable[c] || Dungeon.level.avoid[c] || Actor.findChar(c) != null) continue;
				if (PathFinder.distance[c] > maxD * 2) continue;
				(Dungeon.level.heroFOV[c] ? seen : unseen).add(c);
			}
		}
		seen.addAll(unseen);
		return seen;
	}

	private static boolean spawn( String kind, Hero hero ){
		Mob m = make(kind);
		if (m == null) return false;
		if (m instanceof Colossus){
			int spot = FaunaTest.nearSpot((Colossus) m, hero.pos);
			if (spot == -1) return false;
			m.pos = spot;
			GameScene.add(m);
			CoopLog.line("cmob test: " + kind + " at " + spot + " hero at " + hero.pos + " hp=" + m.HP + "/" + m.HT);
			return true;
		}
		ArrayList<Integer> cells = freeNear(hero.pos, 3, 6);
		int cell = -1;
		for (int c : cells){
			boolean roomy = true;
			for (int n : PathFinder.NEIGHBOURS8){
				if (Dungeon.level.solid[c + n]) roomy = false;
			}
			if (roomy){
				cell = c;
				break;
			}
		}
		if (cell == -1 && !cells.isEmpty()) cell = cells.get(0);
		if (cell == -1) return false;
		if (m instanceof Salamander) lavaPool(cell, hero.pos);
		m.pos = cell;
		GameScene.add(m);
		m.aggro(hero);
		m.beckon(hero.pos);
		if (m instanceof YogLarva) ((YogLarva) m).hatch();
		CoopLog.line("cmob test: " + kind + " at " + cell + " hero at " + hero.pos + " hp=" + m.HP + "/" + m.HT);
		return true;
	}

	//the salamander wants lava (water stands in for it outside the continent)
	private static void lavaPool( int center, int keepDry ){
		for (int c : SKCombat.area(center, 1)){
			if (Dungeon.level.distance(c, keepDry) < 2 || Dungeon.level.map[c] == Terrain.WATER) continue;
			if (Dungeon.level.solid[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null && c != center) continue;
			if (Dungeon.level.heaps.get(c) != null) continue;
			Level.set(c, Terrain.WATER);
			GameScene.updateMap(c);
		}
	}

	//-Dspd.test.cmobkit=N: greatsword and plate armor +N, more or less what a party carries over there
	private static void giveKit( int plus ){
		for (Hero h : Party.living()){
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
			h.updateHT(true);
			h.HP = h.HT;
			CoopLog.line("cmob test kit: hero " + Party.indexOf(h) + " lvl=" + h.lvl + " HT=" + h.HT + " +" + plus);
		}
	}

	// *** the bot ***

	private static final boolean ON = DevFlags.cmob() != null;
	private static float timer = 0, lastNow = -1;
	private static int stuck = 0, lastHP = -1;

	//solo runs: nobody else plays the hero
	public static void solo( Hero me ){
		if (!ON || me == null || !me.isAlive() || !me.ready) return;
		if (GameScene.topWindow() != null || GameScene.isSelectingCell()) return;
		timer -= Game.elapsed;
		if (timer > 0) return;
		timer = 0.1f;
		float now = Actor.now();
		if (now == lastNow) stuck++;
		else {
			stuck = 0;
			lastNow = now;
		}
		if (stuck >= 6 || !act(me)){
			stuck = 0;
			CoopOrders.named("rest");
		}
	}

	//closest unmarked cell up to 3 steps away (own little search, the bot runs off the game thread)
	private static int safeSpot( int from, int toward ){
		int len = Dungeon.level.length();
		int[] dist = new int[len];
		Arrays.fill(dist, -1);
		ArrayList<Integer> queue = new ArrayList<>();
		queue.add(from);
		dist[from] = 0;
		int best = -1;
		for (int i = 0; i < queue.size(); i++){
			int c = queue.get(i);
			if (best != -1 && dist[c] > dist[best]) break;
			if (c != from && TargetedCell.cells.get(c) == null){
				if (best == -1 || Dungeon.level.distance(c, toward) < Dungeon.level.distance(best, toward)) best = c;
				continue;
			}
			if (dist[c] >= 3) continue;
			for (int n : PathFinder.NEIGHBOURS8){
				int k = c + n;
				if (k < 0 || k >= len || dist[k] != -1 || !Dungeon.level.passable[k] || Actor.findChar(k) != null) continue;
				dist[k] = dist[c] + 1;
				queue.add(k);
			}
		}
		return best;
	}

	//off the marked cells, then at the closest one it can hurt. false = nothing of ours around
	public static boolean act( Hero me ){
		if (!ON || me == null || !me.isAlive()) return false;
		Mob target = null, hidden = null;
		for (Mob m : Dungeon.level.mobs){
			if (!m.isAlive() || m.alignment != Char.Alignment.ENEMY || !ours(m)) continue;
			if (m.invisible > 0 || m.isInvulnerable(Hero.class)){
				if (hidden == null || Dungeon.level.distance(me.pos, m.pos) < Dungeon.level.distance(me.pos, hidden.pos)) hidden = m;
				continue;
			}
			if (target == null || Dungeon.level.distance(me.pos, m.pos) < Dungeon.level.distance(me.pos, target.pos)) target = m;
		}
		if (target == null && hidden == null) return false;
		if (me.HP != lastHP){
			lastHP = me.HP;
			CoopLog.line("cmob bot hp=" + me.HP + "/" + me.HT + " t=" + (int) Actor.now()
					+ (target == null ? "" : " target=" + target.getClass().getSimpleName() + " " + target.HP + "/" + target.HT));
		}
		if (me.HP < me.HT / 3){
			PotionOfHealing potion = me.belongings.getItem(PotionOfHealing.class);
			if (potion != null){
				CoopOrders.execute(potion, "DRINK");
				return true;
			}
		}
		if (TargetedCell.cells.get(me.pos) != null && !me.rooted){
			int best = safeSpot(me.pos, target == null ? me.pos : target.pos);
			if (best != -1){
				CoopOrders.handleCell(best);
				return true;
			}
		}
		if (target == null){
			//something under the lava: get in its range so it comes out
			if (Dungeon.level.distance(me.pos, hidden.pos) > 2){
				int step = -1;
				//no PathFinder in here: it's the render thread
				for (int c : SKCombat.area(hidden.pos, 2)){
					if (Dungeon.level.water[c] || !Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
					if (!Dungeon.level.visited[c] && !Dungeon.level.heroFOV[c]) continue;
					if (step == -1 || Dungeon.level.distance(me.pos, c) < Dungeon.level.distance(me.pos, step)) step = c;
				}
				CoopOrders.handleCell(step != -1 ? step : hidden.pos);
				return true;
			}
			CoopOrders.named("rest");
			return true;
		}
		CoopOrders.handleCell(target.pos);
		return true;
	}
}
