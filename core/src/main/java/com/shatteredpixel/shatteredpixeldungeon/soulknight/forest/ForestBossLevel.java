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

import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.HuntQuest;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.FigureEightBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.SewerPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss.SewerBossEntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.exit.ExitRoom;
import com.shatteredpixel.shatteredpixeldungeon.rift.RiftBosses;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

/*
 * Forest floor 5, the goblin high priest's clearing. like Goo's floor: exit locked until the boss drops
 * the key, floor sealed during the fight.
 * ritual clearing = big bonfire in the middle w/ the priest in front, 4 sacred stones, grass everywhere.
 * roots close the doors while the fight lasts
 */
public class ForestBossLevel extends ForestLevel {

	//clearing bounds, saved w/ the floor (bonfire = -1 on floors made before it existed, partidas viejas)
	public int clrL, clrT, clrR, clrB;
	public int bonfire = -1;
	public int[] doors = new int[0];
	public int[] stoneCells = new int[0];

	public static GoblinPriest priest( Level level ){
		if (level == null) return null;
		for (Mob m : level.mobs){
			if (m instanceof GoblinPriest) return (GoblinPriest) m;
		}
		return null;
	}

	public boolean inClearing( int cell ){
		int x = cell % width(), y = cell / width();
		return x > clrL && x < clrR && y > clrT && y < clrB;
	}

	public int center(){
		return bonfire;
	}

	public boolean nearDoor( int cell ){
		for (int d : doors) if (distance(cell, d) <= 1) return true;
		return false;
	}

	public ArrayList<PriestProps.SacredStone> stones(){
		ArrayList<PriestProps.SacredStone> out = new ArrayList<>();
		for (Mob m : mobs) if (m instanceof PriestProps.SacredStone) out.add((PriestProps.SacredStone) m);
		return out;
	}

	//next to a sacred stone = the giant star can't get you
	public boolean safeFromStar( int cell ){
		for (int s : stoneCells){
			if (s != cell && distance(cell, s) <= 1) return true;
		}
		return false;
	}

	public void bonfire( int look ){
		for (Mob m : mobs){
			if (m instanceof PriestProps.Bonfire) ((PriestProps.Bonfire) m).look(look);
		}
	}

