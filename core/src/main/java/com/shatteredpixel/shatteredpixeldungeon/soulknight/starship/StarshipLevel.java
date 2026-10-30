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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKEvents;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKFloor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKArmory;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.AndroidQuest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.MiniBoss;
import com.watabou.noosa.Group;
import java.util.ArrayList;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;

//Starship (floors 11-14), wrecked alien ship full of varkolyns.
//built like the caves w/ its own look. no blacksmith here, M-4 takes his spot
public class StarshipLevel extends CavesLevel {

	{
		color1 = 0x2f4a5c;
		color2 = 0x6fd0e8;
	}

	@Override
	public String tilesTex() {
		return SKAssets.TILES_STARSHIP;
	}

	//M-4 instead of the blacksmith
	@Override
	protected ArrayList<Room> questRooms( ArrayList<Room> rooms ){
		if (!AndroidQuest.READY) return super.questRooms(rooms);
		return AndroidQuest.rooms(rooms);
	}

	@Override
	protected void placeQuests() {
		AndroidQuest.place(this);
	}

	@Override
	public void playLevelMusic() {
		if (Statistics.amuletObtained){
			super.playLevelMusic();
			return;
		}
		MiniBoss b = MiniBoss.active();
		if (b != null) b.playMusic();
		else super.playLevelMusic();
	}

	//teleporter, varkolyn nest, laser fences + console, blackouts
	@Override
	protected void createMobs() {
		super.createMobs();
		StarshipFeatures.decorate(this, rooms);
	}

	@Override
	public Group addVisuals() {
		Group g = super.addVisuals();
		g.add(new SKFloor.FloorLayer());
		return g;
	}

	@Override
	public String waterTex() {
		return SKAssets.WATER_STARSHIP;
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(StarshipLevel.class, "water_name");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(StarshipLevel.class, "region_deco_name");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(StarshipLevel.class, "statue_name");
			default:
				return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.EMPTY_DECO:
				return Messages.get(StarshipLevel.class, "empty_deco_desc");
			case Terrain.WALL_DECO:
				return Messages.get(StarshipLevel.class, "wall_deco_desc");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(StarshipLevel.class, "region_deco_desc");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(StarshipLevel.class, "statue_desc");
			case Terrain.BOOKSHELF:
				return Messages.get(StarshipLevel.class, "bookshelf_desc");
			default:
				return super.tileDesc(tile);
		}
	}

	//armory merchant again, 2nd floor of the world
	@Override
	protected void createItems() {
		super.createItems();
		SKArmory.placeShop(this, rooms);
		//ambush rooms, trap chests, crates & barrels, world statue
		SKEvents.decorate(this, rooms);
	}
}
