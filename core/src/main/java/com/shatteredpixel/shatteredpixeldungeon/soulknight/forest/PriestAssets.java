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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forest;

//priest clearing pics
public class PriestAssets {

	private static final String D = "sprites/priest/";

	public static final String RITE         = D + "priest_rite.png";     //20x20, 11 frames. thin priest extra poses
	public static final String BRUTE        = D + "priest_brute.png";    //32x32, 3 rows of 11
	public static final String TOTEM        = D + "ritual_totem.png";    //20x32, 4 rows of 8
	public static final String BONFIRE      = D + "bonfire.png";         //32x48, 9
	public static final String STONE        = D + "sacred_stone.png";    //16x32, 3
	public static final String STAR         = D + "giant_star.png";      //48x48, 6
	public static final String ROOTS        = D + "roots.png";           //16x32, 6
	public static final String PORTABLE     = D + "portable_totem.png";  //16x24, 5

	public static final String BAR          = "interfaces/priest_bar.png";       //208x40
	public static final String BAR_STAR     = "interfaces/priest_bar_star.png";  //40x40, 3
	public static final String ICONS        = "interfaces/priest_icons.png";     //12x16, 6

	//rows of ritual_totem.png
	public static final int TOTEM_FLOOR = 0, TOTEM_FIRE = 1, TOTEM_IVY = 2, TOTEM_SPIRIT = 3;

	//priest_icons.png frames
	public static final int ICON_FLOOR = 0, ICON_FLOOR_BROKEN = 1, ICON_FIRE = 2, ICON_IVY = 3, ICON_SPIRIT = 4, ICON_BROKEN = 5;
}
