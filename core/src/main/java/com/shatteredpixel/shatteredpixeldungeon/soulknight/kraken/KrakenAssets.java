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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

//kraken pngs
public class KrakenAssets {
	public static final String WATER_STORM = "environment/water_storm.png";

	private static final String D = "sprites/kraken/";
	public static final String SHIP        = D + "ship.png";
	public static final String MAST        = D + "mast.png";
	public static final String MAST_FIRE   = D + "mast_fire.png";
	public static final String MAST_FALLEN = D + "mast_fallen.png";
	public static final String HELM        = D + "helm.png";
	public static final String CANNON      = D + "cannon.png";
	public static final String HOLES       = D + "holes.png";
	public static final String WAVE        = D + "wave.png";
	public static final String SPLASH      = D + "splash.png";
	public static final String GRAB        = D + "tentacle_grab.png";
	public static final String SUCKER      = D + "sucker_glow.png";
	public static final String GUARD       = D + "guard_tentacle.png";
	public static final String HEAD        = D + "kraken_head.png";
	public static final String WHIRLPOOL   = D + "whirlpool.png";
	public static final String SHORE       = D + "shore.png";
	public static final String EYE_CLOSEUP = D + "eye_closeup.png";
	public static final String GRUMETE     = D + "grumete.png";

	public static String rise( int type ){
		return D + "tentacle_rise_" + type + ".png";
	}

	public static String lie( int type ){
		return D + "tentacle_lie_" + type + ".png";
	}

	private static final String I = "interfaces/";
	public static final String BAR_KRAKEN = I + "kraken_bar.png";
	public static final String BAR_SHIP   = I + "ship_bar.png";
	public static final String BAR_ROUTE  = I + "route_bar.png";
	public static final String ROUTE_SHIP = I + "route_ship.png";
}
