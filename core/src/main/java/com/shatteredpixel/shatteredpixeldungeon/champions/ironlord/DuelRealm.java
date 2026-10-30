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

package com.shatteredpixel.shatteredpixeldungeon.champions.ironlord;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.IronRealmTiles;

//the realm of death room. sealed blue-brick hall glued as extra rows at the END of every map in a run
//w/ an ironlord, so existing cell indexes don't move (old saves!!). not connected to anything, only the 2 duellists go in
public class DuelRealm {

	//inside rows only, ROWS adds the 2 walls
	public static final int INTERIOR_H = 5;
	public static final int ROWS = INTERIOR_H + 2;

	public static boolean wanted(){
		if (Party.active()){
			for (Hero h : Party.heroes()){
				if (h.heroClass == HeroClass.IRONLORD) return true;
			}
			return false;
		}
		return Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.IRONLORD;
	}

	private static int interiorWidth( Level l ){
		int iw = Math.min(9, l.width() - 2);
		if (iw % 2 == 0) iw--;
		return iw;
	}

	public static void build( Level l ){
		int iw = interiorWidth(l);
		if (iw < 3) return;
		int w = l.width();
		int top = l.height();
		l.appendRows(ROWS);
		l.realmTop = top;
		int x0 = (w - iw) / 2;
		for (int y = 1; y <= INTERIOR_H; y++){
			for (int x = x0; x < x0 + iw; x++){
				l.map[(top + y) * w + x] = Terrain.EMPTY_SP;
			}
		}
		l.buildFlagMaps();
		l.cleanWalls();

		IronRealmTiles floor = new IronRealmTiles();
		floor.kind = 0;
		floor.setRect(x0 - 1, top, iw + 2, ROWS);
		l.customTerrain.add(floor);
		IronRealmTiles walls = new IronRealmTiles();
		walls.kind = 1;
		walls.setRect(x0 - 1, top, iw + 2, ROWS);
		l.customWalls.add(walls);
	}

	public static boolean exists( Level l ){
		return l.realmTop >= 0;
	}

	//start positions
	public static int heroCell( Level l ){
		int iw = interiorWidth(l);
		int x0 = (l.width() - iw) / 2;
		return (l.realmTop + 1 + INTERIOR_H / 2) * l.width() + x0 + iw / 2 - 1;
	}

	public static int enemyCell( Level l ){
		return heroCell(l) + 2;
	}

	public static void place( Char ch, int cell ){
		ScrollOfTeleportation.appear(ch, cell);
		Dungeon.level.occupyCell(ch);
		Dungeon.observe();
		GameScene.updateFog();
	}

	//loot from the room goes to the hero, nobody's walking back in there
	public static void rescueLoot( Hero hero ){
		Level l = Dungeon.level;
		for (Heap h : l.heaps.valueList()){
			if (!l.inRealm(h.pos)) continue;
			for (Item i : new java.util.ArrayList<>(h.items)){
				Heap moved = l.drop(i, hero.pos);
				if (moved.sprite != null) moved.sprite.drop();
			}
			h.destroy();
		}
	}

	//back where you were, or closest free cell
	public static void leave( Char ch, int origin ){
		Level l = Dungeon.level;
		int cell = freeCellNear(l, origin, ch);
		if (cell == -1) cell = l.randomRespawnCell(ch);
		if (cell == -1) return;
		place(ch, cell);
	}

	private static int freeCellNear( Level l, int origin, Char ch ){
		int w = l.width();
		int ox = origin % w;
		int oy = origin / w;
		for (int r = 0; r <= 8; r++){
			for (int y = oy - r; y <= oy + r; y++){
				for (int x = ox - r; x <= ox + r; x++){
					if (Math.max(Math.abs(x - ox), Math.abs(y - oy)) != r) continue;
					if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
					int c = x + y * w;
					if (l.inRealm(c) || !l.passable[c] || l.solid[c] || l.pit[c]) continue;
					if (Actor.findChar(c) != null) continue;
					if (Char.hasProp(ch, Char.Property.LARGE) && !l.openSpace[c]) continue;
					return c;
				}
			}
		}
		return -1;
	}
}
