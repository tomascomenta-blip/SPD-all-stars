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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.land.AshSurface;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.ColossusPresence;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.EdgeTransition;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalFinale;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

//the true ending of the dark continent. yog's true form falls with seven drops of essence in the
//party and the whole continent wakes up: everything over there comes for them, more of it and
//faster, the corruption gets in quicker and there's no leaping home. the way out is the portal they
//came in by, on the south edge of the ash plains, and crossing it with the essence seals it.
//all of it is game state, the same on every device; what it looks like is AwakeSky and AwakeScenes
public class Awakening {

	public static final String MESSAGES = "messages/soulknight/continent_awake";

	//how far off the party something still hears the call (the big ones come from anywhere)
	public static final int CALL_RANGE = 40;
	//the longest leap a hero can make over there while it's awake
	public static final int LEAP = 8;
	//what sealing the portal adds to the score, on top of what the continent gives (a fall never gets this)
	public static final float SEAL_BONUS = 1.5f;

	private static boolean awake = false;
	private static boolean sealed = false;
	//when and where it woke, for the log and the tests
	private static int wokeAt = -1;
	private static int wokeZone = 0;

	public static boolean on(){
		return awake;
	}

	public static boolean sealed(){
		return sealed;
	}

	public static void reset(){
		awake = false;
		sealed = false;
		wokeAt = -1;
		wokeZone = 0;
	}

	//with the rest of the continent (Continent.store)
	public static void store( Bundle b ){
		b.put("awake", awake);
		b.put("sealed", sealed);
		b.put("woke_at", wokeAt);
		b.put("woke_zone", wokeZone);
	}

	public static void restore( Bundle b ){
		awake = b.getBoolean("awake");
		sealed = b.getBoolean("sealed");
		wokeAt = b.contains("woke_at") ? b.getInt("woke_at") : -1;
		wokeZone = b.getInt("woke_zone");
	}

	private static ArrayList<Hero> party(){
		ArrayList<Hero> list = new ArrayList<>();
		if (Party.active()) list.addAll(Party.heroes());
		else if (Dungeon.hero != null) list.addAll(Collections.singletonList(Dungeon.hero));
		return list;
	}

	// *** waking up ***

	//yog's true form fell with seven drops or more (YogTrueForm.onDefeated). a game action:
	//every device runs it on the same turn
	public static void wake(){
		if (awake) return;
		awake = true;
		wokeAt = (int) Actor.now();
		wokeZone = Continent.zone();
		CoopLog.line("awake: the continent wakes on floor " + Continent.floor() + " t=" + wokeAt
				+ " essence " + Continent.essence);
		GLog.n(Messages.get(Awakening.class, "woke"));
		Hero leader = null;
		for (Hero h : party()){
			if (leader == null && h.isAlive() && !h.coopDown) leader = h;
			Buff.affect(h, Hunted.class);
		}
		if (leader != null) Buff.affect(leader, WakeCall.class);
	}

