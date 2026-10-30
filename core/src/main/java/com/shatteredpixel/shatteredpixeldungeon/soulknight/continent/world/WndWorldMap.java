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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.AwakeMap;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

import java.util.ArrayList;

//the map of the open world: what the party has seen of this layer, scaled to fit the screen,
//with the heroes and the places found so far. it only draws, nothing in the game changes
public class WndWorldMap extends Window {

	//a level that knows how it looks from above
	public interface Charted {
		int mapColor( int cell );
		void marks( ArrayList<Mark> out );
	}

	//a place worth a mark, found as soon as any of its cells has been seen
	public static class Mark {

		public final int x, y, w, h, color;
		public final String name;

		public Mark( int x, int y, int w, int h, int color, String name ){
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.color = color;
			this.name = name;
		}

		public boolean found( Level level ){
			for (int cy = Math.max(0, y); cy < Math.min(level.height(), y + h); cy++){
				for (int cx = Math.max(0, x); cx < Math.min(level.width(), x + w); cx++){
					int c = cx + cy * level.width();
					if (level.visited[c] || level.mapped[c]) return true;
				}
			}
			return false;
		}
	}

	private static final String KEY = "world_map";
	private static final int GAP = 2;
	private static final int MIN_WIDTH = 120;

	private final Level level;
	private final Image map;
	private final float scale;
	private final ArrayList<ColorBlock> dots = new ArrayList<>();
	private final ArrayList<Hero> heroes = new ArrayList<>();
	private float refresh = 0, blink = 0;

	public WndWorldMap(){
		super();
		level = Dungeon.level;
		int lw = level.width(), lh = level.height();

		float maxW = Math.min(PixelScene.uiCamera.width - 16, 300);
		float maxH = PixelScene.uiCamera.height - 70;
		float s = Math.min(maxW / lw, maxH / lh);
		if (s >= 1) s = (int)s;
		scale = s;
		int mw = Math.round(lw * scale), mh = Math.round(lh * scale);
		int width = Math.max(mw + 2, MIN_WIDTH);

		String place = Continent.place();
		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.titleCase(place != null ? place : Messages.get(this, "title")), 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((width - title.width()) / 2f, 0);
		PixelScene.align(title);
		add(title);

		float top = title.bottom() + 2 * GAP;
		float left = (width - mw) / 2f;
		ColorBlock back = new ColorBlock(mw + 2, mh + 2, 0xFF050303);
		back.x = left - 1;
		back.y = top - 1;
		add(back);

		paint(scale < 1);
		map = new Image(KEY);
		map.scale.set(scale);
		map.x = left;
		map.y = top;
		add(map);

		//the places found so far, and a line for each under the map
		ArrayList<Mark> marks = new ArrayList<>();
		if (level instanceof Charted) ((Charted) level).marks(marks);
		ArrayList<Mark> found = new ArrayList<>();
		for (Mark m : marks){
			if (!m.found(level)) continue;
			found.add(m);
			float cx = left + (m.x + m.w / 2f) * scale, cy = top + (m.y + m.h / 2f) * scale;
			ColorBlock rim = new ColorBlock(5, 5, 0xFF000000);
			rim.x = Math.round(cx - 2.5f);
			rim.y = Math.round(cy - 2.5f);
			add(rim);
			ColorBlock dot = new ColorBlock(3, 3, m.color);
			dot.x = rim.x + 1;
			dot.y = rim.y + 1;
			add(dot);
		}

		if (Party.active()) heroes.addAll(Party.heroes());
		else if (Dungeon.local() != null) heroes.add(Dungeon.local());
		for (Hero h : heroes){
			ColorBlock d = new ColorBlock(3, 3, h == Dungeon.local() ? 0xFFFFFFFF : 0xFF7AD0FF);
			dots.add(d);
			add(d);
		}
		placeHeroes();

		float y = top + mh + 2 * GAP;
		if (found.isEmpty()){
			RenderedTextBlock none = PixelScene.renderTextBlock(Messages.get(this, "nothing"), 6);
			none.hardlight(0xA09890);
			none.maxWidth(width);
			none.setPos(0, y);
			add(none);
			y = none.bottom() + GAP;
		} else {
			int columns = width >= 200 ? 2 : 1;
			float colW = width / (float)columns;
			float rowBottom = y;
			for (int i = 0; i < found.size(); i++){
				Mark m = found.get(i);
				float x = (i % columns) * colW;
				if (i % columns == 0) y = rowBottom;
				ColorBlock sw = new ColorBlock(4, 4, m.color);
				sw.x = x;
				sw.y = y + 1;
				add(sw);
				RenderedTextBlock t = PixelScene.renderTextBlock(Messages.titleCase(m.name), 6);
				t.maxWidth((int)colW - 8);
				t.setPos(x + 6, y);
				add(t);
				rowBottom = Math.max(rowBottom, t.bottom() + 1);
			}
			y = rowBottom + GAP;
		}

		y = AwakeMap.route(this, level, left, top, scale, width, y);
		y = fan(width, y);
		resize(width, (int)Math.ceil(y));
	}

