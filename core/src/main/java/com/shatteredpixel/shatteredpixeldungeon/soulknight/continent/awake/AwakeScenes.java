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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.FinaleAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalParts;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;

import java.util.ArrayList;

//the two scenes of the true ending, played on each screen while the game waits (WakeCall, PortalSeal):
//the continent waking up, and the essence closing the portal
public class AwakeScenes {

	public static void wake( Callback done ){
		show(new Wake(done), done);
	}

	public static void seal( int drops, Callback done ){
		show(new Seal(drops, done), done);
	}

	private static void show( Group g, Callback done ){
		if (Game.scene() == null){
			done.call();
			return;
		}
		g.camera = PixelScene.uiCamera;
		Game.scene().add(g);
	}

	//fade in over 'in' seconds from 'from', hold, fade out over 'out' ending at 'to'
	private static float window( float t, float from, float in, float to, float out ){
		if (t < from || t > to) return 0;
		return Math.min(GameMath.gate(0, (t - from) / in, 1), GameMath.gate(0, (to - t) / out, 1));
	}

	private static class Light extends Image {

		Light( int color ){
			super(AwakeSky.glow());
			hardlight(color);
			origin.set(width / 2f, height / 2f);
		}

		void at( float x, float y, float size ){
			scale.set(size / width);
			this.x = x - width / 2f;
			this.y = y - height / 2f;
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	// *** the continent wakes up ***

	private static class Wake extends Group {

		private static final float LENGTH = 5.4f;

		private final Callback done;
		private boolean called = false;
		private final ColorBlock red, band;
		private final Image edges;
		private final RenderedTextBlock title, sub;
		private final float titleX, titleY;
		private float t = 0;
		private int step = 0;

		Wake( Callback done ){
			this.done = done;
			float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
			red = new ColorBlock(uiw, uih, 0xFF8A0E06);
			red.alpha(0);
			add(red);
			edges = new Image(AwakeSky.vignette());
			edges.scale.set(uiw / edges.width, uih / edges.height);
			edges.alpha(0);
			add(edges);

			title = PixelScene.renderTextBlock(Messages.get(Awakening.class, "wake_title"), uiw >= 300 ? 16 : 12);
			title.hardlight(0xFF5A3A);
			sub = PixelScene.renderTextBlock(Messages.get(Awakening.class, "wake_sub"), 8);
			sub.maxWidth((int) Math.min(uiw - 24, 300));
			sub.hardlight(0xF4CDB4);
			band = new ColorBlock(uiw, title.height() + sub.height() + 26, 0xFF0A0202);
			band.y = uih * 0.33f - 10;
			band.alpha(0);
			add(band);
			titleX = (uiw - title.width()) / 2f;
			titleY = uih * 0.33f;
			title.setPos(titleX, titleY);
			PixelScene.align(title);
			title.alpha(0);
			add(title);
			sub.setPos((uiw - sub.width()) / 2f, title.bottom() + 6);
			PixelScene.align(sub);
			sub.alpha(0);
			add(sub);
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (step == 0){
				step++;
				Sample.INSTANCE.play(Assets.Sounds.BOSS, 1f, 0.5f);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.4f);
				PixelScene.shake(7, 3.6f);
			} else if (step == 1 && t > 1.2f){
				step++;
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.45f);
			} else if (step == 2 && t > 2.5f){
				step++;
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.35f);
				PixelScene.shake(4, 1.6f);
			}

			//a flash of red, then it beats and settles into the sky
			float beat = t % 1.2f;
			float a = t < 0.35f ? 0.62f * t / 0.35f : 0.24f + 0.38f * (float) Math.exp(-(t - 0.35f) * 1.6f);
			a += 0.12f * (float) Math.exp(-beat * 5f);
			a *= GameMath.gate(0, (LENGTH - t) / 1.0f, 1);
			red.alpha(a);
			edges.alpha(GameMath.gate(0, t / 0.6f, 1) * GameMath.gate(0, (LENGTH - t) / 1.0f, 1));

			float ta = window(t, 0.6f, 0.6f, LENGTH - 0.6f, 0.8f);
			float sa = window(t, 1.4f, 0.6f, LENGTH - 0.6f, 0.8f);
			title.alpha(ta);
			sub.alpha(sa);
			band.alpha(0.75f * Math.max(ta, sa));
			//the letters shake with the ground for a while
			float j = t < 2.6f ? 1.5f * (1 - t / 2.6f) : 0;
			title.setPos(titleX + Math.round((float) Math.sin(t * 53) * j), titleY + Math.round((float) Math.cos(t * 41) * j));

