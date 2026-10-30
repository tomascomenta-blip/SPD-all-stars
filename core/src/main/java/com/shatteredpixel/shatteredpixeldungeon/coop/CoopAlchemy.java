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

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//alchemy in co-op. every hero gets a "bench" with what they put in the pot. in/out/combine are
//orders so both devices touch the inventory in the same order. the screen itself only exists on the
//alchemist's device (the other one just sees potions appear, magic)
public class CoopAlchemy {

	public static final int SLOTS = 3;

	private static final Item[][] benches = new Item[Party.MAX_HEROES][SLOTS];

	public static boolean active(){
		return CoopSession.active() && Party.active();
	}

	public static Item[] bench( Hero h ){
		int idx = Math.max(0, Party.indexOf(h));
		return benches[idx];
	}

	public static void reset(){
		for (Item[] b : benches){
			for (int i = 0; i < b.length; i++) b[i] = null;
		}
	}

	// *** shared logic (runs on both devices) ***

	//puts an item from the hero's bag on the bench, returns the slot or -1
	static int add( Hero h, Item item, boolean all ){
		Item[] bench = bench(h);
		for (int i = 0; i < bench.length; i++){
			if (bench[i] == null){
				if (all || item instanceof LiquidMetal || item instanceof MissileWeapon){
					bench[i] = item.detachAll(h.belongings.backpack);
				} else {
					bench[i] = item.detach(h.belongings.backpack);
				}
				return i;
			}
		}
		return -1;
	}

	//puts an item from the bench back in the bag (or on the floor)
	static void remove( Hero h, int slot ){
		Item[] bench = bench(h);
		if (slot < 0 || slot >= bench.length || bench[slot] == null) return;
		Item item = bench[slot];
		bench[slot] = null;
		if (!item.collect(h.belongings.backpack)){
			Dungeon.level.drop(item, h.pos);
		}
	}

	static ArrayList<Item> ingredients( Hero h ){
		ArrayList<Item> result = new ArrayList<>();
		for (Item i : bench(h)){
			if (i != null) result.add(i);
		}
		return result;
	}

	//brews the recipe at the given position of Recipe.findRecipes, returns the result
	static Item combine( Hero h, int recipeIndex ){
		ArrayList<Item> ingredients = ingredients(h);
		if (ingredients.isEmpty()) return null;
		ArrayList<Recipe> recipes = Recipe.findRecipes(ingredients);
		if (recipeIndex < 0 || recipeIndex >= recipes.size()) return null;
		Recipe recipe = recipes.get(recipeIndex);

		int cost = recipe.cost(ingredients);
		if (cost > Dungeon.energy) return null;
		Catalog.countUses(EnergyCrystal.class, cost);
		Dungeon.energy -= cost;

		Item result = recipe.brew(ingredients);
		if (result != null){
			int quantity = result.quantity();
			if (!result.collect(h.belongings.backpack)){
				Dungeon.level.drop(result, h.pos);
			}
			result.quantity(quantity);
			Statistics.itemsCrafted++;
			Badges.validateItemsCrafted();
		}

		//used up ingredients leave the bench
		Item[] bench = bench(h);
		for (int i = 0; i < bench.length; i++){
			if (bench[i] != null && bench[i].quantity() <= 0) bench[i] = null;
		}
		return result;
	}

	//turns an item into alchemical energy
	static Item energize( Hero h, Item item, boolean one ){
		if (one && item.quantity() > 1){
			item = item.detach(h.belongings.backpack);
		} else {
			if (item.isEquipped(h) && !((EquipableItem)item).doUnequip(h, false)) {
				return null;
			}
			item.detachAll(h.belongings.backpack);
		}
		Dungeon.energy += item.energyVal();
		return item;
	}

	// *** orders of the local player (alchemy screen) ***

	private static Hero me(){
		return Dungeon.local();
	}

	public static int localAdd( final Item item, final boolean all ){
		final int[] slot = {-1};
		Bundle o = CoopSession.order("AA");
		o.put("i", ItemRef.of(me(), item));
		o.put("all", all);
		CoopSession.localUnchecked(o, new Runnable() {
			@Override
			public void run() {
				slot[0] = add(me(), item, all);
			}
		});
		return slot[0];
	}

	public static void localRemove( final int slot ){
		Bundle o = CoopSession.order("AX");
		o.put("s", slot);
		CoopSession.localUnchecked(o, new Runnable() {
			@Override
			public void run() {
				remove(me(), slot);
			}
		});
	}

	public static Item localCombine( final int recipeIndex ){
		final Item[] result = {null};
		Bundle o = CoopSession.order("AC");
		o.put("r", recipeIndex);
		CoopSession.localUnchecked(o, new Runnable() {
			@Override
			public void run() {
				result[0] = combine(me(), recipeIndex);
			}
		});
		return result[0];
	}

	public static Item localEnergize( final Item item, final boolean one ){
		final Item[] result = {null};
		Bundle o = CoopSession.order("AE");
		o.put("i", ItemRef.of(me(), item));
		o.put("one", one);
		CoopSession.localUnchecked(o, new Runnable() {
			@Override
			public void run() {
				result[0] = energize(me(), item, one);
			}
		});
		return result[0];
	}

	// *** replay of the other player's alchemy ***

	static void apply( Hero h, Bundle ev ){
		switch (ev.getString("k")){
			case "AA": {
				Item item = ItemRef.resolve(h, ev.getString("i"));
				if (item != null) add(h, item, ev.getBoolean("all"));
				else CoopSession.reportMismatch("alchemy item");
				break;
			}
			case "AX":
				remove(h, ev.getInt("s"));
				break;
			case "AC":
				combine(h, ev.getInt("r"));
				break;
			case "AE": {
				Item item = ItemRef.resolve(h, ev.getString("i"));
				if (item != null) energize(h, item, ev.getBoolean("one"));
				else CoopSession.reportMismatch("energize item");
				break;
			}
		}
	}
}
