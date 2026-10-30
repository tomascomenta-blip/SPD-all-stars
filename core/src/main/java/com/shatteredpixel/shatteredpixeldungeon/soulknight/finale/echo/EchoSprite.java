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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.echo;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

//the boss sprite as an echo: see-through, cyan with a gold pulse, leaves copies of itself behind when it moves.
//comes out of the portal small and grows, and turns into sand when it dies. subclasses only set up the frames
public abstract class EchoSprite extends MobSprite {

	private static final float APPEAR = 0.9f;
	private static final float FADE = 1.6f;

	protected float echoScale = 1f;
	protected float baseAlpha = 0.72f;
	protected float t = 0;

	private float appear = -1;
	private float fade = -1;
	private float trailT = 0;
	private float shimmerT = 1f;
	private Emitter motes;

	protected void echo( float s ){
		echoScale = s;
		scale.set(s);
	}

	protected static Animation anim( TextureFilm film, int fps, boolean loop, int... frames ){
		Animation a = new Animation(fps, loop);
		Object[] f = new Object[frames.length];
		for (int i = 0; i < frames.length; i++) f[i] = frames[i];
		a.frames(film, f);
		return a;
	}

	@Override
	public void link( Char ch ) {
		super.link(ch);
		if (ch instanceof EchoBoss && ((EchoBoss) ch).emerging()){
			appear = 0;
			scale.set(echoScale * 0.2f);
			place(ch.pos);
		}
		if (motes == null){
			motes = emitter();
			if (motes != null) motes.pour(EchoFx.MOTE, 0.14f);
		}
	}

	public boolean appearing(){
		return appear >= 0;
	}

	//how see-through right now
	protected float alphaNow(){
		float a = baseAlpha;
		if (appear >= 0) a *= Math.min(1f, appear * 1.5f);
		if (fade >= 0) a *= Math.max(0f, 1f - fade / FADE);
		if (invisible != null) a *= 0.5f;
		return a;
	}

	//washed out toward cyan, a slow gold pulse going through it
	protected void tintNow(){
		if (flashTime > 0) return;
		float p = gold();
		float s = 0.5f;
		rm = gm = bm = 1f - s;
		ra = (0.45f + 0.55f * p) * s;
		ga = (0.92f - 0.07f * p) * s;
		ba = (1.00f - 0.60f * p) * s;
		am = alphaNow();
		aa = 0;
	}

	//0 = all cyan, up to ~0.45 at the top of the pulse
	private float gold(){
		float p = 0.5f + 0.5f * (float) Math.sin(t * 2.2f);
		return p * p * 0.45f;
	}

	//glowing outline: the same frame a bit bigger, flat color, added on top of what's behind
	@Override
	public void draw() {
		if (visible && texture != null && am > 0.05f && flashTime <= 0){
			float sx = scale.x, sy = scale.y, ox = x, oy = y;
			float r0 = rm, g0 = gm, b0 = bm, a0 = am, r1 = ra, g1 = ga, b1 = ba, a1 = aa;
			boolean shadow = renderShadow;
			renderShadow = false;
			float k = 1f + 2.5f / Math.max(8f, height);
			scale.set(sx * k, sy * k);
			x = ox - width * sx * (k - 1) / 2f;
			y = oy - height * sy * (k - 1) * 0.7f;
			float p = gold();
			rm = gm = bm = 0;
			ra = 0.35f + p;
			ga = 0.85f;
			ba = 1f - p;
			am = a0 * 0.5f;
			aa = 0;
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
			scale.set(sx, sy);
			x = ox;
			y = oy;
			rm = r0; gm = g0; bm = b0; am = a0;
			ra = r1; ga = g1; ba = b1; aa = a1;
			renderShadow = shadow;
		}
		super.draw();
	}

