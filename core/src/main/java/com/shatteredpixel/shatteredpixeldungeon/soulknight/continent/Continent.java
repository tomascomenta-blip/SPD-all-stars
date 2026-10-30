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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.DeadEndLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.boss.AshBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.land.AshAbyss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.land.AshDepths;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.land.AshSurface;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.AwakeTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.Awakening;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral.Cathedral;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral.CathedralLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral.CathedralTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.events.WorldEvents;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Hunt;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Mutations;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.Survival;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.LandWalk;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalFinale;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward.SpecPowers;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;

//the dark continent, through Thal-Vekhorr's portal. each zone is its own branch and uses depths
//21-25 inside it (so every table that goes by depth thinks "deep halls" and nothing breaks).
//a zone is an open world: its surface is 21, the tunnels under it 22 and its abyss 23
//(ash plains = branch 4, flesh forest = 5, devoured city = 6, frost moor = 7, colossi
//graveyard = 8, isles = 9). the surfaces join at their edges (see Biome).
//old saves may still be on the ash floors from before (AshLevel, the pyre's arena on 25)
public class Continent {

	public static final int ASH   = 4;
	public static final int FLESH = 5;
	public static final int CITY  = 6;
	public static final int FROST = 7;
	public static final int BONES = 8;
	public static final int ISLES = 9;

	//where the sacred essence comes from. every zone but the isles has four places that may hold
	//a drop: its besieged shrine, its guardian, the bottom of its abyss and one of its angry
	//colossi. each run two of the four are dry, so there are 10 drops and 7 close the portal
	public static final int SHRINE_ASH = 0, SHRINE_FLESH = 1, SHRINE_CITY = 2;
	public static final int GUARDIAN_ASH = 3, GUARDIAN_FLESH = 4, GUARDIAN_CITY = 5;
	//6 was the master's drop, from before: no longer handed out
	public static final int ABYSS_ASH = 7, ABYSS_FLESH = 8, ABYSS_CITY = 9;
	public static final int COLOSSUS_ASH = 10, COLOSSUS_FLESH = 11, COLOSSUS_CITY = 12;
	//every zone of the open world by number, and the four kinds of place that may hold a drop.
	//source(zone, kind) is the source id for essenceAt / addEssence (ash, flesh and city keep their
	//old numbers, so older saves still know what they took)
	public static final int Z_ASH = 1, Z_FLESH = 2, Z_CITY = 3, Z_FROST = 4, Z_BONES = 5, Z_ISLES = 6;
	public static final int SHRINE = 0, GUARDIAN = 1, ABYSS = 2, COLOSSUS = 3;

	public static int source( int zone, int kind ){
		switch (zone){
			case Z_ASH:   return new int[]{SHRINE_ASH, GUARDIAN_ASH, ABYSS_ASH, COLOSSUS_ASH}[kind];
			case Z_FLESH: return new int[]{SHRINE_FLESH, GUARDIAN_FLESH, ABYSS_FLESH, COLOSSUS_FLESH}[kind];
			case Z_CITY:  return new int[]{SHRINE_CITY, GUARDIAN_CITY, ABYSS_CITY, COLOSSUS_CITY}[kind];
			case Z_FROST: return 13 + kind;
			case Z_BONES: return 17 + kind;
			default:      return -1;
		}
	}

	public static final int NEEDED = 7;
	public static final int TOTAL = 10;
	//the zones that hold essence, two drops each
	private static final int[] ESSENCE_ZONES = {Z_ASH, Z_FLESH, Z_CITY, Z_FROST, Z_BONES};
	public static final int DRY_PER_ZONE = 2;

	//zones that are actually in the game yet
	public static final int OPEN_ZONES = Z_ISLES;

	public static int essence = 0;
	public static int claimed = 0;
	//deepest continent floor reached, for the score
	public static int deepest = 0;
	//which sources hold a drop this run (a bit per source), -1 until rolled
	private static int sources = -1;
	//the zones the party has set foot in, a bit each (for the map)
	public static int visited = 0;

	public static void reset(){
		essence = 0;
		claimed = 0;
		deepest = 0;
		sources = -1;
		visited = 0;
		Hunt.reset();
		Awakening.reset();
		WorldEvents.reset();
		Cathedral.reset();
	}

