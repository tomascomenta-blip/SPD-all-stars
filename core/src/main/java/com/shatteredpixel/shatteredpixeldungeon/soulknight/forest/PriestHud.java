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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forest;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.Random;

import java.util.ArrayList;

//high priest bar: his carved staff, the star on it dims as the fight goes, ritual totems on top while they're up.
//also does the big title when he turns, short banners, the giant star count (+ how much is left to break it)
//and the act 3 night
public class PriestHud extends Group {

	//where the hp goes inside priest_bar.png
	public static int FILL_X = 60, FILL_Y = 17, FILL_W = 132, FILL_H = 7;

	private static PriestHud instance;
	private static String[] queuedTitle;
	private static String queuedBanner;

	private final ColorBlock night;
	private final ArrayList<ColorBlock> stars = new ArrayList<>();
	private final ArrayList<Float> starPhase = new ArrayList<>();
	private ColorBlock shooting;
	private float shootT = -1, shootVX, shootVY;

	private final ColorBlock back, fill, breakBack, breakFill;
	private final Image frame, starIcon;
	private final Image[] totemIcons = new Image[3];
	private final TextureFilm starFilm, iconFilm;
	private final RenderedTextBlock info, count;
	private RenderedTextBlock title, sub, banner;
	private ColorBlock titleBack, bannerBack;
	private float titleT = 99, bannerT = 99, t = 0, s = 1f, x0, y0;

	public PriestHud( float top ){
		instance = this;
		BossHealthBar.hidden = true;
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;

		night = new ColorBlock(uiw, uih, 0xFF070A24);
		night.alpha(0);
		add(night);
		for (int i = 0; i < 40; i++){
			ColorBlock st = new ColorBlock(1, 1, i % 5 == 0 ? 0xFFFFE8A0 : 0xFFE8F0FF);
			st.x = Random.Float(uiw);
			st.y = Random.Float(uih * 0.75f);
			st.alpha(0);
			stars.add(st);
			starPhase.add(Random.Float(6.28f));
			add(st);
		}
		shooting = new ColorBlock(6, 1, 0xFFFFF0C0);
		shooting.alpha(0);
		add(shooting);

		s = Math.min(1f, (uiw - 8) / 208f);
		x0 = (uiw - 208 * s) / 2f;
		y0 = top;
		back = new ColorBlock(FILL_W * s, FILL_H * s, 0xFF140C06);
		back.x = x0 + FILL_X * s;
		back.y = y0 + FILL_Y * s;
		fill = new ColorBlock(FILL_W * s, FILL_H * s, 0xFFB03A2A);
		fill.x = back.x;
		fill.y = back.y;
		frame = new Image(PriestAssets.BAR);
		frame.scale.set(s);
		frame.x = x0;
		frame.y = y0;
		starIcon = new Image(PriestAssets.BAR_STAR);
		starFilm = new TextureFilm(starIcon.texture, 40, 40);
		starIcon.frame(starFilm.get(0));
		starIcon.scale.set(s);
		starIcon.x = x0;
		starIcon.y = y0;
		add(back);
		add(fill);
		add(frame);
		add(starIcon);

		Image probe = new Image(PriestAssets.ICONS);
		iconFilm = new TextureFilm(probe.texture, 12, 16);
		for (int i = 0; i < 3; i++){
			totemIcons[i] = new Image(PriestAssets.ICONS);
			totemIcons[i].frame(iconFilm.get(PriestAssets.ICON_FIRE + i));
			totemIcons[i].x = x0 + (FILL_X + FILL_W) * s - (3 - i) * 14;
			totemIcons[i].y = y0 + 40 * s + 8;
			totemIcons[i].visible = false;
			add(totemIcons[i]);
		}

		//how much is left to break the giant star spell
		breakBack = new ColorBlock(FILL_W * s, 3, 0xFF101830);
		breakBack.x = back.x;
		breakBack.y = y0 + 40 * s + 10;
		breakFill = new ColorBlock(FILL_W * s, 3, 0xFFFFD860);
		breakFill.x = breakBack.x;
		breakFill.y = breakBack.y;
		add(breakBack);
		add(breakFill);

		info = PixelScene.renderTextBlock(6);
		add(info);
		count = PixelScene.renderTextBlock(9);
		count.hardlight(0xFFE7A0);
		add(count);

		if (queuedTitle != null) showTitle(queuedTitle[0], queuedTitle[1]);
		queuedTitle = null;
		if (queuedBanner != null) showBanner(queuedBanner);
		queuedBanner = null;
	}

	@Override
	public void destroy() {
		super.destroy();
		if (instance == this){
			instance = null;
			BossHealthBar.hidden = false;
		}
	}

