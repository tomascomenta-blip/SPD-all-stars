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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;

import java.util.ArrayList;
import java.util.Random;

//his hud: melted clock bar, act + world, big banners, the time stop tint, and the two cutscenes
//(the victory that cracks, and the rewind)
public class ThalHud extends Group {

	private static ThalHud instance;
	private static final ArrayList<String> queued = new ArrayList<>();
	private static boolean nameQueued = false;

	private final ColorBlock tint;
	private final ColorBlock back, fill, lag;
	private final RenderedTextBlock info;
	private Image frame;
	private RenderedTextBlock banner, bigTitle, bigSub;
	private ColorBlock bannerBack;
	private float bannerT = 99, bigT = 99, t = 0, flashT = 99, s = 1f, x0, y0;
	private float lagW = -1;
	private int flashColor = 0xFFFFFF;

	//bar area inside bar.png
	private static final int BW = 160, BX = 24, BY = 10, BH = 8, FW = 208;

	public ThalHud( float top ){
		instance = this;
		BossHealthBar.hidden = true;
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;

		tint = new ColorBlock(uiw, uih, 0xFF66CCFF);
		tint.alpha(0);
		add(tint);

		s = Math.min(1f, (uiw - 8) / (float) FW);
		x0 = (uiw - FW * s) / 2f;
		y0 = top;
		back = new ColorBlock(BW * s, BH * s, 0xFF0A0814);
		back.x = x0 + BX * s;
		back.y = y0 + BY * s;
		lag = new ColorBlock(BW * s, BH * s, 0xFFFFFFFF);
		lag.x = back.x;
		lag.y = back.y;
		fill = new ColorBlock(BW * s, BH * s, 0xFFFFE08A);
		fill.x = back.x;
		fill.y = back.y;
		add(back);
		add(lag);
		add(fill);
		try {
			frame = new Image(FinaleAssets.BAR);
			frame.scale.set(s);
			frame.x = x0;
			frame.y = y0;
			add(frame);
		} catch (Exception e){
			frame = null;
		}
		info = PixelScene.renderTextBlock(6);
		add(info);

		for (String q : queued) show(q);
		queued.clear();
		if (nameQueued){
			nameQueued = false;
			showName();
		}
		if (ThalVekhorr.rewoundBanner){
			ThalVekhorr.rewoundBanner = false;
			show(Messages.get(ThalHud.class, "rewound"));
			flash(0x66CCFF);
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (instance == this){
			instance = null;
			BossHealthBar.hidden = false;
		}
	}

	public static void banner( final String text ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.show(text);
				else queued.add(text);
			}
		});
	}

	public static void nameBanner(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.showName();
				else nameQueued = true;
			}
		});
	}

	public static void actBanner( final int act ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance == null) return;
				instance.big(Messages.get(ThalHud.class, "act_title_" + act),
						Messages.get(ThalHud.class, "act_sub_" + act));
				instance.flash(ThalParts.worldColor(act <= 5 ? act : 0));
			}
		});
	}

	public static void stopFlash(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.flash(0x66CCFF);
			}
		});
	}

	public static void rewindFlash(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.flash(0xFFFFFF);
			}
		});
	}

	private void flash( int color ){
		flashColor = color;
		flashT = 0;
	}

	private void showName(){
		big(Messages.get(ThalVekhorr.class, "name").toUpperCase(), Messages.get(ThalHud.class, "title"));
	}

	private void big( String title, String sub ){
		if (bigTitle != null) bigTitle.killAndErase();
		if (bigSub != null) bigSub.killAndErase();
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		bigTitle = PixelScene.renderTextBlock(title, 14);
		bigTitle.hardlight(0xFFE08A);
		bigTitle.setPos((uiw - bigTitle.width()) / 2f, uih * 0.36f);
		PixelScene.align(bigTitle);
		bigSub = PixelScene.renderTextBlock(sub, 8);
		bigSub.hardlight(0xBFE6FF);
		bigSub.setPos((uiw - bigSub.width()) / 2f, bigTitle.bottom() + 4);
		PixelScene.align(bigSub);
		add(bigTitle);
		add(bigSub);
		bigT = 0;
	}

	private void show( String text ){
		if (banner != null){
			banner.killAndErase();
			bannerBack.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		banner = PixelScene.renderTextBlock(text, 9);
		banner.maxWidth((int) Math.min(uiw - 20, 220));
		banner.hardlight(0xBFE6FF);
		bannerBack = new ColorBlock(banner.width() + 16, banner.height() + 10, 0xCC0A0814);
		bannerBack.x = (uiw - bannerBack.width()) / 2f;
		bannerBack.y = uih * 0.24f;
		banner.setPos(bannerBack.x + 8, bannerBack.y + 5);
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
		ThalVekhorr th = ThalVekhorr.find();
		boolean fight = th != null && th.currentAct() >= 1 && !th.dying();

		back.visible = fill.visible = lag.visible = info.visible = fight;
		if (frame != null) frame.visible = fight;
		if (fight){
			float w = BW * s * Math.max(0, th.HP) / (float) th.HT;
			fill.size(w, BH * s);
			if (lagW < w) lagW = w;
			lagW = Math.max(w, lagW - 40 * s * e);
			lag.size(lagW, BH * s);
			lag.alpha(0.6f);
			int c = ThalParts.worldColor(th.currentAct() <= 5 ? th.currentAct() : 0);
			if (th.currentAct() >= 6 && ((int)(t * 3)) % 2 == 0) c = 0xFFFFFF;
			fill.color(0xFF000000 | c);
			info.text(Messages.get(ThalHud.class, "act_short_" + Math.min(6, th.currentAct())));
			info.setPos(back.x, back.y + BH * s + 3);
			PixelScene.align(info);
		}

		//time stop: the local hero is frozen
		Hero me = Dungeon.local();
		boolean frozen = me != null && me.buff(ThalCall.Stasis.class) != null;
		float target = frozen ? 0.18f + 0.04f * (float) Math.sin(t * 6) : 0;
		if (flashT < 0.6f){
			flashT += e;
			tint.color(0xFF000000 | flashColor);
			tint.alpha(0.55f * (1 - flashT / 0.6f));
		} else {
			tint.color(0xFF66CCFF);
			float a = tint.alpha();
			tint.alpha(a + (target - a) * Math.min(1, e * 6));
		}

		if (banner != null){
			bannerT += e;
			float a = bannerT < 0.3f ? bannerT / 0.3f : GameMath.gate(0, (4f - bannerT) / 0.8f, 1);
			banner.alpha(a);
			bannerBack.alpha(a * 0.8f);
			if (bannerT > 4f){
				banner.killAndErase();
				bannerBack.killAndErase();
				banner = null;
				bannerBack = null;
			}
		}
		if (bigTitle != null){
			bigT += e;
			float a = bigT < 0.4f ? bigT / 0.4f : GameMath.gate(0, (3.6f - bigT) / 0.8f, 1);
			bigTitle.alpha(a);
			bigSub.alpha(a);
			if (bigT > 3.6f){
				bigTitle.killAndErase();
				bigSub.killAndErase();
				bigTitle = null;
				bigSub = null;
			}
		}
	}

	// *** the victory screen that cracks ***

	public static void victoryCrack( Callback done ){
		if (Game.scene() == null){
			done.call();
			return;
		}
		CrackOverlay o = new CrackOverlay(done);
		o.camera = PixelScene.uiCamera;
		Game.scene().add(o);
	}

	private static class CrackOverlay extends Group {

		private static final float LENGTH = 5.6f;

		private final Callback done;
		private boolean called = false;
		private final ColorBlock dark, gold, white;
		private final RenderedTextBlock title, sub;
		private final Group cracks = new Group();
		private Image eye;
		private float t = 0;
		private boolean froze = false, cracked = false, opened = false;
		private final Random rnd = new Random();

		CrackOverlay( Callback done ){
			this.done = done;
			float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
			dark = new ColorBlock(uiw, uih, 0xFF05040A);
			dark.alpha(0);
			add(dark);
			gold = new ColorBlock(uiw, uih, 0xFFFFE6A0);
			gold.alpha(0);
			add(gold);
			title = PixelScene.renderTextBlock(Messages.get(ThalHud.class, "victory"), 12);
			title.hardlight(0xFFE08A);
			title.setPos((uiw - title.width()) / 2f, uih * 0.38f);
			PixelScene.align(title);
			title.alpha(0);
			add(title);
			sub = PixelScene.renderTextBlock(Messages.get(ThalHud.class, "victory_sub"), 7);
			sub.hardlight(0xFFF4D0);
			sub.setPos((uiw - sub.width()) / 2f, title.bottom() + 4);
			PixelScene.align(sub);
			sub.alpha(0);
			add(sub);
			add(cracks);
			white = new ColorBlock(uiw, uih, 0xFFFFFFFF);
			white.alpha(0);
			add(white);
		}

		private void crack( float x, float y, float ang, float len, int depth ){
			float seg = len / 3f;
			for (int i = 0; i < 3; i++){
				ColorBlock c = new ColorBlock(seg, depth == 0 ? 2 : 1, depth == 0 ? 0xFFEFFBFF : 0xFFAEE2FF);
				c.origin.set(0, 0.5f);
				c.x = x;
				c.y = y;
				c.angle = ang;
				cracks.add(c);
				x += (float) Math.cos(Math.toRadians(ang)) * seg;
				y += (float) Math.sin(Math.toRadians(ang)) * seg;
				ang += rnd.nextInt(50) - 25;
				if (depth < 1 && rnd.nextInt(3) == 0) crack(x, y, ang + 40 - rnd.nextInt(80), seg * 1.5f, depth + 1);
			}
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
			if (t < 1.6f){
				float a = Math.min(1, t / 0.8f);
				dark.alpha(0.55f * a);
				gold.alpha(0.25f * a * (1 - t / 1.6f) + 0.05f);
				title.alpha(a);
				sub.alpha(a);
			} else if (!froze){
				froze = true;
				Sample.INSTANCE.play(Assets.Sounds.DEGRADE, 1f, 0.6f);
				gold.alpha(0);
				title.hardlight(0x8A8A9A);
				sub.hardlight(0x6A6A7A);
			}
			if (t > 2.4f && !cracked){
				cracked = true;
				Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.5f);
				PixelScene.shake(4, 1.2f);
				float cx = uiw / 2f, cy = uih * 0.4f;
				for (int i = 0; i < 9; i++){
					crack(cx, cy, i * 40 + rnd.nextInt(20), Math.max(uiw, uih) * 0.35f, 0);
				}
			}
			if (t > 3.6f && !opened){
				opened = true;
				Sample.INSTANCE.play(Assets.Sounds.BOSS, 1f, 0.6f);
				PixelScene.shake(3, 1.6f);
				try {
					eye = new Image(FinaleAssets.EYE);
					TextureFilm film = new TextureFilm(eye.texture, ThalVisuals.EYE_W, ThalVisuals.EYE_H);
					eye.frame(film.get(0));
					eye.scale.set(3f);
					eye.x = uiw / 2f - ThalVisuals.EYE_W * 1.5f;
					eye.y = uih * 0.4f - ThalVisuals.EYE_H * 1.5f;
					eye.alpha(0);
					add(eye);
				} catch (Exception e){
					eye = null;
				}
			}
			if (eye != null) eye.alpha(GameMath.gate(0, (t - 3.6f) / 0.8f, 1));
			if (t > LENGTH - 0.6f){
				white.alpha(GameMath.gate(0, (t - (LENGTH - 0.6f)) / 0.5f, 1));
			}
			if (t >= LENGTH && !called){
				called = true;
				done.call();
			}
		}
	}
}
