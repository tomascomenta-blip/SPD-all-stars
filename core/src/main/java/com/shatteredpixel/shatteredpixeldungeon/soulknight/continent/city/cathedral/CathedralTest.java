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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.land.AshSurface;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.FurnaceCathedral;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalFinale;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Arrays;

//testing only. -Dspd.test.cathedral=2 starts a run on the streets of the devoured city with a furnace
//cathedral lying knocked down a few steps away, doors open (for a long while); =1 the same, but the
//party walks in on its first turn. -Dspd.test.cathedralbot=1 sends the hero (every bot in co-op)
//in, up the nave and at the heart until it breaks; =2 in, a few steps, and out by the portal; =3 a
//walk round the whole inside (porch, chapels, tower, crypt, apse) and out by the portal
public class CathedralTest {

	//Continent.testStart: true if it put the party somewhere itself
	public static boolean start(){
		int n = DevFlags.cathedral();
		if (n <= 0) return false;
		Dungeon.branch = Continent.CITY;
		Dungeon.depth = 21;
		ThalFinale.defeated = true;
		ThalFinale.called = true;
		Cathedral.testEnter = n == 1;
		CoopLog.line("cathedral test: start on the city streets" + (n == 1 ? ", walking in" : ""));
		return true;
	}

	//Continent.fixTransitions, a floor just made (on the host only): the cathedral by the way in, with
	//nothing big around to get in the way
	public static void onCreate( Level level ){
		if (DevFlags.cathedral() <= 0 || Dungeon.branch != Continent.CITY || Dungeon.depth != 21
				|| !(level instanceof AshSurface)) return;
		Level was = Dungeon.level;
		Dungeon.level = level;
		try {
			place(level);
		} finally {
			Dungeon.level = was;
		}
	}

	private static void place( Level level ){
		int start = level.entrance();
		for (Mob m : level.mobs.toArray(new Mob[0])){
			int d = level.distance(m.pos, start);
			if (m instanceof FurnaceCathedral || m instanceof Colossus && d < 45 || d < 20) level.mobs.remove(m);
		}
		FurnaceCathedral c = new FurnaceCathedral();
		boolean[] f = Colossus.fits(c.size, c);
		int w = level.width(), best = -1, bestD = Integer.MAX_VALUE;
		for (int i = 0; i < f.length; i++){
			if (!f[i]) continue;
			c.pos = i;
			int step = c.doorstep();
			if (step < 0 || step >= level.length() || !level.passable[step] || c.covers(start)) continue;
			int d = level.distance(step, start);
			if (d < 4 || d >= bestD) continue;
			//the doors have to be reachable: a few free cells below them
			if (!level.passable[step + w] || !level.passable[step + 2 * w]) continue;
			best = i;
			bestD = d;
		}
		if (best == -1){
			CoopLog.line("cathedral test: no room for it near " + start);
			return;
		}
		c.pos = best;
		c.home = best;
		c.down = 400;
		c.falls = 1;
		level.mobs.add(c);
		Cathedral.openDoors(level, c);
		CoopLog.line("cathedral test: down at " + (best % w) + "," + (best / w) + ", doorstep " + c.doorstep()
				+ " (" + bestD + " from the way in)");
	}

	//Cathedral.onArrive, outside: with =1 the leader walks in on its first turn. with the bot, the
	//streets round the doors are on the map (the hero only walks where it knows)
	static void outside( Level l ){
		if (DevFlags.cathedralBot() > 0){
			for (LevelTransition t : l.transitions){
				if (t instanceof CathedralDoor) chart(l, t.cell(), 999);
			}
		}
		if (!Cathedral.testEnter) return;
		Cathedral.testEnter = false;
		for (LevelTransition t : l.transitions){
			if (!(t instanceof CathedralDoor)) continue;
			Hero leader = Party.leader();
			if (leader == null) return;
			Buff.affect(leader, EnterNow.class).door = t.cell();
			return;
		}
	}

	//and the whole inside, for the bot
	static void inside( Level l ){
		if (DevFlags.cathedralBot() > 0) chart(l, l.entrance(), 999);
	}

	private static void chart( Level l, int around, int r ){
		for (int i = 0; i < l.length(); i++){
			if (l.distance(i, around) <= r && (!l.solid[i] || l.getTransition(i) != null)) l.mapped[i] = true;
		}
	}

