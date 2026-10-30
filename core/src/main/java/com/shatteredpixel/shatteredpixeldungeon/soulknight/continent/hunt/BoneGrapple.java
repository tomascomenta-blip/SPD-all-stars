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
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

//a hook of bone on a cord: thrown at one of the big ones from right next to it, it gets you up
//on its back (Climbing). it isn't used up
public class BoneGrapple extends Item {

	public static final String AC_CLIMB = "CLIMB";

	{
		image = ItemSpriteSheet.BONE_GRAPPLE;
		defaultAction = AC_CLIMB;
		unique = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_CLIMB);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_CLIMB)) return;
		if (hero.buff(Climbing.class) != null){
			if (hero == Dungeon.local()) GLog.i(Messages.get(this, "already"));
			return;
		}
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell == null) return;
				Colossus c = Colossus.at(cell);
				if (!Climbing.canClimb(hero, c)){
					if (hero == Dungeon.local()){
						GLog.w(Messages.get(BoneGrapple.class, c == null || c.size < Climbing.SIZE ? "nothing" : "far"));
					}
					return;
				}
				Climbing.climb(hero, c);
				hero.spendAndNext(1f);
			}

			@Override
			public String prompt() {
				return Messages.get(BoneGrapple.class, "prompt");
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
		return 20;
	}
}
