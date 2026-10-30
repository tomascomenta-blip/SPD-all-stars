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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.AshWalker;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.SmokeVulture;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshTarantula;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshWhale;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.BasaltBison;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.BeakRex;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.EmberFlea;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.Fauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.LavaSalamander;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.MimicBird;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.ObsidianBrute;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.ScoriaJackal;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.SlagGiant;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.SmelterBeetle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.VolcanoTurtle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.WalkingPyre;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.BurningFist;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogEye;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;
import com.watabou.utils.Reflection;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

//who lives in the ash plains, by layer (OpenWorld.SURFACE / DEPTHS / ABYSS). on the surface a bit of
//everything, the bigger the further from the road; down in the depths the narrow ones and ambushes;
//in the abyss the biggest. the small ones and the yogs everywhere. of the big ones, roughly 30% are
//3x3, 40% 5x5, 20% 8-10 and 10% 15x15. the 8x8 and bigger never come back once dead; herds, packs
//and the 3x3 and 5x5 do, slowly; the small ones like any monster
public class AshFauna {

	// *** ground ***

	//black lava (the "water" of the ash levels)
	public static boolean lava( int cell ){
		return Dungeon.level.water[cell];
	}

	//deep loose ash, where the whale swims: open ground that isn't lava or the road
	public static boolean deepAsh( int cell ){
		Level l = Dungeon.level;
		if (!l.passable[cell] || l.pit[cell] || l.water[cell]) return false;
		return !(l instanceof OpenWorld) || ((OpenWorld) l).danger(cell) >= 0.12f;
	}

	//0 = the road (or the stairs), 1 = as bad as it gets. levels that aren't open world (tests) go
	//by the distance from the entrance
	public static float danger( Level level, int cell ){
		if (level instanceof OpenWorld) return ((OpenWorld) level).danger(cell);
		int e = level.entrance();
		if (e < 0) return 0.5f;
		return Math.min(1f, level.distance(e, cell) / (float) Math.max(20, Math.max(level.width(), level.height()) / 2));
	}

	// *** helpers ***

	//mobs in a fixed order (level.mobs is a hash set: its order isn't the same on every device)
	public static ArrayList<Mob> sorted( Collection<Mob> mobs ){
		ArrayList<Mob> out = new ArrayList<>(mobs);
		Collections.sort(out, new Comparator<Mob>() {
			@Override
			public int compare( Mob a, Mob b ) {
				return a.id() - b.id();
			}
		});
		return out;
	}

	//free open cells within r of a cell, in a fixed order
	public static ArrayList<Integer> openAround( int cell, int r ){
		ArrayList<Integer> out = new ArrayList<>();
		Level l = Dungeon.level;
		int w = l.width(), cx = cell % w, cy = cell / w;
		for (int y = cy - r; y <= cy + r; y++){
			for (int x = cx - r; x <= cx + r; x++){
				if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
				int c = x + y * w;
				if (c == cell || !l.passable[c] || l.pit[c] || l.water[c] || Actor.findChar(c) != null) continue;
				out.add(c);
			}
		}
		return out;
	}

	// *** the essence ***

	//which colossi of this floor carry sacred essence, as {source} handed out by size: the walking
	//pyre the guardian's, the biggest of the angry ones the other. -1 where that source is dry this
	//run or already taken
	public static int[] essenceFor( Level level, int layer ){
		if (layer != OpenWorld.SURFACE) return new int[0];
		return new int[]{drop(Continent.GUARDIAN_ASH), drop(Continent.COLOSSUS_ASH)};
	}

	private static int drop( int source ){
		return Continent.essenceAt(source) && !Continent.hasClaimed(source) ? source : -1;
	}

	// *** filling a floor ***

	private static class Pick {
		Class<? extends AshColossus> kind;
		float minDanger;
		Pick( Class<? extends AshColossus> kind, float minDanger ){
			this.kind = kind;
			this.minDanger = minDanger;
		}
	}