			if (t >= LENGTH && !called){
				called = true;
				killAndErase();
				done.call();
			}
		}
	}

	// *** the portal closes ***

	private static class Seal extends Group {

		private static final int LINES = 4;
		private static final float SPACING = 1.6f;

		private final Callback done;
		private boolean called = false;
		private final int n;
		private final float cx, cy;
		private final ColorBlock dark, white;
		private final Light halo;
		private final Image portal;
		private final TextureFilm film;
		private final ArrayList<Image> drops = new ArrayList<>();
		private final ArrayList<Light> sparks = new ArrayList<>();
		private final boolean[] gone;
		private final ArrayList<RenderedTextBlock> lines = new ArrayList<>();
		private final float closeAt, textAt, length;
		private float t = 0;
		private boolean closed = false;

		Seal( int count, Callback done ){
			this.done = done;
			n = Math.max(1, Math.min(10, count));
			gone = new boolean[n];
			float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
			cx = uiw / 2f;
			cy = uih * 0.32f;
			closeAt = 2.6f + n * 0.14f + 1.0f;
			textAt = closeAt + 1.1f;
			length = textAt + LINES * SPACING + 2.4f;

			dark = new ColorBlock(uiw, uih, 0xFF05040A);
			dark.alpha(0);
			add(dark);

			halo = new Light(0xFFD27A);
			add(halo);
			portal = new Image(FinaleAssets.PORTAL);
			film = new TextureFilm(portal.texture, ThalParts.PortalSprite.W, ThalParts.PortalSprite.H);
			portal.frame(film.get(0));
			portal.hardlight(ThalParts.worldColor(6));
			portal.origin.set(ThalParts.PortalSprite.W / 2f, ThalParts.PortalSprite.H);
			portal.scale.set(3f);
			portal.x = cx - ThalParts.PortalSprite.W / 2f;
			portal.y = cy + ThalParts.PortalSprite.H * 1.5f - ThalParts.PortalSprite.H;
			portal.alpha(0);
			add(portal);

			for (int i = 0; i < n; i++){
				Light l = new Light(0xFFF0B0);
				l.alpha(0);
				sparks.add(l);
				add(l);
				Image d = new ItemSprite(ItemSpriteSheet.SACRED_ESSENCE);
				d.origin.set(d.width / 2f, d.height / 2f);
				d.scale.set(2f);
				d.alpha(0);
				drops.add(d);
				add(d);
			}

			float y = uih * 0.6f;
			int size = uiw >= 300 ? 8 : 7;
			for (int i = 1; i <= LINES; i++){
				RenderedTextBlock b = PixelScene.renderTextBlock(Messages.get(Awakening.class, "seal_" + i), size);
				b.maxWidth((int) Math.min(uiw - 24, 300));
				b.hardlight(i == LINES ? 0xFFE08A : 0xE8DCC8);
				b.setPos((uiw - b.width()) / 2f, y);
				PixelScene.align(b);
				b.alpha(0);
				lines.add(b);
				add(b);
				y = b.bottom() + 5;
			}

			white = new ColorBlock(uiw, uih, 0xFFFFF6E0);
			white.alpha(0);
			add(white);
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			dark.alpha(0.96f * GameMath.gate(0, t / 0.8f, 1));

			//the portal turns until the last drop is in, then folds shut
			float pa = GameMath.gate(0, (t - 0.2f) / 0.6f, 1);
			if (!closed){
				portal.frame(film.get((int) (t * 8) % 4));
				portal.alpha(pa);
				halo.at(cx, cy, 150 + 14 * (float) Math.sin(t * 3));
				halo.alpha(0.45f * pa);
				if (t >= closeAt){
					closed = true;
					Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 1f, 0.6f);
					Sample.INSTANCE.play(Assets.Sounds.BEACON, 1f, 0.8f);
					PixelScene.shake(3, 1f);
				}
			} else {
				float c = t - closeAt;
				int f = Math.min(9, 7 + (int) (c * 8));
				portal.frame(film.get(f));
				portal.alpha(GameMath.gate(0, 1 - (c - 0.4f) / 0.6f, 1));
				halo.at(cx, cy, Math.max(0, 150 - 260 * c));
				halo.alpha(0.6f * GameMath.gate(0, 1 - c / 0.6f, 1));
				white.alpha(0.85f * GameMath.gate(0, 1 - c / 0.9f, 1));
			}

			//the drops come out round it, circle it and go in one by one
			for (int i = 0; i < n; i++){
				Image d = drops.get(i);
				Light l = sparks.get(i);
				if (gone[i]){
					d.alpha(0);
					l.alpha(0);
					continue;
				}
				float born = 0.8f + i * 0.18f, dive = 2.6f + i * 0.14f;
				if (t < born) continue;
				float ang = (float) (Math.PI * 2 * i / n + t * 0.7f);
				float r = 84;
				float in = GameMath.gate(0, (t - dive) / 0.9f, 1);
				if (in > 0){
					r *= 1 - in * in;
					ang += in * 3f;
				}
				float x = cx + (float) Math.cos(ang) * r, y = cy + (float) Math.sin(ang) * r * 0.8f;
				float a = GameMath.gate(0, (t - born) / 0.4f, 1);
				d.x = x - d.width / 2f;
				d.y = y - d.height / 2f;
				d.alpha(a);
				d.scale.set(2f * (1 - 0.6f * in));
				l.at(x, y, 26 + 6 * (float) Math.sin(t * 6 + i));
				l.alpha(0.7f * a);
				if (in >= 1){
					gone[i] = true;
					Sample.INSTANCE.play(Assets.Sounds.MASTERY, 0.5f, 1.2f + 0.06f * i);
				}
				if (t >= born && t - e < born) Sample.INSTANCE.play(Assets.Sounds.CHARMS, 0.4f, 1.4f);
			}

			//and the words, one line at a time
			for (int i = 0; i < lines.size(); i++){
				lines.get(i).alpha(window(t, textAt + i * SPACING, 0.7f, length - 0.2f, 0.8f));
			}
			if (t > length - 0.8f){
				dark.alpha(0.96f + 0.04f * GameMath.gate(0, (t - (length - 0.8f)) / 0.8f, 1));
			}

			if (t >= length && !called){
				called = true;
				done.call();
			}
		}
	}
}
