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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;

import java.util.HashMap;

//throne hall drawing: polished floor, red carpet, dais + throne, stained glass light, scorch marks, broken
//columns. on the walls: windows, torches (lit one by one when he wakes, out in act 4), banners, chandeliers
//on their ropes. plus short fx, the horse coming in and falling and a chandelier crashing.
//pictures only, doesn't touch the game
public class ThroneVisuals {

	static final int S = DungeonTilemap.SIZE;

	private static Floor floor;
	private static Above above;
	private static float lightT = -1;      //secs since the knight woke up, -1 before
	static float horseT = 99;              //secs since the horse was called

	private static void later( final Runnable r ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				r.run();
			}
		});
	}

	// *** floor atlas (names from hall_floor.json) ***

	private static HashMap<String, Integer> tiles;

	static int tile( String name ){
		if (tiles == null){
			tiles = new HashMap<>();
			try {
				JsonValue v = new JsonReader().parse(Gdx.files.internal(ThroneAssets.FLOOR.replace(".png", ".json")).readString());
				for (JsonValue e = v.child; e != null; e = e.next) tiles.put(e.name, e.asInt());
			} catch (Exception e){
				ShatteredPixelDungeon.reportException(e);
			}
		}
		Integer i = tiles.get(name);
		return i == null ? -1 : i;
	}

	//hash the cell so the floor looks the same on every device
	private static float hash( int cell, int salt ){
		int h = cell * 73856093 ^ salt * 19349663;
		h ^= (h >>> 13);
		h *= 0x5bd1e995;
		h ^= (h >>> 15);
		return (h & 0xFFFF) / 65535f;
	}

	// short fx, the game calls these

	public static void lightUp(){
		later(new Runnable() { public void run() { lightT = 0; } });
	}

	public static void horseComes( final int cell ){
		later(new Runnable() { public void run() {
			horseT = 0;
			if (floor != null) floor.horseIn(cell);
		} });
	}

	public static boolean horseRunning(){
		return horseT < 0.7f;
	}

	public static void horseFalls( final int cell ){
		later(new Runnable() { public void run() {
			if (floor != null) floor.horseDown(cell);
		} });
	}

	public static void chandelierFalls( final int cell ){
		later(new Runnable() { public void run() {
			if (above != null) above.fall(cell);
		} });
	}

	// *** under the characters ***

	public static class Floor extends Group {

		final KingdomBossLevel L;
		final Tilemap base, light, marks;
		final int[] baseData, lightData, markData;
		final Group things = new Group();
		final Group effects = new Group();
		int lastVersion = -1;
		float t = 0;

		public Floor( KingdomBossLevel level ){
			L = level;
			floor = this;
			lightT = -1;
			horseT = 99;
			int n = level.length();
			baseData = new int[n];
			lightData = new int[n];
			markData = new int[n];
			TextureFilm film = new TextureFilm(ThroneAssets.FLOOR, S, S);
			base = new Tilemap(ThroneAssets.FLOOR, film);
			light = new Tilemap(ThroneAssets.FLOOR, film);
			marks = new Tilemap(ThroneAssets.FLOOR, film);
			paintBase();
			rebuild();
			base.map(baseData, level.width());
			light.map(lightData, level.width());
			marks.map(markData, level.width());
			add(base);
			add(marks);
			add(things);
			add(light);
			add(effects);

			//throne, anchored like a character on its cell
			Image throne = new Image(ThroneAssets.THRONE);
			throne.x = (L.throne % L.width() + 0.5f) * S - throne.width / 2f;
			throne.y = (L.throne / L.width() + 1f) * S - throne.height - S * 6 / 16f;
			add(throne);

			//each chandelier's shadow on the floor = where it'd fall
			for (int c : L.chandeliers){
				ColorBlock sh = new ColorBlock(S * 3 - 8, S * 3 - 8, 0xFF000000);
				sh.x = (c % L.width() - 1) * S + 4;
				sh.y = (c / L.width() - 1) * S + 4;
				sh.alpha(0.16f);
				things.add(sh);
			}
		}

		@Override
		public void destroy() {
			super.destroy();
			if (floor == this) floor = null;
		}

		//slabs all over the hall, carpet, dais
		private void paintBase(){
			int w = L.width();
			int cx = L.carpetX();
			int ty = L.throne / w;
			for (int i = 0; i < baseData.length; i++){
				baseData[i] = -1;
				if (!L.inHall(i) || L.map[i] == Terrain.WALL) continue;
				int x = i % w, y = i / w;
				int slab = (int)(hash(i, 3) * 4) % 4;
				baseData[i] = tile(hash(i, 7) < 0.08f ? "losa_grieta_" + ((int)(hash(i, 9) * 2) % 2) : "losa_" + slab);
				//dais: 2 rows under/behind the throne + the step in front, 5 wide
				if (Math.abs(x - cx) <= 2 && y >= ty - 1 && y <= ty + 1){
					String side = x == cx - 2 ? "izq" : x == cx + 2 ? "der" : "centro";
					baseData[i] = tile((y <= ty ? "tarima_" : "peldano_") + side);
				} else if (Math.abs(x - cx) <= 1 && y > ty + 1){
					String side = x == cx - 1 ? "izq" : x == cx + 1 ? "der" : "centro";
					baseData[i] = tile((y == L.hallB - 1 ? "alfombra_fin_" : "alfombra_") + side);
				}
				if (baseData[i] == -1) baseData[i] = tile("losa_0");
			}
		}

		//the parts that change: window light, scorch marks
		private void rebuild(){
			int w = L.width();
			int cx = L.carpetX();
			for (int i = 0; i < lightData.length; i++){
				lightData[i] = -1;
				markData[i] = -1;
			}
			//light from the windows behind the throne
			String[] glass = {"luz_roja", "luz_azul", "luz_dorada"};
			int k = 0;
			for (int wx : windows(L)){
				String g = glass[k++ % 3];
				for (int y = L.hallT + 1; y <= L.hallT + 3; y++){
					int c = wx + (y - L.hallT - 1) * (wx < cx ? 1 : -1) + y * w;
					if (L.inHall(c)) lightData[c] = tile(g);
				}
			}
			for (int c : L.scorch){
				markData[c] = tile("quemadura_" + ((int)(hash(c, 5) * 2) % 2));
			}
		}

		void horseIn( int cell ){
			int w = L.width();
			//from the nearest side of the hall
			int x = cell % w;
			boolean fromLeft = x - L.hallL < L.hallR - x;
			float sx = ((fromLeft ? L.hallL : L.hallR) + 0.5f) * S - 28;
			float ex = (x + 0.5f) * S - 28;
			float y = (cell / w + 1f) * S - 48 - S * 6 / 16f;
			final Image horse = new Image(ThroneAssets.KNIGHT_HORSE);
			horse.frame(0, 48, 56, 48);
			horse.flipHorizontal = !fromLeft;
			horse.x = sx;
			horse.y = y;
			final float fx = sx, tx = ex;
			effects.add(horse);
			effects.add(new Gizmo(){
				float tt = 0;
				@Override
				public void update() {
					tt += Game.elapsed;
					float p = Math.min(1, tt / 0.7f);
					horse.x = fx + (tx - fx) * p;
					horse.frame(56 * (((int)(tt * 12)) % 4), 48, 56, 48);
					if (p >= 1){
						horse.killAndErase();
						killAndErase();
					}
				}
			});
		}

		void horseDown( int cell ){
			int w = L.width();
			final Image horse = new Image(ThroneAssets.KNIGHT_HORSE);
			horse.frame(56 * 10, 0, 56, 48);
			horse.x = (cell % w + 0.5f) * S - 28;
			horse.y = (cell / w + 1f) * S - 48 - S * 6 / 16f;
			effects.add(horse);
			effects.add(new Gizmo(){
				float tt = 0;
				@Override
				public void update() {
					tt += Game.elapsed;
					horse.alpha(GameMath.gate(0, 1 - (tt - 3f) / 1.5f, 1));
					if (tt > 4.5f){
						horse.killAndErase();
						killAndErase();
					}
				}
			});
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			if (lightT >= 0) lightT += e;
			horseT += e;
			if (L.visualVersion != lastVersion){
				lastVersion = L.visualVersion;
				rebuild();
				light.updateMap();
				marks.updateMap();
				refreshThings();
			}
			light.alpha(darkness() > 0 ? 0.55f + 0.25f * (float) Math.sin(t * 1.3f) : 0.85f + 0.15f * (float) Math.sin(t * 0.9f));
		}

		//broken columns + fallen chandeliers, the mess stays
		private final HashMap<Integer, Image> debris = new HashMap<>();

		private void refreshThings(){
			int w = L.width();
			for (int c : L.rubble){
				if (debris.containsKey(c)) continue;
				Image r = new Image(ThroneAssets.COLUMN);
				r.frame(32, 0, 16, 32);
				r.x = (c % w) * S;
				r.y = (c / w + 1) * S - 32;
				things.add(r);
				debris.put(c, r);
			}
			for (int i = 0; i < L.chandeliers.length; i++){
				int c = L.chandeliers[i];
				if (!L.chandelierDown[i] || debris.containsKey(-1 - c)) continue;
				Image r = new Image(ThroneAssets.CHANDELIER);
				r.frame(32 * 4, 0, 32, 32);
				r.x = (c % w + 0.5f) * S - 16;
				r.y = (c / w + 1) * S - 32;
				things.add(r);
				debris.put(-1 - c, r);
			}
		}
	}

	//windows on the wall behind the throne (cells of that wall)
	static int[] windows( KingdomBossLevel L ){
		int cx = L.carpetX();
		int half = (L.hallR - L.hallL) / 2;
		if (half >= 8) return new int[]{cx - 7, cx - 5, cx + 5, cx + 7};
		return new int[]{cx - 5, cx + 5};
	}

	//0 while the hall's lit, up to 1 when the shadow comes (act 4)
	static float darkness(){
		DarkGrandKnight k = Dungeon.level == null ? null : KingdomBossLevel.knight(Dungeon.level);
		return k != null && k.act >= 4 && k.isAlive() ? 1f : 0f;
	}

	// *** above walls + chars ***

	public static class Above extends Group {

		final KingdomBossLevel L;
		final Group effects = new Group();
		final Image[] torches;
		final float[] torchDelay;
		final Image[] lamps;
		final ColorBlock[] ropes;
		float t = 0;

		public Above( KingdomBossLevel level ){
			L = level;
			above = this;
			int w = L.width();
			int cx = L.carpetX();
			int wallY = L.hallT;

			//windows + banners behind the throne
			int k = 0;
			for (int wx : windows(L)){
				Image g = wallImage(ThroneAssets.WALL_GLASS + (k++ % 3), wx, wallY);
				add(g);
			}
			add(wallImage(ThroneAssets.WALL_BANNER, cx - 2, wallY));
			add(wallImage(ThroneAssets.WALL_BANNER_TORN, cx + 2, wallY));

			//torches, lit from the ends of the wall towrds the throne
			int[] tx = {cx - 6, cx - 4, cx + 4, cx + 6};
			torches = new Image[tx.length];
			torchDelay = new float[tx.length];
			for (int i = 0; i < tx.length; i++){
				torches[i] = wallImage(ThroneAssets.WALL_TORCH_OFF, tx[i], wallY);
				torchDelay[i] = 0.2f + (6 - Math.abs(tx[i] - cx)) * 0.35f;
				add(torches[i]);
			}

			//chandeliers + ropes
			lamps = new Image[L.chandeliers.length];
			ropes = new ColorBlock[L.chandeliers.length];
			for (int i = 0; i < lamps.length; i++){
				int c = L.chandeliers[i];
				lamps[i] = new Image(ThroneAssets.CHANDELIER);
				lamps[i].frame(0, 0, 32, 32);
				lamps[i].x = (c % w + 0.5f) * S - 16;
				lamps[i].y = (c / w + 1) * S - 10 - 32;
				ropes[i] = new ColorBlock(1, 1, 0xFF6A5236);
				int cleat = cleatOf(i);
				if (cleat != -1){
					boolean rightWall = L.solid[cleat + 1];
					float x0 = (cleat % w + 0.5f) * S + (rightWall ? 3 : -3), y0 = (cleat / w) * S + 4;
					float x1 = lamps[i].x + 16, y1 = lamps[i].y + 2;
					float len = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
					ropes[i].size(len, 1);
					ropes[i].x = x0;
					ropes[i].y = y0;
					ropes[i].origin.set(0, 0.5f);
					ropes[i].angle = (float) Math.toDegrees(Math.atan2(y1 - y0, x1 - x0));
					add(ropes[i]);
				}
				add(lamps[i]);
			}
			add(effects);
		}

		private int cleatOf( int i ){
			for (Mob m : L.mobs){
				if (m instanceof ThroneProps.RopeCleat && ((ThroneProps.RopeCleat) m).chandelier == i) return m.pos;
			}
			return -1;
		}

		private Image wallImage( int frame, int x, int y ){
			Image img = new Image(ThroneAssets.WALLS);
			img.frame(frame * 16, 0, 16, 32);
			img.x = x * S;
			img.y = (y + 1) * S - 32;
			return img;
		}

		@Override
		public void destroy() {
			super.destroy();
			if (above == this) above = null;
		}

		void fall( int cell ){
			int w = L.width();
			for (int i = 0; i < L.chandeliers.length; i++){
				if (L.chandeliers[i] != cell) continue;
				lamps[i].visible = false;
				ropes[i].visible = false;
			}
			final Image img = new Image(ThroneAssets.CHANDELIER);
			img.frame(32 * 3, 0, 32, 32);
			final float x = (cell % w + 0.5f) * S - 16;
			final float y0 = (cell / w + 1) * S - 10 - 32 + 5, y1 = (cell / w + 1) * S - 32;
			img.x = x;
			img.y = y0;
			effects.add(img);
			effects.add(new Gizmo(){
				float tt = 0;
				@Override
				public void update() {
					tt += Game.elapsed;
					float p = Math.min(1, tt / 0.3f);
					img.y = y0 + (y1 - y0) * p * p;
					if (p >= 1){
						img.killAndErase();
						killAndErase();
					}
				}
			});
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			DarkGrandKnight k = KingdomBossLevel.knight(L);
			boolean awake = k == null || k.awake();
			//hall saved after he woke up: lit right away
			float lt = lightT >= 0 ? lightT : (awake ? 99 : -1);
			boolean dark = darkness() > 0;
			for (int i = 0; i < torches.length; i++){
				boolean on = !dark && lt >= torchDelay[i];
				int f = on ? ThroneAssets.WALL_TORCH + ((int)(t * 8 + i)) % 3 : ThroneAssets.WALL_TORCH_OFF;
				torches[i].frame(f * 16, 0, 16, 32);
			}
			for (int i = 0; i < lamps.length; i++){
				boolean down = i < L.chandelierDown.length && L.chandelierDown[i];
				lamps[i].visible = !down;
				ropes[i].visible = !down && ropes[i].width > 1;
				if (!down) lamps[i].frame(32 * (((int)(t * 6 + i)) % 3), 0, 32, 32);
			}
		}
	}

	//textures used here, loaded before the hall shows up
	public static void preload(){
		for (String s : new String[]{ThroneAssets.FLOOR, ThroneAssets.THRONE, ThroneAssets.WALLS,
				ThroneAssets.CHANDELIER, ThroneAssets.COLUMN, ThroneAssets.KNIGHT_HORSE}){
			TextureCache.get(s);
		}
	}
}
