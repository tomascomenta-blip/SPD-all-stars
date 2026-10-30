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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.BurningFist;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogEye;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogTongue;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

//fills a floor of the moor, the graveyard or the isles the way AshFauna fills the ash: the guardian
//on its layer, the big ones by size (about 30% 3x3, 40% 5x5, 20% 8-10, 10% 15x15 on the surface,
//the narrow ones down in the tunnels, the biggest in the abyss), and small things with yog's brood
//everywhere. the 8x8 and bigger never come back; the rest do, slowly
public class WildFauna {

	public static final int S3 = 0, S5 = 1, S10 = 2, S15 = 3;

	//share of each size by layer
	private static final float[][] SHARE = {
			{0.30f, 0.40f, 0.20f, 0.10f},
			{0.50f, 0.50f, 0f, 0f},
			{0.15f, 0.35f, 0.30f, 0.20f}
	};

	public abstract static class Biome {

		//Continent.Z_FROST and so on
		public abstract int zone();

		//the big kinds by size class (S3, S5, S10, S15)
		public abstract ArrayList<Class<? extends AshColossus>> bigs( int sizeClass );

		//the guardian (null for none) and the layer it lives on
		public Class<? extends AshColossus> guardian(){
			return null;
		}

		public int guardianLayer(){
			return OpenWorld.SURFACE;
		}

		//a small one of this biome (yogs are added on top)
		public abstract Mob small( int layer );

		//the sacred essence this biome hands out, or none
		public boolean essence(){
			return true;
		}

		//what comes along with a big one once it's down (herds, flocks...)
		public void herd( Level level, AshColossus c, boolean[] taken ){
		}

		//a newly placed small one (packs...)
		public void spawned( Mob m ){
		}

		//how many animals a small one stands for (a pack counts for all of it)
		public int weight( Mob m ){
			return m instanceof YogLarva ? YogLarva.BROOD : 1;
		}

		//can this big one start here?
		public boolean home( Level level, AshColossus c, int cell ){
			return true;
		}
	}

	@SafeVarargs
	public static ArrayList<Class<? extends AshColossus>> kinds( Class<? extends AshColossus>... k ){
		ArrayList<Class<? extends AshColossus>> out = new ArrayList<>();
		Collections.addAll(out, k);
		return out;
	}

	// *** the essence ***

	public static int[] essenceFor( Biome b, int layer ){
		if (!b.essence() || layer != OpenWorld.SURFACE) return new int[0];
		return new int[]{drop(Continent.source(b.zone(), Continent.GUARDIAN)), drop(Continent.source(b.zone(), Continent.COLOSSUS))};
	}

	private static int drop( int source ){
		return source >= 0 && Continent.essenceAt(source) && !Continent.hasClaimed(source) ? source : -1;
	}

	// *** filling a floor ***

	public static void populate( Biome b, Level level, int layer ){
		Level was = Dungeon.level;
		Dungeon.level = level;
		try {
			fill(b, level, layer);
		} finally {
			Dungeon.level = was;
		}
	}

	private static float[] dangers;

