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

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Bundle;

//the pass on one edge of a surface, to the surface of the biome next to it. the party walks out
//of the map there and comes in on the opposite edge of the other one, as far along it as they
//were on this one (a regular level transition, so the whole party goes along like on stairs)
public class EdgeTransition extends LevelTransition {

	public int side;
	//where along the edge the one who crossed was (not saved: it's only used while crossing)
	public int along = -1;

	public EdgeTransition(){
		super();
	}

	//the strip of cells right inside the edge, as wide as the pass
	public EdgeTransition( Level level, int side, int at, int half, Biome to ){
		super(level, strip(level, side, at), Type.BRANCH_ENTRANCE, 21, to.branch, Type.BRANCH_ENTRANCE);
		this.side = side;
		int w = level.width(), h = level.height();
		switch (side){
			case Biome.N: set(at - half, 1, at + half, 1); break;
			case Biome.S: set(at - half, h - 2, at + half, h - 2); break;
			case Biome.E: set(w - 2, at - half, w - 2, at + half); break;
			default:      set(1, at - half, 1, at + half); break;
		}
	}

	private static int strip( Level level, int side, int at ){
		int w = level.width(), h = level.height();
		switch (side){
			case Biome.N: return at + w;
			case Biome.S: return at + (h - 2) * w;
			case Biome.E: return w - 2 + at * w;
			default:      return 1 + at * w;
		}
	}

	//the one who crossed, as they leave
	public void crossing( int cell, int width ){
		along = side == Biome.N || side == Biome.S ? cell % width : cell / width;
	}

	//where the party comes in on the level they arrive at: a free cell a few steps inside the
	//opposite edge, as far along it as they were. fallback if that level has no such pass
	public static int arrival( LevelTransition from, Level level, int fallback ){
		if (!(from instanceof EdgeTransition)) return fallback;
		EdgeTransition t = (EdgeTransition) from;
		int side = Biome.opposite(t.side);
		EdgeTransition in = null;
		for (LevelTransition lt : level.transitions){
			if (lt instanceof EdgeTransition && ((EdgeTransition) lt).side == side) in = (EdgeTransition) lt;
		}
		if (in == null) return fallback;
		int w = level.width(), h = level.height();
		boolean across = side == Biome.N || side == Biome.S;
		int lo = across ? in.left : in.top, hi = across ? in.right : in.bottom;
		int a = t.along < 0 ? (lo + hi) / 2 : Math.max(lo, Math.min(hi, t.along));
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int depth = 2; depth <= 8; depth++){
			for (int k = lo - 2; k <= hi + 2; k++){
				int x, y;
				switch (side){
					case Biome.N: x = k; y = depth; break;
					case Biome.S: x = k; y = h - 1 - depth; break;
					case Biome.E: x = w - 1 - depth; y = k; break;
					default:      x = depth; y = k; break;
				}
				if (x < 0 || y < 0 || x >= w || y >= h) continue;
				int c = x + y * w;
				if (!level.passable[c] || level.avoid[c]) continue;
				int d = Math.abs(k - a) * 2 + depth;
				if (d < bestD){
					bestD = d;
					best = c;
				}
			}
		}
		return best != -1 ? best : fallback;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("side", side);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		side = bundle.getInt("side");
	}
}
