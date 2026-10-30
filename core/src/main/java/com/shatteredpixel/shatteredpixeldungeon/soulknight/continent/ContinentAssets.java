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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent;

//every mob sheet is one row: 0-1 idle, 2-5 run, 6-8 attack, 9-11 die, then its own extras from 12
public class ContinentAssets {

	public static final String MESSAGES = "messages/soulknight/continent";
	//one per biome of the open world, so each can grow on its own
	public static final String MESSAGES_FLESH = "messages/soulknight/continent_flesh";
	public static final String MESSAGES_CITY = "messages/soulknight/continent_city";
	public static final String MESSAGES_FROST = "messages/soulknight/continent_frost";
	public static final String MESSAGES_BONES = "messages/soulknight/continent_bones";
	public static final String MESSAGES_ISLES = "messages/soulknight/continent_isles";

	//ash plains
	public static final String TILES_ASH   = "environment/tiles_ash.png";   //tiles_caves layout
	public static final String WATER_ASH   = "environment/water_ash.png";   //black lava, 32x32

	//the surface of the ash plains (the open world): 16x8 tiles, then the big pieces laid on it
	public static final String SURFACE_ASH       = "environment/surface/surface_ash.png";
	public static final String SURFACE_SKULL     = "environment/surface/piezas/boca_cueva.png";  //6x5 cells
	public static final String SURFACE_CHASM     = "environment/surface/piezas/sima.png";        //8x4, on high ash
	public static final String SURFACE_CHASM_LOW = "environment/surface/piezas/sima_baja.png";   //8x4, on low ash
	public static final String SURFACE_CLIFF     = "environment/surface/piezas/acantilado.png";  //6x4, ladder on column 4
	public static final String SURFACE_RIBS_LOW  = "environment/surface/hitos/costillar_bajo.png";//14x8, under the heroes
	public static final String SURFACE_RIBS_HIGH = "environment/surface/hitos/costillar_alto.png";//14x8, over them
	public static final String SURFACE_TOWER     = "environment/surface/hitos/torre_cuerpo.png"; //5x10
	public static final String SURFACE_TOWER_FIRE= "environment/surface/hitos/torre_fuego.png";  //4 frames of 80x48
	public static final String SURFACE_TREE      = "environment/surface/hitos/arbol.png";        //8x10
	public static final String SURFACE_CAMP      = "environment/surface/hitos/campamento.png";   //8x6

	//the other biomes keep their sheet and their pieces in a folder each, surface/<biome>/
	public static String surface( String biome ){
		return piece(biome, "surface_" + biome);
	}

	public static String piece( String biome, String name ){
		return "environment/surface/" + biome + "/" + name + ".png";
	}

	//yogs (every zone)
	public static final String YOG_EYE     = "sprites/continent/yog_eye.png";     //16x16, 12-13 charging the ray
	public static final String YOG_LARVA   = "sprites/continent/yog_larva.png";   //12x10
	public static final String FIST_BURNING= "sprites/continent/fist_burning.png";//20x20
	public static final String YOG_TONGUE  = "sprites/continent/yog_tongue.png";  //16x24, 12 = only the puddle

	//ash creatures
	public static final String ASH_WALKER  = "sprites/continent/ash_walker.png";  //12x16, 12-13 heap of ash (down / getting up)
	public static final String SALAMANDER  = "sprites/continent/salamander.png";  //20x12, 12-13 under the lava
	public static final String VULTURE     = "sprites/continent/vulture.png";     //20x16, 12-13 diving
	public static final String COLOSSUS    = "sprites/continent/colossus.png";    //24x24, 12-14 shell cracking, molten

	//the walking pyre, guardian of the plains
	public static final String PYRE        = "sprites/continent/pyre.png";        //48x48, 12-13 dimmed, 14-15 relighting, 16-17 ash storm
	public static final String BRAZIER     = "sprites/continent/brazier.png";     //16x24, 0-1 lit, 2 knocked over

	//shrine of light, every zone (tinted per zone if needed)
	public static final String SHRINE      = "sprites/continent/shrine.png";      //32x32, 0 besieged, 1-2 charging, 3 pure

	public static final String HUD         = "interfaces/continent_hud.png";
}
