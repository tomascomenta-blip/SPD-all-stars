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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;

//rift_drake.png: 24x24 frames, 15 per row, one row per element (Drake.INFERNAL..CHEMTECH).
//Frames of a row: idle 0-1, run 2-5, attack 6-8, die 9-11, cast 12-14.
public class DrakeSprite extends MobSprite {

	private static final int SIZE = 24;
	private static final int PER_ROW = 15;

	private int row = -1;
	private Animation charge;
	private int bloodColor = 0xFFFF5522;

	public DrakeSprite() {
		super();
		texture(Assets.Sprites.RIFT_DRAKE);
		setElement(0);
	}

	public void setElement( int element ){
		int r = Drake.valid(element);
		if (r == row) return;
		row = r;
		bloodColor = 0xFF000000 | Drake.color(r);

		TextureFilm frames = new TextureFilm(texture, SIZE, SIZE);
		int c = r * PER_ROW;

		idle = new Animation(3, true);
		idle.frames(frames, c, c, c+1, c+1);

		run = new Animation(12, true);
		run.frames(frames, c+2, c+3, c+4, c+5);

		attack = new Animation(14, false);
		attack.frames(frames, c+6, c+7, c+8, c);

		die = new Animation(10, false);
		die.frames(frames, c+9, c+10, c+11);

		zap = new Animation(12, false);
		zap.frames(frames, c+12, c+13, c+14, c+14);

		//holding the cast while an attack is announced
		charge = new Animation(6, true);
		charge.frames(frames, c+12, c+13);

		play(idle);
	}

	@Override
	public void linkVisuals( Char ch ) {
		super.linkVisuals(ch);
		if (ch instanceof Drake) setElement(((Drake) ch).element);
	}

	public void charge(){
		play(charge);
	}

	@Override
	public void onComplete( Animation anim ) {
		super.onComplete(anim);
		if (anim == zap) idle();
	}

	@Override
	public int blood() {
		return bloodColor;
	}
}
