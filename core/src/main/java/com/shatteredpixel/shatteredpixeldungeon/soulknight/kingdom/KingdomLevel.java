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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKEvents;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKFloor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKArmory;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.MiniBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.TourneyQuest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.TourneyVisuals;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import java.util.ArrayList;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;

//Knight Kingdom (floors 6-9): castle w/ knights, slimes and court wizards. built like the prison but
//with its own look. no wandmaker here, the herald's tourney takes his spot
public class KingdomLevel extends PrisonLevel {

	{
		color1 = 0x5a5070;
		color2 = 0x9a8fb8;
	}

	@Override
	public String tilesTex() {
		return SKAssets.TILES_KINGDOM;
	}

	//herald's tourney instead of the wandmaker
	@Override
	protected ArrayList<Room> questRooms( ArrayList<Room> rooms ){
		if (!TourneyQuest.READY) return super.questRooms(rooms);
		return TourneyQuest.rooms(rooms);
	}

	@Override
	protected void placeQuests() {
		if (!TourneyQuest.READY) super.placeQuests();
		else TourneyQuest.placeHerald(this);
	}

	@Override
	public void playLevelMusic() {
		if (Statistics.amuletObtained){
			super.playLevelMusic();
			return;
		}
		MiniBoss b = MiniBoss.active();
		if (b != null) b.playMusic();
		else if (TourneyQuest.Q.duel > 0 && TourneyQuest.here()) Music.INSTANCE.play(Assets.Music.PRISON_TENSE, true);
		else super.playLevelMusic();
	}

	//banner patrols, weapon racks, spikes, arrow slits
	@Override
	protected void createMobs() {
		super.createMobs();
		KingdomFeatures.decorate(this, rooms);
	}

	@Override
	public Group addVisuals() {
		Group g = super.addVisuals();
		g.add(new SKFloor.FloorLayer());
		return g;
	}

	@Override
	public Group addWallVisuals() {
		Group g = super.addWallVisuals();
		g.add(new SKFloor.WallLayer());
		if (TourneyQuest.coliseum(this) != null) g.add(new TourneyVisuals());
		return g;
	}

	@Override
	public String waterTex() {
		return SKAssets.WATER_KINGDOM;
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(KingdomLevel.class, "water_name");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(KingdomLevel.class, "region_deco_name");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(KingdomLevel.class, "statue_name");
			default:
				return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.EMPTY_DECO:
				return Messages.get(KingdomLevel.class, "empty_deco_desc");
			case Terrain.WALL_DECO:
				return Messages.get(KingdomLevel.class, "wall_deco_desc");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(KingdomLevel.class, "region_deco_desc");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(KingdomLevel.class, "statue_desc");
			case Terrain.BOOKSHELF:
				return Messages.get(KingdomLevel.class, "bookshelf_desc");
			default:
				return super.tileDesc(tile);
		}
	}

	//armory merchant, 2nd floor of the world
	@Override
	protected void createItems() {
		super.createItems();
		SKArmory.placeShop(this, rooms);
		//same as the forest: ambushes, trap chests, crates, statue
		SKEvents.decorate(this, rooms);
	}
}
