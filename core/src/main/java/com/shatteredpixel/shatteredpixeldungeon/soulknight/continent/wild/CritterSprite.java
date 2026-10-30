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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild;

import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;

//the 16x16 sheets of the small beasts: 0-1 idle, 2-5 walk, 6 getting ready, 7 the blow, specials
//from 8. no death frames: it flinches and fades
public abstract class CritterSprite extends ContinentMobSprite {

	private Animation shot;

	protected void critter( String tex ){
		setup(tex, 16, 16);
		idle = loop(2, 0, 0, 1, 0, 1, 1);
		run = loop(10, 2, 3, 4, 5);
		attack = clip(12, 6, 7, 7);
		zap = attack.clone();
		die = clip(8, 6, 7, 0);
		play(idle);
	}

	protected Animation clip( int fps, int... ids ){
		Animation a = new Animation(fps, false);
		Object[] o = new Object[ids.length];
		for (int i = 0; i < ids.length; i++) o[i] = ids[i];
		a.frames(frames, o);
		return a;
	}

	//a one-off move that goes back to idle on its own
	public void pose( int fps, int... ids ){
		shot = clip(fps, ids);
		play(shot);
	}

	//a held look (flying, buried...) in place of idle
	public void hold( int fps, int... ids ){
		look(loop(fps, ids));
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == shot){
			shot = null;
			idle();
			return;
		}
		super.onComplete(anim);
	}
}
