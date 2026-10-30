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

//the sheets of the ash plains beasts. the big ones keep the continent layout (0-1 idle, 2-5 walk,
//6-8 attack, 9-11 die, extras from 12) except the giant and the whale, see their sprites.
//pyre, whale, giant and turtle come in two rows
public class FaunaAssets {

	public static final String JACKAL     = "sprites/colosos/chacal.png";      //16x16, 12 howl
	public static final String FLEA       = "sprites/colosos/pulga.png";       //10x10, 12-13 jump, 14 ignite
	public static final String BIRD       = "sprites/colosos/ave.png";         //16x16, 12-13 mimic, 14-15 flight
	public static final String SALAMANDER = "sprites/colosos/salamandra.png";  //64x64 (3x3), 12-13 under, 14-15 surfacing
	public static final String BEETLE     = "sprites/colosos/escarabajo.png";  //64x64 (3x3), 12-13 charge, 14 melting
	public static final String OBSIDIAN   = "sprites/colosos/coloso.png";      //112x112 (5x5), 12 double fist, 13 mirror, 14-16 cracked
	public static final String BISON      = "sprites/colosos/bisonte.png";     //112x112 (5x5), 12 scared, 13-16 stampede, 17 grazing
	public static final String BISON_FLEAS= "sprites/colosos/bisonte_parasitado.png";
	public static final String REX_TAMED  = "sprites/colosos/rex_domado.png";
	public static final String BISON_TAMED= "sprites/colosos/bisonte_domado.png";
	public static final String REX        = "sprites/colosos/rex.png";         //112x112 (5x5), 12-13 roar, 14 charge, 15-16 fire
	public static final String TURTLE     = "sprites/colosos/tortuga.png";     //192x256 (10x10), 10 a row
	public static final String GIANT      = "sprites/colosos/gigante.png";     //176x208 (8x8), 11 a row
	public static final String COLUMN     = "sprites/colosos/gigante_columna.png"; //48x48, 4 turning
	public static final String PYRE       = "sprites/colosos/pira.png";        //288x312 (15x15), 7 a row
	public static final String WHALE      = "sprites/colosos/ballena.png";     //256x256 (15x15), 8 a row
	public static final String TARANTULA  = "sprites/colosos/tarantula.png";   //208x192 (8x8), 9 a row
	public static final String FURROW     = "sprites/colosos/ballena_surco.png"; //32x32, 3 fading
}
