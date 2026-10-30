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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Mutations;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//what a colossus leaves when it falls (Remains). raw it fills you and the dark comes with it,
//off the fire it's the best meal over there
public class ColossusMeat {

	public static final int RAW_TAINT = 6;

	public static class Raw extends Food {

		{
			image = ItemSpriteSheet.COLOSSUS_MEAT;
			energy = Hunger.HUNGRY * 0.75f;
		}

		@Override
		protected void satisfy( Hero hero ) {
			super.satisfy(hero);
			if (!Mutations.ateMeat(hero)) Continent.taint(hero, RAW_TAINT);
		}

		@Override
		public int value() {
			return 6 * quantity;
		}
	}

	public static class Roast extends Food {

		{
			image = ItemSpriteSheet.COLOSSUS_ROAST;
			energy = Hunger.HUNGRY;
		}

		@Override
		protected void satisfy( Hero hero ) {
			super.satisfy(hero);
			Mutations.ateMeat(hero);
		}

		@Override
		public int value() {
			return 12 * quantity;
		}
	}
}
