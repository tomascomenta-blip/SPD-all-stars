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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Mutations;
import com.watabou.utils.Random;

import java.util.ArrayList;

//what's left of a colossus (Colossus.die): meat by its size, and from the big ones a bone
public class Remains {

	public static void drop( Colossus c ){
		Level l = Dungeon.level;
		if (l == null || c.pos < 0) return;
		int meat = Math.max(1, (c.size + 1) / 2);
		put(new ColossusMeat.Raw().quantity(meat), landing(c, c.pos));
		boolean bone = c.size >= 5 || Random.Int(2) == 0;
		if (bone){
			//somewhere else on the patch it lay on
			ArrayList<Integer> cells = new ArrayList<>();
			for (int cell : c.cells()){
				if (cell != c.pos && dry(cell)) cells.add(cell);
			}
			put(new ColossusBone(), cells.isEmpty() ? landing(c, c.pos) : Random.element(cells));
		}
		if (c.size >= 5 && Mutations.extraBones(c) > 0) put(new ColossusBone(), landing(c, c.pos));
		CoopLog.line("survival: " + c.getClass().getSimpleName() + " (size " + c.size + ") leaves "
				+ meat + " meat" + (bone ? " and a bone" : "") + " at " + c.pos);
	}

	//firm ground a hero can walk to: not lava, not a chasm
	private static boolean dry( int cell ){
		Level l = Dungeon.level;
		return cell >= 0 && cell < l.length() && l.passable[cell] && !l.pit[cell] && !l.water[cell] && !l.avoid[cell];
	}

	//the fiery ones die standing in lava: what they leave rolls out to the nearest dry cell of the patch
	private static int landing( Colossus c, int cell ){
		if (dry(cell)) return cell;
		int best = cell, bestD = Integer.MAX_VALUE;
		for (int other : c.cells()){
			int d = Dungeon.level.distance(other, cell);
			if (dry(other) && d < bestD){
				best = other;
				bestD = d;
			}
		}
		return best;
	}

	private static void put( Item item, int cell ){
		Heap h = Dungeon.level.drop(item, cell);
		if (h.sprite != null) h.sprite.drop();
	}
}
