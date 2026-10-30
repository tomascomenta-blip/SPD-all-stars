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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna;

//the beasts of the ash plains. they read the senses (Senses) on their own, so the plain pull of
//noise and corruption leaves them alone. the rest is ground laid for riding, breaking and taming
public interface Fauna {

	//cells of the body that will be weak spots, {column, row} of the footprint facing right
	default int[][] weakSpots(){
		return new int[0][];
	}

	//cells of the body that can be climbed, as {row, first column, last column} of the footprint
	//facing right (taken off the drawings)
	default int[][] climbable(){
		return new int[0][];
	}

	//could be tamed, once there's taming
	default boolean tameable(){
		return false;
	}
}
