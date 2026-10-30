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
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossusSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;

//the big things of the devoured city. same senses, knocking down and climbing as the ash beasts;
//this adds the shapes their blows take (lanes, cones, the side they face) and what they let out
public abstract class CityColossus extends AshColossus {

	//straight unless it's clearly off to the diagonal
	protected int[] toward( int cell ){
		int w = Dungeon.level.width();
		int dx = cell % w - pos % w, dy = cell / w - pos / w;
		int sx = Math.abs(dx) * 2 >= Math.abs(dy) ? Integer.signum(dx) : 0;
		int sy = Math.abs(dy) * 2 >= Math.abs(dx) ? Integer.signum(dy) : 0;
		return new int[]{sx, sy};
	}

	//the middles it would go through running that way, as long as the whole body fits
	protected ArrayList<Integer> path( int dx, int dy, int range ){
		ArrayList<Integer> out = new ArrayList<>();
		if (dx == 0 && dy == 0) return out;
		boolean[] f = fits();
		int w = Dungeon.level.width();
		int x = pos % w, y = pos / w;
		for (int k = 1; k <= range; k++){
			int nx = x + dx * k, ny = y + dy * k;
			if (nx < 0 || ny < 0 || nx >= w || ny >= Dungeon.level.height()) break;
			if (!f[nx + ny * w]) break;
			out.add(nx + ny * w);
		}
		return out;
	}

	//every cell the body sweeps going through those middles
	protected LinkedHashSet<Integer> lane( ArrayList<Integer> centers ){
		LinkedHashSet<Integer> out = new LinkedHashSet<>();
		int w = Dungeon.level.width(), h = half();
		for (int c : centers){
			for (int y = -h; y < size - h; y++){
				for (int x = -h; x < size - h; x++){
					int k = c + x + y * w;
					if (k >= 0 && k < Dungeon.level.length() && !covers(k) && !Dungeon.level.solid[k]) out.add(k);
				}
			}
		}
		return out;
	}

	//a cone from a cell towards another, as far as range (cos: how narrow, 0.86 is 60 degrees)
	protected ArrayList<Integer> cone( int from, int aim, int range, double cos ){
		ArrayList<Integer> out = new ArrayList<>();
		Level l = Dungeon.level;
		int w = l.width();
		double fx = from % w, fy = from / w;
		double ax = aim % w - fx, ay = aim / w - fy;
		double len = Math.sqrt(ax * ax + ay * ay);
		if (len < 0.5) return out;
		for (int c : SKCombat.area(from, range)){
			if (covers(c) || l.pit[c] || l.solid[c]) continue;
			double cx = c % w - fx, cy = c / w - fy;
			double cl = Math.sqrt(cx * cx + cy * cy);
			if (cl > range + 0.5) continue;
			if (cl >= 0.5 && (cx * ax + cy * ay) / (cl * len) < cos) continue;
			if (c != from && new Ballistica(from, c, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos != c) continue;
			out.add(c);
		}
		return out;
	}

	//the edge cell of the body that faces a cell (where a mouth or a neck would be)
	protected int edgeToward( int cell ){
		int w = Dungeon.level.width();
		int x = Math.max(left(), Math.min(left() + size - 1, cell % w));
		int y = Math.max(top(), Math.min(top() + size - 1, cell / w));
		return x + y * w;
	}

	//the ground in front of the side that faces a cell: depth cells out, as wide as the body
	//plus spread on each side
	protected ArrayList<Integer> front( int aim, int depth, int spread ){
		ArrayList<Integer> out = new ArrayList<>();
		int[] d = toward(aim);
		if (d[0] == 0 && d[1] == 0) d[0] = right ? 1 : -1;
		Level l = Dungeon.level;
		int w = l.width();
		for (int y = top() - depth; y < top() + size + depth; y++){
			for (int x = left() - depth; x < left() + size + depth; x++){
				if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
				int c = x + y * w;
				if (covers(c) || l.solid[c] || l.pit[c]) continue;
				int ox = x < left() ? -1 : x >= left() + size ? 1 : 0;
				int oy = y < top() ? -1 : y >= top() + size ? 1 : 0;
				boolean ahead = d[0] != 0 && ox == d[0] || d[1] != 0 && oy == d[1];
				if (!ahead) continue;
				//not past the sides by more than spread
				if (d[0] != 0 && d[1] == 0 && (y < top() - spread || y >= top() + size + spread)) continue;
				if (d[1] != 0 && d[0] == 0 && (x < left() - spread || x >= left() + size + spread)) continue;
				out.add(c);
			}
		}
		return out;
	}

	//runs down a path, trampling what's there. returns how far it got
	//whoever it ran over this time, so one charge hurts each of them once
	private final HashSet<Char> runOver = new HashSet<>();

	protected boolean firstRunOver( Char ch ){
		return runOver.add(ch);
	}

	protected int run( ArrayList<Integer> centers ){
		runOver.clear();
		int start = pos, n = 0;
		for (int c : centers){
			if (!isAlive() || !fitsAt(c)) break;
			walkTo(c);
			n++;
		}
		if (sprite != null && pos != start) sprite.move(start, pos);
		return n;
	}

	//small things let out of its body, right around it. they come and go with it: no xp, no meat
	protected void letOut( Mob m, int near ){
		ArrayList<Integer> cells = AshFauna.openAround(near, 1);
		for (int c : cells.toArray(new Integer[0])){
			if (covers(c)) cells.remove((Integer) c);
		}
		if (cells.isEmpty()) return;
		m.pos = cells.get(Random.Int(cells.size()));
		if (m instanceof SKMob) ((SKMob) m).summoned();
		GameScene.add(m);
		Char foe = enemy;
		if (foe != null && foe.isAlive()) m.aggro(foe);
	}

	//a free cell next to the body, nearest to a cell
	protected int besideToward( int cell ){
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int c : ring(1)){
			if (Actor.findChar(c) != null) continue;
			int d = Dungeon.level.distance(c, cell);
			if (d < bestD){
				bestD = d;
				best = c;
			}
		}
		return best;
	}

	protected boolean anyHeroWithin( int r ){
		for (Hero h : Party.living()){
			if (reach(h.pos) <= r) return true;
		}
		return false;
	}

	//one of the usual sheets of the city. beasts: 0-1 idle, 2-5 walk, 6 the blow coming, 7 the
	//blow, then extras; the 5x5 lie down on 8 and die on 9. the giants lie down on 9 and die on 10
	public abstract static class Sheet extends AshColossusSprite {

		private int down = -1;

		protected void layout( String tex, int size, int frameW, int frameH, int ox, int oy, int down, int dead ){
			setup(tex, size, frameW, frameH, ox, oy);
			this.down = down;
			idle = loop(2, 0, 0, 1, 1);
			run = loop(size >= 10 ? 5 : 8, 2, 3, 4, 5);
			attack = clip(10, 6, 7, 7);
			zap = attack.clone();
			die = dead >= 0 ? clip(6, down >= 0 ? down : 7, dead, dead) : clip(8, 6, 7, 7);
			play(idle);
		}

		@Override
		protected int[] downFrames() {
			return down < 0 ? null : new int[]{down, down, down, 0};
		}

		@Override
		public void strike( int kind ) {
			once(clip(10, 7, 7, 7, 0));
		}
	}
}
