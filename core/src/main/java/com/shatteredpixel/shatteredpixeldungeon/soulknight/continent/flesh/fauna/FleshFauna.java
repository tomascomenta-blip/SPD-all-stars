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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.BurningFist;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogEye;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

//the beasts of the flesh forest, by layer (OpenWorld.SURFACE, DEPTHS, ABYSS), like AshFauna does for
//the ash. the heart of the forest walks the surface, one per floor, as far from the road as it fits.
//of the rest of the big ones roughly 30% are 3x3, 40% 5x5, 20% the elk and 10% the giraffe; the
//deeper, the fewer big ones and the more ambushes. the small ones and the yogs everywhere
public class FleshFauna {

	// *** the essence ***

	//the heart carries the guardian's; the biggest of the angry ones the other
	public static int[] essenceFor( int layer ){
		if (layer != OpenWorld.SURFACE) return new int[0];
		return new int[]{drop(Continent.source(Continent.Z_FLESH, Continent.GUARDIAN)),
				drop(Continent.source(Continent.Z_FLESH, Continent.COLOSSUS))};
	}

	private static int drop( int source ){
		return Continent.essenceAt(source) && !Continent.hasClaimed(source) ? source : -1;
	}

	// *** filling a floor ***

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

	private static float[] dangers;

	private static void fill( Level level, int layer ){
		dangers = new float[level.length()];
		for (int i = 0; i < dangers.length; i++) dangers[i] = AshFauna.danger(level, i);
		boolean[] taken = new boolean[level.length()];
		int entrance = level.entrance();
		int big = AshFauna.bigCount(level, layer);
		ArrayList<AshColossus> placed = new ArrayList<>();
		AshColossus guardian = null;

		if (layer == OpenWorld.SURFACE){
			guardian = place(level, new ForestHeart(), 0.6f, entrance, taken);
			if (guardian != null){
				placed.add(guardian);
				big--;
			}
		}
		for (int i = 0; i < big; i++){
			Class<? extends AshColossus> kind = pickBig(layer);
			if (count(placed, kind) >= cap(kind, layer)) kind = Random.Int(2) == 0 ? FleshChimera.class : HollowBear.class;
			AshColossus c = place(level, make(kind), minDanger(kind), entrance, taken);
			if (c != null) placed.add(c);
		}

		//the essence: the guardian's in the heart, the other in the biggest of the angry ones
		int[] essence = essenceFor(layer);
		if (essence.length > 0){
			if (guardian != null) guardian.essence = essence[0];
			ArrayList<AshColossus> bySize = new ArrayList<>(placed);
			Collections.sort(bySize, new Comparator<AshColossus>() {
				@Override
				public int compare( AshColossus a, AshColossus b ) {
					return b.size != a.size ? b.size - a.size : a.pos - b.pos;
				}
			});
			for (AshColossus c : bySize){
				if (c == guardian || c.peaceful()) continue;
				c.essence = essence[1];
				break;
			}
		}
		for (AshColossus c : placed){
			herd(level, c, taken);
		}

		int small = AshFauna.smallCount(level, layer);
		for (int left = small; left > 0; ){
			Mob m = makeSmall(layer);
			int cell = smallCell(level, entrance, m instanceof YogEye ? 0f : 0.08f, taken);
			left -= weight(m);
			if (cell == -1) continue;
			m.pos = cell;
			taken[cell] = true;
			level.mobs.add(m);
			grow(m, 0);
		}
		dangers = null;
		CoopLog.line("fauna: flesh layer " + layer + " big=" + placed.size() + "/" + AshFauna.bigCount(level, layer) + " small=" + small + " " + census(level.mobs));
	}

	//packs and herds start as one that brings the rest on its first turn
	private static void grow( Mob m, int less ){
		if (m instanceof FlayedFox) ((FlayedFox) m).pack = Math.max(1, 3 + Random.Int(3) - less);
		if (m instanceof WatcherFawn) ((WatcherFawn) m).herd = Math.max(1, 3 + Random.Int(3) - less);
	}

