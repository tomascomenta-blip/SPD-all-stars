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

import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKEvents;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKFloor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKArmory;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.HuntQuest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.MiniBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.HuntVisuals;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WindParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.SewerPainter;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//the Forest (floors 1-4): goblins, boars, man-eating flowers.
//built like the sewers (same rooms, same quests except the Ghost, see below) with its own look
public class ForestLevel extends SewerLevel {

	{
		color1 = 0x3d6b2a;
		color2 = 0x6fa046;
	}

	@Override
	protected Painter painter() {
		//more grass, less water than the sewers. it's a forest pues
		return new SewerPainter()
				.setWater(feeling == Feeling.WATER ? 0.75f : 0.18f, 5)
				.setGrass(feeling == Feeling.GRASS ? 0.80f : 0.30f, 4)
				.setTraps(nTraps(), trapClasses(), trapChances());
	}

	@Override
	public String tilesTex() {
		return SKAssets.TILES_FOREST;
	}

	//boar pairs, goblin camps, totems, brambles, mushrooms
	@Override
	protected void placeQuests() {
		//no ghost in the forest, old hunter instead
		if (HuntQuest.READY) HuntQuest.spawn( this, rooms, roomEntrance );
		else super.placeQuests();
	}

	@Override
	protected void createMobs() {
		super.createMobs();
		ForestFeatures.pairBoars(this);
		ForestFeatures.decorate(this, rooms);
	}

	//standing in tall grass = can't be seen past 2 cells
	@Override
	public void updateFieldOfView( Char c, boolean[] fieldOfView ) {
		super.updateFieldOfView(c, fieldOfView);
		for (int i = 0; i < fieldOfView.length; i++){
			if (fieldOfView[i] && map[i] == Terrain.HIGH_GRASS && i != c.pos && distance(i, c.pos) > 2){
				fieldOfView[i] = false;
			}
		}
	}

	@Override
	public void playLevelMusic() {
		if (Statistics.amuletObtained){
			super.playLevelMusic();
			return;
		}
		//quest mob already fighting keeps its own music
		MiniBoss b = MiniBoss.active();
		if (b != null) b.playMusic();
		else super.playLevelMusic();
	}

	@Override
	public String waterTex() {
		return SKAssets.WATER_FOREST;
	}

	@Override
	public Group addVisuals() {
		//like Level.addVisuals minus the sewer drips. fireflies over the grass instead
		if (visuals == null || visuals.parent == null){
			visuals = new Group();
		} else {
			visuals.clear();
			visuals.camera = null;
		}
		visuals.add(new SKFloor.FloorLayer());
		visuals.add(new HuntVisuals());
		for (int i = 0; i < length(); i++) {
			if (pit[i]) {
				visuals.add( new WindParticle.Wind( i ) );
				if (i >= width() && water[i-width()]) {
					visuals.add( new FlowParticle.Flow( i - width() ) );
				}
			}
			if (map[i] == Terrain.HIGH_GRASS && Random.Visual.Int(6) == 0) {
				visuals.add(new Fireflies(i));
			}
		}
		return visuals;
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(ForestLevel.class, "water_name");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(ForestLevel.class, "region_deco_name");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(ForestLevel.class, "statue_name");
			default:
				return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.EMPTY_DECO:
				return Messages.get(ForestLevel.class, "empty_deco_desc");
			case Terrain.WALL_DECO:
				return Messages.get(ForestLevel.class, "wall_deco_desc");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(ForestLevel.class, "region_deco_desc");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(ForestLevel.class, "statue_desc");
			case Terrain.BOOKSHELF:
				return Messages.get(ForestLevel.class, "bookshelf_desc");
			default:
				return super.tileDesc(tile);
		}
	}

	//a few green lights floating over the tall grass. pretty, does nothing
	public static class Fireflies extends Emitter {

		private final int pos;

		private static final Factory FACTORY = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((Firefly) emitter.recycle(Firefly.class)).reset(x, y);
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public Fireflies( int pos ) {
			super();
			this.pos = pos;
			PointF p = DungeonTilemap.tileCenterToWorld(pos);
			pos(p.x - 6, p.y - 6, 12, 8);
			pour(FACTORY, 1.2f);
		}

		@Override
		public void update() {
			if (visible = (pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos])) {
				super.update();
			}
		}
	}

	public static class Firefly extends PixelParticle {

		public Firefly() {
			super();
			color(0xd8ff7a);
			size(1);
		}

		public void reset( float x, float y ) {
			revive();
			this.x = x;
			this.y = y;
			speed.set(Random.Visual.Float(-3, 3), Random.Visual.Float(-4, -1));
			left = lifespan = Random.Visual.Float(1.2f, 2f);
		}

		@Override
		public void update() {
			super.update();
			float p = left / lifespan;
			am = p > 0.5f ? (1 - p) * 2f : p * 2f;
			if (Game.elapsed > 0) speed.x += Random.Visual.Float(-0.5f, 0.5f);
		}
	}

	//armory weapons merchant, 2nd floor of the world
	@Override
	protected void createItems() {
		super.createItems();
		SKArmory.placeShop(this, rooms);
		//ambush rooms, trapped chests, crates/barrels, the world statue
		SKEvents.decorate(this, rooms);
	}
}
