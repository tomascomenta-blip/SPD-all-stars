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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.FigureEightBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKArena;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

//floor 25: frozen hall, tharog sleeping in a block of ice. shaman at the entrance w/ the legend.
//amulet exit locked till tharog drops the key. hall temperature follows the fight (Tharog has the details)
public class ForgottenBossLevel extends ForgottenLevel {

	@Override
	public void playLevelMusic() {
		if (locked){
			Music.INSTANCE.play(Assets.Music.HALLS_BOSS, true);
			return;
		}
		boolean bossAlive = false;
		for (Mob m : mobs){
			if (m instanceof Tharog) {
				bossAlive = true;
				break;
			}
		}
		if (bossAlive){
			Music.INSTANCE.end();
		} else {
			super.playLevelMusic();
		}
	}

	@Override
	public String tilesTex() {
		return ForgottenAssets.TILES_ICE;
	}

	@Override
	public String waterTex() {
		return ForgottenAssets.WATER_ICE;
	}

	@Override
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> initRooms = new ArrayList<>();
		initRooms.add( roomEntrance = new ShamanEntrance() );
		initRooms.add( roomExit = new SKArena.LockedExit() );
		for (int i = 0; i < 2; i++) {
			StandardRoom s = StandardRoom.createRoom();
			s.setSizeCat(0, 0);
			initRooms.add(s);
		}
		TharogArena arena = new TharogArena();
		initRooms.add(arena);
		((FigureEightBuilder)builder).setLandmarkRoom(arena);
		return initRooms;
	}

	@Override
	protected int standardRooms( boolean forceMax ) {
		return 2;
	}

	@Override
	protected int specialRooms( boolean forceMax ) {
		return 0;
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
		//the rooms place everything themselves
		ForgottenFloor.getOrMake(this);
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
	}

	//same as the demon halls: going up with the amulet starts the ascent
	@Override
	public boolean activateTransition(Hero hero, LevelTransition transition) {
		if (transition.type == LevelTransition.Type.REGULAR_ENTRANCE
				&& hero.belongings.getItem(Amulet.class) != null
				&& hero.buff(AscensionChallenge.class) == null) {

			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndOptions( new ItemSprite(ItemSpriteSheet.AMULET),
							Messages.get(Amulet.class, "ascent_title"),
							Messages.get(Amulet.class, "ascent_desc"),
							Messages.get(Amulet.class, "ascent_yes"),
							Messages.get(Amulet.class, "ascent_no")){
						@Override
						protected void onSelect(int index) {
							if (index == 0){
								Buff.affect(hero, AscensionChallenge.class);
								Statistics.highestAscent = 25;
								ForgottenBossLevel.super.activateTransition(hero, transition);
							}
						}
					} );
				}
			});
			return false;

		} else {
			return super.activateTransition(hero, transition);
		}
	}

	//hall warmth

	public static Tharog tharog( Level level ){
		for (Mob m : level.mobs){
			if (m instanceof Tharog) return (Tharog) m;
		}
		return null;
	}

	@Override
	public float warmth( int cell ) {
		Tharog t = tharog(this);
		if (t == null){
			return locked ? 0.1f : 0.8f;
		}
		return t.warmthAt(cell);
	}

	@Override
	public boolean cold( int cell ) {
		Tharog t = tharog(this);
		return t != null && t.freezing(cell);
	}

	@Override
	public int visualVersion() {
		Tharog t = tharog(this);
		return t == null ? (locked ? 1 : 2) : t.arenaVersion;
	}

	//entrance + shaman
	public static class ShamanEntrance extends EntranceRoom {
		@Override
		public void paint( Level level ) {
			super.paint(level);
			Point c = center();
			int cell = level.pointToCell(c);
			for (int n : PathFinder.NEIGHBOURS8){
				int k = cell + n;
				if (level.map[k] == Terrain.EMPTY && level.findMob(k) == null){
					Tribe.Shaman s = new Tribe.Shaman();
					s.pos = k;
					level.mobs.add(s);
					break;
				}
			}
		}
	}

	//the arena. wide, ice pillars, 4 braziers
	public static class TharogArena extends StandardRoom {

		@Override
		public float[] sizeCatProbs() {
			return new float[]{0, 0, 1};
		}

		@Override
		public boolean canMerge( Level l, Room other, Point p, int mergeTerrain ) {
			return false;
		}

		@Override
		public void paint( Level level ) {
			Painter.fill( level, this, Terrain.WALL );
			Painter.fill( level, this, 1, Terrain.EMPTY );
			for (Room.Door door : connected.values()) {
				door.set( Room.Door.Type.REGULAR );
			}
			Point c = center();
			int w = width(), h = height();
			//2x3 pillars
			int[] xs = {left + w / 4, c.x, right - w / 4};
			int[] ys = {top + h / 3 + 1, bottom - h / 3 - 1};
			for (int y : ys){
				for (int x : xs){
					if (x == c.x && y == ys[0]) continue; //except where tharog sits
					add(level, new Tharog.IcePillar(), level.pointToCell(new Point(x, y)));
				}
			}
			//corner braziers
			int[][] corners = {{left + 2, top + 2}, {right - 2, top + 2}, {left + 2, bottom - 2}, {right - 2, bottom - 2}};
			for (int[] k : corners){
				add(level, new ForgottenFeatures.Brazier(), level.pointToCell(new Point(k[0], k[1])));
			}
			Tharog t = new Tharog();
			t.pos = level.pointToCell(new Point(c.x, top + 3));
			level.mobs.add(t);
		}

		private void add( Level level, Mob m, int cell ){
			if (level.findMob(cell) != null) return;
			m.pos = cell;
			level.mobs.add(m);
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
	}
}
