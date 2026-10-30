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

package com.shatteredpixel.shatteredpixeldungeon.champions;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;

import java.util.ArrayList;

//square area helpers. FIXED ORDER, coop
public class ChampionAreas {

	//non-solid + visible from the center
	public static ArrayList<Integer> cells( int center, int radius ){
		ArrayList<Integer> result = new ArrayList<>();
		int w = Dungeon.level.width();
		int cx = center % w;
		int cy = center / w;
		for (int dy = -radius; dy <= radius; dy++){
			for (int dx = -radius; dx <= radius; dx++){
				int x = cx + dx;
				int y = cy + dy;
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
				int c = x + y*w;
				if (Dungeon.level.solid[c] && c != center) continue;
				if (c != center && Dungeon.level.distance(c, center) > 1 && !sees(center, c)) continue;
				result.add(c);
			}
		}
		return result;
	}

	private static boolean sees( int from, int to ){
		return new Ballistica(from, to,
				Ballistica.STOP_SOLID).collisionPos == to;
	}

	//what an area ability of that hero hits: enemies, and the other heroes with friendly fire on
	public static ArrayList<Char> enemiesIn( Char attacker, ArrayList<Integer> cells ){
		ArrayList<Char> result = new ArrayList<>();
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null && ch.isAlive() && CoopRules.hits(attacker, ch)) result.add(ch);
		}
		return result;
	}

	public static ArrayList<Char> friendsIn( ArrayList<Integer> cells ){
		ArrayList<Char> result = new ArrayList<>();
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null && ch.isAlive() && ch.alignment == Char.Alignment.ALLY) result.add(ch);
		}
		return result;
	}
}
