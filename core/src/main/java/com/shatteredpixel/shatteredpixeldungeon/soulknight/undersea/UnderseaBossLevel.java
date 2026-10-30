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

import com.shatteredpixel.shatteredpixeldungeon.rift.RiftBosses;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.LabQuest;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.ImpShopRoom;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.FigureEightBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKArena;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Undersea floor 20, the abyssal sub's dock. exit locked until it drops the key, sealed during the fight.
//keeps the Imp's shop like the Dwarf King floor, opens after the boss if the Imp's quest (or the lab one) was done
public class UnderseaBossLevel extends UnderseaLevel {

	@Override
	public void playLevelMusic() {
		if (locked){
			Music.INSTANCE.play(Assets.Music.CITY_BOSS, true);
			return;
		}
		boolean bossAlive = false;
		for (Mob m : mobs){
			if (m instanceof AbyssalSubmariner) {
				bossAlive = true;
				break;
			}
		}
		if (bossAlive){
			Music.INSTANCE.end();
		} else {
			Music.INSTANCE.playTracks(CityLevel.CITY_TRACK_LIST, CityLevel.CITY_TRACK_CHANCES, false);
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

		SKArena arena = new SKArena(SKArena.FLOODED, AbyssalSubmariner.class);
		initRooms.add(arena);
		((FigureEightBuilder)builder).setLandmarkRoom(arena);
		addExtraRooms(initRooms);
		return initRooms;
	}

	private SKImpShop impShop;

	protected void addExtraRooms( ArrayList<Room> rooms ){
		rooms.add( impShop = new SKImpShop() );
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
		//boss gets placed by the hall
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

			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.play(Assets.Music.CITY_BOSS, true);
				}
			});
		}
		//coop: arena's closed, everyone in
		gatherParty();
	}

	@Override
	public void unseal() {
		//coop: keep it sealed while the Rift boss lives
		if (RiftBosses.keepSealed(this)) return;
		if (locked) {
			super.unseal();

			set( entrance(), Terrain.ENTRANCE );
			GameScene.updateMap( entrance() );
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
		if (impShop != null && (Imp.Quest.earnedShop() || LabQuest.Q.done) && !impShop.shopSpawned()){
			impShop.spawnShop(this);
			Dungeon.observe();
		}
	}

	private static final String IMP_SHOP = "imp_shop";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(IMP_SHOP, impShop);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		impShop = (SKImpShop) bundle.get(IMP_SHOP);
		//quest might've been finished after the boss died, check again
		if (impShop != null && !locked && (Imp.Quest.earnedShop() || LabQuest.Q.done) && !impShop.shopSpawned()){
			boolean bossAlive = false;
			for (Mob m : mobs) if (m instanceof AbyssalSubmariner) bossAlive = true;
			if (!bossAlive) impShop.spawnShop(this);
		}
	}

	//Imp's shop, painted as a plain room until it opens
	public static class SKImpShop extends ImpShopRoom {
		@Override
		public void paint( Level level ) {
			Painter.fill( level, this, Terrain.WALL );
			Painter.fill( level, this, 1, Terrain.EMPTY_SP );
			Painter.set( level, center(), Terrain.PEDESTAL );
			for (Room.Door door : connected.values()) {
				door.set( Room.Door.Type.REGULAR );
			}
			super.paint(level);
		}
	}
}
