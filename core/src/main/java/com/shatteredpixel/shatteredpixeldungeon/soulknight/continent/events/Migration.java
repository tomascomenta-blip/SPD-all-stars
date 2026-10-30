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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.events;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.BasaltBison;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.SkinlessColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.SootRats;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.WatcherFawn;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.IceElk;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.LighthouseCrab;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//a herd of one of the beasts of the biome crosses the floor along a straight corridor, right by
//the party: bison on the ash (they stampede when they get close), fawns in the forest, a black tide
//of rats in the city, elk on the moor, young skinless colossi crawling over the graveyard, crabs
//over the isles. whoever stands in the middle gets run over, but it's game walking past: meat,
//bones, parts, and the tameable ones can still be tamed. dust on the horizon gives it away first
public class Migration extends WorldEvent {

	static final int WARN = 0, CROSS = 1;

	//how far (walking) from the party they come in, and about how far they go out
	private static final int IN = 26, OUT = 30;
	//turns of warning, and the longest it can take
	private static final int WARN_TURNS = 10, LONGEST = 220;

	public int zone;
	int phase = WARN, timer;
	public int from = -1, to = -1;
	int size;
	int[] herd = new int[0];
	private boolean bolted = false;
	int crossed = 0;

	@Override
	public int kind() {
		return WorldEvents.MIGRATION;
	}

	@Override
	boolean begin( int layer ) {
		if (layer != OpenWorld.SURFACE) return false;
		Level l = Dungeon.level;
		int party = WorldEvents.partyCell();
		if (party == -1) return false;
		zone = Biome.current().zone;
		size = herdSize();
		int w = l.width();
		//both ends on ground this kind can walk, some way off, on opposite sides of the party and
		//joined through it: the way between them goes right by everyone
		boolean[] ground = ground(make());
		int near = nearest(ground, party, 8);
		if (near == -1){
			CoopLog.line("events: no ground for a herd near the party");
			return false;
		}
		int[] dist = walk(ground, near);
		ArrayList<Integer> starts = new ArrayList<>(), ends = new ArrayList<>();
		for (int c = 0; c < dist.length; c++){
			if (dist[c] < IN - 4) continue;
			if (dist[c] <= IN + 8 && WorldEvents.nearestHero(c) >= 16) starts.add(c);
			if (dist[c] <= OUT + 10) ends.add(c);
		}
		int px = party % w, py = party / w;
		to = -1;
		for (int tries = 0; tries < 20 && !starts.isEmpty(); tries++){
			from = Random.element(starts);
			int vx = from % w - px, vy = from / w - py;
			to = -1;
			//the most opposite way there is; by an edge of the map it may only be off to a side
			double best = 0.35;
			for (int c : ends){
				int ux = c % w - px, uy = c / w - py;
				double cos = (ux * (double) vx + uy * (double) vy) / Math.sqrt((ux * (double) ux + uy * (double) uy) * (vx * (double) vx + vy * (double) vy) + 1);
				if (cos < best){
					best = cos;
					to = c;
				}
			}
			//backed up against the edge of the land there's no other side: then just as far from where
			//they come in as the ground goes
			if (to == -1 && tries >= 10){
				int far = 0;
				for (int c : ends){
					int d = l.distance(c, from);
					if (d > far){
						far = d;
						to = c;
					}
				}
				if (far < 20) to = -1;
			}
			if (to != -1 && spots(size).size() >= Math.min(size, 2)) break;
			to = -1;
		}
		if (to == -1){
			CoopLog.line("events: no way across for a herd (" + starts.size() + " starts, " + ends.size() + " ends)");
			return false;
		}
		timer = WARN_TURNS;
		GLog.w(Messages.get(this, "warn", WorldEvents.compass(party, from), Messages.get(this, "herd_" + key())));
		card++;
		CoopLog.line("events: migration of " + size + " " + key() + " from " + from + " to " + to);
		return true;
	}

	public String key(){
		return Biome.zone(zone).key;
	}

	private int herdSize(){
		switch (zone){
			case Continent.Z_FLESH: return 7 + Random.Int(4);
			case Continent.Z_CITY:  return 10 + Random.Int(5);
			case Continent.Z_FROST: return 4 + Random.Int(2);
			case Continent.Z_BONES: return 3 + Random.Int(2);
			case Continent.Z_ISLES: return 3 + Random.Int(2);
			default:                return 4 + Random.Int(3);
		}
	}

