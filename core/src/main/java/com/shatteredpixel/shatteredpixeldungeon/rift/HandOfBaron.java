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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//hand of baron, rest of the run: every turn any ally on the floor without it gets empowered
//(summons, bear, clones, ghost, corrupted guys... all of them)
public class HandOfBaron extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	@Override
	public boolean act() {
		empowerAllies();
		spend(TICK);
		return true;
	}

	public static void empowerAllies(){
		if (Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			BaronEmpowered.empower(m);
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0x9933FF);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", Math.round((BaronEmpowered.FACTOR-1)*100),
				Math.round((BaronEmpowered.SKILL-1)*100));
	}
}
