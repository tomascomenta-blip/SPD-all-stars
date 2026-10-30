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

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.WalkingPyre;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.LastStanding;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.LastKing;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.ForestHeart;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.FrostShepherd;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.YogTrueForm;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the land over there doesn't hold still: now and then something happens on the floor the party is
//on. the steps of a giant nobody ever sees, a tide of yog, the storm of the biome, a herd crossing.
//one at a time, never while a guardian is fighting, and every one gives warning (a line in the log
//and signs on the screen) before it arrives. what happens runs on the turns with the game's dice,
//the same on every device; how it looks and sounds is drawn by each screen for its own hero
public class WorldEvents {

	public static final String MESSAGES = "messages/soulknight/continent_events";

	public static final int STEPS = 0, TIDE = 1, STORM = 2, MIGRATION = 3;
	static final String[] NAMES = {"steps", "tide", "storm", "migration"};

	//turns of play left until the next one may come. the layers below count for less
	private static float wait = -1;
	//how many there have been this run, mixed into the dice of the next
	private static int count = 0;
	private static int last = -1;
	static volatile WorldEvent active;
	//turns spent on the open floors of the continent, to age what the events leave behind
	private static volatile int clock = 0;

	//testing: kinds still to come, and turns before the next one (the same on every device: a
	//cooperative host reloads the game once the guests are in, and the count starts over then too)
	private static ArrayList<Integer> tests = null;
	private static int testDelay = 0;
	private static boolean testsRead = false;

	public static void reset(){
		wait = -1;
		count = 0;
		last = -1;
		active = null;
		clock = 0;
		GiantSteps.clearPrints();
		//the test list belongs to this run of the program, not to the game: it outlives a reload
		testDelay = 3;
	}

	public static void store( Bundle b ){
		Bundle e = new Bundle();
		e.put("wait", wait);
		e.put("count", count);
		e.put("last", last);
		e.put("clock", clock);
		if (active != null) e.put("active", active);
		GiantSteps.storePrints(e);
		b.put("world_events", e);
	}

	public static void restore( Bundle b ){
		reset();
		if (!b.contains("world_events")) return;
		Bundle e = b.getBundle("world_events");
		wait = e.getFloat("wait");
		count = e.getInt("count");
		last = e.getInt("last");
		clock = e.getInt("clock");
		if (e.contains("active")) active = (WorldEvent) e.get("active");
		GiantSteps.restorePrints(e);
	}

	//Continent.onArrive, every floor of the continent
	public static void onArrive(){
		WorldEvent e = active;
		if (e != null && !e.here()){
			//the party walked off the floor in the middle of it
			CoopLog.line("events: " + NAMES[e.kind()] + " left behind on " + e.depth + "/" + e.branch);
			e.leave();
			active = null;
			wait = Math.max(wait, 60);
		}
		if (!(Dungeon.level instanceof OpenWorld) || !Continent.in()) return;
		if (tests == null && !testsRead && DevFlags.event() != null){
			testsRead = true;
			tests = new ArrayList<>();
			for (String s : DevFlags.event().split(",")){
				for (int k = 0; k < NAMES.length; k++){
					if (NAMES[k].equals(s.trim())) tests.add(k);
				}
			}
			testDelay = 3;
			//no kind named: they come on their own clock, as in a real run
			if (tests.isEmpty()) tests = null;
		}
		Actor.add(new Director());
	}

	public static int clock(){
		return clock;
	}

	public static WorldEvent active(){
		WorldEvent e = active;
		return e != null && e.here() ? e : null;
	}

	//the turns of the world, at the end of each
	public static class Director extends Actor {

		{
			actPriority = BUFF_PRIO;
		}

		@Override
		protected boolean act() {
			tick();
			spend(TICK);
			return true;
		}
	}

	private static void tick(){
		if (!(Dungeon.level instanceof OpenWorld) || !Continent.in() || Party.living().isEmpty()) return;
		int layer = ((OpenWorld) Dungeon.level).layer();
		clock++;
		WorldEvent e = active;
		if (e != null){
			if (!e.here()) return;
			if (!e.calming && guardianFight()){
				CoopLog.line("events: a guardian fights, the " + NAMES[e.kind()] + " winds down");
				e.calm();
			}
			if (!e.act()){
				CoopLog.line("events: " + NAMES[e.kind()] + " over t=" + (int) Actor.now());
				last = e.kind();
				active = null;
				wait = gap();
				if (tests != null) testDelay = 6;
			}
			return;
		}
		if (tests != null){
			if (!tests.isEmpty() && --testDelay <= 0){
				int kind = tests.remove(0);
				if (!start(kind, layer)) CoopLog.line("events: test " + NAMES[kind] + " has no room on this layer");
			}
			return;
		}
		if (wait < 0) wait = first();
		wait -= layer == OpenWorld.SURFACE ? 1f : 0.5f;
		if (wait > 0) return;
		if (guardianFight()){
			wait = 20;
			return;
		}
		//the clock in the dice too: one that has no room here doesn't come back to fail again and again
		Random.pushGenerator(Dungeon.seed + 0x3E7A + count * 7919L + clock);
		int kind = pick(layer);
		Random.popGenerator();
		if (kind == -1 || !start(kind, layer)) wait = 25;
	}

