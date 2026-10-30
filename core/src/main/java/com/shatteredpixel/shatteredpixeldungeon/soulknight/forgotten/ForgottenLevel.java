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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten;

import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.MiniBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.YetiQuest;
import java.util.ArrayList;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKEvents;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKArmory;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.PathFinder;

import java.util.Arrays;

//forgotten land 21-24. lost world under a glacier. 21 all ice, melts as you go down, 24 is full jungle.
//also changes inside each floor: entrance side colder, exit side warmer (warmth()). uses the caves builder
public class ForgottenLevel extends CavesLevel {

	{
		color1 = 0x6a8fa8;
		color2 = 0xd8f0ff;
	}

	@Override
	public void playLevelMusic() {
		if (Statistics.amuletObtained){
			Music.INSTANCE.play(Assets.Music.HALLS_TENSE, true);
		} else if (YetiQuest.READY && MiniBoss.active() != null){
			MiniBoss.active().playMusic();
		} else {
			Music.INSTANCE.playTracks(HallsLevel.HALLS_TRACK_LIST, HallsLevel.HALLS_TRACK_CHANCES, false);
		}
	}

	//0 frozen .. 1 jungle, entrance->exit per floor
	protected float[] warmthRange(){
		switch (Dungeon.depth){
			case 21: default: return new float[]{0.00f, 0.25f};
			case 22: return new float[]{0.15f, 0.45f};
			case 23: return new float[]{0.38f, 0.70f};
			case 24: return new float[]{0.65f, 0.95f};
			case 25: return new float[]{0.10f, 0.10f};
		}
	}

	private float[] warmth = null;

	//by distance to entrance/exit, deterministic. cuidado con meter Random aquí
	public float warmth( int cell ){
		if (warmth == null || warmth.length != length()){
			warmth = new float[length()];
			float[] r = warmthRange();
			boolean[] open = new boolean[length()];
			for (int i = 0; i < length(); i++) open[i] = !solid[i] || passable[i];
			int en = entrance(), ex = exit();
			if (en < 0 || ex < 0){
				Arrays.fill(warmth, (r[0] + r[1]) / 2f);
			} else {
				PathFinder.setMapSize(width(), height());
				PathFinder.buildDistanceMap(en, open);
				int[] dEn = PathFinder.distance.clone();
				PathFinder.buildDistanceMap(ex, open);
				int[] dEx = PathFinder.distance.clone();
				for (int i = 0; i < length(); i++){
					float a = dEn[i] == Integer.MAX_VALUE ? 0 : dEn[i];
					float b = dEx[i] == Integer.MAX_VALUE ? 0 : dEx[i];
					float t = a + b <= 0 ? 0.5f : a / (a + b);
					warmth[i] = r[0] + (r[1] - r[0]) * t;
				}
			}
		}
		if (cell < 0 || cell >= warmth.length) return 0.5f;
		return warmth[cell];
	}

	public boolean cold( int cell ){
		return warmth(cell) < 0.5f;
	}

	//bumps when warmth changes mid-fight (tharog)
	public int visualVersion(){
		return 0;
	}

	public void resetWarmth(){
		warmth = null;
	}

	@Override
	public String tilesTex() {
		if (Dungeon.depth <= 22) return ForgottenAssets.TILES_ICE;
		if (Dungeon.depth == 23) return ForgottenAssets.TILES_THAW;
		return ForgottenAssets.TILES_JUNGLE;
	}

	@Override
	public String waterTex() {
		return Dungeon.depth <= 23 ? ForgottenAssets.WATER_ICE : ForgottenAssets.WATER_JUNGLE;
	}

	//yeti king cave, 22
	@Override
	protected ArrayList<Room> questRooms( ArrayList<Room> rooms ){
		return YetiQuest.rooms(super.questRooms(rooms));
	}

	@Override
	protected void placeQuests() {
		YetiQuest.place(this, roomEntrance);
	}

	@Override
	protected void createMobs() {
		super.createMobs();
		ForgottenFeatures.decorate(this, rooms);
	}

	//merchant on the 2nd floor + the usual per-world systems
	@Override
	protected void createItems() {
		super.createItems();
		SKArmory.placeShop(this, rooms);
		SKEvents.decorate(this, rooms);
	}

	@Override
	public Group addVisuals() {
		Group g = super.addVisuals();
		g.add(new ForgottenVisuals.Floor(this));
		return g;
	}

	@Override
	public Group addWallVisuals() {
		Group g = super.addWallVisuals();
		g.add(new ForgottenVisuals.Walls(this));
		return g;
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell(ch);
		ForgottenFloor.stepped(this, ch);
	}

	private String theme(){
		return Dungeon.depth <= 22 || Dungeon.depth == 25 ? "ice" : Dungeon.depth == 23 ? "thaw" : "jungle";
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(ForgottenLevel.class, "water_name_" + theme());
			case Terrain.GRASS:
			case Terrain.HIGH_GRASS:
			case Terrain.FURROWED_GRASS:
				return Messages.get(ForgottenLevel.class, "grass_name_" + theme());
			default:
				return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(ForgottenLevel.class, "water_desc_" + theme());
			case Terrain.WALL:
			case Terrain.WALL_DECO:
				return Messages.get(ForgottenLevel.class, "wall_desc_" + theme());
			case Terrain.EMPTY_DECO:
				return Messages.get(ForgottenLevel.class, "empty_deco_desc_" + theme());
			default:
				return super.tileDesc(tile);
		}
	}
}
