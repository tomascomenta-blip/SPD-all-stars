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

package com.shatteredpixel.shatteredpixeldungeon.coop;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;

//bigger party = beefier enemies. each hero above 2: +35% hp, +10% dmg (8 heroes = +210%/+60%, rip).
//only depends on party size so every device agrees. rift bosses scale too, they were a joke w/ 4 people
public class CoopBalance {

	public static final float HP_PER_HERO = 0.35f;
	public static final float DMG_PER_HERO = 0.10f;

	public static int extraHeroes(){
		return Party.active() ? Math.max(0, Party.size() - 2) : 0;
	}

	private static boolean enemy( Char ch ){
		return ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY;
	}

	//health multiplier, applied once when an enemy first enters the level
	public static float hpFactor( Char ch ){
		int extra = extraHeroes();
		return extra > 0 && enemy(ch) ? 1f + HP_PER_HERO*extra : 1f;
	}

	//damage multiplier of an enemy's attacks
	public static float damageFactor( Char ch ){
		int extra = extraHeroes();
		return extra > 0 && enemy(ch) ? 1f + DMG_PER_HERO*extra : 1f;
	}
}
