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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom;

//throne hall pics
public class ThroneAssets {

	private static final String D = "sprites/throne/";

	public static final String KNIGHT       = D + "grand_knight.png";       //40x40, 4 rows of 11
	public static final String KNIGHT_HORSE = D + "grand_knight_horse.png"; //56x48, 11
	public static final String BANNER       = D + "grand_banner.png";       //24x40, 8
	public static final String SHADOW       = D + "shadow_knight.png";      //32x36, 11
	public static final String THRONE       = D + "throne.png";             //40x40
	public static final String COLUMN       = D + "column.png";             //16x32, 3
	public static final String SHIELD_WALL  = D + "shield_wall.png";        //16x16, 3
	public static final String CHANDELIER   = D + "chandelier.png";         //32x32, 5
	public static final String CLEAT        = D + "rope_cleat.png";         //16x16, 2
	public static final String BRAZIER      = D + "hall_brazier.png";       //16x24, 4
	public static final String WALLS        = D + "hall_walls.png";         //16x32, 9
	public static final String FLOOR        = D + "hall_floor.png";         //16x16 atlas, 8 per row

	public static final String BAR          = "interfaces/knight_bar.png";
	public static final String BAR_CRACKS   = "interfaces/knight_bar_cracks.png";

	//hall_walls.png frames
	public static final int WALL_GLASS = 0, WALL_TORCH_OFF = 3, WALL_TORCH = 4, WALL_BANNER = 7, WALL_BANNER_TORN = 8;
}