	//fills a new floor, called at the end of its createMobs
	public static void populate( Level level, int layer ){
		Level was = Dungeon.level;
		Dungeon.level = level;
		try {
			fill(level, layer);
		} finally {
			Dungeon.level = was;
		}
	}

	private static int open( Level level ){
		int n = 0;
		for (int i = 0; i < level.length(); i++){
			if (level.passable[i] && !level.pit[i] && !level.solid[i]) n++;
		}
		return n;
	}

	//how many big ones a floor of this layer holds
	public static int bigCount( Level level, int layer ){
		int open = open(level);
		switch (layer){
			case OpenWorld.SURFACE: default: return Math.max(3, Math.min(24, open / 1000));
			case OpenWorld.DEPTHS: return Math.max(2, Math.min(14, open / 700));
			case OpenWorld.ABYSS: return Math.max(5, Math.min(14, open / 800));
		}
	}

	public static int smallCount( Level level, int layer ){
		int open = open(level);
		return Math.max(4, Math.min(60, open / (layer == OpenWorld.DEPTHS ? 260 : 450)));
	}

	private static float[] dangers;

	private static void fill( Level level, int layer ){
		dangers = new float[level.length()];
		for (int i = 0; i < dangers.length; i++) dangers[i] = danger(level, i);
		boolean[] taken = new boolean[level.length()];
		int entrance = level.entrance();
		int big = bigCount(level, layer);
		ArrayList<AshColossus> placed = new ArrayList<>();

		//the walking pyre: one, the surface only, as far off the road as it fits
		if (layer == OpenWorld.SURFACE){
			AshColossus p = place(level, new WalkingPyre(), 0.6f, entrance, taken);
			if (p != null){
				placed.add(p);
				big--;
			}
		}
		for (int i = 0; i < big; i++){
			Pick pick = pickBig(layer);
			//no more than a couple of the really big ones on one floor
			if (count(placed, pick.kind) >= cap(pick.kind)) pick = new Pick(Random.Int(2) == 0 ? ObsidianBrute.class : BeakRex.class, 0.3f);
			AshColossus c = place(level, makeBig(pick.kind), pick.minDanger, entrance, taken);
			if (c != null) placed.add(c);
		}
		//the essence goes into the biggest ones there are (pyre first)
		int[] essence = essenceFor(level, layer);
		ArrayList<AshColossus> bySize = new ArrayList<>(placed);
		Collections.sort(bySize, new Comparator<AshColossus>() {
			@Override
			public int compare( AshColossus a, AshColossus b ) {
				return b.size - a.size;
			}
		});
		int next = 0;
		for (AshColossus c : bySize){
			if (next >= essence.length) break;
			//grazers don't count as angry
			if (next > 0 && c.peaceful()) continue;
			c.essence = essence[next++];
		}
		for (AshColossus c : placed){
			herd(level, c, taken);
		}
		//a pack or a brood counts for every animal in it
		int small = smallCount(level, layer);
		for (int left = small; left > 0; ){
			Mob m = makeSmall(layer);
			int cell = smallCell(level, entrance, m instanceof YogEye ? 0f : 0.08f, taken);
			left -= weight(m);
			if (cell == -1) continue;
			m.pos = cell;
			taken[cell] = true;
			level.mobs.add(m);
			if (m instanceof ScoriaJackal) ((ScoriaJackal) m).pack = 3 + Random.Int(3);
		}
		dangers = null;
		CoopLog.line("fauna: layer " + layer + " big=" + placed.size() + "/" + bigCount(level, layer) + " small=" + small + " " + census(level.mobs));
	}

	private static int cap( Class<? extends AshColossus> kind ){
		if (kind == AshWhale.class) return 2;
		if (kind == VolcanoTurtle.class) return 2;
		if (kind == SlagGiant.class) return 3;
		if (kind == AshTarantula.class) return 2;
		return 99;
	}

	private static int count( ArrayList<AshColossus> list, Class<? extends AshColossus> kind ){
		int n = 0;
		for (AshColossus c : list) if (c.getClass() == kind) n++;
		return n;
	}