	//the dice of an event's setting up are its own: the rest of the game rolls the same with or without it
	private static boolean start( int kind, int layer ){
		WorldEvent e;
		switch (kind){
			case STEPS:     e = new GiantSteps(); break;
			case TIDE:      e = new YogTide(); break;
			case STORM:     e = new Storm(); break;
			default:        e = new Migration(); break;
		}
		e.depth = Dungeon.depth;
		e.branch = Dungeon.branch;
		Random.pushGenerator(Dungeon.seed + 0x5EED + count * 104729L + (long) Actor.now());
		boolean ok;
		try {
			ok = e.begin(layer);
		} finally {
			Random.popGenerator();
		}
		if (!ok) return false;
		count++;
		active = e;
		CoopLog.line("events: " + NAMES[kind] + " begins on " + e.depth + "/" + e.branch + " t=" + (int) Actor.now());
		return true;
	}

	//every so often, not all the time
	private static float first(){
		Random.pushGenerator(Dungeon.seed + 0x1F57);
		float f = Random.IntRange(220, 340);
		Random.popGenerator();
		return f;
	}

	private static float gap(){
		Random.pushGenerator(Dungeon.seed + 0x6A9 + count * 31L);
		float f = Random.IntRange(380, 620);
		Random.popGenerator();
		return f;
	}

	//what may come on each layer: under the ground only what makes sense there, and never the same twice running
	private static int pick( int layer ){
		float[] w = new float[4];
		if (layer == OpenWorld.SURFACE){
			w[STEPS] = 3;
			w[TIDE] = Biome.current() == Biome.ISLES ? 5 : 3;
			w[STORM] = 4;
			w[MIGRATION] = 3;
		} else if (layer == OpenWorld.DEPTHS){
			w[STEPS] = 1;
			w[TIDE] = 1;
		} else {
			w[TIDE] = 1;
		}
		int options = 0;
		for (float f : w) if (f > 0) options++;
		if (options > 1 && last >= 0) w[last] = 0;
		return Random.chances(w);
	}

	// *** guardians ***

	public static boolean guardian( Mob m ){
		return m instanceof WalkingPyre || m instanceof ForestHeart || m instanceof LastKing
				|| m instanceof FrostShepherd || m instanceof LastStanding || m instanceof YogTrueForm;
	}

	//a guardian of the floor is after someone
	public static boolean guardianFight(){
		for (Mob m : Dungeon.level.mobs){
			if (guardian(m) && m.isAlive() && m.state == m.HUNTING) return true;
		}
		return false;
	}

	// *** what the weather does (hooks in the senses and the sight) ***

	private static Storm storm(){
		WorldEvent e = active();
		return e instanceof Storm && ((Storm) e).raging() ? (Storm) e : null;
	}

	//Survival.sight: how much less far the heroes see
	public static int sightCut(){
		Storm s = storm();
		return s == null ? 0 : s.sightCut();
	}

	//Senses.dim: how much less far the beasts make the heroes out
	public static int hideCut(){
		Storm s = storm();
		return s == null ? 0 : s.hideCut();
	}

	//Senses.noise: how far a racket carries in this weather
	public static int muffle( int radius ){
		Storm s = storm();
		return s == null ? radius : Math.max(1, radius * s.noise() / 100);
	}

	//Senses.scent: the rain of blood washes the trail away
	public static boolean scentless(){
		Storm s = storm();
		return s != null && s.twist() == Storm.SCENTLESS;
	}

	//Continent.glowSight: the soot hides every glow
	public static boolean noGlow(){
		Storm s = storm();
		return s != null && s.twist() == Storm.NOGLOW;
	}

	// *** shared bits ***

	//the middle of the party, -1 if nobody is standing
	static int partyCell(){
		Level l = Dungeon.level;
		int w = l.width(), sx = 0, sy = 0, n = 0;
		for (Hero h : Party.living()){
			sx += h.pos % w;
			sy += h.pos / w;
			n++;
		}
		return n == 0 ? -1 : (sx / n) + (sy / n) * w;
	}

	static int nearestHero( int cell ){
		int best = Integer.MAX_VALUE;
		for (Hero h : Party.living()) best = Math.min(best, Dungeon.level.distance(cell, h.pos));
		return best;
	}

	//a word for the way from a cell to another: north, south-east...
	static String compass( int from, int to ){
		int w = Dungeon.level.width();
		int dx = to % w - from % w, dy = to / w - from / w;
		int ax = Math.abs(dx), ay = Math.abs(dy);
		int sx = ax * 5 < ay * 2 ? 0 : Integer.signum(dx);
		int sy = ay * 5 < ax * 2 ? 0 : Integer.signum(dy);
		int k;
		if (sy < 0) k = sx < 0 ? 7 : sx > 0 ? 1 : 0;
		else if (sy > 0) k = sx < 0 ? 5 : sx > 0 ? 3 : 4;
		else k = sx < 0 ? 6 : 2;
		return Messages.get(WorldEvents.class, "dir_" + k);
	}

	//somewhere free to stand near a cell, closest first, -1 if nothing within r
	static int freeNear( int cell, int r ){
		Level l = Dungeon.level;
		int w = l.width(), cx = cell % w, cy = cell / w;
		for (int d = 0; d <= r; d++){
			for (int y = cy - d; y <= cy + d; y++){
				for (int x = cx - d; x <= cx + d; x++){
					if (Math.max(Math.abs(x - cx), Math.abs(y - cy)) != d) continue;
					if (x < 1 || y < 1 || x >= w - 1 || y >= l.height() - 1) continue;
					int c = x + y * w;
					if (l.passable[c] && !l.pit[c] && Actor.findChar(c) == null) return c;
				}
			}
		}
		return -1;
	}

	//gone without a death: no experience, no remains, nothing counted
	static void vanish( Mob m ){
		m.EXP = 0;
		m.alignment = Char.Alignment.NEUTRAL;
		m.destroy();
		if (m.sprite != null) EventScreen.fadeOut(m.sprite);
	}
}
