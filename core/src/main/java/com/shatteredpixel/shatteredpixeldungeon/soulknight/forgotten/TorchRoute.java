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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.watabou.utils.PathFinder;

import java.util.HashSet;

//the way the sacred flame has to go on each floor: camp fire (21) or the stairs (22-24) to the stairs
//down, or to the altar on 24. at levelgen the cold floors get braziers along it, so no cold stretch is
//longer than GAP whatever the map rolled. measure() is the same walk on paper, for the tests
public class TorchRoute {

	//flame used between two warm spots before a brazier goes in. the flame has 100,
	//the rest is for fights, blizzards and looking around
	public static final int GAP = 24;

	//what a step costs the flame, the same numbers as Tribe.flameTurn
	public static final int COLD = 1, WET = 3, WARM_BACK = 2;

	public static void place( ForgottenLevel level ){
		//-Dspd.test.tribe=4 measures the floors as they were before
		if (Dungeon.depth < 21 || Dungeon.depth > 23 || DevFlags.tribe() == 4) return;
		int[] path = route(level);
		if (path == null || path.length == 0) return;
		int used = 0;
		//22, 23: one next to the stairs, the flame comes down from the floor above
		if (Dungeon.depth > 21 && !ForgottenFloor.nearBrazier(level, path[0], 2)){
			for (int i = 0; i < Math.min(8, path.length); i++){
				if (brazierBy(level, path, i)) break;
			}
		}
		for (int i = 0; i < path.length; i++){
			int c = path[i];
			if (ForgottenFloor.nearBrazier(level, c, 2)){
				used = 0;
				continue;
			}
			if (!level.cold(c)){
				used = Math.max(0, used - WARM_BACK);
				continue;
			}
			used += cost(level, c);
			if (used >= GAP && brazierBy(level, path, i)) used = 0;
		}
	}

	//dry floor first. in a flooded stretch the tribe sets it on a stone in the water
	private static boolean brazierBy( ForgottenLevel level, int[] path, int i ){
		int spot = spotNear(level, path, i, false);
		if (spot == -1){
			spot = spotNear(level, path, i, true);
			if (spot == -1) return false;
			Level.set(spot, Terrain.EMPTY, level);
		}
		ForgottenFeatures.add(level, new ForgottenFeatures.Brazier(), spot);
		return true;
	}

	public static int cost( Level level, int cell ){
		return COLD + (level.water[cell] ? WET : 0);
	}

	//where the flame starts on this floor and where it has to get to
	private static int start( Level level ){
		if (Dungeon.depth == 21){
			for (Mob m : level.mobs){
				if (m instanceof Tribe.SacredFire) return m.pos;
			}
		}
		return level.entrance();
	}

	private static int goal( Level level ){
		if (Dungeon.depth == 24){
			for (Mob m : level.mobs){
				if (m instanceof Tribe.Altar) return m.pos;
			}
		}
		return level.exit();
	}

	//what you can walk on. the tribe's things (braziers, huts, the fire...), statues and the shaman
	//never move. with 'around' also not through what could be broken (ice walls, frozen beasts,
	//crates); with 'hidden' through secret and locked doors too (the key is on the floor)
	private static boolean[] open( Level level, boolean around, boolean hidden ){
		boolean[] open = new boolean[level.length()];
		for (int i = 0; i < open.length; i++){
			open[i] = (level.passable[i] && !level.pit[i])
					|| (hidden && (level.secret[i] || level.map[i] == Terrain.LOCKED_DOOR));
		}
		for (Mob m : level.mobs){
			if (m instanceof ForgottenFeatures.Thing || (around && m instanceof SKProps.SKProp)
					|| (m.alignment != Char.Alignment.ENEMY && m.properties().contains(Char.Property.IMMOVABLE))){
				open[m.pos] = false;
			}
		}
		return open;
	}

	//shortest walk, start excluded, goal included
	public static int[] route( Level level ){
		return way(level, start(level), goal(level));
	}

	//the way anyone would take: in plain sight, around the props unless breaking one saves a good
	//detour, and through hidden doors only if there's nothing else. the stairs on the way don't count
	public static int[] way( Level level, int from, int to ){
		if (from < 0 || to < 0 || from == to) return null;
		int[] around = way(level, from, to, true, false);
		int[] through = way(level, from, to, false, false);
		if (around != null && (through == null || around.length <= through.length + 4)) return around;
		if (through != null) return through;
		return way(level, from, to, false, true);
	}

	private static int[] way( Level level, int from, int to, boolean around, boolean hidden ){
		boolean[] open = open(level, around, hidden);
		for (LevelTransition t : level.transitions) open[t.cell()] = false;
		open[from] = true;
		open[to] = true;
		PathFinder.setMapSize(level.width(), level.height());
		PathFinder.Path p = PathFinder.find(from, to, open);
		if (p == null) return null;
		int[] out = new int[p.size()];
		int i = 0;
		for (int c : p) out[i++] = c;
		return out;
	}

