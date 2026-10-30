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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.quests;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.ForestBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.RitualHud;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;

//quest mob hp bar, big title when they show up, short banners (new phase, it ran off...).
//one per game screen, hides when nobody's fighting
public class MiniBossHud extends Group {

	public static final int W = 176, H = 32;
	public static final int FILL_X = 38, FILL_Y = 12, FILL_W = 130, FILL_H = 8;

	private static MiniBossHud instance;
	private static String[] queuedTitle;
	private static String queuedBanner;

	private final ColorBlock back, fill;
	private Image frame;
	private String frameAsset;
	private final RenderedTextBlock info;
	private RenderedTextBlock title, sub, banner;
	private ColorBlock titleBack, bannerBack;
	private float titleT = 99, bannerT = 99, t = 0, s = 1f, x0, y0, top;
	private String lastInfo = "";

	public MiniBossHud( float top ){
		instance = this;
		this.top = top;
		float uiw = PixelScene.uiCamera.width;
		s = Math.min(1f, (uiw - 8) / W);
		x0 = (uiw - W * s) / 2f;
		y0 = top;

		back = new ColorBlock(FILL_W * s, FILL_H * s, 0xFF140C06);
		fill = new ColorBlock(FILL_W * s, FILL_H * s, 0xFFB03A2A);
		add(back);
		add(fill);
		info = PixelScene.renderTextBlock(6);
		add(info);
		place();

		if (queuedTitle != null) showTitle(queuedTitle[0], queuedTitle[1]);
		queuedTitle = null;
		if (queuedBanner != null) showBanner(queuedBanner);
		queuedBanner = null;
	}

	//under a real boss's bar if both are up (quest mob joining a boss fight)
	private void place(){
		y0 = BossHealthBar.isAssigned() ? top + 44 : top;
		//the ritual totem counter sits up there on forest floors
		if (RitualHud.wanted() && !(Dungeon.level instanceof ForestBossLevel)) y0 += 18;
		back.x = x0 + FILL_X * s;
		back.y = y0 + FILL_Y * s;
		fill.x = back.x;
		fill.y = back.y;
		if (frame != null){
			frame.x = x0;
			frame.y = y0;
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (instance == this) instance = null;
	}

	public static void title( final String name, final String subtitle ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.showTitle(name, subtitle);
				else queuedTitle = new String[]{name, subtitle};
			}
		});
	}

	public static void banner( final String text ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.showBanner(text);
				else queuedBanner = text;
			}
		});
	}

	private void showTitle( String name, String subtitle ){
		if (title != null){
			title.killAndErase();
			sub.killAndErase();
			titleBack.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		title = PixelScene.renderTextBlock(name, 12);
		title.hardlight(0xFFD27A);
		sub = PixelScene.renderTextBlock(subtitle, 7);
		sub.hardlight(0xE0D0B0);
		float w = Math.max(title.width(), sub.width()) + 24;
		titleBack = new ColorBlock(w, title.height() + sub.height() + 16, 0xDD0E0A04);
		titleBack.x = (uiw - titleBack.width()) / 2f;
		titleBack.y = uih * 0.28f;
		title.setPos((uiw - title.width()) / 2f, titleBack.y + 6);
		sub.setPos((uiw - sub.width()) / 2f, title.bottom() + 4);
		PixelScene.align(title);
		PixelScene.align(sub);
		add(titleBack);
		add(title);
		add(sub);
		titleT = 0;
	}

	private void showBanner( String text ){
		if (banner != null){
			banner.killAndErase();
			bannerBack.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		banner = PixelScene.renderTextBlock(text, 9);
		banner.hardlight(0xFFE0A0);
		banner.maxWidth((int) (uiw - 24));
		bannerBack = new ColorBlock(banner.width() + 16, banner.height() + 8, 0xCC0E0A04);
		bannerBack.x = (uiw - bannerBack.width()) / 2f;
		bannerBack.y = uih * 0.40f;
		banner.setPos((uiw - banner.width()) / 2f, bannerBack.y + 4);
		PixelScene.align(banner);
		add(bannerBack);
		add(banner);
		bannerT = 0;
	}

	@Override
	public void update() {
		super.update();
		float e = Game.elapsed;
		t += e;
		MiniBoss b = MiniBoss.active();
		boolean show = b != null;
		if (show && !b.barAsset().equals(frameAsset)){
			if (frame != null) frame.killAndErase();
			frame = new Image(b.barAsset());
			frame.scale.set(s);
			frameAsset = b.barAsset();
			add(frame);
		}
		place();
		back.visible = fill.visible = info.visible = show;
		if (frame != null) frame.visible = show;
		if (show){
			fill.size(FILL_W * s * Math.min(1f, b.barFill()), FILL_H * s);
			int c = b.barColor();
			if (b.inIntro()) c = ((int) (t * 6)) % 2 == 0 ? 0xFFFFE070 : c;
			fill.color(c);
			String txt = b.status();
			if (!txt.equals(lastInfo)){
				info.text(txt);
				lastInfo = txt;
			}
			info.setPos(x0 + (W * s - info.width()) / 2f, y0 + H * s + 1);
			PixelScene.align(info);
		}

		if (title != null){
			titleT += e;
			float a = titleT < 0.4f ? titleT / 0.4f : titleT > 3.2f ? Math.max(0, 1 - (titleT - 3.2f) / 0.6f) : 1;
			title.alpha(a);
			sub.alpha(a);
			titleBack.alpha(a * 0.87f);
			if (titleT > 3.8f){
				title.killAndErase();
				sub.killAndErase();
				titleBack.killAndErase();
				title = null;
			}
		}
		if (banner != null){
			bannerT += e;
			float a = bannerT < 0.3f ? bannerT / 0.3f : bannerT > 2.6f ? Math.max(0, 1 - (bannerT - 2.6f) / 0.5f) : 1;
			banner.alpha(a);
			bannerBack.alpha(a * 0.8f);
			if (bannerT > 3.1f){
				banner.killAndErase();
				bannerBack.killAndErase();
				banner = null;
			}
		}
	}
}
