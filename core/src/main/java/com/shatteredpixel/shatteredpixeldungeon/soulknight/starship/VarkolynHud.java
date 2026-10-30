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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.AndroidQuest;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
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

//varkolyn queen bar: a ship monitor w/ her scanned shape, green while she fights, red while she hides.
//also the alarm + ship darkness, the motion radar while she hides (rough position, nests, other heroes),
//big title, banners
public class VarkolynHud extends Group {

	//hp area in varkolyn_bar.png. yes, 60/17/132/7 again
	public static int FILL_X = 60, FILL_Y = 17, FILL_W = 132, FILL_H = 7;
	private static final float RADAR_RANGE = 12f;

	private static VarkolynHud instance;
	private static String[] queuedTitle;
	private static String queuedBanner;
	private static boolean queuedLow;

	private final ColorBlock dark, alarm;
	private final ColorBlock back, fill;
	private final Image frame, alert;
	private final RenderedTextBlock info;
	private RenderedTextBlock title, sub, banner;
	private ColorBlock titleBack, bannerBack;
	private float titleT = 99, bannerT = 99, t = 0, s = 1f, x0, y0;

	private final Image radar, sweep;
	private final TextureFilm radarFilm, blipFilm;
	private final ArrayList<Image> blips = new ArrayList<>();
	private float rx, ry, noiseT = 0, pingT = 99;
	private int noiseX = 0, noiseY = 0;

	public VarkolynHud( float top ){
		instance = this;
		BossHealthBar.hidden = true;
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;

		dark = new ColorBlock(uiw, uih, 0xFF020408);
		dark.alpha(0);
		add(dark);
		alarm = new ColorBlock(uiw, uih, 0xFFFF1010);
		alarm.alpha(0);
		add(alarm);

		s = Math.min(1f, (uiw - 8) / 208f);
		x0 = (uiw - 208 * s) / 2f;
		y0 = top;
		back = new ColorBlock(FILL_W * s, FILL_H * s, 0xFF041008);
		back.x = x0 + FILL_X * s;
		back.y = y0 + FILL_Y * s;
		fill = new ColorBlock(FILL_W * s, FILL_H * s, 0xFF40E060);
		fill.x = back.x;
		fill.y = back.y;
		frame = new Image(VarkolynAssets.BAR);
		frame.scale.set(s);
		frame.x = x0;
		frame.y = y0;
		alert = new Image(VarkolynAssets.BAR_ALERT);
		alert.scale.set(s);
		alert.x = x0;
		alert.y = y0;
		alert.visible = false;
		add(back);
		add(fill);
		add(frame);
		add(alert);
		info = PixelScene.renderTextBlock(6);
		add(info);

		radar = new Image(VarkolynAssets.RADAR);
		radarFilm = new TextureFilm(radar.texture, 72, 72);
		radar.frame(radarFilm.get(0));
		rx = uiw - 76;
		ry = top + 46;
		radar.x = rx;
		radar.y = ry;
		sweep = new Image(VarkolynAssets.RADAR);
		sweep.frame(radarFilm.get(1));
		sweep.x = rx;
		sweep.y = ry;
		Image probe = new Image(VarkolynAssets.BLIPS);
		blipFilm = new TextureFilm(probe.texture, 8, 8);
		add(radar);
		add(sweep);
		radar.visible = sweep.visible = false;

		if (queuedTitle != null) showTitle(queuedTitle[0], queuedTitle[1]);
		queuedTitle = null;
		if (queuedBanner != null) showBanner(queuedBanner, queuedLow);
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
		banner(text, false);
	}