	public static void store( Bundle quests ){
		Bundle b = new Bundle();
		b.put("essence", essence);
		b.put("claimed", claimed);
		b.put("deepest", deepest);
		b.put("sources", sources());
		b.put("visited", visited);
		Hunt.store(b);
		Awakening.store(b);
		WorldEvents.store(b);
		Cathedral.store(b);
		quests.put("dark_continent", b);
	}

	public static void restore( Bundle quests ){
		if (!quests.contains("dark_continent")){
			reset();
			return;
		}
		Bundle b = quests.getBundle("dark_continent");
		essence = b.getInt("essence");
		claimed = b.getInt("claimed");
		deepest = b.getInt("deepest");
		sources = b.contains("sources") ? b.getInt("sources") : -1;
		visited = b.getInt("visited");
		Hunt.restore(b);
		Awakening.restore(b);
		WorldEvents.restore(b);
		Cathedral.restore(b);
	}

	//same seed, same dry sources: every device of a co-op run gets the same without talking
	private static int sources(){
		if (sources == -1){
			Random.pushGenerator(Dungeon.seed + 0x4E55);
			int mask = 0;
			for (int zone : ESSENCE_ZONES){
				Integer[] kinds = {SHRINE, GUARDIAN, ABYSS, COLOSSUS};
				Random.shuffle(kinds);
				for (int i = DRY_PER_ZONE; i < kinds.length; i++){
					mask |= 1 << source(zone, kinds[i]);
				}
			}
			Random.popGenerator();
			sources = mask;
		}
		return sources;
	}

	//does that source hold a drop of essence this run? shrines, guardians, the abyss and the
	//colossi all ask before they give one
	public static boolean essenceAt( int source ){
		return source >= 0 && (sources() & (1 << source)) != 0;
	}

	public static boolean branch( int branch ){
		return branch >= ASH && branch <= ISLES;
	}

	public static boolean in(){
		return branch(Dungeon.branch);
	}

	//Z_ASH .. Z_ISLES
	public static int zone( int depth, int branch ){
		return Biome.branch(branch).zone;
	}

	public static int zone(){
		return in() ? zone(Dungeon.depth, Dungeon.branch) : 0;
	}

	//1-15
	public static int floor( int depth, int branch ){
		return (branch - ASH) * 5 + (depth - 20);
	}

	public static int floor(){
		return in() ? floor(Dungeon.depth, Dungeon.branch) : 0;
	}

	public static int displayDepth(){
		return in() ? 26 + floor() : Dungeon.depth;
	}

	//the open world has places, not floors: null anywhere else
	public static String place(){
		if (in() && Cathedral.here()) return Cathedral.place();
		if (!in() || !(Dungeon.level instanceof OpenWorld)) return null;
		return place(Biome.current(), ((OpenWorld) Dungeon.level).layer());
	}

	//the ash plains keep their old keys (place_0, place_1...), the others go by biome
	private static String key( Biome b ){
		return b == Biome.ASH ? "" : b.key + "_";
	}

	public static String place( Biome b, int layer ){
		return Messages.get(Continent.class, "place_" + key(b) + layer);
	}

	//what the log says when the party gets somewhere, null on a regular floor
	public static String arrival(){
		if (in() && Cathedral.here()) return Cathedral.arrival();
		if (place() == null) return null;
		return Messages.get(Continent.class, "arrive_" + key(Biome.current()) + ((OpenWorld) Dungeon.level).layer());
	}

	//under the depth icon of the menu: the surface is 0, the tunnels -1 and the abyss -2
	public static String depthLabel(){
		if (Cathedral.here()) return Cathedral.depthLabel();
		if (place() == null) return Integer.toString(displayDepth());
		int layer = ((OpenWorld) Dungeon.level).layer();
		return layer == 0 ? "0" : "-" + layer;
	}

	private static String where(){
		String p = place();
		return p != null ? p : Messages.get(Continent.class, "floor", displayDepth());
	}

	public static boolean zoneOpen( int zone ){
		return zone <= OPEN_ZONES;
	}

	public static boolean hasClaimed( int source ){
		return (claimed & (1 << source)) != 0;
	}