	//big ones by layer. what doesn't fit anywhere just isn't placed
	private static Pick pickBig( int layer ){
		float r = Random.Float();
		switch (layer){
			case OpenWorld.SURFACE: default:
				if (r < 0.30f) return new Pick(Random.Int(3) == 0 ? LavaSalamander.class : SmelterBeetle.class, 0.25f);
				if (r < 0.70f){
					int k = Random.Int(10);
					return new Pick(k < 3 ? BasaltBison.class : k < 7 ? ObsidianBrute.class : BeakRex.class, 0.35f);
				}
				if (r < 0.92f){
					int k = Random.Int(3);
					return new Pick(k == 0 ? VolcanoTurtle.class : k == 1 ? SlagGiant.class : AshTarantula.class, 0.5f);
				}
				return new Pick(AshWhale.class, 0.55f);
			case OpenWorld.DEPTHS:
				if (r < 0.50f) return new Pick(Random.Int(2) == 0 ? LavaSalamander.class : SmelterBeetle.class, 0.1f);
				return new Pick(Random.Int(2) == 0 ? ObsidianBrute.class : BeakRex.class, 0.2f);
			case OpenWorld.ABYSS:
				if (r < 0.30f) return new Pick(Random.Int(2) == 0 ? ObsidianBrute.class : BeakRex.class, 0.2f);
				if (r < 0.70f){
					int k = Random.Int(3);
					return new Pick(k == 0 ? VolcanoTurtle.class : k == 1 ? SlagGiant.class : AshTarantula.class, 0.3f);
				}
				return new Pick(AshWhale.class, 0.4f);
		}
	}

	private static AshColossus makeBig( Class<? extends AshColossus> kind ){
		AshColossus c = Reflection.newInstance(kind);
		return c == null ? new ObsidianBrute() : c;
	}

	//somewhere it fits, dangerous enough, away from the stairs and from the others. relaxes the
	//danger if nothing is found
	private static AshColossus place( Level level, AshColossus c, float minDanger, int entrance, boolean[] taken ){
		boolean[] f = Colossus.fits(c.size, c);
		int keepOff = 16 + c.size;
		for (float need = minDanger; need >= -0.01f; need -= 0.2f){
			ArrayList<Integer> cells = new ArrayList<>();
			for (int i = 0; i < f.length; i++){
				if (!f[i] || entrance >= 0 && level.distance(i, entrance) < keepOff) continue;
				if (dangers[i] < need) continue;
				if (c instanceof LavaSalamander && !lavaPool(level, i)) continue;
				if (clashes(level, i, c.size, Colossus.spacing(c.size), taken)) continue;
				cells.add(i);
			}
			if (cells.isEmpty()) continue;
			c.pos = Random.element(cells);
			c.home = c.pos;
			claim(level, c.pos, c.size, taken);
			level.mobs.add(c);
			return c;
		}
		return null;
	}

	//the body plus a margin, so they don't start on top of each other: wide when they are scattered
	//over the map (Colossus.spacing), tight for a herd round its leader
	private static boolean clashes( Level level, int center, int size, int margin, boolean[] taken ){
		int w = level.width(), r = size / 2 + margin;
		int cx = center % w, cy = center / w;
		for (int y = cy - r; y <= cy + r; y++){
			for (int x = cx - r; x <= cx + r; x++){
				if (x < 0 || y < 0 || x >= w || y >= level.height()) continue;
				if (taken[x + y * w]) return true;
			}
		}
		return false;
	}

	private static void claim( Level level, int center, int size, boolean[] taken ){
		int w = level.width(), h = size / 2;
		int cx = center % w, cy = center / w;
		for (int y = cy - h; y < cy - h + size; y++){
			for (int x = cx - h; x < cx - h + size; x++){
				if (x < 0 || y < 0 || x >= w || y >= level.height()) continue;
				taken[x + y * w] = true;
			}
		}
	}

