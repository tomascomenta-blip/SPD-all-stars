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

import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.AndroidQuest;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.FigureEightBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.rift.RiftBosses;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKArena;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

/*
 * Starship floor 15: the bridge, now the varkolyn queen's hive. exit locked until she drops the key,
 * floor sealed during the fight.
 * biomass on the floor, her big cocoon on the back wall, a membrane closes the doors while the fight lasts.
 * her walled-up lair has an airlock to space
 */
public class StarshipBossLevel extends StarshipLevel {

	//bridge bounds, saved w/ the floor (cocoon = -1 on floors made before it existed)
	public int brL, brT, brR, brB;
	public int cocoon = -1;
	public int[] doors = new int[0];

	//lair airlock states
	public static final int AIRLOCK_READY = 0, AIRLOCK_ARMED = 1, AIRLOCK_OPEN = 2, AIRLOCK_RECHARGE = 3;
	public int airlock = AIRLOCK_READY;
	public int airlockTimer = 0;

	public static VarkolynLeader queen( Level level ){
		if (level == null) return null;
		for (Mob m : level.mobs){
			if (m instanceof VarkolynLeader) return (VarkolynLeader) m;
		}
		return null;
	}

	public boolean inBridge( int cell ){
		int x = cell % width(), y = cell / width();
		return x > brL && x < brR && y > brT && y < brB;
	}

