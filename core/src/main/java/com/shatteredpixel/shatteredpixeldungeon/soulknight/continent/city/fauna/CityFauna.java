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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna;

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

//the beasts of the devoured city, by layer (OpenWorld.SURFACE, DEPTHS, ABYSS), like AshFauna does for
//the ash. the king on the surface, one per floor; the cathedral and the mother where the streets
//are wide; the catacombs below for the small and the narrow ones, the city under the city for the
//biggest. of the big ones roughly 30% 3x3, 40% 5x5, 20% 8-10 and 10% 15x15. the 8x8 and up never
//come back once dead; the 3x3 and 5x5 do, slowly; the small ones like any monster
public class CityFauna {

	// *** the essence ***

	//{guardian, colossus}: the king carries the guardian's, the biggest of the others the colossus
	//one. -1 where that source is dry this run or already taken
	public static int[] essenceFor( Level level, int layer ){
		if (layer != OpenWorld.SURFACE) return new int[]{-1, -1};
		return new int[]{drop(Continent.source(Continent.Z_CITY, Continent.GUARDIAN)),
				drop(Continent.source(Continent.Z_CITY, Continent.COLOSSUS))};
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

	private static float[] dangers;

	private static void fill( Level level, int layer ){
		dangers = new float[level.length()];
		for (int i = 0; i < dangers.length; i++) dangers[i] = AshFauna.danger(level, i);
		boolean[] taken = new boolean[level.length()];
		int entrance = level.entrance();
		int big = AshFauna.bigCount(level, layer);
		ArrayList<AshColossus> placed = new ArrayList<>();
		int[] essence = essenceFor(level, layer);

		//the king: one, the surface only, as deep into the city as he fits
		if (layer == OpenWorld.SURFACE){
			AshColossus k = place(level, new LastKing(), 0.6f, entrance, taken);
			if (k != null){
				k.essence = essence[0];
				placed.add(k);
				big--;
			}
		}
		for (int i = 0; i < big; i++){
			Pick pick = pickBig(layer);
			if (count(placed, pick.kind) >= cap(pick.kind)) pick = new Pick(Random.Int(2) == 0 ? BronzeGuard.class : LivingCarriage.class, 0.3f);
			AshColossus c = place(level, makeBig(pick.kind), pick.minDanger, entrance, taken);
			if (c != null) placed.add(c);
		}
		//the colossus essence goes into the biggest of the angry ones that isn't the king
		AshColossus best = null;
		for (AshColossus c : placed){
			if (c instanceof LastKing || c.peaceful()) continue;
			if (best == null || c.size > best.size) best = c;
		}
		if (best != null) best.essence = essence[1];
		for (AshColossus c : placed){
			herd(level, c, taken);
		}
		//a swarm counts for every rat in it
		int small = AshFauna.smallCount(level, layer);
		for (int left = small; left > 0; ){
			Mob m = makeSmall(layer);
			int cell = smallCell(level, entrance, m instanceof YogEye ? 0f : 0.08f, taken);
			left -= weight(m);
			if (cell == -1) continue;
			m.pos = cell;
			taken[cell] = true;
			level.mobs.add(m);
			if (m instanceof SootRats) ((SootRats) m).swarm = 4 + Random.Int(3);
		}
		dangers = null;
		CoopLog.line("fauna: city layer " + layer + " big=" + placed.size() + "/" + AshFauna.bigCount(level, layer) + " small=" + small + " " + census(level.mobs));
	}

	private static int cap( Class<? extends AshColossus> kind ){
		if (kind == FurnaceCathedral.class) return 1;
		if (kind == SmelterMother.class) return 2;
		if (kind == ChainedOne.class) return 2;
		return 99;
	}

	private static int count( ArrayList<AshColossus> list, Class<? extends AshColossus> kind ){
		int n = 0;
		for (AshColossus c : list) if (c.getClass() == kind) n++;
		return n;
	}

	private static Pick pick3( float danger ){
		int k = Random.Int(3);
		return new Pick(k == 0 ? FurnaceMole.class : k == 1 ? BellRinger.class : BrassRaptor.class, danger);
	}

	private static Pick pick5( float danger ){
		int k = Random.Int(3);
		return new Pick(k == 0 ? BronzeGuard.class : k == 1 ? LivingCarriage.class : BloatedBeggar.class, danger);
	}

	private static Pick pickGiant( float danger ){
		return new Pick(Random.Int(2) == 0 ? ChainedOne.class : SmelterMother.class, danger);
	}

	//big ones by layer. what doesn't fit anywhere just isn't placed
	private static Pick pickBig( int layer ){
		float r = Random.Float();
		switch (layer){
			case OpenWorld.SURFACE: default:
				if (r < 0.30f) return pick3(0.25f);
				if (r < 0.70f) return pick5(0.35f);
				if (r < 0.90f) return pickGiant(0.5f);
				return new Pick(FurnaceCathedral.class, 0.55f);
			case OpenWorld.DEPTHS:
				if (r < 0.50f) return pick3(0.1f);
				return pick5(0.2f);
			case OpenWorld.ABYSS:
				if (r < 0.30f) return pick5(0.2f);
				if (r < 0.70f) return pickGiant(0.3f);
				return new Pick(FurnaceCathedral.class, 0.4f);
		}
	}

	private static AshColossus makeBig( Class<? extends AshColossus> kind ){
		AshColossus c = Reflection.newInstance(kind);
		return c == null ? new BronzeGuard() : c;
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

	//what comes along with a big one: the rest of a raptor pack
	private static void herd( Level level, AshColossus c, boolean[] taken ){
		if (!(c instanceof BrassRaptor) || c.leader != -1) return;
		int mates = 1 + Random.Int(2);
		for (int i = 0; i < mates; i++){
			BrassRaptor b = new BrassRaptor();
			int cell = nearFit(level, b, c.pos, 10, taken);
			if (cell == -1) break;
			b.pos = cell;
			b.home = cell;
			b.leader = c.id();
			claim(level, cell, b.size, taken);
			level.mobs.add(b);
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
		if (m instanceof SootRats) return 5;
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
				if (r < 0.25f) return new Lamplighter();
				if (r < 0.50f) return new FleshGargoyle();
				if (r < 0.75f) return new SootRats();
				return new ChainDog();
			case OpenWorld.DEPTHS:
				if (r < 0.45f) return new SootRats();
				if (r < 0.70f) return new Lamplighter();
				return new ChainDog();
			case OpenWorld.ABYSS:
				if (r < 0.40f) return new FleshGargoyle();
				if (r < 0.70f) return new Lamplighter();
				return new SootRats();
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
		String[] names = {"LastKing", "FurnaceCathedral", "SmelterMother", "ChainedOne", "BronzeGuard", "LivingCarriage",
				"BloatedBeggar", "FurnaceMole", "BellRinger", "BrassRaptor", "Lamplighter", "FleshGargoyle", "SootRats",
				"ChainDog", "YogEye", "YogLarva", "BurningFist"};
		for (String n : names){
			int k = 0;
			for (Mob m : mobs) if (m.getClass().getSimpleName().equals(n)) k++;
			if (k > 0) s.append(n).append('=').append(k).append(' ');
		}
		return s.toString();
	}

	// *** coming back ***

	//now and then, what died around here comes back far from the heroes: one more thing, already
	//placed (pos set) and ready for GameScene.add, or null
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
			AshColossus c = makeBig(layer == OpenWorld.DEPTHS && Random.Int(2) == 0 ? pick3(0).kind : pick5(0).kind);
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
		if (m instanceof SootRats) ((SootRats) m).swarm = 3 + Random.Int(3);
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

	// *** tests ***

	//every kind, by name, for tests (-Dspd.test.fauna=city:king,cathedral)
	public static Mob make( String kind ){
		switch (kind){
			case "king":
				LastKing k = new LastKing();
				k.essence = essenceFor(Dungeon.level, OpenWorld.SURFACE)[0];
				return k;
			case "cathedral":   return new FurnaceCathedral();
			case "chained":     return new ChainedOne();
			case "mother":      return new SmelterMother();
			case "egg":         return new SmelterMother.MoltenEgg();
			case "guard":       return new BronzeGuard();
			case "carriage":    return new LivingCarriage();
			case "beggar":      return new BloatedBeggar();
			case "mole":        return new FurnaceMole();
			case "ringer":      return new BellRinger();
			case "raptor":      return new BrassRaptor();
			case "lamplighter": return new Lamplighter();
			case "gargoyle":    return new FleshGargoyle();
			case "rats":
				SootRats r = new SootRats();
				r.swarm = 4;
				return r;
			case "dog":         return new ChainDog();
		}
		return null;
	}
}