	//low = under the middle of the screen, so it doesn't cover the game's own "boss slain"
	public static void banner( final String text, final boolean low ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.showBanner(text, low);
				else {
					queuedBanner = text;
					queuedLow = low;
				}
			}
		});
	}

	//nest laid -> radar pings. ping!
	public static void ping(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.pingT = 0;
			}
		});
	}

	private void showTitle( String name, String subtitle ){
		//title replaces a banner still on screen
		if (banner != null){
			banner.killAndErase();
			bannerBack.killAndErase();
			banner = null;
			bannerBack = null;
		}
		if (title != null){
			title.killAndErase();
			sub.killAndErase();
			titleBack.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		title = PixelScene.renderTextBlock(name, 12);
		title.hardlight(0xB8FF9C);
		sub = PixelScene.renderTextBlock(subtitle, 7);
		sub.hardlight(0xD8A8F0);
		float w = Math.max(title.width(), sub.width()) + 24;
		titleBack = new ColorBlock(w, title.height() + sub.height() + 16, 0xDD060A08);
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

	private void showBanner( String text, boolean low ){
		if (banner != null){
			banner.killAndErase();
			bannerBack.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		banner = PixelScene.renderTextBlock(text, 9);
		banner.hardlight(0xB8FF9C);
		bannerBack = new ColorBlock(banner.width() + 16, banner.height() + 8, 0xCC060A08);
		bannerBack.x = (uiw - bannerBack.width()) / 2f;
		bannerBack.y = uih * (low ? 0.62f : 0.36f);
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
		VarkolynLeader q = Dungeon.level == null ? null : StarshipBossLevel.queen(Dungeon.level);
		boolean fight = q != null && q.fighting() && Dungeon.level.locked;
		boolean flight = fight && q.fleeing();

		frame.visible = back.visible = fill.visible = info.visible = fight;
		alert.visible = fight && flight;
		if (fight){
			fill.size(FILL_W * s * q.HP / (float) q.HT, FILL_H * s);
			int c;
			if (flight) c = ((int) (t * 3)) % 2 == 0 ? 0xFFE03030 : 0xFF901818;
			else if (q.holding()) c = ((int) (t * 6)) % 2 == 0 ? 0xFFFFE070 : 0xFF40E060;
			else if (q.molted) c = 0xFFB0F040;
			else c = 0xFF40E060;
			fill.color(c);

			String txt;
			if (flight) txt = Messages.get(VarkolynHud.class, "flight", q.nestsLeftToLay(), q.nestCells().size());
			else if (q.inLair()) txt = Messages.get(VarkolynHud.class, q.molted ? "lair_molted" : "lair");
			else txt = Messages.get(VarkolynHud.class, "bridge");
			if (q.inLair() && Dungeon.level instanceof StarshipBossLevel){
				txt += "   " + Messages.get(VarkolynHud.class, "airlock_" + ((StarshipBossLevel) Dungeon.level).airlock);
			}
			info.text(txt);
			info.setPos(x0 + FILL_X * s, frame.y + 40 * s);
			PixelScene.align(info);
		}

		//alarm while she wakes up and while she hides, darkness only while she hides
		boolean waking = q != null && q.intro >= 1 && q.intro < 4;
		float alarmTarget = waking || flight ? 0.10f + 0.08f * (float) Math.sin(t * 6) : 0f;
		alarm.alpha(alarm.alpha() + GameMath.gate(-e, alarmTarget - alarm.alpha(), e));
		float darkTarget = flight ? 0.45f : 0f;
		dark.alpha(dark.alpha() + GameMath.gate(-e * 0.7f, darkTarget - dark.alpha(), e * 0.7f));

		radar(q, flight, e);

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

	//motion radar, centred on this device's hero
	private void radar( VarkolynLeader q, boolean on, float e ){
		radar.visible = sweep.visible = on;
		for (Image b : blips) b.killAndErase();
		blips.clear();
		if (!on) return;
		Hero me = Dungeon.local();
		if (me == null) me = Dungeon.hero;
		if (me == null) return;
		sweep.frame(radarFilm.get(1 + ((int) (t * 8)) % 8));
		pingT += e;

		//her blip wanders a bit, the radar only knows roughly where she is
		noiseT -= e;
		if (noiseT <= 0){
			noiseT = 1.2f;
			//the ship tracks her exactly if M-4 was shut down
			noiseX = AndroidQuest.helps() ? 0 : Random.IntRange(-2, 2);
			noiseY = AndroidQuest.helps() ? 0 : Random.IntRange(-2, 2);
		}
		int w = Dungeon.level.width();
		if (((int) (t * 2)) % 2 == 0 || pingT < 1f){
			blip(me.pos, q.pos % w + noiseX, q.pos / w + noiseY, 0, w);
		}
		for (int c : q.nestCells()) blip(me.pos, c % w, c / w, 1, w);
		for (Hero h : Party.living()){
			if (h != me) blip(me.pos, h.pos % w, h.pos / w, 2, w);
		}
	}

	private void blip( int center, int x, int y, int kind, int w ){
		float dx = x - center % w, dy = y - center / w;
		float d = (float) Math.sqrt(dx * dx + dy * dy);
		float r = 30f;
		float k = r / RADAR_RANGE;
		if (d > RADAR_RANGE){
			dx = dx / d * RADAR_RANGE;
			dy = dy / d * RADAR_RANGE;
		}
		Image b = new Image(VarkolynAssets.BLIPS);
		b.frame(blipFilm.get(kind));
		b.x = rx + 36 + dx * k - 4;
		b.y = ry + 36 + dy * k - 4;
		if (kind == 1 && pingT < 1f) b.scale.set(1.5f);
		blips.add(b);
		add(b);
	}
}
