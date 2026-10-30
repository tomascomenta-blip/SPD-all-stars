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

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral.CathedralScreen;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.events.EventScreen;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.GripBar;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.TetherWarning;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.WndWorldMap;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

//top right, under the menu: essence carried (x/7) and the local hero's corruption bar.
//in the open world, a button for its map under them, and the warning when the local hero is
//alone in the dark
public class ContinentHud extends Group {

	private static final int BAR_X = 18, BAR_Y = 5, BAR_W = 44, BAR_H = 6;

	private final Image icon, frame;
	private final ColorBlock back, fill;
	private final RenderedTextBlock count;
	private float t = 0;
	private int shownEssence = -1;
	private float mapTest = 2f;

	public ContinentHud(){
		float uiw = PixelScene.uiCamera.width;
		float x0 = uiw - 70, y0 = 68;

		TextureFilm film = new TextureFilm(ContinentAssets.HUD, 16, 16);
		icon = new Image(ContinentAssets.HUD);
		icon.frame(film.get(0));
		icon.x = x0 - 2;
		icon.y = y0;
		add(icon);

		back = new ColorBlock(BAR_W, BAR_H, 0xFF140A1C);
		back.x = x0 + BAR_X;
		back.y = y0 + BAR_Y;
		add(back);
		fill = new ColorBlock(BAR_W, BAR_H, 0xFF8A50C8);
		fill.x = back.x;
		fill.y = back.y;
		add(fill);

		frame = new Image(ContinentAssets.HUD);
		frame.frame(frame.texture.uvRect(16, 0, 64, 16));
		frame.x = x0 + 16;
		frame.y = y0;
		add(frame);

		count = PixelScene.renderTextBlock(7);
		count.hardlight(0xFFF2C0);
		add(count);
		count.setPos(x0 + 10, y0 + 4);

		add(new TetherWarning());
		add(new GripBar());
		add(new EventScreen());
		add(new CathedralScreen.Gauge(x0 - 2, y0 + 17));
		if (Dungeon.level instanceof OpenWorld){
			MapButton map = new MapButton();
			map.setRect(x0 - 3, y0 + 16, 18, 16);
			add(map);
		}
	}

	private static class MapButton extends IconButton {

		MapButton(){
			super(new Image(mapIcon()));
		}

		@Override
		protected void onClick() {
			GameScene.show(new WndWorldMap());
		}

		@Override
		protected String hoverText() {
			return Messages.get(ContinentHud.class, "map");
		}
	}

	private static final String MAP_ICON = "continent_map_icon";

	//a folded map, 14x12: three panels, a red trail and a cross
	private static Object mapIcon(){
		if (TextureCache.contains(MAP_ICON)) return MAP_ICON;
		SmartTexture tx = TextureCache.create(MAP_ICON, 14, 12);
		tx.filter(Texture.NEAREST, Texture.NEAREST);
		Pixmap p = tx.bitmap;
		p.setColor(0);
		p.fill();
		//x, top, width and color of each panel: the middle one is folded back
		int[][] panels = {{0, 1, 5, 0xD8C090}, {5, 0, 4, 0xB89C6C}, {9, 1, 5, 0xD8C090}};
		for (int[] pn : panels){
			p.setColor(rgba(0x2A1A10));
			p.fillRectangle(pn[0], pn[1], pn[2], 11);
			p.setColor(rgba(pn[3]));
			p.fillRectangle(pn[0] + 1, pn[1] + 1, pn[2] - 1, 9);
		}
		p.setColor(rgba(0x2A1A10));
		p.fillRectangle(13, 1, 1, 11);
		int[][] trail = {{2, 9}, {3, 8}, {4, 7}, {6, 6}, {7, 5}, {8, 6}};
		p.setColor(rgba(0xB03020));
		for (int[] d : trail) p.drawPixel(d[0], d[1]);
		int[][] cross = {{10, 3}, {12, 3}, {11, 4}, {10, 5}, {12, 5}};
		p.setColor(rgba(0xD04028));
		for (int[] d : cross) p.drawPixel(d[0], d[1]);
		return MAP_ICON;
	}

	private static int rgba( int rgb ){
		return (rgb << 8) | 0xFF;
	}

	@Override
	public void update() {
		super.update();
		t += Game.elapsed;
		if (shownEssence != Continent.essence){
			shownEssence = Continent.essence;
			count.text(Integer.toString(Continent.essence));
			PixelScene.align(count);
		}
		Hero me = Dungeon.local();
		int level = me == null ? 0 : DarkTaint.of(me);
		fill.size(BAR_W * level / 100f, BAR_H);
		int c = level >= 100 ? 0xFFE02040 : level >= 75 ? 0xFFB040F0 : level >= 50 ? 0xFF9A50D8 : 0xFF7A48B0;
		if (level >= 100 && ((int)(t * 4)) % 2 == 0) c = 0xFFFF6080;
		fill.color(c);

		//-Dspd.test.worldmap=1
		if (mapTest > 0 && DevFlags.worldMap() && Dungeon.level instanceof OpenWorld){
			mapTest -= Game.elapsed;
			if (mapTest <= 0) GameScene.show(new WndWorldMap());
		}
	}
}
