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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale;

import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenAssets;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.RectF;

import java.util.Arrays;
import java.util.Random;

//what you see of him and of the floor turning into each world. all cosmetic: reads the level
//and the director, never changes them
public class ThalVisuals {

	private static final int S = 16;

	//frame sizes, see the art's LEEME
	public static final int BODY_W = 320, BODY_H = 192, BODY_FRAMES = 4;
	public static final int EYE_W = 32, EYE_H = 20;
	//eye centre inside a body frame
	public static final int EYE_X = 158, EYE_Y = 48;
	//everything under this row of the body hides behind the arena's edge
	public static final int BODY_CUT = 150;
	public static final int PUDDLE_FRAMES = 4;
	public static final int BUBBLE = 80;

	private static String tilesOf( int world ){
		switch (world){
			case SKWorlds.FOREST:    return SKAssets.TILES_FOREST;
			case SKWorlds.KINGDOM:   return SKAssets.TILES_KINGDOM;
			case SKWorlds.STARSHIP:  return SKAssets.TILES_STARSHIP;
			case SKWorlds.UNDERSEA:  return SKAssets.TILES_UNDERSEA;
			default:                 return ForgottenAssets.TILES_JUNGLE;
		}
	}

	//same answer on every device, no Random
	private static float hash( int cell, int salt ){
		int h = cell * 73856093 ^ salt * 19349663;
		h ^= (h >>> 13);
		h *= 0x5bd1e995;
		h ^= (h >>> 15);
		return (h & 0xFFFF) / 65535f;
	}

	public static class Low extends Group {

		final RiftLevel L;
		final Tilemap[] worlds = new Tilemap[6];
		final int[][] data = new int[6][];
		final int[] owner;
		final Tilemap puddles;
		final int[] puddleData;
		final Group bubbles = new Group();
		final Group stars = new Group();
		Image sky, body, eye;
		TextureFilm bodyFilm, eyeFilm, deadFilm;
		float deadT = -1;
		int shownAct = -1;
		float t = 0, actT = 0, puddleT = 0, eyeT = 0;
		String bubbleKey = "";
		final Random rnd = new Random();

		public Low( RiftLevel level ){
			L = level;
			int len = level.length();
			owner = new int[len];

			//the sky: whole top band, then a dark veil on every other hole around the oval
			try {
				sky = new Image(FinaleAssets.SKY);
				sky.scale.set((RiftLevel.W * S) / (float) sky.width, (11 * S) / (float) sky.height);
				add(sky);
			} catch (Exception e){
				sky = null;
			}
			add(stars);
			for (int i = 0; i < 60; i++){
				ColorBlock st = new ColorBlock(1, 1, 0xFFEFF6FF);
				st.x = rnd.nextFloat() * RiftLevel.W * S;
				st.y = rnd.nextFloat() * 10 * S;
				stars.add(st);
			}

			try {
				body = new Image(FinaleAssets.BODY);
				bodyFilm = new TextureFilm(body.texture, BODY_W, BODY_H);
				bodyFrame(0);
				add(body);
				eye = new Image(FinaleAssets.EYE);
				eyeFilm = new TextureFilm(eye.texture, EYE_W, EYE_H);
				eye.frame(eyeFilm.get(2));
				eye.x = body.x + EYE_X - EYE_W/2f;
				eye.y = body.y + EYE_Y - EYE_H/2f;
				add(eye);
			} catch (Exception e){
				body = null;
				eye = null;
			}

			for (int w = 1; w <= 5; w++){
				data[w] = new int[len];
				Arrays.fill(data[w], -1);
				String tx = tilesOf(w);
				worlds[w] = new Tilemap(tx, new TextureFilm(tx, S, S));
				worlds[w].map(data[w], RiftLevel.W);
				add(worlds[w]);
			}

			puddleData = new int[len];
			Arrays.fill(puddleData, -1);
			puddles = new Tilemap(FinaleAssets.PUDDLE, new TextureFilm(FinaleAssets.PUDDLE, S, S));
			puddles.map(puddleData, RiftLevel.W);
			add(puddles);
			add(bubbles);
		}

		//which world a floor cell belongs to in this act
		private int worldFor( int cell, int act ){
			if (act >= 1 && act <= 5) return act;
			if (act == 6) return 1 + (int)(hash(cell, 7) * 4.999f);
			return 0;
		}

