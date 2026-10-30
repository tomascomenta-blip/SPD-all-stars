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

package com.shatteredpixel.shatteredpixeldungeon.champions;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//just a FlavourBuff timer, nada más
public abstract class AbilityCooldown extends FlavourBuff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	public float max = 1;

	public abstract ChampionAbility ability();

	public void reduce( float turns ){
		if (cooldown() <= turns){
			detach();
		} else {
			spend(-turns);
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.TIME;
	}

	@Override
	public void tintIcon( Image icon ) {
		int c = ability().color();
		icon.hardlight(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, Math.min(1, visualcooldown() / Math.max(1, max)));
	}

	@Override
	public String name() {
		return Messages.get(AbilityCooldown.class, "name", ability().name());
	}

	@Override
	public String desc() {
		return Messages.get(AbilityCooldown.class, "desc", ability().name(), dispTurns());
	}

	private static final String MAX = "max";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(MAX, max);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		max = bundle.getFloat(MAX);
	}
}