	//Continent.onArrive, every floor and every load: has to be safe to run twice
	public static void onArrive(){
		if (!awake) return;
		for (Hero h : party()) Buff.affect(h, Hunted.class);
		Level level = Dungeon.level;
		//the portal is on the map from anywhere on the plains, seen or not
		int portal = portalCell(level);
		if (portal != -1){
			int w = level.width();
			for (int dy = -2; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int c = portal + dx + dy * w;
					if (c >= 0 && c < level.length()) level.mapped[c] = true;
				}
			}
		}
		AwakeTest.onArrive(level);
	}

	// *** the monsters ***

	//an enemy of the continent that answers the call right now
	public static boolean hunts( Mob m ){
		if (!awake || !Continent.in() || !(m instanceof ContinentMob) || !m.isAlive()) return false;
		if (m.alignment != Char.Alignment.ENEMY || m.state == m.PASSIVE) return false;
		if (Char.hasProp(m, Char.Property.IMMOVABLE)) return false;
		return !(m instanceof AshColossus) || !((AshColossus) m).tamed;
	}

	private static int dist( Mob m, int cell ){
		if (m instanceof Colossus) return ((Colossus) m).reach(cell);
		return Dungeon.level.distance(m.pos, cell);
	}

	//the closest of the party still standing, or null. whoever can't be seen is left alone: going
	//invisible is still a way out
	private static Hero nearest( Mob m ){
		Hero best = null;
		int bestD = Integer.MAX_VALUE;
		for (Hero h : Party.living()){
			if (h.invisible > 0) continue;
			int d = dist(m, h.pos);
			if (d < bestD){
				best = h;
				bestD = d;
			}
		}
		return best;
	}

	//the big ones walk to the party from wherever they are (Colossus.farFromParty)
	public static boolean calls( Colossus c ){
		return hunts(c) && c.size >= ColossusPresence.BIG && c.solid();
	}

	//ContinentMob.act, before anything else: grazers, the ones that keep to a patch of ground, the
	//sleepers in reach, all of them hunt the party. the cell it should head to, or -1 to leave its
	//target alone (it sees who it's after, or it doesn't answer)
	public static int drive( Mob m ){
		if (!hunts(m) || m.state == m.FLEEING) return -1;
		Hero h = nearest(m);
		if (h == null) return -1;
		if (dist(m, h.pos) > CALL_RANGE && !(m instanceof Colossus && calls((Colossus) m))) return -1;
		Char e = m.focusTarget();
		if (e == null || !e.isAlive() || e.invisible > 0 || e instanceof Hero && ((Hero) e).coopDown){
			m.aggro(h);
			e = h;
		}
		if (m.state != m.HUNTING) m.state = m.HUNTING;
		boolean[] fov = m.fieldOfView;
		if (fov != null && e.pos >= 0 && e.pos < fov.length && fov[e.pos]) return -1;
		return e.pos;
	}

	//Mob.act, on whoever it picked: whatever kept it from looking at the party (calm, chains, its
	//ground) doesn't count while it's awake
	public static Char prey( Mob m, Char picked ){
		if (picked != null || !awake || m.state != m.HUNTING || !hunts(m)) return picked;
		return nearest(m);
	}

	//the faster the place comes back once it's awake (AshLand.Comeback)
	public static float comeback( float delay ){
		return awake && Continent.in() ? delay / 3f : delay;
	}

	//corruption from the place itself gets in quicker (DarkTaint)
	public static int taintEvery( int every ){
		if (!awake || every <= 0) return every;
		return Math.max(2, every * 3 / 5);
	}

	// *** no way to skip the road ***

	//Dungeon.interfloorTeleportAllowed: no beacons, no passages home
	public static boolean blocksTravel(){
		return awake && Continent.in();
	}

	//a teleport of that character to that cell: false = it doesn't happen
	public static boolean mayLeap( Char ch, int to ){
		if (!blocksTravel() || !(ch instanceof Hero) || ch.pos < 0) return true;
		if (Dungeon.level.distance(ch.pos, to) <= LEAP) return true;
		if (ch == Dungeon.local()) GLog.w(Messages.get(Awakening.class, "held"));
		return false;
	}

	//random teleports only land close by
	public static boolean leapCell( Char ch, int cell ){
		return !blocksTravel() || !(ch instanceof Hero) || ch.pos < 0 || Dungeon.level.distance(ch.pos, cell) <= LEAP;
	}

	// *** the portal ***

	//the portal on the ash plains, the way they came in, or -1 if this level hasn't got it
	public static int portalCell( Level level ){
		LevelTransition t = portal(level);
		return t == null ? -1 : t.cell();
	}

	public static LevelTransition portal( Level level ){
		if (!(level instanceof AshSurface) || Biome.branch(Dungeon.branch) != Biome.ASH) return null;
		for (LevelTransition t : level.transitions){
			if (isPortal(t)) return t;
		}
		return null;
	}

	private static boolean isPortal( LevelTransition t ){
		return t != null && t.type == LevelTransition.Type.REGULAR_ENTRANCE
				&& t.destDepth == ThalFinale.DEPTH && t.destBranch == ThalFinale.BRANCH;
	}

	//AshLand.activateTransition: a hero walks into the portal. with the essence it gets sealed and
	//that's the end (true = taken over); without it they go through as always
	public static boolean closes( Hero hero, LevelTransition t ){
		if (!awake || sealed || !isPortal(t) || !(Dungeon.level instanceof AshSurface)) return false;
		if (Continent.essence < Continent.NEEDED){
			GLog.w(Messages.get(Awakening.class, "too_few", Continent.essence, Continent.NEEDED));
			CoopLog.line("awake: portal crossed with only " + Continent.essence + " essence, not sealed");
			return false;
		}
		CoopLog.line("awake: hero " + Party.indexOf(hero) + " walks into the portal with " + Continent.essence
				+ " essence t=" + (int) Actor.now());
		Buff.affect(hero, PortalSeal.class);
		hero.spendAndNext(Actor.TICK);
		return true;
	}

	//the straight way between the isles and the plains caves in while it's awake: home is the long
	//way round, through the moor or the graveyard and then the forest or the city
	public static boolean caved( int a, int b ){
		return awake && !sealed && (a == Continent.Z_ASH && b == Continent.Z_ISLES || a == Continent.Z_ISLES && b == Continent.Z_ASH);
	}

	//walking into that pass: it's rubble, they stay on this side
	public static boolean blocks( Hero hero, LevelTransition t ){
		if (!(t instanceof EdgeTransition) || !caved(Continent.zone(), Biome.branch(t.destBranch).zone)) return false;
		if (hero == Dungeon.local()) GLog.w(Messages.get(Awakening.class, "caved"));
		return true;
	}

	//PortalSeal, once the essence went in
	static void seal(){
		sealed = true;
		CoopLog.line("awake: the portal is sealed t=" + (int) Actor.now() + ", woke at " + wokeAt
				+ " in zone " + wokeZone);
	}

	// *** what's left of it ***

	//burning a drop would leave the party short to seal it: SacredTorch says so before it asks
	public static String torchWarning(){
		if (!awake || Continent.essence > Continent.NEEDED) return "";
		return Messages.get(Awakening.class, "torch", Continent.NEEDED) + "\n\n";
	}

	public static float scoreBonus(){
		return sealed ? SEAL_BONUS : 0;
	}

	//the two ways a run can end over there, for the rankings
	public static class Sealed {
	}

	public static class Fallen {
	}

	//Rankings.Record.desc, for a win: the continent's own line, or null
	public static String rankingsDesc( Class cause ){
		if (cause != Sealed.class && cause != Fallen.class) return null;
		return Messages.get(cause, "rankings_desc");
	}

	// *** which way is home ***

	//the zone next to this one on the way to the ash plains, 0 if this is them (or none)
	public static int nextZoneHome( int zone ){
		if (zone == Continent.Z_ASH) return 0;
		ArrayList<Integer> queue = new ArrayList<>();
		int[] from = new int[16];
		Arrays.fill(from, -1);
		queue.add(Continent.Z_ASH);
		from[Continent.Z_ASH] = Continent.Z_ASH;
		while (!queue.isEmpty()){
			int z = queue.remove(0);
			Biome b = Biome.zone(z);
			for (int side = 0; side < 4; side++){
				int n = b.next[side];
				if (n == 0 || from[n] != -1 || !Continent.zoneOpen(n) || caved(z, n)) continue;
				from[n] = z;
				if (n == zone) return z;
				queue.add(n);
			}
		}
		return 0;
	}

	//the cell the party should head for on this level to get home: the portal, the pass towards the
	//plains, or the way up. -1 if there's nothing to point at
	public static int homeward( Level level ){
		if (!(level instanceof OpenWorld)) return -1;
		int portal = portalCell(level);
		if (portal != -1) return portal;
		if (((OpenWorld) level).layer() != OpenWorld.SURFACE){
			LevelTransition up = level.getTransition((LevelTransition.Type) null);
			return up == null ? -1 : up.cell();
		}
		int next = nextZoneHome(Continent.zone());
		if (next == 0) return -1;
		for (LevelTransition t : level.transitions){
			if (t.destBranch == Biome.zone(next).branch && t.destDepth == 21){
				return level.pointToCell(t.center());
			}
		}
		return -1;
	}
}
