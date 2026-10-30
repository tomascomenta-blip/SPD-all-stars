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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//sheets for the new mobs: rows of 11 frames, 0-1 idle 2-3 attack 4-7 run 8-10 death
//shooters fire a projectile if the target isn't adjacent. mobs with more than one look (bubble etc) get a row each
public abstract class SKMobSprite extends MobSprite {

	private static final int FRAMES = 11;

	//-1 = melee only
	protected int shot = -1;
	protected boolean spinning = false;
	protected String shotSound = Assets.Sounds.HIT_ARROW;

	private TextureFilm film;
	private int row = 0;
	private int cols = FRAMES; //some sheets are wider than 11, ugh

	protected void setup( String texture, int width, int height ){
		texture( texture );
		film = new TextureFilm( this.texture, width, height );
		cols = Math.max(FRAMES, (int)(this.texture.width / width));
		animations();
		play( idle );
	}

	private void animations(){
		int o = row * cols;

		idle = new Animation( 2, true );
		idle.frames( film, o, o, o, o+1, o, o, o+1, o+1 );

		run = new Animation( 12, true );
		run.frames( film, o+4, o+5, o+6, o+7 );

		attack = new Animation( 12, false );
		attack.frames( film, o+2, o+3, o );

		zap = attack.clone();

		die = new Animation( 12, false );
		die.frames( film, o+8, o+9, o+10 );
	}

	//swap row, but only once idle/running or the anim glitches
	protected void row( int r ){
		if (r == row || film == null) return;
		boolean running = curAnim == run;
		if (curAnim != null && curAnim != idle && !running) return;
		row = r;
		animations();
		play( running ? run : idle );
	}

	@Override
	public void attack( int cell ) {
		if (shot >= 0 && !Dungeon.level.adjacent( cell, ch.pos )) {
			((MissileSprite) parent.recycle( MissileSprite.class )).reset(
					this, cell, spinning ? new SpinningShot(shot) : new Shot(shot), new Callback() {
						@Override
						public void call() {
							ch.onAttackComplete();
						}
					} );
			play( zap );
			turnTo( ch.pos, cell );
			if (shotSound != null) Sample.INSTANCE.play( shotSound, 1f, 1.1f );
		} else {
			super.attack( cell );
		}
	}

	//attack anim, sin atacar
	public void cast(){
		play( zap );
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == zap) {
			idle();
		}
		super.onComplete( anim );
	}

	public static class Shot extends Item {
		public Shot(){
			this(0);
		}
		public Shot( int image ){
			this.image = image;
		}
	}

	//spinny
	public static class SpinningShot extends Item {
		public SpinningShot(){
			this(0);
		}
		public SpinningShot( int image ){
			this.image = image;
		}
	}
}
