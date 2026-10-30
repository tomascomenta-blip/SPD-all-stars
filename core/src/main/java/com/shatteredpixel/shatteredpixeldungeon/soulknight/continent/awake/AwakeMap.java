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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

//the world map while the continent is awake: a ring that beats on the way home (the portal itself
//on the plains) and a line under the map saying which way it is
public class AwakeMap {

	//WndWorldMap, under the list of places: the y below what it added
	public static float route( Group wnd, Level level, float left, float top, float scale, int width, float y ){
		if (!Awakening.on() || Awakening.sealed() || !(level instanceof OpenWorld)) return y;
		int home = Awakening.homeward(level);
		if (home != -1){
			float x = left + (home % level.width() + 0.5f) * scale, hy = top + (home / level.width() + 0.5f) * scale;
			wnd.add(new Ring(x, hy));
		}
		RenderedTextBlock t = PixelScene.renderTextBlock(Messages.get(Awakening.class, which(level),
				Continent.place(Biome.zone(Awakening.nextZoneHome(Continent.zone())), OpenWorld.SURFACE)), 6);
		t.hardlight(0xFF7050);
		t.maxWidth(width);
		t.setPos(0, y);
		wnd.add(t);
		return t.bottom() + 2;
	}

	private static String which( Level level ){
		if (Awakening.portalCell(level) != -1) return "map_portal";
		if (((OpenWorld) level).layer() != OpenWorld.SURFACE) return "map_up";
		return "map_pass";
	}

	private static class Ring extends Group {

		private final ColorBlock[] sides = new ColorBlock[4];
		private final ColorBlock core;
		private final float x, y;
		private float t = 0;

		Ring( float x, float y ){
			this.x = x;
			this.y = y;
			for (int i = 0; i < 4; i++){
				sides[i] = new ColorBlock(1, 1, 0xFFFF5A3A);
				add(sides[i]);
			}
			core = new ColorBlock(3, 3, 0xFFFFE08A);
			core.x = Math.round(x - 1.5f);
			core.y = Math.round(y - 1.5f);
			add(core);
			place();
		}

		private void place(){
			float r = 3 + 3 * ((t * 1.2f) % 1f);
			float a = 1 - ((t * 1.2f) % 1f) * 0.8f;
			float d = r * 2 + 1;
			sides[0].size(d, 1);
			sides[0].x = x - r - 0.5f;
			sides[0].y = y - r - 0.5f;
			sides[1].size(d, 1);
			sides[1].x = x - r - 0.5f;
			sides[1].y = y + r - 0.5f;
			sides[2].size(1, d);
			sides[2].x = x - r - 0.5f;
			sides[2].y = y - r - 0.5f;
			sides[3].size(1, d);
			sides[3].x = x + r - 0.5f;
			sides[3].y = y - r - 0.5f;
			for (ColorBlock c : sides) c.alpha(a);
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			place();
		}
	}
}