	@Override
	public void update() {
		super.update();
		t += Game.elapsed;

		if (appear >= 0){
			if (appear == 0){
				//not straight into parent: we're in the middle of its update loop
				EchoFx.later(new Runnable() { public void run() {
					if (parent == null) return;
					new Flare(7, 18 + 6 * echoScale).color(EchoFx.CYAN, true).show(EchoSprite.this, 1.4f).angularSpeed = 140;
					new Flare(5, 12 + 4 * echoScale).color(EchoFx.GOLD, true).show(EchoSprite.this, 1.1f).angularSpeed = -90;
				} });
				if (visible) Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.9f, 0.8f);
			}
			appear += Game.elapsed / APPEAR;
			float p = Math.min(1f, appear);
			float e = 1f - (1f - p) * (1f - p);
			scale.set(echoScale * (0.2f + 0.8f * e));
			if (ch != null && !isMoving) place(ch.pos);
			if (visible && Random.Visual.Float() < Game.elapsed * 30) burst(EchoFx.MOTE, 2);
			if (p >= 1f){
				appear = -1;
				scale.set(echoScale);
				if (ch != null && !isMoving) place(ch.pos);
			}
		}

		if (fade >= 0){
			fade += Game.elapsed;
			float p = Math.min(1f, fade / FADE);
			scale.set(echoScale * (1f + 0.15f * p), echoScale * (1f - 0.45f * p));
			if (ch != null) place(ch.pos);
			if (visible && Random.Visual.Float() < Game.elapsed * 40) burst(EchoFx.SAND, 3);
			if (fade >= FADE){
				killAndErase();
				return;
			}
		}

		tintNow();

		if (motes != null){
			motes.visible = visible;
			if (fade >= 0) motes.on = false;
		}

		if (!visible || ch == null || fade >= 0 || appear >= 0) return;
		if (isMoving){
			trailT -= Game.elapsed;
			if (trailT <= 0){
				trailT = 0.045f;
				ghost(0, 0, 0, 0.32f, 0.38f);
			}
		} else {
			//now and then a copy slips off to the side, a beat behind
			shimmerT -= Game.elapsed;
			if (shimmerT <= 0){
				shimmerT = Random.Visual.Float(0.7f, 1.5f);
				ghost(0, 0, Random.Visual.Int(2) == 0 ? -10 : 10, 0.6f, 0.26f);
			}
		}
	}

	protected void burst( Emitter.Factory f, int n ){
		Emitter e = emitter();
		if (e != null) e.burst(f, n);
	}

	//ghosts go in the effects layer. adding them to our own group from here made that group update
	//this sprite again and again when the game lagged (froze a slow pc for good)
	private void ghost( float dx, float dy, float drift, float life, float a ){
		if (texture == null || frame() == null) return;
		Ghost g = new Ghost(this, life, a);
		g.x = x + dx;
		g.y = y + dy;
		g.speed.set(drift, 0);
		GameScene.effect(g);
	}

	@Override
	public void die() {
		super.die();
		fade = 0;
		EchoFx.later(new Runnable() { public void run() {
			if (parent == null || !visible) return;
			new Flare(6, 20 * echoScale).color(EchoFx.GOLD, true).show(EchoSprite.this, 1.2f);
			burst(EchoFx.SAND, 18);
			burst(EchoFx.MOTE, 10);
		} });
		if (visible) Sample.INSTANCE.play(Assets.Sounds.DEGRADE, 0.8f, 1.3f);
	}

	@Override
	public synchronized void onComplete( Animation anim ) {
		//the sand does the rest
		if (anim == die) return;
		boolean pose = animCallback == null && anim != attack && anim != operate && !anim.looped;
		super.onComplete(anim);
		if (pose && ch != null && ch.isAlive()) idle();
	}

	@Override
	public void kill() {
		super.kill();
		if (motes != null){
			motes.on = false;
			motes = null;
		}
	}

	//a copy of the current frame that fades away
	public static class Ghost extends Image {

		private final float life, a0;
		private float left;

		public Ghost( EchoSprite s, float life, float a ){
			super(s.texture);
			frame(s.frame());
			scale.set(s.scale.x, s.scale.y);
			flipHorizontal = s.flipHorizontal;
			if (Random.Visual.Int(3) == 0) hardlight(1f, 0.85f, 0.4f);
			else hardlight(0.45f, 0.95f, 1.1f);
			this.life = left = life;
			a0 = a;
			alpha(a);
		}

		@Override
		public void update() {
			super.update();
			left -= Game.elapsed;
			if (left <= 0){
				killAndErase();
				return;
			}
			am = a0 * left / life;
		}
	}
}
