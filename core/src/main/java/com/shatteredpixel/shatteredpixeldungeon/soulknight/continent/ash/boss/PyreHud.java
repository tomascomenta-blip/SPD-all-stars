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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.boss;

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Random;

//the walking pyre's bar: a medallion with its heart (dims when it gutters, flares when it drinks), a long
//slab of ash for the health, a thin line for the heat, four little fires for the braziers.
//all of it is painted here, pixel by pixel. also the banners, the big title, and the storm on the screen
public class PyreHud extends Group {

	private static final int BW = 208, BH = 34;
	//health and heat slots inside the frame
	private static final int FILL_X = 37, FILL_Y = 11, FILL_W = 162, FILL_H = 8;
	private static final int HEAT_Y = 21, HEAT_H = 3;

	private static PyreHud instance;
	private static final ArrayList<String> queued = new ArrayList<>();
	private static String[] queuedTitle;

	private final Image frame, heart;
	private final ColorBlock back, fill, gloss, heatBack, heat;
	private final ColorBlock[][] pips = new ColorBlock[4][2];
	private final RenderedTextBlock info;
	private RenderedTextBlock banner, title, sub;
	private ColorBlock bannerBack, titleBack;
	private float bannerT = 99, titleT = 99, t = 0, s = 1f, x0, y0;
	private String lastInfo = "";

	public PyreHud( float top ){
		instance = this;
		BossHealthBar.hidden = true;
		float uiw = PixelScene.uiCamera.width;

		s = Math.min(1f, (uiw - 8) / (float) BW);
		x0 = (uiw - BW * s) / 2f;
		y0 = top;

		frame = new Image(frameTexture());
		frame.scale.set(s);
		frame.x = x0;
		frame.y = y0;
		add(frame);

		back = block(FILL_X, FILL_Y, FILL_W, FILL_H, 0xFF140C08);
		fill = block(FILL_X, FILL_Y, FILL_W, FILL_H, 0xFFE86A1C);
		gloss = block(FILL_X, FILL_Y, FILL_W, 1, 0xFFFFB070);
		heatBack = block(FILL_X, HEAT_Y, FILL_W, HEAT_H, 0xFF1A1210);
		heat = block(FILL_X, HEAT_Y, FILL_W, HEAT_H, 0xFFFFE08A);
		add(back);
		add(fill);
		add(gloss);
		add(heatBack);
		add(heat);

		heart = new Image(heartTexture());
		heart.scale.set(s);
		heart.x = x0 + 12 * s;
		heart.y = y0 + 12 * s;
		add(heart);

		for (int i = 0; i < 4; i++){
			pips[i][0] = block(FILL_X + FILL_W - 46 + i * 12, BH + 2, 4, 5, 0xFFE86A1C);
			pips[i][1] = block(FILL_X + FILL_W - 45 + i * 12, BH + 1, 2, 3, 0xFFFFE08A);
			add(pips[i][0]);
			add(pips[i][1]);
		}

		info = PixelScene.renderTextBlock(6);
		add(info);

		for (String q : queued) showBanner(q);
		queued.clear();
		if (queuedTitle != null) showTitle(queuedTitle[0], queuedTitle[1]);
		queuedTitle = null;
	}

	private ColorBlock block( float x, float y, float w, float h, int color ){
		ColorBlock b = new ColorBlock(w * s, h * s, color);
		b.x = x0 + x * s;
		b.y = y0 + y * s;
		return b;
	}

	@Override
	public void destroy() {
		super.destroy();
		if (instance == this){
			instance = null;
			BossHealthBar.hidden = false;
		}
	}

	// *** painting the frame ***

	private static final String FRAME_KEY = "pyre_hud_frame";
	private static final String HEART_KEY = "pyre_hud_heart";

	private static int rgba( int argb ){
		return (argb << 8) | (argb >>> 24);
	}

	private static void px( Pixmap p, int x, int y, int argb ){
		p.drawPixel(x, y, rgba(argb));
	}

