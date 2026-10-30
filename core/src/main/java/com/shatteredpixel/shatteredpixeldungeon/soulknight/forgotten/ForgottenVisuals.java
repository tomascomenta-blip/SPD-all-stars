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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken.KrakenAssets;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;

import java.util.Random;

//forgotten land visuals. room floors go snow -> mud -> ferns with warmth, thin ice, tar, blizzard snow,
//tharog's shadow in the ice walls (gets closer each floor, creepy), + small fx (icicles, splashes, meteors)
public class ForgottenVisuals {

	static final int S = DungeonTilemap.SIZE;
	private static final Random rnd = new Random();
	private static Floor floor;

	private static void later( final Runnable r ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				r.run();
			}
		});
	}

	public static void splash( final int cell ){
		later(new Runnable() { public void run() {
			if (floor != null) floor.effect(cell, KrakenAssets.SPLASH, 48, 48, 14, 0, 0.7f);
		} });
	}

	public static void icicle( final int cell ){
		later(new Runnable() { public void run() {
			if (floor != null) floor.falling(cell, ForgottenAssets.ICICLE, 16, 32, 120, 0.25f);
		} });
	}

	public static void meteor( final int cell ){
		later(new Runnable() { public void run() {
			if (floor != null){
				floor.falling(cell, ForgottenAssets.METEOR, 24, 48, 260, 0.35f);
				floor.effectLater(cell, ForgottenAssets.CRATER, 48, 32, 10, 0.35f);
			}
			PixelScene.shake(3, 0.4f);
		} });
	}

	public static class Floor extends Group {

		final ForgottenLevel L;
		final Tilemap overlay;
		final int[] data;
		final Group effects = new Group();
		final Group snow = new Group();
		int lastVersion = -1;
		float t = 0, tarT = 0;

		public Floor( ForgottenLevel level ){
			L = level;
			floor = this;
			data = new int[level.length()];
			overlay = new Tilemap(ForgottenAssets.OVERLAYS, new TextureFilm(ForgottenAssets.OVERLAYS, S, S));
			rebuild();
			overlay.map(data, level.width());
			add(overlay);
			add(effects);
			add(snow);
			for (int i = 0; i < 90; i++){
				ColorBlock f = new ColorBlock(1 + rnd.nextInt(2), 1 + rnd.nextInt(2), 0xFFF4FAFF);
				f.visible = false;
				snow.add(f);
			}
		}

		@Override
		public void destroy() {
			super.destroy();
			if (floor == this) floor = null;
		}

		//hash, not Random. both devices have to draw the same floor
		private static float hash( int cell, int salt ){
			int h = cell * 73856093 ^ salt * 19349663;
			h ^= (h >>> 13);
			h *= 0x5bd1e995;
			h ^= (h >>> 15);
			return (h & 0xFFFF) / 65535f;
		}

		void rebuild(){
			ForgottenFloor f = ForgottenFloor.get(L);
			int tarFrame = ((int)(tarT * 3)) % 3;
			for (int i = 0; i < data.length; i++){
				int kind = f == null ? 0 : f.kindAt(i);
				if (kind == ForgottenFloor.THIN_ICE){ data[i] = 12; continue; }
				if (kind == ForgottenFloor.CRACKED){ data[i] = 13; continue; }
				if (kind == ForgottenFloor.TAR){ data[i] = 15 + tarFrame; continue; }
				int tile = L.map[i];
				if (tile != Terrain.EMPTY && tile != Terrain.EMPTY_DECO && tile != Terrain.EMPTY_SP && tile != Terrain.GRASS){
					data[i] = -1;
					continue;
				}
				float w = L.warmth(i);
				float h = hash(i, 1);
				int variant = (int)(hash(i, 2) * 4) % 4;
				if (w < 0.35f && h < 0.25f + (0.35f - w) * 1.4f){
					data[i] = variant;            //snow
				} else if (w >= 0.35f && w < 0.62f && h < 0.22f){
					data[i] = 4 + variant;        //mud
				} else if (w >= 0.55f && h < 0.12f + (w - 0.55f) * 0.9f){
					data[i] = 8 + variant;        //ferns
				} else {
					data[i] = -1;
				}
			}
		}

		void effect( int cell, String asset, int w, int h, float fps, float delay, float scale ){
			Strip s = new Strip(asset, w, h, fps, delay);
			s.origin.set(w / 2f, h);
			s.scale.set(scale);
			s.x = (cell % L.width()) * S + S / 2f - w / 2f;
			s.y = (cell / L.width()) * S + S - h;
			effects.add(s);
		}

		void effectLater( int cell, String asset, int w, int h, float fps, float delay ){
			effect(cell, asset, w, h, fps, delay, 1f);
		}

		//cae del techo
		void falling( final int cell, String asset, int w, int h, float height, final float time ){
			final Image img = new Image(asset);
			final int frames = Math.max(1, (int)(img.texture.width / w));
			img.frame(0, 0, w, h);
			final float tx = (cell % L.width()) * S + S / 2f - w / 2f;
			final float ty = (cell / L.width()) * S + S - h;
			final float fh = height;
			final int fw = w, fH = h;
			Gizmo g = new Gizmo(){
				float tt = 0;
				{
					img.x = tx;
					img.y = ty - fh;
					effects.add(img);
				}
				@Override
				public void update() {
					tt += Game.elapsed;
					float p = Math.min(1, tt / time);
					img.y = ty - fh * (1 - p * p);
					if (p >= 1){
						int f = Math.min(frames - 1, 1 + (int)((tt - time) * 10));
						img.frame(f * fw, 0, fw, fH);
						img.alpha(GameMath.gate(0, 1 - (tt - time) * 2, 1));
						if (tt - time > 0.5f){
							img.killAndErase();
							killAndErase();
						}
					}
				}
			};
			effects.add(g);
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			tarT += e;
			ForgottenFloor f = ForgottenFloor.get(L);
			int v = (f == null ? 0 : f.version) * 3 + ((int)(tarT * 3)) % 3 + L.visualVersion() * 1000;
			if (v != lastVersion){
				lastVersion = v;
				rebuild();
				overlay.updateMap();
			}

			//blizzard, only around the camera
			boolean blizzard = f != null && f.blizzard() && Dungeon.local() != null && L.cold(Dungeon.local().pos);
			Camera cam = Camera.main;
			for (Gizmo g : snow.membersList()){
				if (!(g instanceof ColorBlock)) continue;
				ColorBlock b = (ColorBlock) g;
				if (!blizzard){
					b.visible = false;
					continue;
				}
				float vx = cam.scroll.x, vy = cam.scroll.y, vw = cam.width / cam.zoom * 1f + 40, vh = cam.height / cam.zoom * 1f + 40;
				if (!b.visible || b.x < vx - 20 || b.x > vx + vw || b.y > vy + vh){
					b.visible = true;
					b.x = vx - 20 + rnd.nextFloat() * vw;
					b.y = vy - 20 + rnd.nextFloat() * 20;
				}
				b.x += 90 * e;
				b.y += 70 * e + (b.hashCode() % 30) * e;
				b.alpha(0.8f);
			}
		}
	}

	static class Strip extends Image {
		final int fw, fh, count;
		final float fps;
		float t;
		Strip( String asset, int fw, int fh, float fps, float delay ){
			super(asset);
			this.fw = fw;
			this.fh = fh;
			this.fps = fps;
			this.count = Math.max(1, (int)(texture.width / fw));
			this.t = -delay;
			frame(0, 0, fw, fh);
			visible = delay <= 0;
		}
		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (t < 0) return;
			visible = true;
			int f = (int)(t * fps);
			if (f >= count){
				killAndErase();
				return;
			}
			frame(f * fw, 0, fw, fh);
		}
	}

	//tharog in the ice walls, 22-24

	public static class Walls extends Group {

		public Walls( ForgottenLevel L ){
			if (Dungeon.depth < 22 || Dungeon.depth > 24) return;
			//longest wall w/ floor under it, closest to the middle
			int best = -1, bestLen = 0;
			int w = L.width();
			for (int y = 1; y < L.height() - 2; y++){
				int run = 0;
				for (int x = 1; x < w - 1; x++){
					int c = x + y * w;
					boolean ok = (L.map[c] == Terrain.WALL || L.map[c] == Terrain.WALL_DECO) && L.passable[c + w];
					run = ok ? run + 1 : 0;
					if (run >= 6 && run > bestLen){
						bestLen = run;
						best = c - run / 2;
					}
				}
			}
			if (best == -1) return;
			Image s = new Image(ForgottenAssets.SILHOUETTE);
			float scale = Dungeon.depth == 22 ? 0.55f : Dungeon.depth == 23 ? 0.8f : 1.05f;
			s.scale.set(scale);
			s.origin.set(s.width / 2f, s.height);
			s.x = (best % w) * S + S / 2f - s.width / 2f;
			s.y = (best / w) * S + S - s.height;
			s.alpha(Dungeon.depth == 24 ? 0.55f : 0.4f);
			add(s);
		}
	}
}
