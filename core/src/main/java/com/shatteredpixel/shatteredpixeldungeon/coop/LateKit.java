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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfElements;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;

import java.util.List;

//testing: -Dspd.coop.latekit=N the gear of a long run, for trying the late bosses by hand.
//greatsword and plate armor +N (the mage keeps the staff, upgraded; the warrior's seal moves to
//the new armor), two rings and two wands at a third of that, torches and a few scrolls
public class LateKit {

	public static void prepare( List<Hero> heroes ){
		int plus = Integer.getInteger("spd.coop.latekit", 0);
		if (plus <= 0) return;
		int small = Math.max(1, plus / 3);
		for (final Hero h : heroes){
			if (h.belongings.weapon instanceof MagesStaff){
				h.belongings.weapon.upgrade(plus);
			} else {
				MeleeWeapon w = new Greatsword();
				w.identify();
				w.upgrade(plus);
				w.cursed = false;
				h.belongings.weapon = w;
			}

			Armor a = new PlateArmor();
			a.identify();
			a.upgrade(plus);
			a.cursed = false;
			if (h.belongings.armor != null){
				BrokenSeal seal = h.belongings.armor.detachSeal();
				if (seal != null) a.affixSeal(seal);
			}
			h.belongings.armor = a;
			a.activate(h);
			h.STR = Math.max(h.STR, a.STRReq());
			if (h.belongings.weapon instanceof MeleeWeapon){
				h.STR = Math.max(h.STR, ((MeleeWeapon) h.belongings.weapon).STRReq());
			}

			Ring elements = new RingOfElements();
			Ring haste = new RingOfHaste();
			for (Ring r : new Ring[]{elements, haste}){
				r.upgrade(small);
				r.identify();
				r.cursed = false;
			}
			h.belongings.ring = elements;
			elements.activate(h);
			if (h.belongings.misc == null){
				h.belongings.misc = haste;
				haste.activate(h);
			}

			final Item[] bag = {
					new WandOfFrost(), new WandOfLightning(),
					new Torch().quantity(4),
					new ScrollOfTeleportation().quantity(2),
					new ScrollOfMagicMapping(),
					h.belongings.misc == haste ? null : haste};
			Party.withHero(h, new Runnable() {
				@Override
				public void run() {
					for (Item i : bag){
						if (i == null) continue;
						if (i instanceof Wand){
							i.upgrade(small);
							((Wand) i).curCharges = ((Wand) i).maxCharges;
						}
						i.identify();
						i.collect(h.belongings.backpack);
					}
				}
			});
			h.updateHT(true);
			h.HP = h.HT;
			CoopLog.line("late kit for " + h.heroClass + " +" + plus + " str=" + h.STR);
		}
	}
}