	//a salamander wants a lava pool under it
	private static boolean lavaPool( Level level, int center ){
		if (!level.water[center]) return false;
		int n = 0, w = level.width();
		for (int y = -1; y <= 1; y++){
			for (int x = -1; x <= 1; x++){
				int c = center + x + y * w;
				if (c >= 0 && c < level.length() && level.water[c]) n++;
			}
		}
		return n >= 5;
	}

	//what comes along with a big one: the rest of a bison herd, the fleas on the fire beasts
	private static void herd( Level level, AshColossus c, boolean[] taken ){
		if (c instanceof BasaltBison){
			if (Random.Int(3) == 0) ((BasaltBison) c).infest(4 + Random.Int(3));
			int mates = 1 + Random.Int(3);
			for (int i = 0; i < mates; i++){
				BasaltBison b = new BasaltBison();
				int cell = nearFit(level, b, c.pos, 12, taken);
				if (cell == -1) break;
				b.pos = cell;
				b.home = cell;
				b.leader = c.id();
				if (Random.Int(4) == 0) b.infest(3 + Random.Int(3));
				claim(level, cell, b.size, taken);
				level.mobs.add(b);
			}
		} else if (c instanceof SlagGiant){
			c.fleas = 3;
		} else if (c instanceof VolcanoTurtle){
			c.fleas = 4;
		} else if (c instanceof WalkingPyre){
			c.fleas = 6;
		}
	}