	//big title w/ his name when he turns around
	public static void title( final String name, final String subtitle ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.showTitle(name, subtitle);
				else queuedTitle = new String[]{name, subtitle};
			}
		});
	}

	//short line mid screen (new act, ritual broken...)
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
		sub.hardlight(0xC8E0A0);
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
		bannerBack = new ColorBlock(banner.width() + 16, banner.height() + 8, 0xCC0E0A04);
		bannerBack.x = (uiw - bannerBack.width()) / 2f;
		bannerBack.y = uih * 0.36f;
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
		GoblinPriest p = Dungeon.level == null ? null : ForestBossLevel.priest(Dungeon.level);
		boolean fight = p != null && p.fighting() && Dungeon.level.locked;

		frame.visible = back.visible = fill.visible = info.visible = starIcon.visible = fight;
		boolean star = fight && p.starTimer > 0;
		breakBack.visible = breakFill.visible = count.visible = star;
		if (fight){
			fill.size(FILL_W * s * p.HP / (float) p.HT, FILL_H * s);
			int c;
			if (p.ritualUp()) c = ((int) (t * 3)) % 2 == 0 ? 0xFF50A040 : 0xFF2E7A28;
			else if (p.exposed > 0) c = ((int) (t * 6)) % 2 == 0 ? 0xFFFFE070 : 0xFFB03A2A;
			else if (p.act >= 3) c = ((int) (t * 2)) % 2 == 0 ? 0xFF6A50D0 : 0xFF4A34A8;
			else c = p.brute ? 0xFFC02A1A : 0xFFB03A2A;
			fill.color(c);
			starIcon.frame(starFilm.get(Math.max(0, Math.min(2, p.act - 1))));

			String txt = Messages.get(PriestHud.class, "act_" + Math.max(1, Math.min(3, p.act)));
			if (p.stunned > 0) txt += "   " + Messages.get(PriestHud.class, "stunned");
			else if (p.ritualUp()) txt += "   " + Messages.get(PriestHud.class, "ritual");
			if (p.brute) txt = Messages.get(PriestHud.class, "brute") + "  " + txt;
			info.text(txt);
			info.setPos(x0 + FILL_X * s, frame.y + 40 * s);
			PixelScene.align(info);

			//ritual totems over the bar
			GoblinPriest.RitualTotem[] tt = p.ritualTotems();
			boolean ritual = p.ritualUp();
			for (int i = 0; i < 3; i++){
				totemIcons[i].visible = ritual;
				if (ritual) totemIcons[i].frame(iconFilm.get(tt[i] != null ? PriestAssets.ICON_FIRE + i : PriestAssets.ICON_BROKEN));
			}

			if (star){
				float k = p.starNeed <= 0 ? 0 : GameMath.gate(0, p.starHurt / (float) p.starNeed, 1);
				breakFill.size(FILL_W * s * k, 3);
				count.text(Messages.get(PriestHud.class, "star", p.starTimer, Math.max(0, p.starNeed - p.starHurt)));
				count.setPos((PixelScene.uiCamera.width - count.width()) / 2f, breakBack.y + 6);
				PixelScene.align(count);
			}
		} else {
			for (Image i : totemIcons) i.visible = false;
		}

		//act 3 night. 0.38 = dark but you can still see stuff
		float target = fight && p.act >= 3 ? 0.38f : 0f;
		night.alpha(night.alpha() + GameMath.gate(-e * 0.5f, target - night.alpha(), e * 0.5f));
		float k = night.alpha() / 0.38f;
		for (int i = 0; i < stars.size(); i++){
			float ph = starPhase.get(i);
			stars.get(i).alpha(k * (0.4f + 0.6f * Math.abs((float) Math.sin(t * 1.3f + ph))));
		}
		if (k > 0.5f){
			if (shootT < 0 && Random.Float() < e * 0.6f){
				shootT = 0;
				shooting.x = Random.Float(PixelScene.uiCamera.width * 0.7f);
				shooting.y = Random.Float(PixelScene.uiCamera.height * 0.3f);
				shootVX = 160 + Random.Float(80);
				shootVY = 60 + Random.Float(40);
			}
		}
		if (shootT >= 0){
			shootT += e;
			shooting.x += shootVX * e;
			shooting.y += shootVY * e;
			shooting.alpha(GameMath.gate(0, 1 - shootT / 0.7f, 1) * k);
			if (shootT > 0.7f){
				shootT = -1;
				shooting.alpha(0);
			}
		}

		if (title != null){
			titleT += e;
			float a = titleT < 0.4f ? titleT / 0.4f : GameMath.gate(0, (4f - titleT) / 0.8f, 1);
			title.alpha(a);
			sub.alpha(a);
			titleBack.alpha(a * 0.85f);
			if (titleT > 4f){
				title.killAndErase();
				sub.killAndErase();
				titleBack.killAndErase();
				title = null;
				sub = null;
				titleBack = null;
			}
		}
		if (banner != null){
			bannerT += e;
			float a = bannerT < 0.3f ? bannerT / 0.3f : GameMath.gate(0, (2.8f - bannerT) / 0.6f, 1);
			banner.alpha(a);
			bannerBack.alpha(a * 0.8f);
			if (bannerT > 2.8f){
				banner.killAndErase();
				bannerBack.killAndErase();
				banner = null;
				bannerBack = null;
			}
		}
	}
}