	private static void fill( Biome b, Level level, int layer ){
		dangers = new float[level.length()];
		for (int i = 0; i < dangers.length; i++) dangers[i] = AshFauna.danger(level, i);
		boolean[] taken = new boolean[level.length()];
		int entrance = level.entrance();
		int big = AshFauna.bigCount(level, layer);
		ArrayList<AshColossus> placed = new ArrayList<>();
		AshColossus guardian = null;

		if (b.guardian() != null && layer == b.guardianLayer()){
			guardian = place(b, level, Reflection.newInstance(b.guardian()), 0.6f, entrance, taken);
			if (guardian != null){
				placed.add(guardian);
				big--;
			}
		}
		for (int i = 0; i < big; i++){
			Class<? extends AshColossus> kind = pick(b, layer);
			if (kind == null) continue;
			if (count(placed, kind) >= cap(kind)) kind = Random.element(b.bigs(S5));
			AshColossus c = place(b, level, Reflection.newInstance(kind), minDanger(kind), entrance, taken);
			if (c != null) placed.add(c);
		}
		//the guardian's essence to the guardian, the other to the biggest angry one
		int[] essence = essenceFor(b, layer);
		if (essence.length > 0){
			if (guardian != null) guardian.essence = essence[0];
			ArrayList<AshColossus> bySize = new ArrayList<>(placed);
			Collections.sort(bySize, new Comparator<AshColossus>() {
				@Override
				public int compare( AshColossus x, AshColossus y ){
					return y.size != x.size ? y.size - x.size : x.id() - y.id();
				}
			});
			for (AshColossus c : bySize){
				if (c == guardian || c.peaceful()) continue;
				c.essence = essence[1];
				break;
			}
		}
		for (AshColossus c : new ArrayList<>(placed)) b.herd(level, c, taken);

		int small = AshFauna.smallCount(level, layer);
		for (int left = small; left > 0; ){
			Mob m = Random.Float() < 0.3f ? yog() : b.small(layer);
			left -= b.weight(m);
			int cell = smallCell(level, entrance, m instanceof YogEye ? 0f : 0.08f, taken);
			if (cell == -1) continue;
			m.pos = cell;
			taken[cell] = true;
			b.spawned(m);
			level.mobs.add(m);
		}
		dangers = null;
		CoopLog.line("fauna: zone " + b.zone() + " layer " + layer + " big=" + placed.size() + " small=" + small + " " + census(level.mobs));
	}

	//the four small yogs, everywhere
	public static Mob yog(){
		int k = Random.Int(8);
		if (k < 3) return new YogEye();
		if (k < 5) return new YogLarva();
		if (k < 7) return new BurningFist();
		return new YogTongue();
	}

	private static Class<? extends AshColossus> pick( Biome b, int layer ){
		float[] share = SHARE[Math.max(0, Math.min(2, layer))];
		float r = Random.Float();
		int s = 0;
		while (s < 3 && r >= share[s]){
			r -= share[s];
			s++;
		}
		ArrayList<Class<? extends AshColossus>> kinds = b.bigs(s);
		if (kinds.isEmpty()) kinds = b.bigs(S5);
		return kinds.isEmpty() ? null : Random.element(kinds);
	}

	private static int sizeOf( Class<? extends AshColossus> kind ){
		AshColossus c = Reflection.newInstance(kind);
		return c == null ? 5 : c.size;
	}

	private static int cap( Class<? extends AshColossus> kind ){
		int s = sizeOf(kind);
		return s >= 15 ? 1 : s >= 8 ? 2 : 99;
	}

	private static float minDanger( Class<? extends AshColossus> kind ){
		int s = sizeOf(kind);
		return s >= 15 ? 0.55f : s >= 8 ? 0.45f : s >= 5 ? 0.3f : 0.2f;
	}

	private static int count( ArrayList<AshColossus> list, Class<? extends AshColossus> kind ){
		int n = 0;
		for (AshColossus c : list) if (c.getClass() == kind) n++;
		return n;
	}

