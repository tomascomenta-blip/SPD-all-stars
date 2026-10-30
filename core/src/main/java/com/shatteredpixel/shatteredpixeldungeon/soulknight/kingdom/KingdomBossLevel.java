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
import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.FigureEightBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.rift.RiftBosses;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKArena;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Knight Kingdom floor 10, the dark grand knight's throne hall.
//long hall, red carpet from the door to the throne, 4 columns, braziers, shield walls, powder barrels and
//2 chandeliers on ropes tied to the walls. doors slam when he wakes up, open again when he dies.
//exit locked until he drops the key
public class KingdomBossLevel extends KingdomLevel {

	//hall bounds, saved w/ the floor (don't need the rooms to redraw it)
	public int hallL, hallT, hallR, hallB;
	public int throne = -1;
	public int[] doors = new int[0];
	public int[] chandeliers = new int[0];
	public boolean[] chandelierDown = new boolean[0];
	public int[] barrelSpots = new int[0];
	public final ArrayList<Integer> rubble = new ArrayList<>();
	public final ArrayList<Integer> scorch = new ArrayList<>();
	public int visualVersion = 0;

	public static DarkGrandKnight knight( Level level ){
		if (level == null) return null;
		for (Mob m : level.mobs){
			if (m instanceof DarkGrandKnight) return (DarkGrandKnight) m;
		}
		return null;
	}

	public boolean inHall( int cell ){
		int x = cell % width(), y = cell / width();
		return x > hallL && x < hallR && y > hallT && y < hallB;
	}

	public int carpetX(){
		return throne % width();
	}

	@Override
	public void playLevelMusic() {
		if (locked){
			DarkGrandKnight k = knight(this);
			Music.INSTANCE.play(k != null && k.act >= 4 ? Assets.Music.CITY_BOSS_FINALE : Assets.Music.CITY_BOSS, true);
			return;
		}
		if (knight(this) != null){
			Music.INSTANCE.end();
		} else {
			Music.INSTANCE.playTracks(PrisonLevel.PRISON_TRACK_LIST, PrisonLevel.PRISON_TRACK_CHANCES, false);
		}
	}

