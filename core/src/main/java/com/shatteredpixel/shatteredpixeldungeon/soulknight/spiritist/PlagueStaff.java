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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.SKGun;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//spiritist starting staff. green spike, dmg + poison on the first thing it hits
public class PlagueStaff extends SKGun {

	{
		image = ItemSpriteSheet.PLAGUE_STAFF;
		bones = false;
	}

	@Override
	protected int shotImage() {
		return ItemSpriteSheet.SHOT_PLAGUE;
	}

	@Override
	public int initialCharges() {
		return 3;
	}

	@Override
	public int min( int lvl ) {
		return 1 + lvl;
	}

	@Override
	public int max( int lvl ) {
		return 4 + 2*lvl;
	}

	public int poison( Char user ){
		int p = 3 + buffedLvl();
		if (user instanceof Hero) p += 2*((Hero) user).pointsInTalent(Talent.SP_GRAVE_CHILL);
		return p;
	}

	@Override
	public void onZap( Ballistica shot ) {
		Char ch = Actor.findChar(shot.collisionPos);
		if (ch != null && ch != curUser){
			shoot(ch, damageRoll());
			if (ch.isAlive()) Buff.affect(ch, Poison.class).set(poison(curUser));
		}
	}

	@Override
	public String statsDesc() {
		return Messages.get(this, "stats_desc", min(), max(), poison(Dungeon.hero));
	}
}