	private static void rect( Pixmap p, int x, int y, int w, int h, int argb ){
		p.setColor(rgba(argb));
		p.fillRectangle(x, y, w, h);
	}

	private static final int OUTLINE = 0xFF120A08, STONE_D = 0xFF3A322E, STONE = 0xFF564A44, STONE_L = 0xFF7A6C62,
			EMBER_D = 0xFF7A240A, EMBER = 0xFFE0661A, EMBER_L = 0xFFFFA63A;

	private static Object frameTexture(){
		if (TextureCache.contains(FRAME_KEY)) return FRAME_KEY;
		SmartTexture tx = TextureCache.create(FRAME_KEY, BW, BH + 8);
		tx.filter(Texture.NEAREST, Texture.NEAREST);
		Pixmap p = tx.bitmap;
		p.setColor(0);
		p.fill();

		//the slab
		rect(p, 30, 7, 176, 21, OUTLINE);
		rect(p, 31, 8, 174, 19, STONE_D);
		rect(p, 31, 8, 174, 1, STONE_L);
		rect(p, 31, 9, 174, 1, STONE);
		rect(p, 31, 25, 174, 1, OUTLINE);
		//the slots get their bars on top
		rect(p, FILL_X - 1, FILL_Y - 1, FILL_W + 2, 1, OUTLINE);
		rect(p, FILL_X - 1, FILL_Y + FILL_H, FILL_W + 2, 1, OUTLINE);
		rect(p, FILL_X - 1, FILL_Y, 1, FILL_H, OUTLINE);
		rect(p, FILL_X + FILL_W, FILL_Y, 1, FILL_H, OUTLINE);
		rect(p, FILL_X - 1, HEAT_Y + HEAT_H, FILL_W + 2, 1, OUTLINE);
		rect(p, FILL_X - 1, HEAT_Y, 1, HEAT_H, OUTLINE);
		rect(p, FILL_X + FILL_W, HEAT_Y, 1, HEAT_H, OUTLINE);
		//embers stuck in the stone, every 18 px
		for (int x = 44; x < 200; x += 18){
			px(p, x, 8, EMBER_D);
			px(p, x + 1, 8, EMBER);
			px(p, x + 9, 25, EMBER_D);
		}
		//cracks glowing on the edge
		int[][] cracks = {{60, 26}, {61, 27}, {120, 26}, {121, 26}, {122, 27}, {170, 26}, {96, 7}, {97, 6}, {150, 7}};
		for (int[] c : cracks) px(p, c[0], c[1], EMBER);

		//a little flame at the end of the slab
		int[][] flame = {
				{205, 12, EMBER_D}, {205, 13, EMBER}, {205, 14, EMBER}, {205, 15, EMBER_D}, {205, 16, EMBER}, {205, 17, EMBER_D},
				{206, 13, EMBER}, {206, 14, EMBER_L}, {206, 15, EMBER_L}, {206, 16, EMBER},
				{207, 14, EMBER_L}, {207, 15, EMBER}};
		for (int[] f : flame) px(p, f[0], f[1], f[2]);

		//the medallion
		p.setColor(rgba(OUTLINE));
		p.fillCircle(17, 17, 17);
		p.setColor(rgba(STONE));
		p.fillCircle(17, 17, 15);
		p.setColor(rgba(STONE_D));
		p.fillCircle(18, 18, 13);
		p.setColor(rgba(OUTLINE));
		p.fillCircle(17, 17, 11);
		p.setColor(rgba(0xFF1E1410));
		p.fillCircle(17, 17, 10);
		//light on the top left of the ring
		for (int a = 140; a < 260; a += 6){
			double r = Math.toRadians(a);
			px(p, 17 + (int) Math.round(Math.cos(r) * 14), 17 + (int) Math.round(Math.sin(r) * 14), STONE_L);
		}
		//four embers on the ring, like the braziers
		int[][] studs = {{17, 2}, {32, 17}, {17, 32}, {2, 17}};
		for (int[] st : studs){
			px(p, st[0], st[1], EMBER);
			px(p, st[0] + 1, st[1], EMBER_D);
		}
		return FRAME_KEY;
	}

