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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.ColossusSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Hunt;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;

//the big beasts: a held pose while a heavy hit is marked (windup), the blow when it lands (strike),
//and one-shot moves (roars, cracking...) that go back to idle on their own. knocked down it lies
//there, tamed it wears the sun of the sacred light, and the big ones show their weak spot pulsing
public abstract class AshColossusSprite extends ColossusSprite {

	private Animation shot;
	private boolean downed, tamed;
	private Hunt.WeakGlow glow;
	private Image sun;

	protected Animation clip( int fps, int... ids ){
		Animation a = new Animation( fps, false );
		Object[] o = new Object[ids.length];
		for (int i = 0; i < ids.length; i++) o[i] = ids[i];
		a.frames( frames, o );
		return a;
	}

	//by default the first frame of the attack, held
	public void windup( int kind ){
		play( loop( 1, 6 ) );
	}

	public void strike( int kind ){
		once( clip( 10, 7, 8, 8 ) );
	}

	public void once( Animation a ){
		shot = a;
		play( a );
	}

	// *** what the hunt needs from the drawings ***

	//{falling, lying A, lying B, getting up}, or null when it has no such frames
	protected int[] downFrames(){
		return null;
	}

	//the weak spot in the frame (facing right), for the ones too big to have it drawn in every frame
	protected PointF weakAnchor(){
		return null;
	}

	//a sheet laid out like the usual one with the sacred collar on, or frames to stand in when tamed
	protected String tamedSheet(){
		return null;
	}

	protected int[] tamedIdle(){
		return null;
	}

	public void knockDown(){
		downed = true;
		int[] f = downFrames();
		if (f != null) play( loop( 2, f[1], f[2] ) );
	}

	public void rise(){
		downed = false;
		int[] f = downFrames();
		if (f != null) once( clip( 4, f[3] ) );
		else idle();
	}

	public void tame(){
		tamed = true;
		if (tamedSheet() != null) texture( tamedSheet() );
		int[] t = tamedIdle();
		if (t != null) idle = loop( 2, t );
		idle();
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );
		if (ch instanceof AshColossus){
			AshColossus a = (AshColossus) ch;
			if (a.tamed) tame();
			if (a.down > 0) knockDown();
		}
		if (weakAnchor() != null && glow == null){
			glow = new Hunt.WeakGlow();
			GameScene.effect( glow );
		}
	}

	@Override
	public void update() {
		super.update();
		if (glow != null){
			glow.visible = visible && !tamed && ch != null && ch.isAlive();
			PointF a = weakAnchor();
			glow.at( new PointF( x + (flipHorizontal ? frameW - a.x : a.x), y + a.y ) );
		}
		if (tamed){
			if (sun == null){
				sun = new Image( Hunt.TAMED_SUN );
				if (parent != null) parent.add( sun );
			}
			sun.visible = visible;
			sun.x = x + footLeft() + size * DungeonTilemap.SIZE / 2f - 4;
			sun.y = y + oy - 10;
		}
	}

	@Override
	public void kill() {
		super.kill();
		if (glow != null) glow.killAndErase();
		if (sun != null) sun.killAndErase();
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == shot){
			shot = null;
			if (downed) knockDown();
			else idle();
			return;
		}
		super.onComplete( anim );
	}
}