		private int tileFor( int cell ){
			switch (L.map[cell]){
				case Terrain.EMPTY_SP: return DungeonTileSheet.FLOOR_SP;
				case Terrain.GRASS:    return DungeonTileSheet.GRASS;
				default:               return DungeonTileSheet.FLOOR;
			}
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;

			//the floor turns into the act's world, spreading out from the portals
			if (L.act != shownAct){
				shownAct = L.act;
				actT = 0;
			}
			actT += e;
			float radius = actT * 9f;
			boolean changed = false;
			int[] portals = RiftLevel.portalCells();
			for (int i = 0; i < owner.length; i++){
				if (!L.floor(i)) continue;
				int want = L.act >= 7 ? 0 : worldFor(i, L.act);
				if (owner[i] == want) continue;
				int d = 99;
				for (int p : portals) d = Math.min(d, L.distance(i, p));
				if (d > radius && want != 0) continue;
				if (owner[i] != 0) data[owner[i]][i] = -1;
				owner[i] = want;
				if (want != 0) data[want][i] = tileFor(i);
				changed = true;
			}
			if (changed){
				for (int w = 1; w <= 5; w++) worlds[w].updateMap();
			}

			puddleT += e;
			if (puddleT > 0.25f){
				puddleT = 0;
				boolean pc = false;
				for (int i = 0; i < puddleData.length; i++){
					int want = L.puddle(i) ? (int)(hash(i, 3) * (PUDDLE_FRAMES - 0.001f)) : -1;
					if (puddleData[i] != want){
						puddleData[i] = want;
						pc = true;
					}
				}
				if (pc) puddles.updateMap();
				refreshBubbles();
			}
			for (int i = 0; i < bubbles.length; i++){
				if (bubbles.membersList().get(i) instanceof Image){
					((Image) bubbles.membersList().get(i)).alpha(0.45f + 0.15f * (float) Math.sin(t * 3 + i));
				}
			}

			for (int i = 0; i < stars.length; i++){
				if (stars.membersList().get(i) instanceof ColorBlock){
					((ColorBlock) stars.membersList().get(i)).alpha(0.4f + 0.6f * (float) Math.abs(Math.sin(t * 1.3f + i * 1.7f)));
				}
			}

			if (body != null){
				if (L.act >= 7){
					//melts down behind the edge, once
					if (deadT < 0){
						deadT = 0;
						body.texture(FinaleAssets.BODY_DEAD);
						deadFilm = new TextureFilm(body.texture, BODY_W, BODY_H);
					}
					deadT += e;
					bodyFrame(Math.min(5, (int)(deadT * 1.5f)));
				} else {
					bodyFrame(((int)(t * 3)) % BODY_FRAMES);
				}
			}
			if (eye != null && body != null){
				eye.x = body.x + EYE_X - EYE_W/2f;
				eye.y = body.y + EYE_Y - EYE_H/2f;
				eye.alpha(body.alpha());
				eyeT += e;
				int ef;
				if (L.act == 0) ef = 2;               //closed until the fight starts
				else if (L.act >= 7) ef = -1;
				else if (eyeT % 5f < 0.15f) ef = 1;  //blink
				else ef = L.act >= 6 ? 5 : 0;        //glares in the last act
				eye.visible = ef >= 0;
				if (ef >= 0) eye.frame(eyeFilm.get(ef));
			}
		}

		//cropped at the arena's edge, he's too big to fit anyway
		private void bodyFrame( int f ){
			RectF r = (deadFilm != null ? deadFilm : bodyFilm).get(f);
			body.frame(new RectF(r.left, r.top, r.right, r.top + (r.bottom - r.top) * BODY_CUT / BODY_H));
			body.x = RiftLevel.CX * S + S/2f - BODY_W/2f + (float) Math.sin(t * 0.7f) * 3;
			body.y = 11 * S - BODY_CUT;
		}

		private void refreshBubbles(){
			StringBuilder key = new StringBuilder();
			for (int[] b : L.bubbles) key.append(b[0]).append(',').append(b[3]).append(';');
			if (key.toString().equals(bubbleKey)) return;
			bubbleKey = key.toString();
			bubbles.clear();
			for (int[] b : L.bubbles){
				Image img = new Image(b[3] == 0 ? FinaleAssets.BUBBLE_SLOW : FinaleAssets.BUBBLE_FAST);
				int x = b[0] % RiftLevel.W, y = b[0] / RiftLevel.W;
				img.x = x * S + S/2f - img.width/2f;
				img.y = y * S + S/2f - img.height/2f;
				bubbles.add(img);
			}
		}
	}

	//over the walls/mobs: drips falling from the sky
	public static class High extends Group {

		final RiftLevel L;
		final Random rnd = new Random();
		final Group drips = new Group();
		float t = 0;

		public High( RiftLevel level ){
			L = level;
			add(drips);
			for (int i = 0; i < 14; i++){
				ColorBlock d = new ColorBlock(1, 3, 0xFFD8B860);
				d.x = rnd.nextFloat() * RiftLevel.W * S;
				d.y = rnd.nextFloat() * 10 * S;
				drips.add(d);
			}
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			for (int i = 0; i < drips.length; i++){
				if (!(drips.membersList().get(i) instanceof ColorBlock)) continue;
				ColorBlock d = (ColorBlock) drips.membersList().get(i);
				d.visible = L.act >= 1 && L.act < 7;
				d.y += (30 + (i % 5) * 8) * e;
				if (d.y > 12 * S){
					d.y = 3 * S + rnd.nextFloat() * 4 * S;
					d.x = (RiftLevel.CX - 9) * S + rnd.nextFloat() * 18 * S;
				}
			}
		}
	}
}
