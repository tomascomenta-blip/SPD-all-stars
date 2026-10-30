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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.events.WorldEvents;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.BoneGrapple;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Hunt;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.HuntTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Mutations;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

//the rules of staying alive over there that aren't any one thing's: how fast the dark gets into
//you on each layer, how far you see, and what everyone carries when the party arrives
public class Survival {

	public static final String MESSAGES = "messages/soulknight/continent_survival";

	//16x24 frames: 0-3 burning, 4 out, 5 just built
	public static final String CAMPFIRE = "environment/surface/fogata.png";

	//turns between points of corruption from the place itself
	public static final int SURFACE_TAINT = 20, DEPTHS_TAINT = 6, ABYSS_TAINT = 3;

	//the old floors of the continent aren't open world: they count as surface
	public static int layer(){
		return Dungeon.level instanceof OpenWorld ? ((OpenWorld) Dungeon.level).layer() : OpenWorld.SURFACE;
	}

	//DarkTaint: 0 means it doesn't grow right now (by a fire)
	public static int taintEvery( Char ch ){
		if (Campfire.warms(ch) || Hunt.sheltered(ch)) return 0;
		switch (layer()){
			case OpenWorld.ABYSS:  return ABYSS_TAINT;
			case OpenWorld.DEPTHS: return DEPTHS_TAINT;
			default:               return SURFACE_TAINT;
		}
	}

	//Continent.vision: the sacred light reaches far, alone in the dark you see nothing
	public static int sight( Hero h, int v ){
		if (h.buff(SacredLight.class) != null) return Math.max(v, SacredLight.RADIUS);
		if (Tether.lost(h)) return 1;
		return v + Mutations.sight(h) - WorldEvents.sightCut();
	}

	//Continent.onArrive, every hero on every floor. first = they just came through the portal
	public static void onArrive( final Hero h, boolean first ){
		if (Tether.applies() && h.buff(Tether.class) == null) Buff.affect(h, Tether.class);
		if (!first) return;
		give(h, new CampfireKit());
		give(h, new BoneGrapple());
		HuntTest.stock(h);
		if (h == Dungeon.local()){
			GLog.i(Messages.get(Survival.class, "first"));
			if (Party.active()) GLog.w(Messages.get(Survival.class, "first_coop"));
		}
		CoopLog.line("survival: campfire kit for hero " + Party.indexOf(h));
	}

	//into the bag, or at their feet if it's full
	public static void give( final Hero h, final Item item ){
		Party.withHero(h, new Runnable() {
			@Override
			public void run() {
				if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos);
			}
		});
	}

	public static int count( Hero h, Class<? extends Item> cls ){
		int n = 0;
		for (Item i : h.belongings.getAllItems(cls)) n += i.quantity();
		return n;
	}

	//takes n of that kind out of the bag. false (and nothing taken) if there aren't enough
	public static boolean take( Hero h, Class<? extends Item> cls, int n ){
		if (count(h, cls) < n) return false;
		for (int i = 0; i < n; i++){
			Item it = h.belongings.getItem(cls);
			it.detach(h.belongings.backpack);
		}
		return true;
	}
}