	//every device runs this at the same point (picking up the essence is a game action)
	public static void addEssence( int source ){
		if (hasClaimed(source)) return;
		claimed |= 1 << source;
		essence++;
		CoopLog.line("continent: essence " + essence + "/" + NEEDED + " from " + source);
		GLog.p(Messages.get(Continent.class, "essence", essence, NEEDED));
	}

	//"the essence calls": the more you carry, the meaner everything over there gets
	public static float mobFactor(){
		return 1f + 0.08f * essence;
	}

	//corruption, heroes only. a lightbearer's charge of essence soaks one hit of it
	public static void taint( Char ch, int amount ){
		if (!(ch instanceof Hero) || amount <= 0 || !ch.isAlive()) return;
		SpecPowers.HolyEssence ward = ch.buff(SpecPowers.HolyEssence.class);
		if (ward != null && ward.count > 0){
			ward.count--;
			return;
		}
		amount = Mutations.taint(ch, amount);
		if (amount > 0) Buff.affect(ch, DarkTaint.class).add(amount);
	}

	public static float hpFactor( Char ch ){
		return in() && ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY ? mobFactor() : 1f;
	}

	public static float scoreBonus(){
		float b = 0.1f * essence;
		if (deepest > 0) b += 0.25f + 0.02f * deepest;
		return b + Awakening.scoreBonus();
	}

	private static ArrayList<Hero> party(){
		ArrayList<Hero> list = new ArrayList<>();
		if (Party.active()) list.addAll(Party.heroes());
		else if (Dungeon.hero != null) list.addAll(Collections.singletonList(Dungeon.hero));
		return list;
	}

	// *** levels ***

	public static Level newLevel( int depth, int branch ){
		if (!zoneOpen(zone(depth, branch))) return new DeadEndLevel();
		if (branch == ASH){
			if (ColossusPreview.on()) return new ColossusPreview.PreviewLevel();
			//-Dspd.test.pyre=1 still wants the old arena, and a party already down the old
			//floors of the plains (an older save) goes on down them
			if (depth == 25 && DevFlags.pyre()) return new AshBossLevel();
			if (Dungeon.level instanceof AshLevel) return depth >= 25 ? new AshBossLevel() : new AshLevel();
		}
		//inside a furnace cathedral of the city
		if (Cathedral.depth(depth, branch)) return new CathedralLevel();
		//every biome is the same three layers, each made from its own sheets and pieces
		switch (depth){
			case 21: return new AshSurface();
			case 22: return Biome.branch(branch).depths ? new AshDepths() : new DeadEndLevel();
			case 23: return new AshAbyss();
			default: return new DeadEndLevel();
		}
	}

	//stairs between zones and back to the rift. the regular ones (depth +-1, same branch) are already right,
	//and the open world sets its own ways down and its passes: only the portal is fixed here
	public static void fixTransitions( Level level ){
		int depth = Dungeon.depth, branch = Dungeon.branch;
		boolean open = level instanceof OpenWorld;
		CathedralTest.onCreate(level);
		for (LevelTransition t : new ArrayList<>(level.transitions)){
			if (open){
				if (t.type == LevelTransition.Type.REGULAR_ENTRANCE && depth == 21 && branch == ASH){
					t.destDepth = ThalFinale.DEPTH;
					t.destBranch = ThalFinale.BRANCH;
					t.destType = LevelTransition.Type.BRANCH_ENTRANCE;
				}
				continue;
			}
			if (t.type == LevelTransition.Type.REGULAR_ENTRANCE && depth == 21){
				if (branch == ASH){
					t.destDepth = ThalFinale.DEPTH;
					t.destBranch = ThalFinale.BRANCH;
					t.destType = LevelTransition.Type.BRANCH_ENTRANCE;
				} else {
					t.destDepth = 25;
					t.destBranch = branch - 1;
					t.destType = LevelTransition.Type.REGULAR_EXIT;
				}
			} else if (t.type == LevelTransition.Type.REGULAR_EXIT){
				int nextDepth = depth + 1, nextBranch = branch;
				if (depth >= 25 && branch < CITY){
					nextDepth = 21;
					nextBranch = branch + 1;
					t.destType = LevelTransition.Type.REGULAR_ENTRANCE;
				}
				t.destDepth = nextDepth;
				t.destBranch = nextBranch;
				//the way on isn't there yet (or this is the bottom of it all)
				if (depth >= 25 && branch == CITY || !zoneOpen(zone(nextDepth, nextBranch))){
					level.transitions.remove(t);
				}
			}
		}
	}

