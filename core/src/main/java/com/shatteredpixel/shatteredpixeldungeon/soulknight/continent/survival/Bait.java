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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

//a lump of meat on a string, smeared in blood. throw it and everything hungry around goes to see:
//to pull them off someone, or away from where you want to go
public class Bait extends Item {

	public static final int RANGE = 10;

	{
		image = ItemSpriteSheet.BAIT;
		stackable = true;
		defaultAction = AC_THROW;
	}

	@Override
	protected void onThrow( int cell ) {
		Senses.noise(cell, RANGE);
		int lured = 0;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!m.isAlive() || m.alignment != Char.Alignment.ENEMY || Char.hasProp(m, Char.Property.IMMOVABLE)) continue;
			if (Dungeon.level.distance(m.pos, cell) > RANGE) continue;
			m.beckon(cell);
			lured++;
		}
		CoopLog.line("survival: bait at " + cell + ", " + lured + " come to it");
		if (Dungeon.level.heroFOV[cell]){
			Splash.at(cell, 0xFF8A1A1A, 8);
			Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 0.6f, 0.7f);
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 5 * quantity;
	}
}