	private static int weight( Mob m ){
		if (m instanceof FlayedFox) return 5;
		if (m instanceof WatcherFawn) return 4;
		if (m instanceof YogLarva) return YogLarva.BROOD;
		return 1;
	}

	//no more than a couple of the really big ones on one floor
	private static int cap( Class<? extends AshColossus> kind, int layer ){
		if (kind == InfectedGiraffe.class) return layer == OpenWorld.ABYSS ? 2 : 1;
		if (kind == RootElk.class) return 3;
		return 99;
	}

	private static float minDanger( Class<? extends AshColossus> kind ){
		if (kind == InfectedGiraffe.class) return 0.55f;
		if (kind == RootElk.class) return 0.45f;
		if (kind == StiltStag.class || kind == PustuleToad.class || kind == BoneRaptor.class) return 0.2f;
		return 0.3f;
	}

	private static int count( ArrayList<AshColossus> list, Class<? extends AshColossus> kind ){
		int n = 0;
		for (AshColossus c : list) if (c.getClass() == kind) n++;
		return n;
	}

	//30% 3x3, 40% 5x5, 20% the elk, 10% the giraffe on the surface. the depths are narrow (3x3 and
	//5x5), the abyss holds the biggest
	private static Class<? extends AshColossus> pickBig( int layer ){
		float r = Random.Float();
		switch (layer){
			case OpenWorld.SURFACE: default:
				if (r < 0.30f) return three();
				if (r < 0.70f) return five();
				if (r < 0.90f) return RootElk.class;
				return InfectedGiraffe.class;
			case OpenWorld.DEPTHS:
				if (r < 0.55f) return three();
				return five();
			case OpenWorld.ABYSS:
				if (r < 0.30f) return five();
				if (r < 0.75f) return RootElk.class;
				return InfectedGiraffe.class;
		}
	}

	private static Class<? extends AshColossus> three(){
		int k = Random.Int(3);
		return k == 0 ? StiltStag.class : k == 1 ? PustuleToad.class : BoneRaptor.class;
	}

	private static Class<? extends AshColossus> five(){
		int k = Random.Int(3);
		return k == 0 ? FleshChimera.class : k == 1 ? HiveBoar.class : HollowBear.class;
	}

