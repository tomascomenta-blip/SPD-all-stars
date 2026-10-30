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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake;

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

//what the awake continent looks like on each screen, and only there: the light turns red and the
//edges of the sky go dark, a column of sacred light stands over the portal (over the fog, so it's
//seen from far off on the plains) and an arrow at the edge of the screen points the way home
public class AwakeSky extends Group {

	private final ColorBlock tint;
	private final Image vignette;
	private final Group marker = new Group();
	private final Image arrow, arrowGlow;

	private float level = 0, t = 0, lookT = 99;
	private int home = -1;
	private boolean beamDone = false;
	private Image beam;

	public AwakeSky(){
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		level = Awakening.on() && !Awakening.sealed() ? 1 : 0;

		tint = new ColorBlock(uiw, uih, 0xFF6A0A06);
		tint.alpha(0);
		add(tint);

		vignette = new Image(vignette());
		vignette.scale.set(uiw / vignette.width, uih / vignette.height);
		vignette.alpha(0);
		add(vignette);

		arrowGlow = new Image(glow()){
			@Override
			public void draw() {
				Blending.setLightMode();
				super.draw();
				Blending.setNormalMode();
			}
		};
		arrowGlow.hardlight(0xFFD070);
		arrowGlow.origin.set(arrowGlow.width / 2f, arrowGlow.height / 2f);
		arrowGlow.scale.set(0.9f);
		marker.add(arrowGlow);
		arrow = new Image(arrow());
		arrow.origin.set(arrow.width / 2f, arrow.height / 2f);
		marker.add(arrow);
		marker.visible = false;
		add(marker);
	}

	private boolean wanted(){
		return Awakening.on() && !Awakening.sealed() && Continent.in();
	}

	@Override
	public void update() {
		super.update();
		float e = Game.elapsed;
		t += e;
		float target = wanted() ? 1 : 0;
		level += Math.max(-e * 0.5f, Math.min(e * 0.5f, target - level));

		//a slow heartbeat in the red
		float beat = (t % 2.6f);
		float pulse = (float) (Math.exp(-beat * 4) + 0.6 * Math.exp(-Math.abs(beat - 0.35) * 8));
		tint.alpha(level * (0.13f + 0.05f * pulse));
		vignette.alpha(level * (0.85f + 0.15f * pulse));

		if (level > 0 && !beamDone) beamDone = beam();
		if (beam != null){
			beam.alpha(level * (0.55f + 0.12f * (float) Math.sin(t * 2.1f) + 0.06f * (float) Math.sin(t * 7.3f)));
			sparks(e);
		}
		pointHome(e);
	}

	// *** the column of light over the portal ***

	//false while there's no scene to put it in yet
	private boolean beam(){
		Level l = Dungeon.level;
		if (l == null) return false;
		int cell = Awakening.portalCell(l);
		if (cell == -1) return true;
		float s = DungeonTilemap.SIZE;
		float cx = (cell % l.width() + 0.5f) * s, bottom = (cell / l.width() + 1) * s;
		beam = new Image(beamTexture()){
			@Override
			public void draw() {
				Blending.setLightMode();
				super.draw();
				Blending.setNormalMode();
			}
		};
		beam.scale.set(1.75f, 8f);
		beam.x = cx - beam.width * beam.scale.x / 2f;
		beam.y = bottom - beam.height * beam.scale.y;
		beam.alpha(0);
		GameScene.effectOverFog(beam);
		baseX = cx;
		baseY = bottom;
		for (int i = 0; i < SPARKS; i++){
			Image sp = new Image(glow()){
				@Override
				public void draw() {
					Blending.setLightMode();
					super.draw();
					Blending.setNormalMode();
				}
			};
			sp.hardlight(i % 3 == 0 ? 0xFFFFFF : 0xFFE08A);
			sp.alpha(0);
			sparks[i] = sp;
			sparkT[i] = -i * 3f / SPARKS;
			GameScene.effectOverFog(sp);
		}
		return true;
	}

