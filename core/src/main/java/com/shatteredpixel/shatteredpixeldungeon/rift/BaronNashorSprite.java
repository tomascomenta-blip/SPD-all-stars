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
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//32x32 frames in one row: idle 0-1, run 2-5, attack 6-8, die 9-11, spit 12-14
public class BaronNashorSprite extends MobSprite {

	public BaronNashorSprite() {
		super();

		texture(Assets.Sprites.BARON);
		TextureFilm frames = new TextureFilm(texture, 32, 32);

		idle = new Animation(3, true);
		idle.frames(frames, 0, 1);

		run = new Animation(10, true);
		run.frames(frames, 2, 3, 4, 5);

		attack = new Animation(12, false);
		attack.frames(frames, 6, 7, 8, 0);

		zap = new Animation(12, false);
		zap.frames(frames, 12, 13, 14);

		die = new Animation(8, false);
		die.frames(frames, 9, 10, 11);

		play(idle);
	}

	//ranged attack: a glob of void spit
	@Override
	public void zap( int cell ) {
		super.zap(cell);
		MagicMissile.boltFromChar(parent, MagicMissile.SHADOW, this, cell, new Callback() {
			@Override
			public void call() {
				((BaronNashor) ch).onZapComplete();
			}
		});
		Sample.INSTANCE.play(Assets.Sounds.ZAP, 1f, 0.7f);
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == zap) idle();
		super.onComplete(anim);
	}

	@Override
	public int blood() {
		return 0xFF7733AA;
	}
}
