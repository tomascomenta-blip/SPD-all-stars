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

package com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

//spooky mist. visual only, visual random
public class MistParticle extends PixelParticle.Shrinking {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((MistParticle) emitter.recycle(MistParticle.class)).reset(x, y);
		}
	};

	//faster rise, for summons/bursts
	public static final Emitter.Factory UP = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((MistParticle) emitter.recycle(MistParticle.class)).resetUp(x, y);
		}
	};

	public MistParticle() {
		super();
		lifespan = 1.2f;
	}

	public void reset( float x, float y ) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan;
		size = 4;
		color(Random.Visual.Int(2) == 0 ? 0x9FD8CB : 0x6E9C95);
		speed.set(Random.Visual.Float(-6, 6), Random.Visual.Float(-6, -2));
		acc.set(0, 0);
	}

	public void resetUp( float x, float y ) {
		reset(x, y);
		speed.set(Random.Visual.Float(-4, 4), Random.Visual.Float(-24, -12));
		size = 3;
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		am = (p < 0.5f ? p : 1 - p) * 1.4f;
	}
}