	//sparks of light going up the column, one after another
	private static final int SPARKS = 14;
	private final Image[] sparks = new Image[SPARKS];
	private final float[] sparkT = new float[SPARKS], sparkX = new float[SPARKS], sparkV = new float[SPARKS];
	private float baseX, baseY;

	private void sparks( float e ){
		for (int i = 0; i < SPARKS; i++){
			Image sp = sparks[i];
			if (sp == null) return;
			sparkT[i] += e;
			if (sparkT[i] < 0) continue;
			if (sparkT[i] > 3f || sparkV[i] == 0){
				sparkT[i] = 0;
				sparkX[i] = Random.Visual.Float(-6, 6);
				sparkV[i] = Random.Visual.Float(50, 120);
			}
			float k = sparkT[i] / 3f;
			float size = 5 + 3 * (1 - k);
			sp.scale.set(size / sp.width);
			sp.x = baseX + sparkX[i] - size / 2f;
			sp.y = baseY - 8 - sparkT[i] * sparkV[i] - size / 2f;
			sp.alpha(level * (k < 0.15f ? k / 0.15f : 1 - k) * 0.9f);
		}
	}

	// *** which way home ***

	private void pointHome( float e ){
		Level l = Dungeon.level;
		if (level < 0.5f || l == null || Dungeon.local() == null){
			marker.visible = false;
			return;
		}
		lookT += e;
		if (lookT > 1f){
			lookT = 0;
			home = Awakening.homeward(l);
		}
		if (home == -1){
			marker.visible = false;
			return;
		}
		float s = DungeonTilemap.SIZE;
		float wx = (home % l.width() + 0.5f) * s, wy = (home / l.width() + 0.5f) * s;
		Point sp = Camera.main.cameraToScreen(wx, wy);
		Camera ui = PixelScene.uiCamera;
		float ux = (sp.x - ui.x) / ui.zoom, uy = (sp.y - ui.y) / ui.zoom;
		float uiw = ui.width, uih = ui.height;
		float left = 14, right = uiw - 14, top = 40, bottom = uih - 46;
		boolean onScreen = ux >= left && ux <= right && uy >= top && uy <= bottom;
		//the portal on screen is its own beacon
		if (onScreen && Awakening.portalCell(l) == home){
			marker.visible = false;
			return;
		}
		marker.visible = true;
		float px, py, angle;
		if (onScreen){
			//right above it, pointing down
			px = ux;
			py = uy - 12 - 3 * (float) Math.abs(Math.sin(t * 3));
			angle = 90;
		} else {
			float cx = uiw / 2f, cy = uih / 2f, dx = ux - cx, dy = uy - cy;
			float k = Float.MAX_VALUE;
			if (dx > 0) k = Math.min(k, (right - cx) / dx);
			if (dx < 0) k = Math.min(k, (left - cx) / dx);
			if (dy > 0) k = Math.min(k, (bottom - cy) / dy);
			if (dy < 0) k = Math.min(k, (top - cy) / dy);
			if (k == Float.MAX_VALUE) k = 0;
			px = cx + dx * k;
			py = cy + dy * k;
			angle = (float) Math.toDegrees(Math.atan2(dy, dx));
		}
		arrow.x = px - arrow.width / 2f;
		arrow.y = py - arrow.height / 2f;
		arrow.angle = angle;
		arrowGlow.x = px - arrowGlow.width / 2f;
		arrowGlow.y = py - arrowGlow.height / 2f;
		float a = 0.75f + 0.25f * (float) Math.sin(t * 4);
		arrow.alpha(level * a);
		arrowGlow.alpha(level * 0.6f * a);
	}

	// *** pictures, drawn here ***

	private static final String VIGNETTE = "awake_vignette", BEAM = "awake_beam", GLOW = "awake_glow", ARROW = "awake_arrow";

	private static int rgba( int r, int g, int b, int a ){
		return (r << 24) | (g << 16) | (b << 8) | Math.max(0, Math.min(255, a));
	}