	private Mob make(){
		switch (zone){
			case Continent.Z_FLESH: return new WatcherFawn();
			case Continent.Z_CITY:  return new SootRats();
			case Continent.Z_FROST: return new IceElk();
			case Continent.Z_BONES: return new SkinlessColossus();
			case Continent.Z_ISLES: return new LighthouseCrab();
			default:                return new BasaltBison();
		}
	}

	//where a beast of this kind can stand: the big ones by where their whole body fits (in the water
	//too for the ones that wade)
	private static boolean[] ground( Mob proto ){
		Level l = Dungeon.level;
		if (proto instanceof Colossus) return Colossus.fits(((Colossus) proto).size, (Colossus) proto);
		boolean[] g = new boolean[l.length()];
		for (int i = 0; i < g.length; i++) g[i] = l.passable[i] && !l.pit[i] && !l.water[i];
		return g;
	}

	//the closest of those cells, -1 if there's none within r
	private static int nearest( boolean[] ok, int cell, int r ){
		Level l = Dungeon.level;
		int w = l.width(), cx = cell % w, cy = cell / w;
		for (int d = 0; d <= r; d++){
			for (int y = cy - d; y <= cy + d; y++){
				for (int x = cx - d; x <= cx + d; x++){
					if (Math.max(Math.abs(x - cx), Math.abs(y - cy)) != d) continue;
					if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
					if (ok[x + y * w]) return x + y * w;
				}
			}
		}
		return -1;
	}

