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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//sticks, a flint and something that burns, tied up. set it down and it's a fire
public class CampfireKit extends Item {

	public static final String AC_PLACE = "PLACE";

	public static final float TIME_TO_BUILD = 2f;

	{
		image = ItemSpriteSheet.CAMPFIRE_KIT;
		stackable = true;
		defaultAction = AC_PLACE;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_PLACE);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_PLACE)) return;

		int cell = Campfire.spotNear(hero.pos);
		if (cell == -1){
			GLog.w(Messages.get(this, "no_room"));
			return;
		}
		detach(hero.belongings.backpack);
		//the fire itself is made on the turn thread
		Buff.affect(hero, Building.class).cell = cell;
		hero.spend(TIME_TO_BUILD);
		hero.busy();
		hero.sprite.operate(cell);
		GLog.i(Messages.get(this, "building"));
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 15 * quantity;
	}

	public static class Building extends Buff {

		public int cell = -1;

		@Override
		public boolean act() {
			Hero h = (Hero) target;
			int at = cell;
			//someone stepped in the way meanwhile: next to the hero, wherever there's room
			if (at == -1 || Dungeon.level.distance(at, h.pos) > 1 || !Campfire.spotFree(at)){
				at = Campfire.spotNear(h.pos);
			}
			if (at == -1){
				Survival.give(h, new CampfireKit());
				if (h == Dungeon.local()) GLog.w(Messages.get(CampfireKit.class, "no_room"));
			} else {
				Campfire.build(at);
			}
			detach();
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("cell", cell);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			cell = bundle.getInt("cell");
		}
	}
}
