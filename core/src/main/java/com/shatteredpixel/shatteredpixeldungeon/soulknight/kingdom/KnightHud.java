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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;

//dark grand knight bar: his coat of arms + a sword whose blade is the hp bar. goes gold while the banner's up,
//blade cracks in acts 3 and 4. also the big title when he stands up, and the hall going dark in act 4
public class KnightHud extends Group {

	//hp area inside knight_bar.png. same numbers as the priest bar, on purpose
	public static int FILL_X = 60, FILL_Y = 17, FILL_W = 132, FILL_H = 7;

	private static KnightHud instance;
	private static String[] queued;

	private final ColorBlock dark;
	private final ColorBlock back, fill;
	private final Image frame, cracks;
	private final RenderedTextBlock info;
	private RenderedTextBlock title, sub;
	private ColorBlock titleBack;
	private float titleT = 99, t = 0, s = 1f, x0, y0;
	private int crackLevel = -1;

	public KnightHud( float top ){
		instance = this;
		BossHealthBar.hidden = true;
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;

		dark = new ColorBlock(uiw, uih, 0xFF0A0414);
		dark.alpha(0);
		add(dark);

		s = Math.min(1f, (uiw - 8) / 208f);
		x0 = (uiw - 208 * s) / 2f;
		y0 = top;
		back = new ColorBlock(FILL_W * s, FILL_H * s, 0xFF120A0C);
		back.x = x0 + FILL_X * s;
		back.y = y0 + FILL_Y * s;
		fill = new ColorBlock(FILL_W * s, FILL_H * s, 0xFFC0303A);
		fill.x = back.x;
		fill.y = back.y;
		frame = new Image(ThroneAssets.BAR);
		frame.scale.set(s);
		frame.x = x0;
		frame.y = y0;
		cracks = new Image(ThroneAssets.BAR_CRACKS);
		cracks.frame(0, 0, 208, 40);
		cracks.scale.set(s);
		cracks.x = x0;
		cracks.y = y0;
		cracks.visible = false;
		info = PixelScene.renderTextBlock(6);
		add(back);
		add(fill);
		add(frame);
		add(cracks);
		add(info);
		if (queued != null) show(queued[0], queued[1]);
		queued = null;
	}

	@Override
	public void destroy() {
		super.destroy();
		if (instance == this){
			instance = null;
			BossHealthBar.hidden = false;
		}
	}

	//big title w/ his name when he stands up
	public static void title( final String name, final String subtitle ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.show(name, subtitle);
				else queued = new String[]{name, subtitle};
			}
		});
	}

	private void show( String name, String subtitle ){
		if (title != null){
			title.killAndErase();
			sub.killAndErase();
			titleBack.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		title = PixelScene.renderTextBlock(name, 12);
		title.hardlight(0xFFE7A8);
		sub = PixelScene.renderTextBlock(subtitle, 7);
		sub.hardlight(0xC9B7E8);
		float w = Math.max(title.width(), sub.width()) + 24;
		titleBack = new ColorBlock(w, title.height() + sub.height() + 16, 0xDD0C0610);
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

	@Override
	public void update() {
		super.update();
		float e = Game.elapsed;
		t += e;
		DarkGrandKnight k = Dungeon.level == null ? null : KingdomBossLevel.knight(Dungeon.level);
		boolean fight = k != null && k.fighting() && Dungeon.level.locked;

		frame.visible = back.visible = fill.visible = info.visible = fight;
		if (fight){
			fill.size(FILL_W * s * k.HP / (float) k.HT, FILL_H * s);
			int c;
			if (k.bannerUp()) c = ((int)(t * 3)) % 2 == 0 ? 0xFFE8C040 : 0xFFC89A20;
			else if (k.act >= 4) c = ((int)(t * 4)) % 2 == 0 ? 0xFF8A3AC0 : 0xFF5A1E90;
			else c = 0xFFC0303A;
			fill.color(c);
			int cl = k.act >= 4 ? 1 : k.act == 3 ? 0 : -1;
			if (cl != crackLevel){
				crackLevel = cl;
				cracks.visible = cl >= 0;
				if (cl >= 0) cracks.frame(0, 40 * cl, 208, 40);
			}
			String txt = Messages.get(KnightHud.class, "act_" + Math.max(1, Math.min(4, k.act)));
			if (k.stunned > 0) txt += "   " + Messages.get(KnightHud.class, "stunned");
			else if (k.bannerUp()) txt += "   " + Messages.get(KnightHud.class, "banner");
			info.text(txt);
			info.setPos(x0 + FILL_X * s, frame.y + 40 * s);
			PixelScene.align(info);
		} else {
			cracks.visible = false;
		}

		//hall goes dark when the shadow comes, w/ a slow flicker
		float target = fight && k.act >= 4 ? 0.32f + 0.05f * (float) Math.sin(t * 1.7f) : 0f;
		dark.alpha(dark.alpha() + GameMath.gate(-e, target - dark.alpha(), e));

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
	}
}
