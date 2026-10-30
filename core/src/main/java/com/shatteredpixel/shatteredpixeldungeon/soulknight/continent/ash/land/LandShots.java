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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.land;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.EdgeTransition;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.WndWorldMap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

import java.util.Arrays;

//testing only: -Dspd.test.landshots=<dir>;x,y,zoom;x,y,zoom;map;... the open world as seen from
//above: "map" opens the world map as it is, "dark:x,y,zoom" is a picture as the heroes see it,
//and the first x,y,zoom lights the whole layer for good ("edge:E,zoom" looks at the pass on that
//edge instead of a cell). the interface is hidden but for the map.
//saved as <dir>/land_<biome><layer>_N.png
class LandShots extends Group {

	private final String dir;
	private final String[] spots;
	private float wait = 3f;
	private int next = 0;
	private boolean lit = false;
	private int uiX;

	LandShots(){
		String[] parts = DevFlags.landShots().split(";");
		dir = parts[0];
		spots = Arrays.copyOfRange(parts, 1, parts.length);
		uiX = PixelScene.uiCamera.x;
	}

	@Override
	public void update() {
		super.update();
		if ((wait -= Game.elapsed) > 0 || next > spots.length) return;
		//a minimized window has nothing to save: wait until it's back
		if (Gdx.graphics.getBackBufferWidth() <= 0) return;
		AshLand land = (AshLand) Dungeon.level;
		if (next > 0) save(dir + "/land_" + land.biome().key + land.layer() + "_" + (next - 1) + ".png");
		if (next == spots.length){
			next++;
			return;
		}
		String s = spots[next++];
		wait = 1.5f;
		Window open = GameScene.topWindow();
		if (open != null) open.hide();
		if (s.equals("map")){
			PixelScene.uiCamera.x = uiX;
			GameScene.show(new WndWorldMap());
			return;
		}
		boolean dark = s.startsWith("dark:");
		if (dark) s = s.substring(5);
		if (!lit && !dark){
			Level l = Dungeon.level;
			Arrays.fill(l.visited, true);
			Arrays.fill(l.mapped, true);
			Arrays.fill(l.heroFOV, true);
			GameScene.updateFog();
			GameScene.afterObserve();
			for (Heap h : l.heaps.valueList()){
				h.seen = true;
				if (h.sprite != null) h.sprite.visible = true;
			}
			lit = true;
		}
		PixelScene.uiCamera.x = -10 * Game.width;
		if (s.startsWith("edge:")){
			String[] e = s.substring(5).split(",");
			int side = "NESW".indexOf(e[0].charAt(0));
			for (LevelTransition t : Dungeon.level.transitions){
				if (t instanceof EdgeTransition && ((EdgeTransition) t).side == side){
					s = (t.left + t.right) / 2 + "," + (t.top + t.bottom) / 2 + "," + e[1];
				}
			}
			if (s.startsWith("edge:")) return;
		}
		String[] p = s.split(",");
		float size = DungeonTilemap.SIZE;
		Camera.main.zoom(Float.parseFloat(p[2]));
		Camera.main.snapTo((Integer.parseInt(p[0]) + 0.5f) * size, (Integer.parseInt(p[1]) + 0.5f) * size);
	}

	private void save( String path ){
		int w = Gdx.graphics.getBackBufferWidth();
		int h = Gdx.graphics.getBackBufferHeight();
		Pixmap p = Pixmap.createFromFrameBuffer(0, 0, w, h);
		Pixmap flipped = new Pixmap(w, h, p.getFormat());
		for (int y = 0; y < h; y++){
			flipped.drawPixmap(p, 0, y, w, 1, 0, h - 1 - y, w, 1);
		}
		PixmapIO.writePNG(Gdx.files.absolute(path), flipped);
		p.dispose();
		flipped.dispose();
		CoopLog.line("land shots: " + path);
	}
}
