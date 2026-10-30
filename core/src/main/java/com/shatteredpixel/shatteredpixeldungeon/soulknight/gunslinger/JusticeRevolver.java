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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.gunslinger;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.SKGun;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//gunslinger starting gun. one big bullet, duel + steady aim buff it
public class JusticeRevolver extends SKGun {

	{
		image = ItemSpriteSheet.JUSTICE_REVOLVER;
		bones = false;
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
		return 2 + lvl;
	}

	@Override
	public int max( int lvl ) {
		return 6 + 2*lvl;
	}

	@Override
	public void onZap( Ballistica shot ) {
		Char ch = Actor.findChar(shot.collisionPos);
		if (ch == null || ch == curUser) return;
		float dmg = damageRoll();
		if (curUser instanceof Hero){
			Hero h = (Hero) curUser;
			dmg += h.pointsInTalent(Talent.GS_STEADY_AIM);
			if (GunslingerHooks.inDuel(h)) dmg *= GunslingerHooks.Duel.DAMAGE;
		}
		shoot(ch, Math.round(dmg));
	}
}
