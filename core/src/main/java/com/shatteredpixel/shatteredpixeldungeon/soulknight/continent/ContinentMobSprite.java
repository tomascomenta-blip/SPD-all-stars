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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent;

import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.watabou.noosa.TextureFilm;

//the continent sheets (see ContinentAssets): one row, 0-1 idle, 2-5 run, 6-8 attack, 9-11 die,
//extras from 12. still an SKMobSprite so the "!" of a marked attack plays zap
public abstract class ContinentMobSprite extends SKMobSprite {

	protected TextureFilm frames;

	@Override
	protected void setup( String tex, int width, int height ){
		texture( tex );
		frames = new TextureFilm( texture, width, height );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1, 0, 0, 1, 1 );

		run = new Animation( 10, true );
		run.frames( frames, 2, 3, 4, 5 );

		attack = new Animation( 12, false );
		attack.frames( frames, 6, 7, 8 );

		zap = attack.clone();

		die = new Animation( 10, false );
		die.frames( frames, 9, 10, 11 );

		play( idle );
	}

	protected Animation loop( int fps, int... ids ){
		Animation a = new Animation( fps, true );
		a.frames( frames, toObjects(ids) );
		return a;
	}

	private static Object[] toObjects( int[] ids ){
		Object[] o = new Object[ids.length];
		for (int i = 0; i < ids.length; i++) o[i] = ids[i];
		return o;
	}

	//new idle loop for a change of look (submerged, cracked...). only restarts it if it was
	//already idling: cutting an attack short would leave its mob waiting forever
	protected synchronized void look( Animation newIdle ){
		boolean idling = curAnim == null || curAnim == idle;
		idle = newIdle;
		if (idling) play( idle );
	}
}
