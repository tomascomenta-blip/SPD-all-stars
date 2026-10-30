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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;

//rules of the run the host picks in the room. they're saved with the run, so every device
//reads the same thing (game logic only reads Dungeon, never the settings)
public class CoopRules {

	//friendly fire: heroes hurt each other with everything they do
	public static boolean friendlyFire(){
		return Dungeon.friendlyFire && Party.active();
	}

	//another hero of the party that a hero's attack or ability may hurt right now
	public static boolean friendlyTarget( Char attacker, Char ch ){
		return ch != attacker && friendlyFire() && Party.isPartyHero(attacker) && Party.isPartyHero(ch);
	}

	//"is this an enemy?" for the attacks and abilities of the heroes: enemies, plus the other
	//heroes when friendly fire is on. use it where the code used to ask ch.alignment == ENEMY
	public static boolean hits( Char attacker, Char ch ){
		if (ch == null || ch == attacker) return false;
		return ch.alignment == Char.Alignment.ENEMY || friendlyTarget(attacker, ch);
	}
}
