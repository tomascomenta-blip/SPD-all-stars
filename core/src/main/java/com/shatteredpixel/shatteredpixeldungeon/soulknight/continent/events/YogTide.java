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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogEye;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

//a wave of yog's corruption comes in over an edge of the land, or wells up out of a hollow, and
//spreads along the ground a cell a turn. inside it the corruption climbs fast, and larvae and eyes
//of yog come with it. it stops a while past where the party was, then ebbs back the way it came.
//what it brought can't live long out of it: a few turns on dry ground and they melt
public class YogTide extends WorldEvent {

	static final int WARN = 0, RISE = 1, HOLD = 2, EBB = 3;

	//turns on dry ground before what came with the tide melts
	public static final int MELT = 5;

	int phase = WARN, timer;
	int[] source = new int[0];
	//the edge it comes in by (Biome.N...), -1 when it wells up from below
	int side = -1;
	//how far along the ground it has come, and how far it will
	public volatile int reach = 0;
	int top = 0;
	int brood = 0, cap = 2;
	private boolean broodSaid = false;
	//walking distance from the source over open ground, for every cell (-1 out of reach)
	private transient volatile int[] dist;
	private transient boolean soakSaid = false;
	private int soakTurn = 0;

	@Override
	public int kind() {
		return WorldEvents.TIDE;
	}

