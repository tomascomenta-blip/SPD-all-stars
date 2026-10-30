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

import com.shatteredpixel.shatteredpixeldungeon.levels.LastLevel;

//amulet floor if region 5 was forgotten land. half ice half jungle cave, amulet on the old altar between 2 totems
public class ForgottenLastLevel extends LastLevel {

	@Override
	public String tilesTex() {
		return ForgottenAssets.TILES_JUNGLE;
	}

	@Override
	public String waterTex() {
		return ForgottenAssets.WATER_JUNGLE;
	}

	@Override
	protected void createMobs() {
		super.createMobs();
		int[] spots = {AMULET_POS - 2, AMULET_POS + 2};
		for (int c : spots){
			if (c > 0 && c < length() && passable[c] && findMob(c) == null){
				Tribe.Totem t = new Tribe.Totem();
				t.pos = c;
				mobs.add(t);
			}
		}
	}
}
