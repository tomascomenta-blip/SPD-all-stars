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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea;

import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKEvents;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKFloor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKArmory;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.LabQuest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.MiniBoss;
import com.watabou.noosa.Group;
import java.util.ArrayList;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;

//Undersea (floors 16-19): sunken base taken over by the abyss and its experiments.
//built like the dwarven city w/ its own look. the Imp doesn't ask for help here, the flooded lab does (his shop is still on 20)
public class UnderseaLevel extends CityLevel {

	{
		color1 = 0x1f3d5a;
		color2 = 0x5fb0d0;
	}

	@Override
	public String tilesTex() {
		return SKAssets.TILES_UNDERSEA;
	}

	//flooded lab instead of the imp
	@Override
	protected ArrayList<Room> questRooms( ArrayList<Room> rooms ){
		if (!LabQuest.READY) return super.questRooms(rooms);
		return LabQuest.rooms(rooms);
	}

	@Override
	protected void placeQuests() {
		if (LabQuest.READY) LabQuest.place(this);
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

	//bubble columns (oxygen), clams, ink geysers
	@Override
	protected void createMobs() {
		super.createMobs();
		UnderseaFeatures.decorate(this, rooms);
	}

	@Override
	public Group addVisuals() {
		Group g = super.addVisuals();
		g.add(new SKFloor.FloorLayer());
		return g;
	}

	//heroes + allies can't see through the ink, sea mobs can
	@Override
	public void updateFieldOfView( Char c, boolean[] fieldOfView ) {
		Blob ink = blobs.get(UnderseaFeatures.InkCloud.class);
		if (ink == null || ink.volume <= 0 || c.alignment != Char.Alignment.ALLY){
			super.updateFieldOfView(c, fieldOfView);
			return;
		}
		boolean[] saved = losBlocking.clone();
		for (int i = 0; i < losBlocking.length; i++){
			if (ink.cur[i] > 0) losBlocking[i] = true;
		}
		try {
			super.updateFieldOfView(c, fieldOfView);
		} finally {
			System.arraycopy(saved, 0, losBlocking, 0, saved.length);
		}
		for (int i = 0; i < fieldOfView.length; i++){
			if (fieldOfView[i] && ink.cur[i] > 0 && distance(i, c.pos) > 1) fieldOfView[i] = false;
		}
	}

	@Override
	public String waterTex() {
		return SKAssets.WATER_UNDERSEA;
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(UnderseaLevel.class, "water_name");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(UnderseaLevel.class, "region_deco_name");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(UnderseaLevel.class, "statue_name");
			default:
				return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.EMPTY_DECO:
				return Messages.get(UnderseaLevel.class, "empty_deco_desc");
			case Terrain.WALL_DECO:
				return Messages.get(UnderseaLevel.class, "wall_deco_desc");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(UnderseaLevel.class, "region_deco_desc");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(UnderseaLevel.class, "statue_desc");
			case Terrain.BOOKSHELF:
				return Messages.get(UnderseaLevel.class, "bookshelf_desc");
			default:
				return super.tileDesc(tile);
		}
	}

	//armory merchant, 2nd floor of the world. same as everywhere
	@Override
	protected void createItems() {
		super.createItems();
		SKArmory.placeShop(this, rooms);
		//ambushes, trap chests, crates, statue. lo de siempre
		SKEvents.decorate(this, rooms);
	}
}
