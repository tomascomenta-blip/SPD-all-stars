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

//maiden just hit this -> her gravedigger does extra to it
public class MaidenMark extends FlavourBuff {

	{
		type = buffType.NEGATIVE;
	}

	public static final float DURATION = 3f;

	public int owner = 0;

	@Override
	public int icon() {
		return BuffIndicator.MARK;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0.75f, 0.85f, 1f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, 1f - visualcooldown() / DURATION);
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