	//dark red creeping in from the edges, clear in the middle
	static Object vignette(){
		if (TextureCache.contains(VIGNETTE)) return VIGNETTE;
		int w = 64, h = 36;
		SmartTexture tx = TextureCache.create(VIGNETTE, w, h);
		Pixmap p = tx.bitmap;
		p.setBlending(Pixmap.Blending.None);
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				float dx = (x + 0.5f - w / 2f) / (w / 2f), dy = (y + 0.5f - h / 2f) / (h / 2f);
				float r = (float) Math.sqrt(dx * dx + dy * dy);
				float a = Math.max(0, Math.min(1, (r - 0.5f) / 0.75f));
				p.drawPixel(x, y, rgba(46, 2, 0, (int) (255 * 0.8f * Math.pow(a, 1.4))));
			}
		}
		tx.bitmap(p);
		return VIGNETTE;
	}

	//white light, bright at the foot and fading as it goes up, soft at the sides
	private static Object beamTexture(){
		if (TextureCache.contains(BEAM)) return BEAM;
		int w = 24, h = 128;
		SmartTexture tx = TextureCache.create(BEAM, w, h);
		Pixmap p = tx.bitmap;
		p.setBlending(Pixmap.Blending.None);
		for (int y = 0; y < h; y++){
			float v = (float) Math.pow(y / (float) (h - 1), 0.75f);
			for (int x = 0; x < w; x++){
				float d = Math.abs(x + 0.5f - w / 2f) / (w / 2f);
				float side = (float) Math.pow(Math.max(0, 1 - d), 2.2f);
				if (d < 0.18f) side = Math.min(1, side + 0.35f);
				int g = d < 0.18f ? 250 : 232, b = d < 0.18f ? 230 : 170;
				p.drawPixel(x, y, rgba(255, g, b, (int) (255 * side * v)));
			}
		}
		tx.bitmap(p);
		return BEAM;
	}

	//a soft round glow, white (tinted where it's used)
	static Object glow(){
		if (TextureCache.contains(GLOW)) return GLOW;
		int n = 32;
		SmartTexture tx = TextureCache.create(GLOW, n, n);
		Pixmap p = tx.bitmap;
		p.setBlending(Pixmap.Blending.None);
		for (int y = 0; y < n; y++){
			for (int x = 0; x < n; x++){
				float dx = (x + 0.5f - n / 2f) / (n / 2f), dy = (y + 0.5f - n / 2f) / (n / 2f);
				float r = Math.min(1, (float) Math.sqrt(dx * dx + dy * dy));
				p.drawPixel(x, y, rgba(255, 255, 255, (int) (255 * (1 - r) * (1 - r))));
			}
		}
		tx.bitmap(p);
		return GLOW;
	}

	//an arrowhead pointing right, gold with a dark rim
	private static Object arrow(){
		if (TextureCache.contains(ARROW)) return ARROW;
		int n = 11;
		SmartTexture tx = TextureCache.create(ARROW, n, n);
		tx.filter(Texture.NEAREST, Texture.NEAREST);
		Pixmap p = tx.bitmap;
		p.setBlending(Pixmap.Blending.None);
		p.setColor(0);
		p.fill();
		boolean[] in = new boolean[n * n];
		for (int y = 0; y < n; y++){
			for (int x = 2; x <= 9; x++){
				if (Math.abs(y - 5) * 7 <= (9 - x) * 4 + 3) in[x + y * n] = true;
			}
		}
		for (int y = 0; y < n; y++){
			for (int x = 0; x < n; x++){
				if (in[x + y * n]){
					int c = y == 5 ? rgba(255, 246, 216, 255) : y < 5 ? rgba(255, 224, 138, 255) : rgba(214, 160, 70, 255);
					p.drawPixel(x, y, c);
					continue;
				}
				boolean rim = false;
				for (int dy = -1; dy <= 1 && !rim; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int nx = x + dx, ny = y + dy;
						if (nx >= 0 && ny >= 0 && nx < n && ny < n && in[nx + ny * n]){
							rim = true;
							break;
						}
					}
				}
				if (rim) p.drawPixel(x, y, rgba(42, 20, 12, 230));
			}
		}
		tx.bitmap(p);
		return ARROW;
	}
}