	public void finalMusic(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.play(Assets.Music.CAVES_BOSS_FINALE, true);
			}
		});
	}

	@Override
	public void playLevelMusic() {
		if (locked){
			GoblinPriest p = priest(this);
			Music.INSTANCE.play(p != null && p.act >= 3 ? Assets.Music.CAVES_BOSS_FINALE : Assets.Music.SEWERS_BOSS, true);
			return;
		}
		boolean bossAlive = false;
		for (Mob m : mobs){
			if (m instanceof GoblinPriest) {
				bossAlive = true;
				break;
			}
		}
		if (bossAlive){
			Music.INSTANCE.end();
		} else {
			Music.INSTANCE.playTracks(SewerLevel.SEWER_TRACK_LIST, SewerLevel.SEWER_TRACK_CHANCES, false);
		}
	}

	@Override
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> initRooms = new ArrayList<>();

		initRooms.add( roomEntrance = new SewerBossEntranceRoom() );
		initRooms.add( roomExit = new ExitClearing() );

		int standards = standardRooms(true);
		for (int i = 0; i < standards; i++) {
			StandardRoom s = StandardRoom.createRoom();
			s.setSizeCat(0, 0);
			initRooms.add(s);
		}

		PriestClearing boss = new PriestClearing();
		initRooms.add(boss);
		((FigureEightBuilder)builder).setLandmarkRoom(boss);
		return initRooms;
	}

	@Override
	protected int standardRooms( boolean forceMax ) {
		if (forceMax) return 3;
		return 2+Random.chances(new float[]{1, 1});
	}

	@Override
	protected Builder builder(){
		return new FigureEightBuilder()
				.setLoopShape( 2 , Random.Float(0.3f, 0.8f), 0f)
				.setPathLength(1f, new float[]{1})
				.setTunnelLength(new float[]{1, 2}, new float[]{1});
	}

	@Override
	protected Painter painter() {
		return new SewerPainter()
				.setWater(0.20f, 5)
				.setGrass(0.35f, 4)
				.setTraps(0, trapClasses(), trapChances());
	}

	@Override
	protected void createMobs() {
		//boss gets placed by the clearing, not here
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
		Random.pushGenerator(Random.Long());
			ArrayList<Item> bonesItems = Bones.get();
			if (bonesItems != null) {
				int pos;
				do {
					pos = pointToCell(roomEntrance.random());
				} while (pos == entrance() || solid[pos]);
				for (Item i : bonesItems) {
					drop(i, pos).setHauntedIfCursed().type = Heap.Type.REMAINS;
				}
			}
		Random.popGenerator();
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (Point p : roomEntrance.getPoints()){
			int cell = pointToCell(p);
			if (passable[cell]
					&& roomEntrance.inside(p)
					&& Actor.findChar(cell) == null
					&& (!Char.hasProp(ch, Char.Property.LARGE) || openSpace[cell])){
				candidates.add(cell);
			}
		}
		if (candidates.isEmpty()){
			return -1;
		} else {
			return Random.element(candidates);
		}
	}

	@Override
	public void seal() {
		if (!locked) {
			super.seal();
			Statistics.qualifiedForBossChallengeBadge = true;

			set( entrance(), Terrain.EMPTY_SP );
			GameScene.updateMap( entrance() );

			//roots close the clearing doors
			for (int d : doors){
				set( d, Terrain.LOCKED_DOOR );
				GameScene.updateMap( d );
				if (heroFOV[d]) CellEmitter.get(d).burst(LeafParticle.GENERAL, 8);
			}
			PriestVisuals.rootsGrow();

			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.play(Assets.Music.SEWERS_BOSS, true);
				}
			});
		}
		//coop: arena's closed now, pull the whole party in
		gatherParty();
	}

	@Override
	public void unseal() {
		//coop: not while the Rift boss is still alive (it unseals when it dies)
		if (RiftBosses.keepSealed(this)) return;
		if (locked) {
			super.unseal();

			set( entrance(), Terrain.ENTRANCE );
			GameScene.updateMap( entrance() );
			for (int d : doors){
				set( d, Terrain.DOOR );
				GameScene.updateMap( d );
			}
			PriestVisuals.rootsWither();

			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.fadeOut(5f, new Callback() {
						@Override
						public void call() {
							Music.INSTANCE.end();
						}
					});
				}
			});
		}
	}

	//exit, locked till the boss is dead
	public static class ExitClearing extends ExitRoom {

		@Override
		public int minWidth() {
			return Math.max(super.minWidth(), 7);
		}

		@Override
		public int minHeight() {
			return Math.max(super.minHeight(), 7);
		}

		@Override
		public void paint( Level level ) {
			Painter.fill( level, this, Terrain.WALL );
			Painter.fill( level, this, 1, Terrain.EMPTY );
			for (Room.Door door : connected.values()) {
				door.set( Room.Door.Type.REGULAR );
			}
			Point c = center();
			Painter.fill( level, c.x-1, c.y-1, 3, 3, Terrain.EMPTY_SP );
			int exitCell = level.pointToCell(c);
			Painter.set( level, exitCell, Terrain.LOCKED_EXIT );
			level.transitions.add(new LevelTransition(level, exitCell, LevelTransition.Type.REGULAR_EXIT));
		}
	}

	@Override
	public Group addVisuals() {
		Group g = super.addVisuals();
		if (bonfire != -1) g.add(new PriestVisuals.Floor(this));
		return g;
	}

	@Override
	public Group addWallVisuals() {
		Group g = super.addWallVisuals();
		if (bonfire != -1) g.add(new PriestVisuals.Above(this));
		return g;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("clearing", new int[]{clrL, clrT, clrR, clrB});
		bundle.put("bonfire", bonfire);
		bundle.put("doors", doors);
		bundle.put("stones", stoneCells);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (!bundle.contains("bonfire")) return; //floor from before the ritual clearing existed
		int[] c = bundle.getIntArray("clearing");
		clrL = c[0]; clrT = c[1]; clrR = c[2]; clrB = c[3];
		bonfire = bundle.getInt("bonfire");
		doors = bundle.getIntArray("doors");
		stoneCells = bundle.getIntArray("stones");
	}

	//the ritual clearing. doors never open in the wall behind the bonfire
	public static class PriestClearing extends StandardRoom {

		@Override
		public float[] sizeCatProbs() {
			return new float[]{0, 0, 1};
		}

		@Override public int minWidth() { return 13; }
		@Override public int maxWidth() { return 15; }
		@Override public int minHeight() { return 13; }
		@Override public int maxHeight() { return 15; }

		@Override
		public int maxConnections( int direction ) {
			if (direction == TOP) return 0;
			return super.maxConnections(direction);
		}

		@Override
		public boolean canConnect( Point p ) {
			return super.canConnect(p) && p.y != top;
		}

		@Override
		public boolean canMerge( Level l, Room other, Point p, int mergeTerrain ) {
			return false;
		}

		@Override
		public void paint( Level level ) {
			Painter.fill( level, this, Terrain.WALL );
			Painter.fill( level, this, 1, Terrain.GRASS );
			Painter.fill( level, this, 2, Terrain.EMPTY );

			ArrayList<Integer> doorCells = new ArrayList<>();
			for (Room.Door door : connected.values()) {
				door.set( Room.Door.Type.REGULAR );
				doorCells.add(level.pointToCell(door));
			}

			int w = level.width();
			Point c = center();
			int cx = c.x, cy = c.y;

			//torches
			Painter.set( level, left, cy, Terrain.WALL_DECO );
			Painter.set( level, right, cy, Terrain.WALL_DECO );

			ForestBossLevel L = level instanceof ForestBossLevel ? (ForestBossLevel) level : null;

			//bonfire in the middle, priest in front of it w/ his back to the entrance
			int fire = cx + (cy - 1) * w;
			PriestProps.Bonfire b = new PriestProps.Bonfire();
			b.pos = fire;
			level.mobs.add(b);

			//4 sacred stones
			int[][] stones = {{cx - 4, cy - 3}, {cx + 4, cy - 3}, {cx - 4, cy + 3}, {cx + 4, cy + 3}};
			ArrayList<Integer> stoneCells = new ArrayList<>();
			for (int[] p : stones){
				int cell = p[0] + p[1] * w;
				if (p[0] <= left + 1 || p[0] >= right - 1 || p[1] <= top + 1 || p[1] >= bottom - 1) continue;
				if (nearDoor(level, cell, doorCells) || level.findMob(cell) != null) continue;
				PriestProps.SacredStone s = new PriestProps.SacredStone();
				s.pos = cell;
				level.mobs.add(s);
				stoneCells.add(cell);
			}

			if (L != null){
				L.clrL = left; L.clrT = top; L.clrR = right; L.clrB = bottom;
				L.bonfire = fire;
				L.doors = new int[doorCells.size()];
				for (int i = 0; i < L.doors.length; i++) L.doors[i] = doorCells.get(i);
				L.stoneCells = new int[stoneCells.size()];
				for (int i = 0; i < L.stoneCells.length; i++) L.stoneCells[i] = stoneCells.get(i);
			}

			GoblinPriest boss = new GoblinPriest();
			boss.pos = cx + cy * w;
			level.mobs.add( boss );
			HuntQuest.placeHelper(level, L == null ? null : L.doors, left, top, right, bottom);
		}

		private static boolean nearDoor( Level level, int cell, ArrayList<Integer> doorCells ){
			for (int d : doorCells){
				if (level.distance(cell, d) <= 2) return true;
			}
			return false;
		}

		@Override
		public boolean canPlaceWater( Point p ) {
			return false;
		}

		@Override
		public boolean canPlaceTrap( Point p ) {
			return false;
		}

		//tall grass would hide the bullets, so nope
		@Override
		public boolean canPlaceGrass( Point p ) {
			return false;
		}

		@Override
		public boolean canPlaceItem( Point p, Level l ) {
			return false;
		}

		@Override
		public boolean canPlaceCharacter( Point p, Level l ) {
			return false;
		}
	}
}
