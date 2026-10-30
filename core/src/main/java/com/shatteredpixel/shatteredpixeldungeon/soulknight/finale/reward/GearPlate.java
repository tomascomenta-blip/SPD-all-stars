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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//plates and cogs off the colossus' chest, still turning. tier 6 armor; now and then a blow
//lands a moment too late and goes right through you
public class GearPlate extends Armor {

	//percent of hits that miss the moment
	public static final int PHASE = 15;

	{
		image = ItemSpriteSheet.GEAR_PLATE;
		unique = true;
		bones = false;
	}

	public GearPlate() {
		super( 6 );
	}

	@Override
	public int proc( Char attacker, Char defender, int damage ) {
		damage = super.proc(attacker, defender, damage);
		if (damage > 0 && Random.Int(100) < PHASE){
			if (defender.sprite != null){
				defender.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "phase"));
			}
			return 0;
		}
		return damage;
	}

	@Override
	public String desc() {
		return super.desc() + "\n\n" + Messages.get(this, "phase_desc", PHASE);
	}
}
