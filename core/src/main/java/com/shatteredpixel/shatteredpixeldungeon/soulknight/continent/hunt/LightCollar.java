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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

//a band of sacred light, made by a fire. put on one of the beasts that can be tamed, when it lets
//you (each kind its own way), it's the party's from then on. only one at a time
public class LightCollar extends Item {

	public static final String AC_TAME = "TAME";

	{
		image = ItemSpriteSheet.LIGHT_COLLAR;
		defaultAction = AC_TAME;
		stackable = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_TAME);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_TAME)) return;
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell == null) return;
				Colossus c = Colossus.at(cell);
				String no;
				if (!(c instanceof AshColossus) || !((AshColossus) c).tameable()){
					no = Messages.get(LightCollar.class, "wild");
				} else if (c.reach(hero.pos) > 1){
					no = Messages.get(LightCollar.class, "far");
				} else if (Hunt.tamedHere() != null){
					no = Messages.get(LightCollar.class, "one");
				} else {
					no = ((AshColossus) c).tameBlock(hero);
				}
				if (no != null){
					if (hero == Dungeon.local()) GLog.w(no);
					return;
				}
				detach(hero.belongings.backpack);
				((AshColossus) c).tame(hero);
				hero.spendAndNext(1f);
			}

			@Override
			public String prompt() {
				return Messages.get(LightCollar.class, "prompt");
			}
		});
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
		return 40 * quantity;
	}
}