	private static AshColossus make( Class<? extends AshColossus> kind ){
		AshColossus c = Reflection.newInstance(kind);
		return c == null ? new FleshChimera() : c;
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

	//what comes along with a big one: raptors hunt in threes, the giraffe and the heart carry lice
	private static void herd( Level level, AshColossus c, boolean[] taken ){
		if (c instanceof BoneRaptor){
			int mates = 1 + Random.Int(2);
			for (int i = 0; i < mates; i++){
				BoneRaptor b = new BoneRaptor();
				int cell = nearFit(level, b, c.pos, 8, taken);
				if (cell == -1) break;
				b.pos = cell;
				b.home = cell;
				b.leader = c.id();
				claim(level, cell, b.size, taken);
				level.mobs.add(b);
			}
		} else if (c instanceof InfectedGiraffe || c instanceof ForestHeart){
			for (int i = 0; i < 3; i++){
				int cell = smallCell(level, -1, 0f, taken, c.pos, c.half() + 4);
				if (cell == -1) break;
				BarkLouse l = new BarkLouse();
				l.pos = cell;
				taken[cell] = true;
				level.mobs.add(l);
			}
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

	//small ones (and the yogs) by layer
	private static Mob makeSmall( int layer ){
		float r = Random.Float();
		if (r < 0.30f){
			int k = Random.Int(6);
			if (k < 3) return new YogEye();
			if (k < 5) return new YogLarva();
			return new BurningFist();
		}
		r = Random.Float();
		switch (layer){
			case OpenWorld.SURFACE: default:
				if (r < 0.25f) return new WatcherFawn();
				if (r < 0.50f) return new FlayedFox();
				if (r < 0.75f) return new BarkLouse();
				return new PustuleMoth();
			case OpenWorld.DEPTHS:
				if (r < 0.40f) return new BarkLouse();
				if (r < 0.70f) return new FlayedFox();
				return new PustuleMoth();
			case OpenWorld.ABYSS:
				if (r < 0.50f) return new PustuleMoth();
				if (r < 0.75f) return new WatcherFawn();
				return new BarkLouse();
		}
	}

	private static int smallCell( Level level, int entrance, float minDanger, boolean[] taken ){
		return smallCell(level, entrance, minDanger, taken, -1, 0);
	}

	//a free cell (near a cell if near >= 0)
	private static int smallCell( Level level, int entrance, float minDanger, boolean[] taken, int near, int r ){
		int w = level.width();
		for (int tries = 0; tries < 60; tries++){
			int c = near < 0 ? Random.Int(level.length())
					: near + Random.IntRange(-r, r) + Random.IntRange(-r, r) * w;
			if (c < 0 || c >= level.length()) continue;
			if (!level.passable[c] || level.pit[c] || level.water[c] || taken[c]) continue;
			if (entrance >= 0 && level.distance(c, entrance) < 10) continue;
			if (dangers != null && dangers[c] < minDanger) continue;
			return c;
		}
		return -1;
	}

	private static String census( Collection<Mob> mobs ){
		StringBuilder s = new StringBuilder();
		String[] names = {"ForestHeart", "InfectedGiraffe", "RootElk", "FleshChimera", "HiveBoar", "HollowBear",
				"StiltStag", "PustuleToad", "BoneRaptor", "WatcherFawn", "BarkLouse", "FlayedFox", "PustuleMoth",
				"YogEye", "YogLarva", "BurningFist"};
		for (String n : names){
			int k = 0;
			for (Mob m : mobs) if (m.getClass().getSimpleName().equals(n)) k++;
			if (k > 0) s.append(n).append('=').append(k).append(' ');
		}
		return s.toString();
	}

	// *** coming back ***

	//now and then, what died around here comes back far from the heroes: one more thing, already
	//placed (pos set) and ready for GameScene.add, or null. mostly small things; now and then a
	//3x3 or a 5x5 if the floor has fewer than it started with. the elk and up never come back
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
		if (Random.Int(5) == 0 && big < AshFauna.bigCount(level, layer) * 2 / 3){
			AshColossus c = make(Random.Int(2) == 0 ? three() : five());
			boolean[] f = Colossus.fits(c.size, c);
			int cell = farCell(level, f, 14 + c.size, 0.3f);
			if (cell != -1){
				c.pos = cell;
				c.home = cell;
				return c;
			}
		}
		if (small >= AshFauna.smallCount(level, layer)) return null;
		Mob m = makeSmall(layer);
		boolean[] f = new boolean[level.length()];
		for (int i = 0; i < f.length; i++){
			f[i] = level.passable[i] && !level.pit[i] && !level.water[i] && Actor.findChar(i) == null;
		}
		int cell = farCell(level, f, 14, 0.05f);
		if (cell == -1) return null;
		m.pos = cell;
		m.state = m.WANDERING;
		grow(m, 1);
		return m;
	}

	private static int farCell( Level level, boolean[] ok, int away, float minDanger ){
		for (int tries = 0; tries < 80; tries++){
			int c = Random.Int(level.length());
			if (!ok[c] || AshFauna.danger(level, c) < minDanger) continue;
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
	public static Mob make( String kind ){
		switch (kind){
			case "heart":
				ForestHeart h = new ForestHeart();
				int[] e = essenceFor(OpenWorld.SURFACE);
				if (e.length > 0) h.essence = e[0];
				return h;
			case "giraffe": return new InfectedGiraffe();
			case "elk":     return new RootElk();
			case "chimera": return new FleshChimera();
			case "boar":    return new HiveBoar();
			case "bear":    return new HollowBear();
			case "stag":    return new StiltStag();
			case "toad":    return new PustuleToad();
			case "raptor":  return new BoneRaptor();
			case "fawn":
				WatcherFawn f = new WatcherFawn();
				f.herd = 4;
				return f;
			case "louse":   return new BarkLouse();
			case "fox":
				FlayedFox x = new FlayedFox();
				x.pack = 3;
				return x;
			case "moth":    return new PustuleMoth();
		}
		return null;
	}
}