	public static class EnterNow extends Buff {

		{
			type = buffType.NEUTRAL;
		}

		public int door = -1;

		@Override
		public boolean act() {
			detach();
			LevelTransition t = Dungeon.level.getTransition(door);
			if (!(t instanceof CathedralDoor) || !(target instanceof Hero)) return true;
			Hero h = (Hero) target;
			h.pos = door;
			if (h.sprite != null) h.sprite.place(door);
			CoopLog.line("cathedral test: hero " + Party.indexOf(h) + " walks in");
			return !Dungeon.level.activateTransition(h, t);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("door", door);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			door = bundle.getInt("door");
		}
	}

	// *** the bot ***

	private static float timer = 2f;
	private static int seenInside = 0, steps = 0, tour = 0, lastFloor = -1;
	private static boolean done = false;
	private static int logT = 0, stuck = 0;
	private static float lastNow = -1;

	//solo: every few frames, from the render thread (it only gives orders, like a player)
	public static void tick( Hero h ){
		if (DevFlags.cathedralBot() <= 0 || h == null || !h.isAlive() || !h.ready) return;
		if (GameScene.topWindow() != null || GameScene.isSelectingCell()) return;
		if ((timer -= Game.elapsed) > 0) return;
		timer = 0.12f;
		if (!act(h)) CoopOrders.named("rest");
	}

	//co-op: the bot asks before doing its own thing
	public static boolean act( Hero me ){
		int mode = DevFlags.cathedralBot();
		if (mode <= 0 || me == null || !me.isAlive() || !Continent.in()) return false;
		Level l = Dungeon.level;
		int floor = Dungeon.depth * 100 + Dungeon.branch;
		if (floor != lastFloor){
			lastFloor = floor;
			steps = 0;
		}
		steps++;
		//orders that don't spend time (somewhere it can't get to): rest, or skip that stop of the walk
		stuck = Actor.now() == lastNow ? stuck + 1 : 0;
		lastNow = Actor.now();
		if (stuck >= 12){
			stuck = 0;
			if (mode == 3 && Cathedral.here() && tour < TOUR.length){
				CoopLog.line("cathedral bot: can't get to stop " + tour + ", skipping it");
				tour++;
			}
			CoopOrders.named("rest");
			return true;
		}
		if (--logT <= 0){
			logT = 30;
			CoopLog.line("cathedral bot: hero " + Party.indexOf(me) + " hp " + me.HP + "/" + me.HT + " at " + me.pos
					+ (Cathedral.here() ? " inside, clock " + Cathedral.clock() + " heat " + Cathedral.heat() : " outside")
					+ " t=" + (int) Actor.now());
		}
		if (me.HP < me.HT / 3){
			PotionOfHealing p = me.belongings.getItem(PotionOfHealing.class);
			if (p != null){
				CoopOrders.execute(p, "DRINK");
				return true;
			}
		}
		//right against the heart it keeps hitting, marks or not (it's a test of breaking it, in god mode)
		boolean atHeart = false;
		for (Mob m : l.mobs){
			if (m instanceof FurnaceHeart && m.isAlive() && ((FurnaceHeart) m).touches(me.pos)) atHeart = mode == 1;
		}
		if (!atHeart && TargetedCell.cells.get(me.pos) != null && !me.rooted){
			int safe = safeSpot(me.pos);
			if (safe != -1){
				CoopOrders.handleCell(safe);
				return true;
			}
		}
		if (!(l instanceof CathedralLevel)){
			if (seenInside > 0) done = true;
			if (done) return false;
			for (LevelTransition t : l.transitions){
				if (!(t instanceof CathedralDoor)) continue;
				CoopOrders.handleCell(t.cell());
				return true;
			}
			return false;
		}
		seenInside++;
		CathedralLevel c = (CathedralLevel) l;
		int portal = c.entrance();
		if (mode == 2 && steps > 25 || mode == 3 && tour >= TOUR.length){
			CoopOrders.handleCell(portal);
			return true;
		}
		Char foe = blocker(me);
		if (foe != null){
			CoopOrders.handleCell(foe.pos);
			return true;
		}
		if (mode == 3){
			int goal = waypoint(c, TOUR[tour], me.pos);
			if (goal == -1 || l.distance(me.pos, goal) <= 1){
				CoopLog.line("cathedral bot: tour stop " + tour + " (zone " + TOUR[tour] + ")");
				tour++;
				return act(me);
			}
			CoopOrders.handleCell(goal);
			return true;
		}
		for (Mob m : l.mobs){
			if (!(m instanceof FurnaceHeart) || !m.isAlive()) continue;
			FurnaceHeart h = (FurnaceHeart) m;
			if (h.touches(me.pos)){
				CoopOrders.handleCell(h.pos);
				return true;
			}
			//walk to the side of it nearest by foot (the dais is ringed by molten metal): over cool
			//floor if there's a way, through the fire if not
			int[] dist = walk(me.pos, true);
			boolean any = false;
			for (int i = 0; i < l.length() && !any; i++) any = h.touches(i) && dist[i] >= 0;
			if (!any) dist = walk(me.pos, false);
			int best = -1;
			for (int i = 0; i < l.length(); i++){
				if (!h.touches(i) || dist[i] < 0 || l.avoid[i] || Actor.findChar(i) != null) continue;
				if (best == -1 || dist[i] < dist[best]) best = i;
			}
			if (best != -1){
				CoopOrders.handleCell(best);
				return true;
			}
		}
		return false;
	}

