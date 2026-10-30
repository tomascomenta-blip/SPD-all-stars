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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshCritter;

//the small things of the devoured city
public abstract class CityCritter extends AshCritter {

	//a bite out of nowhere (no mark) may hurt, never finish anyone off
	protected int unmarked( Char enemy, int damage ){
		if (enemy instanceof Hero && damage >= enemy.HP) return Math.max(0, enemy.HP - 1);
		return damage;
	}

	//16x16: 0-1 idle, 2-5 walk, 6 the blow coming, 7 the blow, 8 and on its own. no death frames:
	//it goes down on the blow and fades
	public abstract static class Sheet extends ContinentMobSprite {

		protected Animation special;

		protected void layout( String tex ){
			setup(tex, 16, 16);
		}

		@Override
		protected void setup( String tex, int width, int height ) {
			super.setup(tex, width, height);
			idle = loop(2, 0, 0, 1, 0, 1, 1);
			attack = new Animation(12, false);
			attack.frames(frames, 6, 7, 7);
			zap = new Animation(3, false);
			zap.frames(frames, 6, 6, 7);
			die = new Animation(8, false);
			die.frames(frames, 7, 6, 6);
			special = new Animation(4, false);
			special.frames(frames, 8, 8, 8, 0);
			play(idle);
		}

		public void special(){
			play(special);
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == special){
				idle();
				return;
			}
			super.onComplete(anim);
		}
	}
}