	//somewhere it fits, dangerous enough, away from the stairs and the others; less picky if it has to
	public static AshColossus place( Biome b, Level level, AshColossus c, float minDanger, int entrance, boolean[] taken ){
		if (c == null) return null;
		boolean[] f = Colossus.fits(c.size, c);
		int keepOff = 16 + c.size;
		for (float need = minDanger; need >= -0.01f; need -= 0.2f){
			ArrayList<Integer> cells = new ArrayList<>();
			for (int i = 0; i < f.length; i++){
				if (!f[i] || entrance >= 0 && level.distance(i, entrance) < keepOff) continue;
				if (dangers != null && dangers[i] < need) continue;
				if (clashes(level, i, c.size, Colossus.spacing(c.size), taken) || !b.home(level, c, i)) continue;
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

	public static boolean clashes( Level level, int center, int size, int margin, boolean[] taken ){
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

	public static void claim( Level level, int center, int size, boolean[] taken ){
		int w = level.width(), h = size / 2;
		int cx = center % w, cy = center / w;
		for (int y = cy - h; y < cy - h + size; y++){
			for (int x = cx - h; x < cx - h + size; x++){
				if (x < 0 || y < 0 || x >= w || y >= level.height()) continue;
				taken[x + y * w] = true;
			}
		}
	}

	//a spot for one more of a herd near its leader, or -1
	public static int nearFit( Level level, AshColossus c, int center, int r, boolean[] taken ){
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

	//n more of a kind round a leader, following it
	public static void herdOf( Level level, AshColossus lead, Class<? extends AshColossus> kind, int n, boolean[] taken ){
		for (int i = 0; i < n; i++){
			AshColossus b = Reflection.newInstance(kind);
			int cell = nearFit(level, b, lead.pos, 12 + lead.size / 2, taken);
			if (cell == -1) return;
			b.pos = cell;
			b.home = cell;
			b.leader = lead.id();
			claim(level, cell, b.size, taken);
			level.mobs.add(b);
		}
	}

	private static int smallCell( Level level, int entrance, float minDanger, boolean[] taken ){
		for (int tries = 0; tries < 60; tries++){
			int c = Random.Int(level.length());
			if (!level.passable[c] || level.pit[c] || level.water[c] || taken[c]) continue;
			if (entrance >= 0 && level.distance(c, entrance) < 10) continue;
			if (dangers != null && dangers[c] < minDanger) continue;
			return c;
		}
		return -1;
	}

	private static String census( Collection<Mob> mobs ){
		ArrayList<String> names = new ArrayList<>();
		for (Mob m : mobs){
			String n = m.getClass().getSimpleName();
			if (!names.contains(n)) names.add(n);
		}
		Collections.sort(names);
		StringBuilder s = new StringBuilder();
		for (String n : names){
			int k = 0;
			for (Mob m : mobs) if (m.getClass().getSimpleName().equals(n)) k++;
			s.append(n).append('=').append(k).append(' ');
		}
		return s.toString();
	}

	// *** coming back ***

	//one more thing back on the floor, already placed (pos set) far from the heroes, or null
	public static Mob next( Biome b, Level level, int layer ){
		Level was = Dungeon.level;
		Dungeon.level = level;
		try {
			return comeBack(b, level, layer);
		} finally {
			Dungeon.level = was;
		}
	}

	//the same, straight onto the floor. the blind worm may come under it too
	public static void respawn( Biome b, Level level, int layer ){
		GreatWorm.maybeEmerge(level);
		Mob m = next(b, level, layer);
		if (m == null) return;
		if (level == Dungeon.level) GameScene.add(m);
		else level.mobs.add(m);
	}

	private static Mob comeBack( Biome b, Level level, int layer ){
		int big = 0, small = 0;
		for (Mob m : level.mobs){
			if (m.alignment != Char.Alignment.ENEMY) continue;
			if (m instanceof AshColossus) big++;
			else small++;
		}
		if (Random.Int(5) == 0 && big < AshFauna.bigCount(level, layer) * 2 / 3){
			ArrayList<Class<? extends AshColossus>> kinds = b.bigs(Random.Int(2) == 0 ? S3 : S5);
			AshColossus c = kinds.isEmpty() ? null : Reflection.newInstance(Random.element(kinds));
			if (c != null){
				boolean[] f = Colossus.fits(c.size, c);
				int cell = farCell(level, f, 14 + c.size, 0.3f);
				if (cell != -1 && b.home(level, c, cell)){
					c.pos = cell;
					c.home = cell;
					return c;
				}
			}
		}
		if (small >= AshFauna.smallCount(level, layer)) return null;
		Mob m = Random.Float() < 0.3f ? yog() : b.small(layer);
		boolean[] f = new boolean[level.length()];
		for (int i = 0; i < f.length; i++){
			f[i] = level.passable[i] && !level.pit[i] && !level.water[i] && Actor.findChar(i) == null;
		}
		int cell = farCell(level, f, 14, 0.05f);
		if (cell == -1) return null;
		m.pos = cell;
		m.state = m.WANDERING;
		b.spawned(m);
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
}