	public void finalMusic(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.play(Assets.Music.HALLS_BOSS_FINALE, true);
			}
		});
	}

	@Override
	public void playLevelMusic() {
		if (locked){
			VarkolynLeader q = queen(this);
			Music.INSTANCE.play(q != null && q.inLair() ? Assets.Music.HALLS_BOSS_FINALE : Assets.Music.CAVES_BOSS, true);
			return;
		}
		boolean bossAlive = false;
		for (Mob m : mobs){
			if (m instanceof VarkolynLeader) {
				bossAlive = true;
				break;
			}
		}
		if (bossAlive){
			Music.INSTANCE.end();
		} else {
			Music.INSTANCE.playTracks(CavesLevel.CAVES_TRACK_LIST, CavesLevel.CAVES_TRACK_CHANCES, false);
		}
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

		HiveBridge arena = new HiveBridge();
		initRooms.add(arena);
		((FigureEightBuilder)builder).setLandmarkRoom(arena);
		addExtraRooms(initRooms);
		return initRooms;
	}

	//the queen's walled-up lair
	protected void addExtraRooms( ArrayList<Room> rooms ){
		rooms.add(new VarkolynLair());
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
		//boss placed by the hall
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

			//hive membrane closes the bridge doors
			for (int d : doors){
				set( d, Terrain.LOCKED_DOOR );
				GameScene.updateMap( d );
				if (heroFOV[d]) CellEmitter.get(d).burst(Speck.factory(Speck.STEAM), 6);
			}
			VarkolynVisuals.membranes(true);

			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.play(Assets.Music.CAVES_BOSS, true);
				}
			});
		}
		//coop: arena closed, gather the party
		gatherParty();
	}

	@Override
	public void unseal() {
		//coop: not while the Rift boss is up (unseals when it dies)
		if (RiftBosses.keepSealed(this)) return;
		if (locked) {
			super.unseal();

			set( entrance(), Terrain.ENTRANCE );
			GameScene.updateMap( entrance() );
			for (int d : doors){
				set( d, Terrain.DOOR );
				GameScene.updateMap( d );
			}
			VarkolynVisuals.membranes(false);
			afterBoss();

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

	protected void afterBoss(){
	}
	// *** lair airlock ***

	//console got hit: one turn of alarm, then the airlock opens
	public void armAirlock(){
		if (airlock != AIRLOCK_READY) return;
		airlock = AIRLOCK_ARMED;
		airlockTimer = 1;
		Sample.INSTANCE.play(Assets.Sounds.ALERT, 1f, 0.8f);
		GLog.w(Messages.get(VarkolynLeader.class, "airlock_armed"));
		VarkolynHud.banner(Messages.get(VarkolynLeader.class, "banner_airlock"));
		CoopLog.line("airlock armed t=" + (int) Actor.now());
	}

	//one airlock tick, the console calls this
	public void airlockTurn(){
		VarkolynLair lair = VarkolynLair.find();
		if (lair == null || lair.airlockCenter == -1) return;
		switch (airlock){
			case AIRLOCK_ARMED:
				if (--airlockTimer <= 0){
					airlock = AIRLOCK_OPEN;
					airlockTimer = 3;
					Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
					Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 0.5f);
					Game.runOnRenderThread(new Callback() {
						@Override
						public void call() {
							PixelScene.shake(4, 3f);
						}
					});
					GLog.n(Messages.get(VarkolynLeader.class, "airlock_open"));
					CoopLog.line("airlock opens t=" + (int) Actor.now());
					vacuum(lair);
				}
				break;
			case AIRLOCK_OPEN:
				if (--airlockTimer <= 0){
					airlock = AIRLOCK_RECHARGE;
					airlockTimer = AndroidQuest.helps() ? 10 : 18;
					Sample.INSTANCE.play(Assets.Sounds.UNLOCK, 1f, 0.6f);
					GLog.i(Messages.get(VarkolynLeader.class, "airlock_closed"));
				} else {
					vacuum(lair);
				}
				break;
			case AIRLOCK_RECHARGE:
				if (--airlockTimer <= 0){
					airlock = AIRLOCK_READY;
					GLog.i(Messages.get(VarkolynLeader.class, "airlock_ready"));
				}
				break;
		}
	}

	//everything not holding on gets pulled toward the airlock, the brood gets sucked out into space
	private void vacuum( final VarkolynLair lair ){
		final int target = lair.airlockFaceCenter(this);
		ArrayList<Char> pulled = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (!ch.isAlive() || ch instanceof VarkolynLeader || Char.hasProp(ch, Char.Property.IMMOVABLE)) continue;
			if (!lair.inside(cellToPoint(ch.pos))) continue;
			if (ch.alignment == Char.Alignment.ALLY && lair.isHandhold(this, ch.pos)) continue;
			pulled.add(ch);
		}
		//closest first so they make room for the rest, si no se atascan
		Collections.sort(pulled, new Comparator<Char>() {
			@Override
			public int compare( Char a, Char b ) {
				int d = Integer.compare(distance(a.pos, target), distance(b.pos, target));
				return d != 0 ? d : Integer.compare(a.id(), b.id());
			}
		});
		CoopLog.line("airlock pulls " + pulled.size() + " t=" + (int) Actor.now());
		for (Char ch : pulled){
			int steps = ch.alignment == Char.Alignment.ENEMY ? 2 : 1;
			for (int s = 0; s < steps && !lair.isFace(this, ch.pos); s++){
				int best = -1;
				for (int n : PathFinder.NEIGHBOURS8){
					int c = ch.pos + n;
					if (!passable[c] || Actor.findChar(c) != null || !lair.inside(cellToPoint(c))) continue;
					if (distance(c, target) >= distance(ch.pos, target)) continue;
					if (best == -1 || distance(c, target) < distance(best, target)) best = c;
				}
				if (best == -1) break;
				int from = ch.pos;
				ch.pos = best;
				Actor.add(new Pushing(ch, from, best));
				occupyCell(ch);
			}
			if (lair.isFace(this, ch.pos)){
				if (ch.alignment == Char.Alignment.ENEMY){
					//out into space. bye bye
					if (heroFOV[ch.pos]) CellEmitter.get(ch.pos).burst(Speck.factory(Speck.WOOL), 6);
					CoopLog.line("airlock sucks out " + ch.getClass().getSimpleName() + " at " + ch.pos);
					ch.die(this);
				} else {
					ch.damage(Random.NormalIntRange(4, 8), this);
					if (ch == Dungeon.local() || ch instanceof Hero) GLog.n(Messages.get(VarkolynLeader.class, "vacuum_hurt"));
				}
			}
		}
	}

	@Override
	public Group addVisuals() {
		Group g = super.addVisuals();
		if (cocoon != -1) g.add(new VarkolynVisuals.Floor(this));
		return g;
	}

	@Override
	public Group addWallVisuals() {
		Group g = super.addWallVisuals();
		if (cocoon != -1) g.add(new VarkolynVisuals.Above(this));
		return g;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("bridge", new int[]{brL, brT, brR, brB});
		bundle.put("cocoon", cocoon);
		bundle.put("doors", doors);
		bundle.put("airlock", airlock);
		bundle.put("airlock_timer", airlockTimer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (!bundle.contains("cocoon")) return; //floor from before the hive (old save)
		int[] b = bundle.getIntArray("bridge");
		brL = b[0]; brT = b[1]; brR = b[2]; brB = b[3];
		cocoon = bundle.getInt("cocoon");
		doors = bundle.getIntArray("doors");
		airlock = bundle.getInt("airlock");
		airlockTimer = bundle.getInt("airlock_timer");
	}

	//bridge taken over by the hive. doors never open in the cocoon wall
	public static class HiveBridge extends StandardRoom {

		@Override
		public float[] sizeCatProbs() {
			return new float[]{0, 0, 1};
		}

		@Override public int minWidth() { return 13; }
		@Override public int maxWidth() { return 15; }
		@Override public int minHeight() { return 11; }
		@Override public int maxHeight() { return 13; }

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
			Painter.fill( level, this, 2, Terrain.EMPTY_SP );

			ArrayList<Integer> doorCells = new ArrayList<>();
			for (Room.Door door : connected.values()) {
				door.set( Room.Door.Type.REGULAR );
				doorCells.add(level.pointToCell(door));
			}
			Painter.set( level, left, (top + bottom) / 2, Terrain.WALL_DECO );
			Painter.set( level, right, (top + bottom) / 2, Terrain.WALL_DECO );

			int w = level.width();
			int cx = (left + right) / 2;
			int cocoonCell = cx + (top + 1) * w;

			StarshipBossLevel L = level instanceof StarshipBossLevel ? (StarshipBossLevel) level : null;
			if (L != null){
				L.brL = left; L.brT = top; L.brR = right; L.brB = bottom;
				L.cocoon = cocoonCell;
				L.doors = new int[doorCells.size()];
				for (int i = 0; i < L.doors.length; i++) L.doors[i] = doorCells.get(i);

				//biomass around the cocoon + a few patches elsewhere
				Hive hive = Hive.getOrMake(level);
				for (int dy = 0; dy <= 2; dy++){
					for (int dx = -2; dx <= 2; dx++){
						if (Math.abs(dx) + dy > 3) continue;
						int c = cocoonCell + dx + dy * w;
						if (L.inBridge(c)) hive.put(level, c);
					}
				}
				for (int k = 0; k < 2; k++){
					int px = left + 2 + Random.Int(width() - 4);
					int py = top + 4 + Random.Int(Math.max(1, height() - 6));
					for (int n : PathFinder.NEIGHBOURS9){
						int c = px + py * w + n;
						if (L.inBridge(c) && Random.Int(4) > 0) hive.put(level, c);
					}
				}
			}

			//queen sleeps in her cocoon, hanging on the back wall
			VarkolynLeader queen = new VarkolynLeader();
			queen.pos = cocoonCell;
			level.mobs.add( queen );
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
