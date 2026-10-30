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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.ArmoryGun;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//kraken drop. eye in coral, chain lightning. yes you're shooting lightning out of an eye
public class KrakenEyeGun extends ArmoryGun {

	{
		image = ItemSpriteSheet.KRAKEN_EYE;
		tier = 5; kind = CHAIN; count = 4; reach = 6; charges = 3; effect = STUN;
		baseMin = 6; lvlMin = 2; baseMax = 14; lvlMax = 3;
		shot = ItemSpriteSheet.SHOT_ORB;
		sound = Assets.Sounds.LIGHTNING;
	}
}
