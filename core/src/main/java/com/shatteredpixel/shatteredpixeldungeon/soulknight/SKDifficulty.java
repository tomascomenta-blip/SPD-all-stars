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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//run difficulty = mob hp mult: x0.5, x1 (default), x2, x3, x4. stacks on top of the coop party bonus (CoopBalance)
//picked before the run (SPDSettings), saved in Dungeon.difficulty, host's pick wins in coop
public class SKDifficulty {

	public static final float[] FACTORS = {0.5f, 1f, 2f, 3f, 4f};
	public static final int NORMAL = 1;

	public static int clamp( int level ){
		return Math.max(0, Math.min(FACTORS.length - 1, level));
	}

	public static float factor( int level ){
		return FACTORS[clamp(level)];
	}

	//applied ONCE when the mob first enters the level. not on load, or hp would multiply every save lol
	public static float hpFactor( Char ch ){
		if (!(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY) return 1f;
		return factor(Dungeon.difficulty);
	}

	public static String name( int level ){
		return Messages.get(SKDifficulty.class, "level_" + clamp(level));
	}

	//"Normal (x1)"
	public static String label( int level ){
		float f = factor(level);
		String num = f == (int) f ? Integer.toString((int) f) : "0,5";
		return Messages.get(SKDifficulty.class, "label", name(level), num);
	}

	//for the next run
	public static int chosen(){
		return SPDSettings.skDifficulty();
	}

	//% per region of the next run getting a new world
	public static int worldChance(){
		return SPDSettings.skWorldChance() * 10;
	}
}