	//the party voted to cross Thal-Vekhorr's portal: they come out of it on the south edge of the ash plains
	public static void enter( int fromPos ){
		CoopLog.line("continent: crossing the portal, essence in " + Integer.toBinaryString(sources()));
		Level.beforeTransition();
		InterlevelScene.curTransition = new LevelTransition(Dungeon.level, fromPos,
				LevelTransition.Type.BRANCH_EXIT, 21, ASH, LevelTransition.Type.REGULAR_ENTRANCE);
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		Game.switchScene(InterlevelScene.class);
	}

	//Dungeon.switchLevel, every floor
	public static void onArrive(){
		if (!in()) return;
		boolean first = deepest == 0;
		deepest = Math.max(deepest, floor());
		visited |= 1 << zone();
		for (Hero h : party()){
			if (h.buff(DarkTaint.class) == null) Buff.affect(h, DarkTaint.class);
			Survival.onArrive(h, first);
			h.viewDistance = vision(h);
		}
		LandWalk.onArrive(Dungeon.level);
		Awakening.onArrive();
		WorldEvents.onArrive();
		Cathedral.onArrive();
	}

	//-Dspd.test.continent=N: the test run starts on that continent floor (1 the surface,
	//2 the tunnels under it, 3 its abyss) of the ash plains, or of the biome in
	//-Dspd.test.biome=flesh|city|frost|bones|isles
	public static void testStart(){
		if (AwakeTest.start()) return;
		if (CathedralTest.start()) return;
		int n = DevFlags.continent();
		if (n <= 0) return;
		n = Math.min(15, n);
		Dungeon.branch = ASH + (n - 1) / 5;
		Dungeon.depth = 21 + (n - 1) % 5;
		String biome = DevFlags.biome();
		if (biome != null){
			for (Biome b : Biome.ALL){
				if (b.key.equals(biome)) Dungeon.branch = b.branch;
			}
		}
		ThalFinale.defeated = true;
		ThalFinale.called = true;
		CoopLog.line("continent: test start on floor " + n);
	}

	// *** darkness ***

	public static final int BASE_SIGHT = 3;

	//2-3 cells in the dark, a torch makes it 6, the essence you carry lights a bit. alone, away from
	//the party, it's 1; a sacred torch makes it 12
	public static int vision( Hero h ){
		int v = BASE_SIGHT + Math.min(2, essence / 2) - DarkTaint.sightPenalty(h);
		if (h.buff(Light.class) != null) v = Math.max(v, Light.DISTANCE);
		if (Cathedral.here()) v = Math.max(v, Cathedral.GLOW_SIGHT);
		return Math.max(1, Survival.sight(h, v));
	}

	//eyes of yog, lava, fire: you see those coming from far away
	public static boolean glows( Char ch ){
		return ch instanceof Yog || Char.hasProp(ch, Char.Property.FIERY);
	}

	public static void glowSight( Level level, Char c, boolean[] fov ){
		if (!(c instanceof Hero) || !in() || WorldEvents.noGlow()) return;
		for (Mob m : level.mobs){
			if (!glows(m) || fov[m.pos] || level.distance(c.pos, m.pos) > 9) continue;
			if (new Ballistica(c.pos, m.pos, Ballistica.PROJECTILE).collisionPos == m.pos){
				fov[m.pos] = true;
			}
		}
	}

	// *** the end over there ***

	private static boolean falling = false;

	//Hero.die, last one standing. they made it this far: it's a win, a sad one
	public static boolean fallOnWipe( Hero dying ){
		if (!in()) return false;
		dying.HP = 1;
		if (falling || Awakening.sealed()) return true;
		falling = true;
		CoopLog.line("continent: the party fell on floor " + floor() + " with " + essence + " essence");
		GLog.n(Messages.get(Continent.class, "fell", where()));
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				falling = false;
				ThalFinale.finish(Awakening.Fallen.class);
			}
		});
		return true;
	}
}
