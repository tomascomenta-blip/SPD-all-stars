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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.FaunaTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.BigTest;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

import java.util.ArrayList;
import java.util.Arrays;

//testing only: -Dspd.test.colpreview=<dir> starts a solo run on an empty plain and lays out the
//drawings listed in <dir>/preview.txt, so the art can be judged in the real game next to the hero.
//then it walks the camera through the "shot" lines and saves each view as <dir>/shot_N.png
//
//  size W H                   plain size in cells
//  hero X Y                   where the hero stands
//  bg FILE PX PY              picture under everything, top left at pixel PX,PY of the map
//  img FILE X Y OX OY         picture whose pixel OX,OY sits on the top left corner of cell X,Y
//  shot X Y ZOOM              camera centred on cell X,Y with that zoom
//  noui                       hides the interface for the shots
//  dark                       keeps the dark (by default the whole plain is lit)
public class ColossusPreview {

	public static boolean on(){
		return DevFlags.colPreview() != null;
	}

	private static ArrayList<String[]> lines(){
		ArrayList<String[]> out = new ArrayList<>();
		String all = Gdx.files.absolute(DevFlags.colPreview() + "/preview.txt").readString("UTF-8");
		for (String l : all.split("\n")){
			l = l.trim();
			if (l.isEmpty() || l.startsWith("#")) continue;
			out.add(l.split("\\s+"));
		}
		return out;
	}

	private static Image picture( String file ){
		Pixmap p = new Pixmap(Gdx.files.absolute(DevFlags.colPreview() + "/" + file));
		return new Image(TextureCache.get(p));
	}

	public static class PreviewLevel extends Level {

		{
			color1 = 0x5a3020;
			color2 = 0x2a1814;
		}

		@Override
		protected boolean build() {
			int w = 60, h = 40, hx = 10, hy = 10;
			for (String[] l : lines()){
				if (l[0].equals("size")){
					w = Integer.parseInt(l[1]);
					h = Integer.parseInt(l[2]);
				} else if (l[0].equals("hero")){
					hx = Integer.parseInt(l[1]);
					hy = Integer.parseInt(l[2]);
				}
			}
			setSize(w, h);
			Arrays.fill(map, Terrain.WALL);
			for (int y = 2; y < h - 2; y++){
				for (int x = 2; x < w - 2; x++){
					map[x + y * w] = Terrain.EMPTY;
				}
			}
			int entrance = hx + hy * w;
			map[entrance] = Terrain.ENTRANCE;
			transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.REGULAR_ENTRANCE));
			return true;
		}

		//the flesh and the city beasts get a floor closer to their own
		@Override
		public String tilesTex() {
			switch (FaunaTest.zone()){
				case "flesh": return Assets.Environment.TILES_HALLS;
				case "city":  return Assets.Environment.TILES_CITY;
				default:      return ContinentAssets.TILES_ASH;
			}
		}

		@Override
		public String waterTex() {
			switch (FaunaTest.zone()){
				case "flesh": return Assets.Environment.WATER_HALLS;
				case "city":  return Assets.Environment.WATER_CITY;
				default:      return ContinentAssets.WATER_ASH;
			}
		}

		@Override
		protected void createMobs() {
			if (BigTest.on()) BigTest.populate(this, entrance());
			if (FaunaTest.on()) FaunaTest.populate(this, entrance());
		}

		@Override
		protected void createItems() {
		}

		@Override
		public Mob createMob() {
			return null;
		}

		@Override
		public Actor addRespawner() {
			return null;
		}

		@Override
		public int randomRespawnCell( Char ch ) {
			return -1;
		}

		@Override
		public Group addVisuals() {
			super.addVisuals();
			float s = DungeonTilemap.SIZE;
			for (String[] l : lines()){
				Image im;
				if (l[0].equals("bg")){
					im = picture(l[1]);
					im.x = Integer.parseInt(l[2]);
					im.y = Integer.parseInt(l[3]);
				} else if (l[0].equals("img")){
					im = picture(l[1]);
					im.x = Integer.parseInt(l[2]) * s - Integer.parseInt(l[4]);
					im.y = Integer.parseInt(l[3]) * s - Integer.parseInt(l[5]);
				} else {
					continue;
				}
				visuals.add(im);
			}
			visuals.add(new Shots());
			return visuals;
		}
	}

	//lights the whole plain, then one screenshot per "shot" line
	private static class Shots extends Group {

		private float wait = 3f;
		private int next = -1;
		private final ArrayList<String[]> shots = new ArrayList<>();
		private boolean noUi = false;
		private boolean dark = false;

		Shots(){
			for (String[] l : lines()){
				if (l[0].equals("shot")) shots.add(l);
				if (l[0].equals("noui")) noUi = true;
				if (l[0].equals("dark")) dark = true;
			}
		}

		@Override
		public void update() {
			super.update();
			if ((wait -= Game.elapsed) > 0) return;
			if (next == -1){
				if (!dark){
					Level l = Dungeon.level;
					Arrays.fill(l.visited, true);
					Arrays.fill(l.mapped, true);
					Arrays.fill(l.heroFOV, true);
					GameScene.updateFog();
				}
				if (noUi){
					//clipped away, off the left side of the screen
					PixelScene.uiCamera.fullScreen = false;
					PixelScene.uiCamera.x = -10 * Game.width;
				}
				next = 0;
				aim();
				return;
			}
			if (next < shots.size()){
				save(DevFlags.colPreview() + "/shot_" + next + ".png");
				next++;
				aim();
			}
		}

		private void aim(){
			wait = 1.5f;
			if (next >= shots.size()) return;
			String[] s = shots.get(next);
			float size = DungeonTilemap.SIZE;
			Camera.main.zoom(Float.parseFloat(s[3]));
			Camera.main.snapTo((Integer.parseInt(s[1]) + 0.5f) * size, (Integer.parseInt(s[2]) + 0.5f) * size);
		}

		private void save( String path ){
			int w = Gdx.graphics.getBackBufferWidth();
			int h = Gdx.graphics.getBackBufferHeight();
			Pixmap p = Pixmap.createFromFrameBuffer(0, 0, w, h);
			//the framebuffer comes upside down
			Pixmap flipped = new Pixmap(w, h, p.getFormat());
			for (int y = 0; y < h; y++){
				flipped.drawPixmap(p, 0, y, w, 1, 0, h - 1 - y, w, 1);
			}
			PixmapIO.writePNG(Gdx.files.absolute(path), flipped);
			p.dispose();
			flipped.dispose();
			CoopLog.line("colossus preview: " + path);
		}
	}
}