	//breadth first over those cells from one of them
	private static int[] walk( boolean[] ok, int from ){
		Level l = Dungeon.level;
		int n = l.length(), w = l.width(), h = l.height();
		int[] dist = new int[n];
		Arrays.fill(dist, -1);
		int[] queue = new int[n];
		int head = 0, tail = 0;
		dist[from] = 0;
		queue[tail++] = from;
		while (head < tail){
			int c = queue[head++];
			int x = c % w, y = c / w;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int nx = x + dx, ny = y + dy;
					if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
					int k = nx + ny * w;
					if (dist[k] != -1 || !ok[k]) continue;
					dist[k] = dist[c] + 1;
					queue[tail++] = k;
				}
			}
		}
		return dist;
	}

	//where they can come in: near the start of the corridor, out of everyone's way, and with a way
	//to the far end
	private ArrayList<Integer> spots( int n ){
		Level l = Dungeon.level;
		Mob proto = make();
		int big = proto instanceof Colossus ? ((Colossus) proto).size : 1;
		boolean[] fit = ground(proto);
		int[] way = walk(fit, to);
		int w = l.width(), cx = from % w, cy = from / w;
		ArrayList<Integer> out = new ArrayList<>();
		for (int r = 0; r <= 10 && out.size() < n; r++){
			for (int y = cy - r; y <= cy + r && out.size() < n; y++){
				for (int x = cx - r; x <= cx + r && out.size() < n; x++){
					if (Math.max(Math.abs(x - cx), Math.abs(y - cy)) != r) continue;
					if (x < 1 || y < 1 || x >= w - 1 || y >= l.height() - 1) continue;
					int c = x + y * w;
					if (!fit[c] || way[c] < 0) continue;
					if (Actor.findChar(c) != null || WorldEvents.nearestHero(c) < 14) continue;
					boolean clear = true;
					for (int o : out){
						if (l.distance(o, c) < big + 1){
							clear = false;
							break;
						}
					}
					if (clear) out.add(c);
				}
			}
		}
		return out;
	}

	// *** the turns ***

	@Override
	boolean act() {
		age++;
		if (phase == WARN){
			if (calming) return false;
			if (--timer > 0) return true;
			phase = CROSS;
			arrive();
			if (herd.length == 0) return false;
			return true;
		}
		Mob lead = null;
		int alive = 0;
		for (int id : herd){
			Actor a = Actor.findById(id);
			if (a instanceof Mob && ((Mob) a).isAlive() && ((Mob) a).buff(Migrant.class) != null){
				alive++;
				if (lead == null) lead = (Mob) a;
			}
		}
		if (alive == 0 || age > LONGEST){
			for (int id : herd){
				Actor a = Actor.findById(id);
				if (a instanceof Mob && ((Mob) a).buff(Migrant.class) != null) ((Mob) a).buff(Migrant.class).detach();
			}
			GLog.i(Messages.get(this, "gone"));
			CoopLog.line("events: migration over, " + crossed + " crossed");
			return false;
		}
		//the bison stampede once they're close: the lane ahead is marked as they bolt
		if (!bolted && lead instanceof BasaltBison && WorldEvents.nearestHero(lead.pos) <= 12){
			bolted = true;
			Level l = Dungeon.level;
			int w = l.width();
			int dx = Integer.signum(to % w - from % w), dy = Integer.signum(to / w - from / w);
			int bx = Math.max(0, Math.min(w - 1, lead.pos % w - dx * 3));
			int by = Math.max(0, Math.min(l.height() - 1, lead.pos / w - dy * 3));
			((BasaltBison) lead).panic(bx + by * w);
			CoopLog.line("events: the herd stampedes");
		}
		return true;
	}

	private void arrive(){
		ArrayList<Integer> cells = spots(size);
		herd = new int[cells.size()];
		for (int i = 0; i < cells.size(); i++){
			Mob m = make();
			m.pos = cells.get(i);
			m.state = m.WANDERING;
			if (m instanceof AshColossus) ((AshColossus) m).home = to;
			GameScene.add(m);
			Buff.affect(m, Migrant.class).to = to;
			m.followTo(to);
			herd[i] = m.id();
		}
		GLog.n(Messages.get(this, "cross_" + key()));
		card++;
		CoopLog.line("events: " + herd.length + " " + key() + " come in");
	}

	void crossed( Mob m, boolean stuck ){
		crossed++;
		CoopLog.line("events: a " + m.getClass().getSimpleName() + (stuck ? " wanders off" : " crosses out") + " t=" + (int) Actor.now());
		WorldEvents.vanish(m);
	}

	//walking with the herd: down the corridor whenever it isn't busy with someone
	public static class Migrant extends Buff {

		{
			type = buffType.NEUTRAL;
			announced = false;
		}

		public int to = -1;
		private int best = Integer.MAX_VALUE, stall = 0, last = -1;

		@Override
		public boolean act() {
			WorldEvent e = WorldEvents.active();
			if (!(e instanceof Migration) || !(target instanceof Mob) || target.alignment == Char.Alignment.ALLY
					|| target instanceof AshColossus && ((AshColossus) target).tamed){
				detach();
				return true;
			}
			Mob m = (Mob) target;
			int d = Senses.dist(m, to);
			if (d <= 3){
				((Migration) e).crossed(m, false);
				return true;
			}
			if (d < best){
				best = d;
				stall = 0;
			} else if (++stall >= 30 && WorldEvents.nearestHero(m.pos) > 10){
				//stuck somewhere nobody is looking: it wanders off out of the way
				((Migration) e).crossed(m, true);
				return true;
			}
			if (m.state == m.WANDERING || m.state == m.SLEEPING) m.followTo(to);
			ahead(m);
			last = m.pos;
			spend(TICK);
			return true;
		}

		//the ground the big ones are about to walk over, on the screen
		private void ahead( Mob m ){
			if (!(m instanceof Colossus) || last == -1 || last == m.pos) return;
			Level l = Dungeon.level;
			int w = l.width();
			int dx = Integer.signum(m.pos % w - last % w), dy = Integer.signum(m.pos / w - last / w);
			Colossus c = (Colossus) m;
			for (int k = 1; k <= 2; k++){
				for (int cell : c.cells()){
					int x = cell % w + dx * k, y = cell / w + dy * k;
					if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
					int n = x + y * w;
					if (!c.covers(n) && !l.solid[n] && l.heroFOV[n]) GameScene.targetedCell(n, 1f);
				}
			}
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("to", to);
			bundle.put("best", best);
			bundle.put("stall", stall);
			bundle.put("last", last);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			to = bundle.getInt("to");
			best = bundle.getInt("best");
			stall = bundle.getInt("stall");
			last = bundle.getInt("last");
		}
	}

	// *** for the screens ***

	@Override
	public float power() {
		return phase == WARN ? (WARN_TURNS - timer) / (float) WARN_TURNS : 1f;
	}

	@Override
	public int origin() {
		return phase == WARN || age < WARN_TURNS + 12 ? from : -1;
	}

	@Override
	public String title() {
		return Messages.get(this, "name");
	}

	@Override
	public String line() {
		return Messages.get(this, "herd_" + key());
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("zone", zone);
		bundle.put("phase", phase);
		bundle.put("timer", timer);
		bundle.put("from", from);
		bundle.put("to", to);
		bundle.put("size", size);
		bundle.put("herd", herd);
		bundle.put("bolted", bolted);
		bundle.put("crossed", crossed);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		zone = bundle.getInt("zone");
		phase = bundle.getInt("phase");
		timer = bundle.getInt("timer");
		from = bundle.getInt("from");
		to = bundle.getInt("to");
		size = bundle.getInt("size");
		herd = bundle.getIntArray("herd");
		bolted = bundle.getBoolean("bolted");
		crossed = bundle.getInt("crossed");
	}
}
