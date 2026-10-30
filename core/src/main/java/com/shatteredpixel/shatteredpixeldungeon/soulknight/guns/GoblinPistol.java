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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.guns;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//goblin pistol: weak, lots of charges. pew pew
public class GoblinPistol extends SKGun {

	{
		image = ItemSpriteSheet.GOBLIN_PISTOL;
	}

	@Override
	protected int shotImage() {
		return ItemSpriteSheet.SHOT_BULLET;
	}

	@Override
	public int initialCharges() {
		return 4;
	}

	@Override
	public int min( int lvl ) {
		return 1 + lvl;
	}

	@Override
	public int max( int lvl ) {
		return 5 + 2*lvl;
	}
}
