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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

//everything Thal-Vekhorr leaves for each hero: a pile of upgrades and supplies for the road, the
//melted clock, the clockmaker's chest (tier 6 gear), the heart (a path for talent row 5) and 10
//more levels to grow into. the fight calls grant() once per hero, same order on every device
public class ThalRewards {

	public static final int UPGRADES = 6;
	public static final int STRENGTH = 2;
	public static final int HEALING  = 3;
	public static final int RATIONS  = 3;

	public static void grant( final Hero hero ){
		final ArrayList<Item> items = new ArrayList<>();
		items.add(new ScrollOfUpgrade().quantity(UPGRADES));
		items.add(new PotionOfStrength().quantity(STRENGTH));
		items.add(new PotionOfHealing().quantity(HEALING));
		items.add(new Food().quantity(RATIONS));
		items.add(new MeatPie());
		items.add(new MeltedClock());
		items.add(new ClockmakerChest());
		items.add(new ThalHeart());

		Party.withHero(hero, new Runnable() {
			@Override
			public void run() {
				for (Item item : items){
					item.identify();
					give(hero, item);
				}
				if (hero.lvlCap < Hero.UNBOUND_LEVEL){
					hero.lvlCap = Hero.UNBOUND_LEVEL;
				}
				GLog.p(Messages.get(ThalRewards.class, "loot", UPGRADES, STRENGTH, HEALING));
				GLog.p(Messages.get(ThalRewards.class, "unbound", Hero.UNBOUND_LEVEL));
			}
		});
		CoopLog.line("thal rewards for " + hero.heroClass + " lvl=" + hero.lvl + " cap=" + hero.lvlCap);
	}

	//backpack, or at their feet if it's full
	private static void give( Hero hero, Item item ){
		if (!item.collect(hero.belongings.backpack) && Dungeon.level != null){
			Heap heap = Dungeon.level.drop(item, hero.pos);
			if (heap.sprite != null) heap.sprite.drop();
		}
	}

	//someone in the party already beat the end of hours (for heroes who join a saved run late)
	public static boolean partyUnbound( Iterable<Hero> party ){
		for (Hero h : party){
			if (h.lvlCap > Hero.MAX_LEVEL) return true;
		}
		return false;
	}
}
