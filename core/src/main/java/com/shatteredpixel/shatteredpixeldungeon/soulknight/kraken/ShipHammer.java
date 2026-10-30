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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//everyone gets one on the ship. 1 turn = patch a hole next to/under you. removed after the fight
public class ShipHammer extends Item {

	public static final String AC_REPAIR = "REPAIR";
	public static final int REPAIR = 4;

	{
		image = ItemSpriteSheet.SHIP_HAMMER;
		defaultAction = AC_REPAIR;
		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_REPAIR);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_REPAIR)) return;
		if (!(Dungeon.level instanceof KrakenLevel)){
			GLog.i(Messages.get(this, "no_ship"));
			return;
		}
		KrakenLevel L = (KrakenLevel) Dungeon.level;
		int hole = L.holeNear(hero.pos, 1);
		if (hole == -1){
			GLog.i(Messages.get(this, "no_hole"));
			return;
		}
		L.removeHole(hole);
		L.repairShip(REPAIR);
		hero.sprite.turnTo(hero.pos, hole);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE, 0.6f, 1.6f);
		CellEmitter.get(hole).burst(Speck.factory(Speck.ROCK), 4);
		KrakenVisuals.holeClosed(hole);
		hero.spendAndNext(1f);
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}
}
