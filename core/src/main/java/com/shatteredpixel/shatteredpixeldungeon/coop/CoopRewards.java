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

package com.shatteredpixel.shatteredpixeldungeon.coop;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward.ThalRewards;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.List;

//tengu's mask & the king's crown drop once per hero that still needs one. fighting over a mask
//with your friend was funny exactly once
public class CoopRewards {

	private static List<Hero> party(){
		if (Party.active()) return Party.heroes();
		ArrayList<Hero> one = new ArrayList<>();
		if (Dungeon.hero != null) one.add(Dungeon.hero);
		return one;
	}

	//heroes without a subclass yet (alive or fallen: a fallen hero can be revived)
	public static int masksNeeded(){
		int n = 0;
		for (Hero h : party()){
			if (h.subClass == HeroSubClass.NONE) n++;
		}
		return n;
	}

	//one crown per hero without an armor ability; at least one, as in the original game
	public static int crownsNeeded(){
		int n = 0;
		for (Hero h : party()){
			if (h.armorAbility == null) n++;
		}
		return Math.max(1, n);
	}

	public static ArrayList<Item> masks(){
		ArrayList<Item> items = new ArrayList<>();
		for (int i = 0; i < masksNeeded(); i++) items.add(new TengusMask());
		return items;
	}

	public static ArrayList<Item> crowns(){
		ArrayList<Item> items = new ArrayList<>();
		for (int i = 0; i < crownsNeeded(); i++) items.add(new KingsCrown());
		return items;
	}

	//someone who joins a saved run late: their new hero gets the party's average level, strength
	//and weapon/armor upgrades, two healing potions, and a mask/crown if the others already chose
	public static void catchUp( final Hero h, List<Hero> party ){
		int n = 0, lvl = 0, str = 0, wep = 0, arm = 0;
		boolean masks = false, crowns = false;
		for (Hero o : party){
			n++;
			lvl += o.lvl;
			str += o.STR;
			if (o.belongings.weapon != null) wep += o.belongings.weapon.level();
			if (o.belongings.armor != null) arm += o.belongings.armor.level();
			if (o.subClass != HeroSubClass.NONE) masks = true;
			if (o.armorAbility != null) crowns = true;
		}
		if (n == 0) return;
		//the party already beat Thal-Vekhorr: the newcomer gets the same loot (and level cap)
		if (ThalRewards.partyUnbound(party)) ThalRewards.grant(h);
		while (h.lvl < Math.min(lvl / n, h.maxLevel())) h.testLevelUp();
		h.STR = Math.max(h.STR, str / n);
		if (h.belongings.weapon != null && wep / n > 0) h.belongings.weapon.upgrade(wep / n);
		if (h.belongings.armor != null && arm / n > 0) h.belongings.armor.upgrade(arm / n);
		h.updateHT(true);
		h.HP = h.HT;
		final boolean mask = masks, crown = crowns;
		Party.withHero(h, new Runnable() {
			@Override
			public void run() {
				if (mask) new TengusMask().collect(h.belongings.backpack);
				if (crown) new KingsCrown().collect(h.belongings.backpack);
				PotionOfHealing p = new PotionOfHealing();
				p.quantity(2);
				p.identify();
				p.collect(h.belongings.backpack);
			}
		});
	}

	//drops the items around a cell, the first one on the cell itself and the rest next to it
	public static void dropAround( ArrayList<Item> items, int pos ){
		int i = 0;
		for (Item item : items){
			int cell = pos;
			if (i > 0){
				for (int n = 0; n < 8; n++){
					int c = pos + PathFinder.NEIGHBOURS8[(i - 1 + n) % 8];
					if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && !Dungeon.level.solid[c]){
						cell = c;
						break;
					}
				}
			}
			Dungeon.level.drop(item, cell).sprite.drop(pos);
			i++;
		}
	}
}
