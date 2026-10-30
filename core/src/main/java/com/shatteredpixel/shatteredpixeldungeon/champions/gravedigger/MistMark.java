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

package com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//marca de la niebla
public class MistMark extends FlavourBuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public static final float DAMAGE_BONUS = 1.25f;

	//party idx of the marker
	public int owner = 0;

	@Override
	public int icon() {
		return BuffIndicator.INVERT_MARK;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0.4f, 0.95f, 0.85f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, 1f - visualcooldown() / 14f);
	}

	private static final String OWNER = "owner";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNER, owner);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		owner = bundle.getInt(OWNER);
	}
}