	//a free cell beside the walk that doesn't cut anything off. next to it first, then 2 away
	//(still in the warm circle of the brazier). same order on every device
	private static int spotNear( Level level, int[] path, int i, boolean wet ){
		HashSet<Integer> onPath = new HashSet<>();
		for (int c : path) onPath.add(c);
		onPath.add(start(level));
		int c = path[i];
		int best = -1, bestRoom = -1;
		for (int d = 1; d <= 2 && best == -1; d++){
			for (int y = -d; y <= d; y++){
				for (int x = -d; x <= d; x++){
					if (Math.max(Math.abs(x), Math.abs(y)) != d) continue;
					int cx = c % level.width() + x, cy = c / level.width() + y;
					if (cx <= 0 || cy <= 0 || cx >= level.width() - 1 || cy >= level.height() - 1) continue;
					int k = cx + cy * level.width();
					if (onPath.contains(k) || !free(level, k, wet)) continue;
					int room = 0;
					for (int n : PathFinder.NEIGHBOURS8) if (level.passable[k + n]) room++;
					if (room > bestRoom && !cutsOff(level, k)){
						best = k;
						bestRoom = room;
					}
				}
			}
		}
		return best;
	}

	private static boolean free( Level level, int c, boolean wet ){
		if (!level.passable[c] || level.solid[c] || level.pit[c]) return false;
		int t = level.map[c];
		if (wet ? t != Terrain.WATER : (level.water[c] || (t != Terrain.EMPTY && t != Terrain.EMPTY_SP
				&& t != Terrain.EMPTY_DECO && t != Terrain.GRASS && t != Terrain.HIGH_GRASS
				&& t != Terrain.FURROWED_GRASS))) return false;
		if (level.heaps.get(c) != null || level.traps.get(c) != null || level.plants.get(c) != null) return false;
		if (level.findMob(c) != null) return false;
		if (level.distance(c, level.entrance()) < 2 || level.distance(c, level.exit()) < 2) return false;
		for (int n : PathFinder.NEIGHBOURS8){
			int k = level.map[c + n];
			if (k == Terrain.DOOR || k == Terrain.OPEN_DOOR || k == Terrain.LOCKED_DOOR || k == Terrain.SECRET_DOOR) return false;
		}
		return true;
	}

	//a brazier there must not wall anything off
	private static boolean cutsOff( Level level, int c ){
		boolean[] open = open(level, false, true);
		int before = reach(level, open);
		open[c] = false;
		return reach(level, open) < before - 1;
	}

	private static int reach( Level level, boolean[] open ){
		int from = level.entrance();
		if (from < 0) return 0;
		boolean was = open[from];
		open[from] = true;
		PathFinder.setMapSize(level.width(), level.height());
		PathFinder.buildDistanceMap(from, open);
		open[from] = was;
		int n = 0;
		for (int i = 0; i < level.length(); i++){
			if (PathFinder.distance[i] < Integer.MAX_VALUE) n++;
		}
		return n;
	}

	//the same walk on paper, for the tests: {steps, cold steps, braziers along it, longest cold stretch,
	//flame at the end, lowest flame, times it went out}. the flame comes in with what it had upstairs.
	//old = the rules before the way had braziers: water took 10, the warm side gave nothing back
	//and once out it stayed out
	public static int[] measure( ForgottenLevel level, int flameIn, boolean old ){
		int[] path = route(level);
		if (path == null) return null;
		HashSet<Mob> seen = new HashSet<>();
		int cold = 0, longest = 0, used = 0, flame = flameIn, lowest = flameIn, outs = 0;
		boolean lit = flameIn > 0;
		for (int c : path){
			for (Mob m : level.mobs){
				if (m instanceof ForgottenFeatures.Brazier && level.distance(m.pos, c) <= 2) seen.add(m);
			}
			if (!lit){
				if (!old && ForgottenFloor.nearBrazier(level, c, 1)){
					lit = true;
					flame = 100;
				}
				continue;
			}
			if (ForgottenFloor.nearBrazier(level, c, 2)){
				used = 0;
				flame = 100;
				continue;
			}
			if (!level.cold(c)){
				used = Math.max(0, used - WARM_BACK);
				if (!old) flame = Math.min(100, flame + WARM_BACK);
				continue;
			}
			cold++;
			int step = old ? COLD + (level.water[c] ? 10 : 0) : cost(level, c);
			used += step;
			flame = Math.max(0, flame - step);
			longest = Math.max(longest, used);
			lowest = Math.min(lowest, flame);
			if (flame == 0){
				lit = false;
				outs++;
			}
		}
		return new int[]{path.length, cold, seen.size(), longest, lit ? flame : 0, lowest, outs};
	}
}
