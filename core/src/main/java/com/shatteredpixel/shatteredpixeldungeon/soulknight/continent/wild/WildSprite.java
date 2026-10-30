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

import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossusSprite;

//sheets of the moor, graveyard and isles beasts: 0-1 idle, 2-5 walk, 6 getting ready, 7 the blow,
//then for the 5x5 ones 8 knocked down (weak spot showing) and 9 dead, and the specials after that.
//the 3x3 ones have no fall nor death of their own: they go back to idle and fade
public abstract class WildSprite extends AshColossusSprite {

	private boolean downs;

	protected void beast( String tex, int size, int fw, int fh, int ox, int oy ){
		setup(tex, size, fw, fh, ox, oy);
		downs = size >= 5;
		idle = loop(2, 0, 0, 1, 1);
		run = loop(8, 2, 3, 4, 5);
		attack = clip(12, 6, 7, 7);
		zap = attack.clone();
		die = downs ? clip(6, 8, 9, 9) : clip(6, 6, 0);
		play(idle);
	}

	//the frame held while a blow of that kind is marked, and the one it lands with
	protected int ready( int kind ){
		return 6;
	}

	protected int blow( int kind ){
		return 7;
	}

	@Override
	public void windup( int kind ) {
		play(loop(1, ready(kind)));
	}

	@Override
	public void strike( int kind ) {
		once(clip(10, blow(kind), blow(kind), blow(kind), 0));
	}

	//a one-off move (roar, rearing...) that goes back to idle on its own
	public void pose( int fps, int... ids ){
		once(clip(fps, ids));
	}

	@Override
	protected int[] downFrames() {
		return downs ? new int[]{8, 8, 8, 0} : null;
	}
}