	private static final String[] HEART = {
			"..OO...OO..",
			".OyyO.OyyO.",
			"OyWWyOyyddO",
			"OyWyyyyyydO",
			"OyyyyyyyddO",
			".OyyyyyddO.",
			"..OyyyddO..",
			"...OyddO...",
			"....OdO....",
			".....O....."};

	private static Object heartTexture(){
		if (TextureCache.contains(HEART_KEY)) return HEART_KEY;
		SmartTexture tx = TextureCache.create(HEART_KEY, 11, 10);
		tx.filter(Texture.NEAREST, Texture.NEAREST);
		Pixmap p = tx.bitmap;
		p.setColor(0);
		p.fill();
		for (int y = 0; y < HEART.length; y++){
			for (int x = 0; x < HEART[y].length(); x++){
				switch (HEART[y].charAt(x)){
					case 'O': px(p, x, y, 0xFF6A3008); break;
					case 'y': px(p, x, y, 0xFFF8C860); break;
					case 'd': px(p, x, y, 0xFFD89A30); break;
					case 'W': px(p, x, y, 0xFFFFF8D8); break;
				}
			}
		}
		return HEART_KEY;
	}

	// *** banners ***

	public static void banner( final String text ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.showBanner(text);
				else queued.add(text);
			}
		});
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

	private void showBanner( String text ){
		if (banner != null){
			banner.killAndErase();
			bannerBack.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		banner = PixelScene.renderTextBlock(text, 9);
		banner.maxWidth((int) Math.min(uiw - 20, 220));
		banner.hardlight(0xFFC27A);
		bannerBack = new ColorBlock(banner.width() + 16, banner.height() + 10, 0xCC140C08);
		bannerBack.x = (uiw - bannerBack.width()) / 2f;
		bannerBack.y = uih * 0.34f;
		banner.setPos(bannerBack.x + 8, bannerBack.y + 5);
		PixelScene.align(banner);
		add(bannerBack);
		add(banner);
		bannerT = 0;
	}

	private void showTitle( String name, String subtitle ){
		if (title != null){
			title.killAndErase();
			sub.killAndErase();
			titleBack.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		title = PixelScene.renderTextBlock(name, 12);
		title.hardlight(0xFFB04A);
		sub = PixelScene.renderTextBlock(subtitle, 7);
		sub.hardlight(0xD8C8B8);
		float w = Math.max(title.width(), sub.width()) + 24;
		titleBack = new ColorBlock(w, title.height() + sub.height() + 16, 0xDD100806);
		titleBack.x = (uiw - titleBack.width()) / 2f;
		titleBack.y = uih * 0.14f;
		title.setPos((uiw - title.width()) / 2f, titleBack.y + 6);
		sub.setPos((uiw - sub.width()) / 2f, title.bottom() + 4);
		PixelScene.align(title);
		PixelScene.align(sub);
		add(titleBack);
		add(title);
		add(sub);
		titleT = 0;
	}

	// *** every frame ***

	//the render thread looks the pyre and the braziers up now and then instead of walking the mob list every frame
	private static AshPyre seenPyre;
	private static final ArrayList<PyreBrazier> seenBraziers = new ArrayList<>();
	private static Object seenLevel;
	private static float lookT = 99;

	private static void look( float elapsed ){
		lookT += elapsed;
		if (seenLevel == Dungeon.level && lookT < 0.5f) return;
		lookT = 0;
		seenLevel = Dungeon.level;
		seenPyre = null;
		seenBraziers.clear();
		if (!(Dungeon.level instanceof AshBossLevel)) return;
		try {
			seenPyre = AshBossLevel.pyre(Dungeon.level);
			seenBraziers.addAll(((AshBossLevel) Dungeon.level).braziers());
		} catch (ConcurrentModificationException e){
			//a mob came or went right now, next time
			lookT = 99;
		}
	}

	@Override
	public void update() {
		super.update();
		float e = Game.elapsed;
		t += e;
		look(e);
		AshPyre p = seenPyre;
		boolean show = p != null && p.isAlive() && p.intro >= 4 && Dungeon.level.locked;

		frame.visible = heart.visible = back.visible = fill.visible = gloss.visible = info.visible = show;
		heatBack.visible = heat.visible = show;
		for (ColorBlock[] pip : pips){
			pip[0].visible = pip[1].visible = show;
		}

		if (show){
			float hp = Math.max(0, p.HP / (float) p.HT);
			fill.size(FILL_W * s * hp, FILL_H * s);
			gloss.size(FILL_W * s * hp, s);
			int c;
			switch (p.mode){
				case AshPyre.DARK: case AshPyre.DYING: c = 0xFF6A5E56; break;
				case AshPyre.GUTTER: c = ((int) (t * 3)) % 2 == 0 ? 0xFF9A4A2A : 0xFF7A3A22; break;
				case AshPyre.CHANNEL: case AshPyre.FLARE: c = ((int) (t * 6)) % 2 == 0 ? 0xFFFFD27A : 0xFFF8A040; break;
				default:
					switch (p.phase){
						case 1: c = 0xFFE86A1C; break;
						case 2: c = 0xFFC8582A; break;
						case 3: c = 0xFFB8341A; break;
						default: c = ((int) (t * 4)) % 2 == 0 ? 0xFFFFD27A : 0xFFF8A040;
					}
			}
			fill.color(c);

			float h = Math.max(0, Math.min(100, p.heat)) / 100f;
			if (p.phase >= 4) h = 1f;
			heat.size(FILL_W * s * h, HEAT_H * s);
			int hc = 0xFFFFE08A;
			if (p.mode == AshPyre.GUTTER || p.mode == AshPyre.DARK || (h < 0.25f && ((int) (t * 4)) % 2 == 0)) hc = 0xFFE0401A;
			heat.color(hc);

			//the heart in the medallion
			float glow;
			if (p.mode == AshPyre.DARK || p.mode == AshPyre.DYING) glow = 0.3f + 0.05f * (float) Math.sin(t * 2);
			else if (p.mode == AshPyre.GUTTER) glow = 0.45f + 0.25f * Math.abs((float) Math.sin(t * 5));
			else if (p.mode == AshPyre.CHANNEL || p.mode == AshPyre.FLARE || p.blazing > 0) glow = 1f;
			else glow = 0.7f + 0.3f * h * (0.8f + 0.2f * (float) Math.sin(t * 3));
			heart.rm = heart.gm = heart.bm = glow;
			float beat = (p.mode == AshPyre.DARK || p.mode == AshPyre.DYING) ? 1f : 1f + 0.08f * Math.max(0, (float) Math.sin(t * (p.phase >= 4 ? 7 : 4)));
			heart.scale.set(s * beat);
			heart.x = x0 + 17 * s - 5.5f * s * beat;
			heart.y = y0 + 17 * s - 5f * s * beat;

			//the braziers
			ArrayList<PyreBrazier> list = seenBraziers;
			for (int i = 0; i < 4; i++){
				boolean exists = i < list.size();
				boolean lit = exists && list.get(i).lit;
				pips[i][0].color(lit ? (((int) (t * 5 + i)) % 2 == 0 ? 0xFFE86A1C : 0xFFF08A2A) : 0xFF4A403A);
				pips[i][1].visible = lit;
				pips[i][0].visible = exists;
			}

			String txt = Messages.get(PyreHud.class, "phase_" + Math.min(4, p.phase));
			switch (p.mode){
				case AshPyre.GUTTER: txt += "   " + Messages.get(PyreHud.class, "gutter"); break;
				case AshPyre.CHANNEL: txt += "   " + Messages.get(PyreHud.class, "drinking"); break;
				case AshPyre.DARK: txt += "   " + Messages.get(PyreHud.class, "dark"); break;
				default:
					if (p.blazing > 0) txt += "   " + Messages.get(PyreHud.class, "blazing");
					else if (((AshBossLevel) Dungeon.level).storm > 0) txt += "   " + Messages.get(PyreHud.class, "storm");
					else if (p.stunned > 0) txt += "   " + Messages.get(PyreHud.class, "stunned");
			}
			if (!txt.equals(lastInfo)){
				lastInfo = txt;
				info.text(txt);
			}
			info.setPos(x0 + FILL_X * s, y0 + (BH + 1) * s);
			PixelScene.align(info);
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
		if (title != null){
			titleT += e;
			float a = titleT < 0.5f ? titleT / 0.5f : GameMath.gate(0, (4.5f - titleT) / 0.8f, 1);
			title.alpha(a);
			sub.alpha(a);
			titleBack.alpha(a * 0.85f);
			if (titleT > 4.5f){
				title.killAndErase();
				sub.killAndErase();
				titleBack.killAndErase();
				title = null;
				sub = null;
				titleBack = null;
			}
		}
	}

	// *** the storm, over the map ***

	public static Group storm(){
		return new Storm();
	}

	//grey tint and ash blowing sideways while the storm lasts; ash falling slowly when the pyre goes out
	public static class Storm extends Group {

		private final ColorBlock tint;
		private final Group flakes = new Group();
		private final Random rnd = new Random();
		private float t = 0;

		public Storm(){
			float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
			tint = new ColorBlock(uiw, uih, 0xFF5A4E46);
			tint.alpha(0);
			add(tint);
			add(flakes);
			for (int i = 0; i < 140; i++){
				int size = 1 + rnd.nextInt(2);
				int shade = rnd.nextInt(4);
				int color = shade == 0 ? 0xFFFFA040 : (shade == 1 ? 0xFF8A7E74 : (shade == 2 ? 0xFFB0A498 : 0xFF6A5E56));
				ColorBlock b = new ColorBlock(size, size, color);
				b.x = rnd.nextFloat() * uiw;
				b.y = rnd.nextFloat() * uih;
				b.alpha(0);
				flakes.add(b);
			}
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
			AshBossLevel L = Dungeon.level instanceof AshBossLevel ? (AshBossLevel) Dungeon.level : null;
			look(e);
			AshPyre p = L == null ? null : seenPyre;
			boolean storming = L != null && L.storm > 0;
			boolean falling = p != null && p.mode == AshPyre.DYING;
			float want = storming ? 0.38f + 0.04f * (float) Math.sin(t * 3) : (falling ? 0.12f : 0f);
			tint.alpha(tint.alpha() + GameMath.gate(-e, want - tint.alpha(), e * 0.8f));
			float level = storming ? 1f : (falling ? 0.6f : 0f);
			int i = 0;
			for (Gizmo g : flakes.membersList()){
				if (!(g instanceof ColorBlock)) continue;
				ColorBlock b = (ColorBlock) g;
				i++;
				float target = level * (0.5f + 0.5f * (i % 3) / 2f);
				b.alpha(b.alpha() + GameMath.gate(-e, target - b.alpha(), e));
				if (b.alpha() <= 0.01f) continue;
				if (storming){
					b.x += (90 + (i % 7) * 12) * e;
					b.y += (float) Math.sin(t * 2 + i) * 18 * e + 10 * e;
				} else {
					b.y += (14 + (i % 5) * 4) * e;
					b.x += (float) Math.sin(t + i) * 6 * e;
				}
				if (b.x > uiw) b.x -= uiw;
				if (b.y > uih) b.y -= uih;
				if (b.x < 0) b.x += uiw;
			}
		}
	}
}
