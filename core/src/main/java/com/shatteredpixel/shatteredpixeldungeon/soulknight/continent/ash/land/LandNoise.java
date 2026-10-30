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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.land;

//numbers that only depend on (seed, cell): the tile variants of a saved map come out the same
//when it's loaded again, on every device, without storing them and without touching Random
class LandNoise {

	static int hash( int seed, int a, int b ){
		int h = seed ^ (a * 0x9E3779B1) ^ (b * 0x85EBCA6B);
		h ^= h >>> 16;
		h *= 0x7FEB352D;
		h ^= h >>> 15;
		h *= 0x846CA68B;
		h ^= h >>> 16;
		return h;
	}

	//0 (inclusive) to 1 (exclusive)
	static float roll( int seed, int cell, int salt ){
		return (hash(seed, cell, salt) >>> 8) / (float)(1 << 24);
	}

	private static float lattice( int seed, int x, int y ){
		return (hash(seed, x, y) >>> 8) / (float)(1 << 24);
	}

	private static float smooth( float t ){
		return t * t * (3 - 2 * t);
	}

	//smooth value noise, 0-1, one blob every "scale" cells
	static float value( int seed, float x, float y, float scale ){
		x /= scale;
		y /= scale;
		int x0 = (int)Math.floor(x), y0 = (int)Math.floor(y);
		float fx = smooth(x - x0), fy = smooth(y - y0);
		float a = lattice(seed, x0, y0), b = lattice(seed, x0 + 1, y0);
		float c = lattice(seed, x0, y0 + 1), d = lattice(seed, x0 + 1, y0 + 1);
		float top = a + (b - a) * fx, bottom = c + (d - c) * fx;
		return top + (bottom - top) * fy;
	}

	//two octaves, -1 to 1
	static float wobble( int seed, float x, float y, float scale ){
		float n = 0.68f * value(seed, x, y, scale) + 0.32f * value(seed + 101, x, y, scale / 2.6f);
		return n * 2 - 1;
	}
}