	@Override
	boolean begin( int layer ) {
		Level l = Dungeon.level;
		int party = WorldEvents.partyCell();
		if (party == -1) return false;
		int[] fromParty = spread(l, new int[]{party});

		//over an edge of the land, if one is close enough
		ArrayList<Integer> edges = new ArrayList<>();
		if (layer == OpenWorld.SURFACE){
			for (int s = 0; s < 4; s++){
				int d = closest(fromParty, edge(l, s));
				if (d >= 12 && d <= 40) edges.add(s);
			}
		}
		//or out of a hollow some way off: black water, lava, a pit
		ArrayList<Integer> wells = new ArrayList<>();
		for (int c = 0; c < l.length(); c++){
			int d = fromParty[c];
			if (d >= 12 && d <= 28 && (l.pit[c] || l.water[c])) wells.add(c);
		}
		if (wells.isEmpty()){
			for (int c = 0; c < l.length(); c++){
				int d = fromParty[c];
				if (d >= 14 && d <= 22 && l.passable[c]) wells.add(c);
			}
		}
		if (!edges.isEmpty() && (wells.isEmpty() || Random.Int(2) == 0)){
			side = Random.element(edges);
			source = edge(l, side);
		} else if (!wells.isEmpty()){
			int c = Random.element(wells);
			ArrayList<Integer> around = new ArrayList<>();
			int w = l.width();
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int n = c + dx + dy * w;
					if (n >= 0 && n < l.length() && !l.solid[n]) around.add(n);
				}
			}
			source = new int[around.size()];
			for (int i = 0; i < source.length; i++) source[i] = around.get(i);
			side = -1;
		} else {
			return false;
		}
		dist = spread(l, source);
		if (dist[party] < 0) return false;
		top = Math.min(64, dist[party] + 8 + Random.Int(6));
		cap = Party.living().size() >= 2 ? 3 : 2;
		timer = 10;
		String way = WorldEvents.compass(party, source[source.length / 2]);
		GLog.w(Messages.get(this, side >= 0 ? "warn_edge" : (layer == OpenWorld.SURFACE ? "warn_well" : "warn_deep"), way));
		card++;
		CoopLog.line("events: tide from " + (side >= 0 ? "edge " + side : "cell " + source[0]) + ", party at " + dist[party] + ", up to " + top);
		return true;
	}

	//the first open cells in from that edge of the map
	private static int[] edge( Level l, int side ){
		int w = l.width(), h = l.height();
		ArrayList<Integer> out = new ArrayList<>();
		int across = side % 2 == 0 ? w : h, deep = side % 2 == 0 ? h / 3 : w / 3;
		for (int a = 0; a < across; a++){
			for (int d = 0; d < deep; d++){
				int x, y;
				switch (side){
					case 0:  x = a; y = d; break;
					case 1:  x = w - 1 - d; y = a; break;
					case 2:  x = a; y = h - 1 - d; break;
					default: x = d; y = a; break;
				}
				int c = x + y * w;
				if (!l.solid[c]){
					out.add(c);
					break;
				}
			}
		}
		int[] arr = new int[out.size()];
		for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
		return arr;
	}

	private static int closest( int[] dist, int[] cells ){
		int best = Integer.MAX_VALUE;
		for (int c : cells) if (dist[c] >= 0) best = Math.min(best, dist[c]);
		return best;
	}

	//breadth first over everything that isn't solid (it flows over water and pits too)
	static int[] spread( Level l, int[] from ){
		int n = l.length(), w = l.width(), h = l.height();
		int[] dist = new int[n];
		Arrays.fill(dist, -1);
		int[] queue = new int[n];
		int head = 0, tail = 0;
		for (int c : from){
			if (c >= 0 && c < n && dist[c] == -1 && !l.solid[c]){
				dist[c] = 0;
				queue[tail++] = c;
			}
		}
		while (head < tail){
			int c = queue[head++];
			int x = c % w, y = c / w;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int nx = x + dx, ny = y + dy;
					if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
					int k = nx + ny * w;
					if (dist[k] != -1 || l.solid[k]) continue;
					dist[k] = dist[c] + 1;
					queue[tail++] = k;
				}
			}
		}
		return dist;
	}

	public int[] dist(){
		return dist;
	}

	public boolean covers( int cell ){
		int[] d = dist;
		return phase != WARN && d != null && cell >= 0 && cell < d.length && d[cell] >= 0 && d[cell] <= reach;
	}

	// *** the turns ***

	@Override
	boolean act() {
		age++;
		if (dist == null) dist = spread(Dungeon.level, source);
		switch (phase){
			case WARN:
				if (calming) return false;
				if (--timer <= 0){
					phase = RISE;
					GLog.n(Messages.get(this, "rise"));
					card++;
				}
				break;
			case RISE:
				reach++;
				if (reach >= top || calming){
					phase = HOLD;
					timer = calming ? 0 : 24 + Random.Int(12);
				}
				break;
			case HOLD:
				if (--timer <= 0){
					phase = EBB;
					GLog.i(Messages.get(this, "ebb"));
				}
				break;
			default:
				reach = Math.max(0, reach - (calming ? 2 : 1));
				if (reach <= 0){
					GLog.i(Messages.get(this, "gone"));
					return false;
				}
				break;
		}
		if (phase != WARN){
			soak();
			if (phase == RISE || phase == HOLD) spawn();
		}
		return true;
	}

	//inside it the corruption climbs a point every other turn: enough to hurt, with time to get out
	private void soak(){
		soakTurn++;
		for (Hero h : Party.living()){
			if (!covers(h.pos)) continue;
			if (soakTurn % 2 == 0) Continent.taint(h, 1);
			if (h == Dungeon.local() && !soakSaid){
				soakSaid = true;
				GLog.w(Messages.get(this, "soaked"));
			}
		}
	}

	//now and then something of yog comes in on the front of it, near enough to someone to matter
	private void spawn(){
		if (calming || brood >= cap || age % 6 != 0) return;
		Level l = Dungeon.level;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c = 0; c < l.length(); c++){
			int d = dist[c];
			if (d < 0 || d > reach || d < reach - 5) continue;
			if (!l.passable[c] || l.pit[c] || Actor.findChar(c) != null) continue;
			int hd = WorldEvents.nearestHero(c);
			if (hd >= 5 && hd <= 13) cells.add(c);
		}
		if (cells.isEmpty()) return;
		int c = Random.element(cells);
		HashSet<Mob> before = new HashSet<>(l.mobs);
		Mob m = brood % 2 == 0 ? new YogLarva() : new YogEye();
		m.pos = c;
		m.state = m.WANDERING;
		GameScene.add(m);
		if (m instanceof YogLarva) ((YogLarva) m).hatch();
		brood++;
		Hero h = Party.nearestLiving(c);
		for (Mob n : AshFauna.sorted(l.mobs)){
			if (before.contains(n)) continue;
			Buff.affect(n, Brood.class);
			if (h != null) n.beckon(h.pos);
		}
		if (!broodSaid){
			broodSaid = true;
			GLog.w(Messages.get(this, "brood"));
		}
		CoopLog.line("events: the tide brings a " + m.getClass().getSimpleName() + " at " + c);
	}

	//what came with the tide: out of it, it melts
	public static class Brood extends Buff {

		{
			type = buffType.NEUTRAL;
			announced = false;
		}

		private int dry = 0;

		@Override
		public boolean act() {
			WorldEvent e = WorldEvents.active();
			boolean wet = e instanceof YogTide && ((YogTide) e).covers(target.pos);
			dry = wet ? 0 : dry + 1;
			if (dry >= MELT && target instanceof Mob && target.isAlive()){
				if (Dungeon.level.heroFOV[target.pos]) CellEmitter.get(target.pos).burst(ShadowParticle.UP, 8);
				CoopLog.line("events: " + target.getClass().getSimpleName() + " melts out of the tide");
				WorldEvents.vanish((Mob) target);
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("dry", dry);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			dry = bundle.getInt("dry");
		}
	}

	// *** for the screens ***

	@Override
	public float power() {
		return phase == WARN || top <= 0 ? 0 : Math.min(1f, reach / (float) top);
	}

	@Override
	public int origin() {
		return phase == WARN || phase == RISE && reach < 8 ? source[source.length / 2] : -1;
	}

	@Override
	public String title() {
		return Messages.get(this, "name");
	}

	@Override
	public String line() {
		return Messages.get(this, phase == WARN ? "card_warn" : "card_rise");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("phase", phase);
		bundle.put("timer", timer);
		bundle.put("source", source);
		bundle.put("side", side);
		bundle.put("reach", reach);
		bundle.put("top", top);
		bundle.put("brood", brood);
		bundle.put("cap", cap);
		bundle.put("brood_said", broodSaid);
		bundle.put("soak_turn", soakTurn);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		phase = bundle.getInt("phase");
		timer = bundle.getInt("timer");
		source = bundle.getIntArray("source");
		side = bundle.getInt("side");
		reach = bundle.getInt("reach");
		top = bundle.getInt("top");
		soakTurn = bundle.getInt("soak_turn");
		brood = bundle.getInt("brood");
		cap = bundle.getInt("cap");
		broodSaid = bundle.getBoolean("brood_said");
	}
}
