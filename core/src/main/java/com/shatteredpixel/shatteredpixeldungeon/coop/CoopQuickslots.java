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
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.scout.Blowgun;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;

import java.util.ArrayList;

//quickslots are per device. the shared save only has the host's, so each device keeps its own
//across reloads (before this you'd see your friend's potions in your bar, fun times)
public class CoopQuickslots {

	//this device's slots, as references to the local hero's items
	public static String[] capture(){
		if (!Party.active()) return null;
		Hero me = Party.local();
		if (me == null) return null;
		String[] refs = new String[QuickSlot.SIZE];
		boolean any = false;
		for (int i = 0; i < QuickSlot.SIZE; i++){
			Item it = Dungeon.quickslot.getItem(i);
			if (it != null && !Dungeon.quickslot.isPlaceholder(i) && me.belongings.contains(it)){
				refs[i] = ItemRef.of(me, it) + "|" + it.getClass().getName();
				any = true;
			}
		}
		return any ? refs : null;
	}

	//after loading the shared save: only this hero's items, in the slots this player chose
	public static void apply( String[] refs ){
		if (!Party.active()) return;
		Hero me = Dungeon.local();
		if (me == null) return;
		for (int i = 0; i < QuickSlot.SIZE; i++){
			Item it = Dungeon.quickslot.getItem(i);
			if (it != null && (Dungeon.quickslot.isPlaceholder(i) || !me.belongings.contains(it))){
				Dungeon.quickslot.clearSlot(i);
			}
		}
		if (refs != null){
			for (int i = 0; i < QuickSlot.SIZE; i++){
				if (refs[i] == null) continue;
				String[] p = refs[i].split("\\|");
				Item it = ItemRef.resolve(me, p[0]);
				if (it == null || p.length < 2 || !it.getClass().getName().equals(p[1])){
					it = firstOfClass(me, p.length < 2 ? null : p[1]);
				}
				if (it != null) Dungeon.quickslot.setSlot(i, it);
			}
		} else {
			defaults(me);
		}
		Item.updateQuickslot();
	}

	private static Item firstOfClass( Hero h, String cls ){
		if (cls == null) return null;
		for (Item it : h.belongings){
			if (it.getClass().getName().equals(cls)) return it;
		}
		return null;
	}

	//the slots a hero of this class starts with
	public static void defaults( Hero me ){
		ArrayList<Item> wanted = new ArrayList<>();
		for (Item it : me.belongings){
			if (it instanceof ChampionKit) wanted.add(it);
		}
		for (Item it : me.belongings){
			if (it instanceof Blowgun || it instanceof MagesStaff || it instanceof SpiritBow
					|| it instanceof CloakOfShadows || it instanceof HolyTome){
				if (!wanted.contains(it)) wanted.add(it);
			}
		}
		if (me.heroClass == HeroClass.DUELIST && me.belongings.weapon != null){
			wanted.add(me.belongings.weapon);
		}
		for (Item it : me.belongings){
			if (it instanceof ThrowingStone || it instanceof ThrowingKnife || it instanceof ThrowingSpike){
				if (!wanted.contains(it)) wanted.add(it);
			}
		}
		if (SPDSettings.quickslotWaterskin()){
			Waterskin w = me.belongings.getItem(Waterskin.class);
			if (w != null) wanted.add(w);
		}
		int slot = 0;
		for (Item it : wanted){
			while (slot < QuickSlot.SIZE && Dungeon.quickslot.getItem(slot) != null) slot++;
			if (slot >= QuickSlot.SIZE) break;
			Dungeon.quickslot.setSlot(slot, it);
		}
	}
}
