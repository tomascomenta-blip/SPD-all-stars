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

package com.shatteredpixel.shatteredpixeldungeon.tiles.custom;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

//Ghostly blue bricks of the Ironlord's duel room (see DuelRealm). kind 0 paints the floor, kind 1 the walls around it.
//Tiles of the texture: 0 and 1 floor, 2 wall, 3 empty.
public class IronRealmTiles extends CustomTilemap {

	{
		texture = Assets.Environment.IRON_REALM;
	}

	public int kind = 0;

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		int[] data = new int[tileW * tileH];
		int i = 0;
		for (int y = 0; y < tileH; y++){
			for (int x = 0; x < tileW; x++){
				boolean ring = x == 0 || y == 0 || x == tileW - 1 || y == tileH - 1;
				if (kind == 0){
					data[i] = ring ? 3 : ((x + y) % 2 == 0 ? 0 : 1);
				} else {
					data[i] = ring ? 2 : 3;
				}
				i++;
			}
		}
		v.map(data, tileW);
		return v;
	}

	private static final String KIND = "kind";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(KIND, kind);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		kind = bundle.getInt(KIND);
	}
}