	private static int nearFit( Level level, AshColossus c, int center, int r, boolean[] taken ){
		boolean[] f = Colossus.fits(c.size, c);
		int w = level.width(), cx = center % w, cy = center / w;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int y = cy - r; y <= cy + r; y++){
			for (int x = cx - r; x <= cx + r; x++){
				if (x < 0 || y < 0 || x >= w || y >= level.height()) continue;
				int i = x + y * w;
				if (f[i] && !clashes(level, i, c.size, 3, taken)) cells.add(i);
			}
		}
		return cells.isEmpty() ? -1 : Random.element(cells);
	}

	private static int weight( Mob m ){
		if (m instanceof ScoriaJackal) return 5;
		if (m instanceof YogLarva) return YogLarva.BROOD;
		return 1;
	}

	//small ones (and the yogs) by layer
	private static Mob makeSmall( int layer ){
		float r = Random.Float();
		//yogs, everywhere
		if (r < 0.30f){
			int k = Random.Int(6);
			if (k < 3) return new YogEye();
			if (k < 5) return new YogLarva();
			return new BurningFist();
		}
		r = Random.Float();
		switch (layer){
			case OpenWorld.SURFACE: default:
				if (r < 0.22f) return new ScoriaJackal();
				if (r < 0.42f) return new AshWalker();
				if (r < 0.60f) return new SmokeVulture();
				if (r < 0.80f) return new EmberFlea();
				return new MimicBird();
			case OpenWorld.DEPTHS:
				if (r < 0.35f) return new ScoriaJackal();
				if (r < 0.60f) return new AshWalker();
				if (r < 0.85f) return new EmberFlea();
				return new MimicBird();
			case OpenWorld.ABYSS:
				if (r < 0.40f) return new SmokeVulture();
				if (r < 0.70f) return new MimicBird();
				return new AshWalker();
		}
	}

	private static int smallCell( Level level, int entrance, float minDanger, boolean[] taken ){
		for (int tries = 0; tries < 60; tries++){
			int c = Random.Int(level.length());
			if (!level.passable[c] || level.pit[c] || level.water[c] || taken[c]) continue;
			if (entrance >= 0 && level.distance(c, entrance) < 10) continue;
			if (dangers[c] < minDanger) continue;
			return c;
		}
		return -1;
	}

	private static String census( Collection<Mob> mobs ){
		StringBuilder s = new StringBuilder();
		String[] names = {"WalkingPyre", "AshWhale", "VolcanoTurtle", "SlagGiant", "AshTarantula", "ObsidianBrute", "BeakRex", "BasaltBison",
				"LavaSalamander", "SmelterBeetle", "ScoriaJackal", "EmberFlea", "MimicBird", "AshWalker", "SmokeVulture",
				"YogEye", "YogLarva", "BurningFist"};
		for (String n : names){
			int k = 0;
			for (Mob m : mobs) if (m.getClass().getSimpleName().equals(n)) k++;
			if (k > 0) s.append(n).append('=').append(k).append(' ');
		}
		return s.toString();
	}

	// *** coming back ***

	//one more thing coming back to the floor over time, or null. it comes already placed (pos set),
	//away from every hero, ready for GameScene.add. mostly small things; now and then a pack or a
	//3x3 / 5x5 if the floor has fewer than it started with. the 8x8 and up never come back
	public static Mob respawn( Level level, int layer ){
		Level was = Dungeon.level;
		Dungeon.level = level;
		try {
			return comeBack(level, layer);
		} finally {
			Dungeon.level = was;
		}
	}

	private static Mob comeBack( Level level, int layer ){
		int big = 0, small = 0;
		for (Mob m : level.mobs){
			if (m.alignment != Char.Alignment.ENEMY) continue;
			if (m instanceof AshColossus) big++;
			else small++;
		}
		if (Random.Int(5) == 0 && big < bigCount(level, layer) * 2 / 3){
			int k = Random.Int(layer == OpenWorld.ABYSS ? 2 : 4);
			AshColossus c = k == 0 ? new ObsidianBrute() : k == 1 ? new BeakRex()
					: layer == OpenWorld.SURFACE && k == 2 ? new BasaltBison() : new SmelterBeetle();
			boolean[] f = Colossus.fits(c.size, c);
			int cell = farCell(level, f, 14 + c.size, 0.3f);
			if (cell != -1){
				c.pos = cell;
				c.home = cell;
				if (c instanceof BasaltBison) ((BasaltBison) c).calves = 1 + Random.Int(2);
				return c;
			}
		}
		if (small >= smallCount(level, layer)) return null;
		Mob m = makeSmall(layer);
		boolean[] f = new boolean[level.length()];
		for (int i = 0; i < f.length; i++){
			f[i] = level.passable[i] && !level.pit[i] && !level.water[i] && Actor.findChar(i) == null;
		}
		int cell = farCell(level, f, 14, 0.05f);
		if (cell == -1) return null;
		m.pos = cell;
		m.state = m.WANDERING;
		if (m instanceof ScoriaJackal) ((ScoriaJackal) m).pack = 2 + Random.Int(3);
		return m;
	}

	//a random good cell far enough from every living hero
	private static int farCell( Level level, boolean[] ok, int away, float minDanger ){
		for (int tries = 0; tries < 80; tries++){
			int c = Random.Int(level.length());
			if (!ok[c] || danger(level, c) < minDanger) continue;
			boolean far = true;
			for (Hero h : Party.living()){
				if (level.distance(h.pos, c) < away){
					far = false;
					break;
				}
			}
			if (far) return c;
		}
		return -1;
	}

	//every kind, by name, for tests
	public static final ArrayList<String> KINDS = new ArrayList<>(Arrays.asList(
			"jackal", "flea", "bird", "salamander", "beetle", "obsidian", "bison", "rex",
			"turtle", "giant", "pyre", "whale"));

	public static Mob make( String kind ){
		switch (kind){
			case "jackal":     return new ScoriaJackal();
			case "flea":       return new EmberFlea();
			case "bird":       return new MimicBird();
			case "salamander": return new LavaSalamander();
			case "beetle":     return new SmelterBeetle();
			case "obsidian":   return new ObsidianBrute();
			case "bison":      return new BasaltBison();
			case "rex":        return new BeakRex();
			case "tarantula":  return new AshTarantula();
			case "turtle":     return new VolcanoTurtle();
			case "giant":      return new SlagGiant();
			case "pyre":       return new WalkingPyre();
			case "whale":      return new AshWhale();
		}
		return null;
	}

	public static boolean ours( Char ch ){
		return ch instanceof Fauna;
	}
}
