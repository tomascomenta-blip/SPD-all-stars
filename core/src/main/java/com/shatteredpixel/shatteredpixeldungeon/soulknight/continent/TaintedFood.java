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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//what you find to eat over there. it fills you up, a bit, and the dark comes with it
public class TaintedFood {

	public static Food random(){
		return Random.Int(3) == 0 ? new TaintedMeat() : new TaintedRation();
	}

	public static class TaintedRation extends Food {

		{
			image = ItemSpriteSheet.TAINTED_RATION;
			energy = Hunger.HUNGRY * 0.75f;
		}

		@Override
		protected void satisfy( Hero hero ) {
			super.satisfy(hero);
			Continent.taint(hero, 8);
		}

		@Override
		public int value() {
			return 5 * quantity;
		}
	}

	public static class TaintedMeat extends Food {

		{
			image = ItemSpriteSheet.TAINTED_MEAT;
			energy = Hunger.HUNGRY / 2f;
		}

		@Override
		protected void satisfy( Hero hero ) {
			super.satisfy(hero);
			Continent.taint(hero, 5);
		}

		@Override
		public int value() {
			return 3 * quantity;
		}
	}
}