	//steps from a cell to every other over the floor, keeping off the molten metal or not
	private static int[] walk( int from, boolean cool ){
		Level l = Dungeon.level;
		int[] dist = new int[l.length()];
		Arrays.fill(dist, -1);
		ArrayList<Integer> queue = new ArrayList<>();
		queue.add(from);
		dist[from] = 0;
		for (int i = 0; i < queue.size(); i++){
			int c = queue.get(i);
			for (int n : PathFinder.NEIGHBOURS8){
				int k = c + n;
				if (k < 0 || k >= l.length() || dist[k] != -1 || !l.passable[k] || cool && l.water[k]) continue;
				if (Actor.findChar(k) instanceof Colossus) continue;
				dist[k] = dist[c] + 1;
				queue.add(k);
			}
		}
		return dist;
	}

	private static final int[] TOUR = {CathedralLevel.NARTHEX, CathedralLevel.CHAPEL, CathedralLevel.AISLE,
			CathedralLevel.TOWER, CathedralLevel.CRYPT, CathedralLevel.APSE, CathedralLevel.NAVE};

	//the middle cell of a zone, near enough
	private static int waypoint( CathedralLevel l, int zone, int from ){
		long sx = 0, sy = 0;
		int n = 0, w = l.width();
		for (int i = 0; i < l.length(); i++){
			if (l.zone(i) != zone || !l.passable[i]) continue;
			sx += i % w;
			sy += i / w;
			n++;
		}
		if (n == 0) return -1;
		int mid = (int) (sx / n) + (int) (sy / n) * w;
		int[] dist = walk(from, false);
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int i = 0; i < l.length(); i++){
			if (l.zone(i) != zone || !l.passable[i] || l.avoid[i] || dist[i] < 0) continue;
			int d = l.distance(i, mid);
			if (d < bestD){
				bestD = d;
				best = i;
			}
		}
		return best;
	}

	//the enemy right next to the hero, if any (the heart counts when it's touching)
	private static Char blocker( Hero me ){
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment != Char.Alignment.ENEMY || !m.isAlive()) continue;
			boolean next = m instanceof Colossus ? ((Colossus) m).touches(me.pos) : Dungeon.level.adjacent(m.pos, me.pos);
			if (next) return m;
		}
		return null;
	}

	private static int safeSpot( int from ){
		Level l = Dungeon.level;
		int[] dist = new int[l.length()];
		Arrays.fill(dist, -1);
		ArrayList<Integer> queue = new ArrayList<>();
		queue.add(from);
		dist[from] = 0;
		for (int i = 0; i < queue.size(); i++){
			int c = queue.get(i);
			if (c != from && TargetedCell.cells.get(c) == null && !l.avoid[c]) return c;
			if (dist[c] >= 4) continue;
			for (int n : PathFinder.NEIGHBOURS8){
				int k = c + n;
				if (k < 0 || k >= l.length() || dist[k] != -1 || !l.passable[k] || Actor.findChar(k) != null) continue;
				dist[k] = dist[c] + 1;
				queue.add(k);
			}
		}
		return -1;
	}
}