	public void finalMusic(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.play(Assets.Music.CITY_BOSS_FINALE, true);
			}
		});
	}

	@Override
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> initRooms = new ArrayList<>();

		initRooms.add( roomEntrance = new EntranceRoom() );
		initRooms.add( roomExit = new SKArena.LockedExit() );

		int standards = standardRooms(true);
		for (int i = 0; i < standards; i++) {
			StandardRoom s = StandardRoom.createRoom();
			s.setSizeCat(0, 0);
			initRooms.add(s);
		}

		ThroneHall hall = new ThroneHall();
		initRooms.add(hall);
		((FigureEightBuilder)builder).setLandmarkRoom(hall);
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
	protected int nTraps() {
		return 0;
	}

	@Override
	protected void createMobs() {
		//knight + hall props get placed by the hall
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

			//slam the hall doors. BOOM
			for (int d : doors){
				set( d, Terrain.LOCKED_DOOR );
				GameScene.updateMap( d );
				if (heroFOV[d]) CellEmitter.get(d).burst(Speck.factory(Speck.DUST), 6);
			}

			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.play(Assets.Music.CITY_BOSS, true);
				}
			});
		}
		//coop: hall's closed now, everyone comes in
		gatherParty();
	}

	@Override
	public void unseal() {
		//coop again: stay sealed while the Rift boss lives, it unseals when it dies
		if (RiftBosses.keepSealed(this)) return;
		if (locked) {
			super.unseal();

			set( entrance(), Terrain.ENTRANCE );
			GameScene.updateMap( entrance() );
			for (int d : doors){
				set( d, Terrain.DOOR );
				GameScene.updateMap( d );
			}

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

	// --- hall events

	//a column broke. the knight did it, not me
	public void rubble( int cell ){
		if (!rubble.contains(cell)) rubble.add(cell);
		visualVersion++;
	}

	//chandelier rope cut: it falls on the 3x3 under it
	public void dropChandelier( int i ){
		if (i < 0 || i >= chandeliers.length || chandelierDown[i]) return;
		chandelierDown[i] = true;
		visualVersion++;
		int c = chandeliers[i];
		CoopLog.line("chandelier falls at " + c);
		ThroneVisuals.chandelierFalls(c);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 1.2f);
		Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.8f);
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				PixelScene.shake(4, 0.5f);
			}
		});
		for (int n : PathFinder.NEIGHBOURS9){
			int cell = c + n;
			if (solid[cell]) continue;
			Char ch = Actor.findChar(cell);
			if (ch == null || !ch.isAlive()) continue;
			if (ch instanceof DarkGrandKnight){
				((DarkGrandKnight) ch).crushed();
			} else if (ch.alignment == Char.Alignment.ALLY){
				ch.damage(Math.max(1, Random.NormalIntRange(10, 16) - ch.drRoll() / 2), this);
			}
		}
	}

	//powder barrels respawn in place every act
	public void refillBarrels(){
		for (int c : barrelSpots){
			if (Actor.findChar(c) != null || !passable[c]) continue;
			SKProps.ExplosiveBarrel b = new SKProps.ExplosiveBarrel();
			b.pos = c;
			GameScene.add(b);
			if (heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 4);
		}
	}

	//scorch marks from the barrels
	public void scorch( int cell ){
		if (inHall(cell) && !scorch.contains(cell)){
			scorch.add(cell);
			visualVersion++;
		}
	}

	public void knightFreed( int cell ){
		visualVersion++;
	}

	@Override
	public Group addVisuals() {
		Group g = super.addVisuals();
		if (throne != -1) g.add(new ThroneVisuals.Floor(this));
		return g;
	}

	@Override
	public Group addWallVisuals() {
		Group g = super.addWallVisuals();
		if (throne != -1) g.add(new ThroneVisuals.Above(this));
		return g;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("hall", new int[]{hallL, hallT, hallR, hallB});
		bundle.put("throne", throne);
		bundle.put("doors", doors);
		bundle.put("chandeliers", chandeliers);
		bundle.put("chandelier_down", chandelierDown);
		bundle.put("barrel_spots", barrelSpots);
		int[] r = new int[rubble.size()];
		for (int i = 0; i < r.length; i++) r[i] = rubble.get(i);
		bundle.put("rubble", r);
		int[] s = new int[scorch.size()];
		for (int i = 0; i < s.length; i++) s[i] = scorch.get(i);
		bundle.put("scorch", s);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (!bundle.contains("throne")) return; //old save, floor from before the throne hall
		int[] h = bundle.getIntArray("hall");
		hallL = h[0]; hallT = h[1]; hallR = h[2]; hallB = h[3];
		throne = bundle.getInt("throne");
		doors = bundle.getIntArray("doors");
		chandeliers = bundle.getIntArray("chandeliers");
		chandelierDown = bundle.getBooleanArray("chandelier_down");
		barrelSpots = bundle.getIntArray("barrel_spots");
		rubble.clear();
		for (int c : bundle.getIntArray("rubble")) rubble.add(c);
		scorch.clear();
		for (int c : bundle.getIntArray("scorch")) scorch.add(c);
	}

	//throne hall. doors never open in the wall behind the throne
	public static class ThroneHall extends StandardRoom {

		@Override
		public float[] sizeCatProbs() {
			return new float[]{0, 0, 1};
		}

		@Override public int minWidth() { return 15; }
		@Override public int maxWidth() { return 17; }
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
			Painter.fill( level, this, 1, Terrain.EMPTY );
			ArrayList<Integer> doorCells = new ArrayList<>();
			for (Room.Door door : connected.values()) {
				door.set( Room.Door.Type.REGULAR );
				doorCells.add(level.pointToCell(door));
			}

			int w = level.width();
			int cx = (left + right) / 2;
			int cy = (top + bottom) / 2;
			int throneCell = cx + (top + 2) * w;

			//carpet + dais are only drawn (ThroneVisuals), the floor itself is plain
			Painter.fill( level, cx - 1, top + 4, 3, bottom - top - 4, Terrain.EMPTY_SP );

			KingdomBossLevel L = level instanceof KingdomBossLevel ? (KingdomBossLevel) level : null;
			if (L != null){
				L.hallL = left; L.hallT = top; L.hallR = right; L.hallB = bottom;
				L.throne = throneCell;
				L.doors = new int[doorCells.size()];
				for (int i = 0; i < L.doors.length; i++) L.doors[i] = doorCells.get(i);
			}

			//4 columns along the carpet
			int[][] columns = {{cx - 4, top + 4}, {cx + 4, top + 4}, {cx - 4, bottom - 4}, {cx + 4, bottom - 4}};
			for (int[] p : columns) add(level, new ThroneProps.Column(), p[0], p[1], doorCells);

			//braziers by the dais and in the corners near the doors
			int[][] braziers = {{cx - 3, top + 1}, {cx + 3, top + 1}, {left + 1, bottom - 1}, {right - 1, bottom - 1}};
			for (int[] p : braziers) add(level, new ThroneProps.Brazier(), p[0], p[1], doorCells);

			//shield walls
			int[][] walls = {{left + 2, cy}, {right - 2, cy}, {cx - 3, bottom - 2}, {cx + 3, bottom - 2}};
			for (int[] p : walls) add(level, new ThroneProps.ShieldWall(), p[0], p[1], doorCells);

			//2 chandeliers over the carpet, ropes tied to the side walls
			int[][] lamps = {{cx, top + 5, left + 1}, {cx, bottom - 3, right - 1}};
			ArrayList<Integer> lampCells = new ArrayList<>();
			for (int[] p : lamps){
				int row = p[1];
				ThroneProps.RopeCleat cleat = new ThroneProps.RopeCleat();
				//not in front of a door
				int cell = -1;
				for (int dy : new int[]{0, 1, -1, 2, -2}){
					int c = p[2] + (row + dy) * w;
					if (level.findMob(c) == null && !nearDoor(level, c, doorCells)){
						cell = c;
						break;
					}
				}
				if (cell == -1) continue;
				cleat.chandelier = lampCells.size();
				cleat.pos = cell;
				level.mobs.add(cleat);
				lampCells.add(p[0] + row * w);
			}
			if (L != null){
				L.chandeliers = new int[lampCells.size()];
				for (int i = 0; i < L.chandeliers.length; i++) L.chandeliers[i] = lampCells.get(i);
				L.chandelierDown = new boolean[L.chandeliers.length];
			}

			//powder barrels right where his charges go. evil, I know
			int[][] barrels = {{cx - 2, cy + 1}, {cx + 2, top + 4}, {cx + 5, bottom - 2}, {cx - 5, top + 3}};
			ArrayList<Integer> spots = new ArrayList<>();
			for (int[] p : barrels){
				int c = p[0] + p[1] * w;
				if (add(level, new SKProps.ExplosiveBarrel(), p[0], p[1], doorCells)) spots.add(c);
			}
			if (L != null){
				L.barrelSpots = new int[spots.size()];
				for (int i = 0; i < L.barrelSpots.length; i++) L.barrelSpots[i] = spots.get(i);
			}

			DarkGrandKnight k = new DarkGrandKnight();
			k.pos = throneCell;
			level.mobs.add(k);
		}

		private static boolean nearDoor( Level level, int cell, ArrayList<Integer> doorCells ){
			for (int d : doorCells){
				if (level.distance(cell, d) <= 1) return true;
			}
			return false;
		}

		private boolean add( Level level, Mob m, int x, int y, ArrayList<Integer> doorCells ){
			int cell = x + y * level.width();
			if (!inside(new Point(x, y)) || x <= left || x >= right || y <= top || y >= bottom) return false;
			if (level.findMob(cell) != null || nearDoor(level, cell, doorCells)) return false;
			m.pos = cell;
			level.mobs.add(m);
			return true;
		}

		@Override
		public boolean canPlaceWater( Point p ) {
			return false;
		}

		@Override
		public boolean canPlaceTrap( Point p ) {
			return false;
		}

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
