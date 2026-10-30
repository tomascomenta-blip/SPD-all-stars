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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.SKGun;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.watabou.utils.Callback;

//void rifle, the queen drops it. 3 round burst of purple bullets at the first thing in the way
public class VoidRifle extends SKGun {

	public static final int BURST = 3;

	{
		image = ItemSpriteSheet.VOID_RIFLE;
		unique = true;
	}

	@Override
	protected int shotImage() {
		return ItemSpriteSheet.SHOT_SPIKE;
	}

	@Override
	public int initialCharges() {
		return 3;
	}

	@Override
	public int min( int lvl ) {
		return 2 + lvl;
	}

	@Override
	public int max( int lvl ) {
		return 6 + lvl;
	}

	@Override
	public void onZap( Ballistica shot ) {
		Char ch = Actor.findChar(shot.collisionPos);
		if (ch == null) return;
		for (int i = 0; i < BURST && ch.isAlive(); i++){
			shoot(ch, damageRoll());
		}
	}

	@Override
	public void fx( Ballistica bolt, Callback callback ) {
		//first 2 are just for show, the 3rd one finishes the burst
		for (int i = 0; i < BURST - 1; i++){
			((MissileSprite) curUser.sprite.parent.recycle(MissileSprite.class)).reset(
					curUser.sprite, bolt.collisionPos, new SKMobSprite.Shot(shotImage()), null);
		}
		super.fx(bolt, callback);
	}
}
