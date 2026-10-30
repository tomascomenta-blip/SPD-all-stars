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

package com.shatteredpixel.shatteredpixeldungeon.coop;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

//a way to say "THAT item" that means the same thing on every machine
//"b:<n>"        n-th item of the hero's belongings (equipped first, then the backpack and its bags)
//"p:<i>:<n>"    n-th item of party hero i's belongings
//"h:<cell>:<n>" n-th item of the heap on a cell
public class ItemRef {

	public static String of( Hero owner, Item item ){
		if (item == null) return "";
		int n = indexIn(owner, item);
		if (n >= 0) return "b:" + n;
		for (Hero h : Party.heroes()){
			if (h == owner) continue;
			n = indexIn(h, item);
			if (n >= 0) return "p:" + Party.indexOf(h) + ":" + n;
		}
		if (Dungeon.level != null){
			for (Heap heap : Dungeon.level.heaps.valueList()){
				int i = 0;
				for (Item it : heap.items){
					if (it == item) return "h:" + heap.pos + ":" + i;
					i++;
				}
			}
		}
		return "";
	}

	public static Item resolve( Hero owner, String ref ){
		if (ref == null || ref.isEmpty()) return null;
		try {
			String[] p = ref.split(":");
			switch (p[0]){
				case "b":
					return nthIn(owner, Integer.parseInt(p[1]));
				case "p":
					return nthIn(Party.get(Integer.parseInt(p[1])), Integer.parseInt(p[2]));
				case "h":
					Heap heap = Dungeon.level.heaps.get(Integer.parseInt(p[1]));
					if (heap == null) return null;
					int n = Integer.parseInt(p[2]);
					int i = 0;
					for (Item it : heap.items){
						if (i == n) return it;
						i++;
					}
					return null;
			}
		} catch (Exception e){
			//bad reference, treated as missing
		}
		return null;
	}

	private static int indexIn( Hero h, Item item ){
		if (h == null) return -1;
		int i = 0;
		for (Item it : h.belongings){
			if (it == item) return i;
			i++;
		}
		return -1;
	}

	private static Item nthIn( Hero h, int n ){
		if (h == null) return null;
		int i = 0;
		for (Item it : h.belongings){
			if (i == n) return it;
			i++;
		}
		return null;
	}
}
