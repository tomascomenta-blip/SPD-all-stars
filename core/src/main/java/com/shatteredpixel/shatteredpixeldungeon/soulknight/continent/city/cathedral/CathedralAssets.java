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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral;

//the sheets of the inside of the furnace cathedral
public class CathedralAssets {

	public static final String MESSAGES = "messages/soulknight/continent_cathedral";

	//the tiles_sewers layout, and the molten metal for its water (32x32)
	public static final String TILES  = "environment/tiles_cathedral.png";
	public static final String MOLTEN = "environment/water_cathedral.png";
	//its furniture, 16x16 pieces, 16 a row (see CathedralDeco)
	public static final String DECO   = "environment/custom_tiles/cathedral_deco.png";

	//16x16: 0-1 idle, 2-5 walk, 6 the blow coming, 7 the blow, 8-9 seated in the pew, 10 tearing loose
	public static final String FAITHFUL = "sprites/colosos/city/catedral/fiel_fundido.png";
	//24x32, hanging: 0-1 swaying, 2-5 swinging, 6 drawn back, 7 the toll, 8-10 cracking and falling
	public static final String BELL     = "sprites/colosos/city/catedral/campana_sorda.png";
	//16x16: 0-1 idle, 2-5 walk, 6 shovel back, 7 the throw, 8 feeding the fire
	public static final String STOKER   = "sprites/colosos/city/catedral/atizador.png";
	//64x64 (3x3 from (8, 16)): 0-1 beating, 2-3 beating cracked, 4 swelling, 5 the burst, 6 the vents,
	//7 fed, 8-9 breaking, 10 dead
	public static final String HEART    = "sprites/colosos/city/catedral/corazon_horno.png";
	//laid over the fallen cathedral while its doors hang open (2 frames, the fire inside flickers)
	public static final String DOORS    = "sprites/colosos/city/catedral/puertas_abiertas.png";
}
