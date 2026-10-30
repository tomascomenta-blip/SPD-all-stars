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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;

import java.util.ArrayList;
import java.util.Random;

//tharog hud: hp bar (carved rib, tribe marks), phase + plates, banners, and the sky
//(icy in p3, red w/ embers in p4)
public class TharogHud extends Group {

	private static TharogHud instance;
	private static final ArrayList<String> queued = new ArrayList<>();

	private final ColorBlock tint;
	private final Group embers = new Group();
	private final Random rnd = new Random();
	private final ColorBlock back, fill;
	private final Image frame;
	private final RenderedTextBlock info;
	private RenderedTextBlock banner;
	private ColorBlock bannerBack;
	private float bannerT = 99, t = 0, s = 1f, x0, y0;

	public TharogHud( float top ){
		instance = this;
		BossHealthBar.hidden = true;
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;

		tint = new ColorBlock(uiw, uih, 0xFFFF3010);
		tint.alpha(0);
		add(tint);
		add(embers);
		for (int i = 0; i < 70; i++){
			ColorBlock b = new ColorBlock(1 + rnd.nextInt(2), 1 + rnd.nextInt(2), 0xFFFFA040);
			b.visible = false;
			b.x = rnd.nextFloat() * uiw;
			b.y = rnd.nextFloat() * uih;
			embers.add(b);
		}

		s = Math.min(1f, (uiw - 8) / 208f);
		x0 = (uiw - 208 * s) / 2f;
		y0 = top;
		back = new ColorBlock(152 * s, 10 * s, 0xFF140C06);
		back.x = x0 + 46 * s;
		back.y = y0 + 16 * s;
		fill = new ColorBlock(152 * s, 10 * s, 0xFF6FC6F0);
		fill.x = back.x;
		fill.y = back.y;
		frame = new Image(ForgottenAssets.BAR_THAROG);
		frame.scale.set(s);
		frame.x = x0;
		frame.y = y0;
		info = PixelScene.renderTextBlock(6);
		add(back);
		add(fill);
		add(frame);
		add(info);
		for (String q : queued) show(q);
		queued.clear();
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

	private void show( String text ){
		if (banner != null){
			banner.killAndErase();
			bannerBack.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		banner = PixelScene.renderTextBlock(text, 9);
		banner.maxWidth((int) Math.min(uiw - 20, 220));
		banner.hardlight(0xFFD27A);
		bannerBack = new ColorBlock(banner.width() + 16, banner.height() + 10, 0xCC120A04);
		bannerBack.x = (uiw - bannerBack.width()) / 2f;
		bannerBack.y = uih * 0.30f;
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
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		Tharog th = Dungeon.level == null ? null : ForgottenBossLevel.tharog(Dungeon.level);
		boolean fight = th != null && Dungeon.level.locked;

		frame.visible = back.visible = fill.visible = info.visible = fight;
		int phase = th == null ? 5 : th.phase;
		if (fight){
			fill.size(152 * s * th.HP / (float) th.HT, 10 * s);
			int c;
			switch (phase){
				case 1: c = 0xFF6FC6F0; break;
				case 2: c = 0xFF7FBF4A; break;
				case 3: c = 0xFFBFE8FF; break;
				default: c = ((int)(t * 4)) % 2 == 0 ? 0xFFFF5A20 : 0xFFC03010;
			}
			fill.color(c);
			String p = Messages.get(TharogHud.class, "phase_" + Math.min(4, phase));
			String txt = th.stunned > 0 ? Messages.get(TharogHud.class, "stunned") : p;
			info.text(txt + "   " + Messages.get(TharogHud.class, "plates", th.plates));
			info.setPos(x0 + 46 * s, frame.y + 40 * s);
			PixelScene.align(info);
		}

		if (phase == 3 && fight){
			tint.color(0xFFCFEFFF);
			tint.alpha(0.12f + 0.03f * (float) Math.sin(t * 2));
		} else if (phase == 4 && fight){
			tint.color(0xFFFF3010);
			tint.alpha(0.16f + 0.06f * (float) Math.abs(Math.sin(t * 1.5f)));
		} else {
			tint.alpha(Math.max(0, tint.alpha() - e));
		}
		boolean burning = phase == 4 && fight;
		for (Gizmo g : embers.membersList()){
			if (!(g instanceof ColorBlock)) continue;
			ColorBlock b = (ColorBlock) g;
			b.visible = burning;
			if (!burning) continue;
			b.y -= (25 + (b.hashCode() % 20)) * e;
			b.x += (float) Math.sin(t * 2 + b.hashCode()) * 10 * e;
			b.alpha(0.5f + 0.5f * (float) Math.abs(Math.sin(t * 3 + b.hashCode())));
			if (b.y < 0){
				b.y = uih + rnd.nextFloat() * 20;
				b.x = rnd.nextFloat() * uiw;
			}
		}

		if (banner != null){
			bannerT += e;
			float a = bannerT < 0.3f ? bannerT / 0.3f : GameMath.gate(0, (4.5f - bannerT) / 0.8f, 1);
			banner.alpha(a);
			bannerBack.alpha(a * 0.8f);
			if (bannerT > 4.5f){
				banner.killAndErase();
				bannerBack.killAndErase();
				banner = null;
				bannerBack = null;
			}
		}
	}
}
