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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral;

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

//how the inside of the furnace looks and shakes on this screen. none of it is game state: the
//shakes and flashes come from what the game does (Cathedral, CathedralLevel), the glow of the grates
//and of the live embers follows the map, and the gauge reads the clock
public class CathedralScreen {

	public static void heat(){
		GameScene.flash(0x40FF5010);
		PixelScene.shake(1.5f, 0.6f);
	}

	public static void risen(){
		GameScene.flash(0x80FF3000);
		PixelScene.shake(5f, 2.5f);
	}

	public static void tremor(){
		PixelScene.shake(2.5f, 0.8f);
	}

	public static void broken(){
		GameScene.flash(0xC0FFC060);
		PixelScene.shake(6f, 2.5f);
	}

	//smoke off the grates and flames off the live embers, wherever the heroes can see them
	public static class Glow extends Group {

		private final CathedralLevel level;
		private Emitter[] at;
		private float scan = 0;

		public Glow( CathedralLevel level ){
			this.level = level;
		}

		@Override
		public void update() {
			super.update();
			if (Dungeon.level != level) return;
			if (at == null) at = new Emitter[level.length()];
			if ((scan -= Game.elapsed) > 0) return;
			scan = 0.5f;
			for (int g : level.grates()){
				if (at[g] == null) at[g] = emitter(g, SmokeParticle.FACTORY, 0.5f);
			}
			for (int c = 0; c < level.length(); c++){
				if (level.map[c] == Terrain.EMBERS && at[c] == null) at[c] = emitter(c, FlameParticle.FACTORY, 1.2f);
			}
			boolean[] fov = level.heroFOV;
			for (int c = 0; c < at.length; c++){
				if (at[c] != null) at[c].on = fov != null && fov[c];
			}
		}

		private Emitter emitter( int cell, Emitter.Factory f, float every ){
			Emitter e = new Emitter();
			PointF p = DungeonTilemap.tileCenterToWorld(cell);
			e.pos(p.x - 4, p.y - 2, 8, 6);
			e.pour(f, every);
			e.autoKill = false;
			add(e);
			return e;
		}
	}

	// *** the gauge ***

	private static final String FLAME = "cathedral_heat_flame";

	//a flame, 9x11, for the corner of the gauge
	private static Object flame(){
		if (TextureCache.contains(FLAME)) return FLAME;
		SmartTexture tx = TextureCache.create(FLAME, 9, 11);
		tx.filter(Texture.NEAREST, Texture.NEAREST);
		Pixmap p = tx.bitmap;
		p.setColor(0);
		p.fill();
		String[] rows = {
				"....1....",
				"...12....",
				"...121...",
				"..12321..",
				".1233321.",
				".1234321.",
				"123444321",
				"123444321",
				".1234421.",
				"..12221..",
				"...111..."};
		int[] cols = {0, 0x5A1408, 0xB03A10, 0xE8781C, 0xFFD27A};
		for (int y = 0; y < rows.length; y++){
			for (int x = 0; x < rows[y].length(); x++){
				char ch = rows[y].charAt(x);
				if (ch == '.') continue;
				p.setColor((cols[ch - '0'] << 8) | 0xFF);
				p.drawPixel(x, y);
			}
		}
		return FLAME;
	}

	//under the corruption bar while the party is inside: how hot it burns (five pips) and how long
	//before the thing gets up out there, then that it's up
	public static class Gauge extends Group {

		private static final int PIP_W = 7, PIP_H = 4;

		private final Image icon;
		private final ColorBlock[] pips = new ColorBlock[Cathedral.MAX_HEAT];
		private final RenderedTextBlock text;
		private float t = 0;
		private int shownRise = -1;

		public Gauge( float x0, float y0 ){
			icon = new Image(flame());
			icon.x = x0;
			icon.y = y0;
			add(icon);
			for (int i = 0; i < pips.length; i++){
				pips[i] = new ColorBlock(PIP_W, PIP_H, 0xFF2A1410);
				pips[i].x = x0 + 12 + i * (PIP_W + 2);
				pips[i].y = y0 + 4;
				add(pips[i]);
			}
			text = PixelScene.renderTextBlock(6);
			text.setPos(x0 + 1, y0 + 13);
			add(text);
			visible = false;
		}

		@Override
		public void update() {
			super.update();
			visible = Cathedral.here();
			if (!visible) return;
			t += Game.elapsed;
			int heat = Cathedral.heat();
			for (int i = 0; i < pips.length; i++){
				int c = i < heat ? (i >= 3 ? 0xFFF05A1C : 0xFFD08A28) : 0xFF2A1410;
				if (i == heat - 1 && ((int) (t * 3)) % 2 == 0) c = 0xFFFFD27A;
				pips[i].color(c);
			}
			int rise = Cathedral.risen() ? 0 : Cathedral.toRise();
			if (rise != shownRise){
				shownRise = rise;
				if (rise > 0){
					text.text(Messages.get(Cathedral.class, "gauge_rise", rise));
					text.hardlight(rise <= 15 ? 0xFF9050 : 0xE8C8A0);
				} else {
					text.text(Messages.get(Cathedral.class, "gauge_up"));
					text.hardlight(0xFF5030);
				}
				PixelScene.align(text);
			}
			if (rise == 0) text.alpha(((int) (t * 2)) % 2 == 0 ? 1f : 0.55f);
		}
	}
}