	//the biomes around the portal as they lie, the ones walked so far in their color and the
	//one you're in framed
	private float fan( int width, float y ){
		RenderedTextBlock t = PixelScene.renderTextBlock(Messages.get(this, "lands"), 6);
		t.hardlight(0xA09890);
		t.setPos((width - t.width()) / 2f, y);
		PixelScene.align(t);
		add(t);
		y = t.bottom() + GAP;
		int cw = 14, ch = 8;
		float left = Math.round((width - 3 * (cw + 1)) / 2f);
		Biome here = Biome.current();
		for (Biome b : Biome.ALL){
			boolean seen = (Continent.visited & (1 << b.zone)) != 0 || b == here;
			float x = left + b.fanX * (cw + 1), top = y + b.fanY * (ch + 1);
			if (b == here){
				ColorBlock frame = new ColorBlock(cw + 2, ch + 2, 0xFFFFFFFF);
				frame.x = x - 1;
				frame.y = top - 1;
				add(frame);
			}
			ColorBlock c = new ColorBlock(cw, ch, seen ? b.fanColor : 0xFF201A1A);
			c.x = x;
			c.y = top;
			add(c);
		}
		//the portal, under the ash plains
		ColorBlock portal = new ColorBlock(4, 3, 0xFFFFE08A);
		portal.x = left + (cw + 1) + cw / 2f - 2;
		portal.y = y + 2 * (ch + 1);
		add(portal);
		return portal.y + portal.height + GAP;
	}

	//one pixel per cell: what has been seen, dimmer what is only known from a map
	private void paint( boolean smooth ){
		int lw = level.width(), lh = level.height();
		SmartTexture tx = TextureCache.contains(KEY) ? TextureCache.get(KEY) : null;
		if (tx == null || tx.width != lw || tx.height != lh){
			TextureCache.remove(KEY);
			tx = TextureCache.create(KEY, lw, lh);
		}
		tx.filter(smooth ? Texture.LINEAR : Texture.NEAREST, smooth ? Texture.LINEAR : Texture.NEAREST);
		Charted charted = level instanceof Charted ? (Charted) level : null;
		for (int c = 0; c < lw * lh; c++){
			boolean seen = level.visited[c], known = seen || level.mapped[c];
			int argb = 0;
			if (known){
				argb = charted != null ? charted.mapColor(c) : plain(c);
				if (!seen) argb = dim(argb);
			}
			tx.bitmap.drawPixel(c % lw, c / lw, (argb << 8) | (argb >>> 24));
		}
		tx.bitmap(tx.bitmap);
	}

	private int plain( int cell ){
		if (level.passable[cell]) return 0xFF6A5E58;
		return level.solid[cell] ? 0xFF1E1816 : 0xFF3A3230;
	}

	private static int dim( int argb ){
		int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
		return 0xFF000000 | ((r * 3 / 5) << 16) | ((g * 3 / 5) << 8) | (b * 3 / 5);
	}

	private void placeHeroes(){
		for (int i = 0; i < heroes.size(); i++){
			Hero h = heroes.get(i);
			ColorBlock d = dots.get(i);
			d.visible = h.isAlive() && !h.coopDown && h.pos >= 0 && h.pos < level.length();
			d.x = map.x + (h.pos % level.width() + 0.5f) * scale - 1.5f;
			d.y = map.y + (h.pos / level.width() + 0.5f) * scale - 1.5f;
		}
		//your own dot blinks, so you find yourself at once
		if (!dots.isEmpty() && heroes.indexOf(Dungeon.local()) >= 0){
			ColorBlock me = dots.get(heroes.indexOf(Dungeon.local()));
			me.alpha(((int)(blink * 3)) % 2 == 0 ? 1f : 0.35f);
			bringToFront(me);
		}
	}

	@Override
	public void update() {
		super.update();
		blink += Game.elapsed;
		refresh += Game.elapsed;
		if (level != Dungeon.level){
			hide();
			return;
		}
		if (refresh > 1f){
			refresh = 0;
			paint(scale < 1);
		}
		placeHeroes();
	}

	@Override
	public void destroy() {
		super.destroy();
		TextureCache.remove(KEY);
	}
}
