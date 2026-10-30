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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.food.StewedMeat;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.TaintedFood;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.BoneGrapple;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.LightCollar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//what a fire can do. all of it runs from a click in WndCampfire, on every device at the same
//point of the game, so it checks everything again: the window may be older than the bag
public class FireRecipes {

	public enum Recipe {
		//2 torches
		KIT,
		//1 colossus bone -> 2 torches
		TORCHES,
		//1 raw meat (colossus or corrupt)
		BAIT,
		//2 corrupt food
		POULTICE,
		//1 torch, the essence is paid when it's lit
		SACRED_TORCH,
		//1 colossus bone
		GRAPPLE,
		//2 torches + 1 colossus bone
		COLLAR
	}

	public static final float TIME_TO_COOK = 3f, TIME_TO_MAKE = 2f, TIME_TO_FEED = 1f;

	public static boolean canMake( Hero h, Recipe r ){
		switch (r){
			case KIT:          return Survival.count(h, Torch.class) >= 2;
			case TORCHES:      return Survival.count(h, ColossusBone.class) >= 1;
			case BAIT:         return Survival.count(h, ColossusMeat.Raw.class) + Survival.count(h, TaintedFood.TaintedMeat.class) >= 1;
			case POULTICE:     return corrupt(h) >= 2;
			case SACRED_TORCH: return Survival.count(h, Torch.class) >= 1;
			case GRAPPLE:      return Survival.count(h, ColossusBone.class) >= 1;
			case COLLAR:       return Survival.count(h, Torch.class) >= 2 && Survival.count(h, ColossusBone.class) >= 1;
		}
		return false;
	}

	private static int corrupt( Hero h ){
		return Survival.count(h, TaintedFood.TaintedMeat.class) + Survival.count(h, TaintedFood.TaintedRation.class);
	}

	public static String label( Recipe r ){
		return Messages.get(FireRecipes.class, r.name().toLowerCase());
	}

	public static void make( Campfire fire, Hero h, Recipe r ){
		if (!fire.isAlive() || !fire.lit() || !canMake(h, r)) return;
		Item out;
		switch (r){
			case KIT:
				Survival.take(h, Torch.class, 2);
				out = new CampfireKit();
				break;
			case TORCHES:
				Survival.take(h, ColossusBone.class, 1);
				out = new Torch().quantity(2);
				break;
			case BAIT:
				if (!Survival.take(h, TaintedFood.TaintedMeat.class, 1)) Survival.take(h, ColossusMeat.Raw.class, 1);
				out = new Bait();
				break;
			case POULTICE:
				for (int i = 0; i < 2; i++){
					if (!Survival.take(h, TaintedFood.TaintedRation.class, 1)) Survival.take(h, TaintedFood.TaintedMeat.class, 1);
				}
				out = new Poultice();
				break;
			case GRAPPLE:
				Survival.take(h, ColossusBone.class, 1);
				out = new BoneGrapple();
				break;
			case COLLAR:
				Survival.take(h, Torch.class, 2);
				Survival.take(h, ColossusBone.class, 1);
				out = new LightCollar();
				break;
			case SACRED_TORCH: default:
				Survival.take(h, Torch.class, 1);
				out = new SacredTorch();
				break;
		}
		CoopLog.line("survival: hero " + Party.indexOf(h) + " made " + r + " at the fire");
		GLog.p(Messages.get(FireRecipes.class, "made", out.name()));
		Survival.give(h, out);
		Sample.INSTANCE.play(Assets.Sounds.ITEM);
		work(h, fire, TIME_TO_MAKE);
	}

	// *** cooking ***

	public static int cookable( Hero h ){
		return Survival.count(h, ColossusMeat.Raw.class) + corrupt(h);
	}

	//everything raw or corrupt in the bag goes on the fire at once
	public static void cook( Campfire fire, Hero h ){
		if (!fire.isAlive() || !fire.lit() || cookable(h) == 0) return;
		ArrayList<Item> done = new ArrayList<>();
		int n = 0;
		n += swap(h, ColossusMeat.Raw.class, new ColossusMeat.Roast(), done);
		n += swap(h, TaintedFood.TaintedMeat.class, new StewedMeat(), done);
		n += swap(h, TaintedFood.TaintedRation.class, new SmallRation(), done);
		for (Item i : done) Survival.give(h, i);
		CoopLog.line("survival: hero " + Party.indexOf(h) + " cooked " + n);
		GLog.p(Messages.get(FireRecipes.class, "cooked", n));
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		work(h, fire, TIME_TO_COOK);
	}

	private static int swap( Hero h, Class<? extends Item> raw, Item cooked, ArrayList<Item> done ){
		int n = Survival.count(h, raw);
		if (n == 0) return 0;
		Survival.take(h, raw, n);
		done.add(cooked.quantity(n));
		return n;
	}

	public static void feed( Campfire fire, Hero h ){
		if (!fire.isAlive() || !Survival.take(h, Torch.class, 1)) return;
		fire.feed();
		GLog.i(Messages.get(FireRecipes.class, "fed"));
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		work(h, fire, TIME_TO_FEED);
	}

	//the hero kneels by the fire for a bit
	private static void work( Hero h, Campfire fire, float time ){
		h.spend(time);
		h.busy();
		if (h.sprite != null && h.sprite.parent != null){
			h.sprite.operate(fire.pos);
		} else {
			h.next();
		}
	}

	public static boolean nearEnough( Hero h, Campfire fire ){
		return fire.isAlive() && Dungeon.level.distance(h.pos, fire.pos) <= 1;
	}
}
