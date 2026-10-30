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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//void corruption from baron's spit: -1 armor per stack (capped at the hero's avg armor), 8 turns,
//new stack refreshes it
public class BaronCorruption extends Buff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public static final int MAX_STACKS = 20;
	public static final float DURATION = 8f;
	public static final int COLOR = 0x9933FF;

	public int stacks = 0;
	public float left = 0;

	public static void addStack( Char ch ){
		if (ch == null || !ch.isAlive()) return;
		BaronCorruption c = Buff.affect(ch, BaronCorruption.class);
		if (c.target != ch) return;
		c.stacks = Math.min(MAX_STACKS, c.stacks + 1);
		c.left = DURATION;
	}

	public static int stacks( Char ch ){
		BaronCorruption c = ch.buff(BaronCorruption.class);
		return c == null ? 0 : c.stacks;
	}

	//armor taken away from a character, never more than what its armor usually blocks
	public static int armorShred( Char ch ){
		int s = stacks(ch);
		if (s <= 0) return 0;
		if (ch instanceof Hero){
			Armor armor = ((Hero) ch).belongings.armor();
			if (armor == null) return 0;
			return Math.min(s, (armor.DRMin() + armor.DRMax()) / 2);
		}
		return s;
	}

	@Override
	public boolean act() {
		left -= TICK;
		if (left <= 0 || !target.isAlive()){
			detach();
		} else {
			spend(TICK);
		}
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.CORRUPT;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(COLOR);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - left) / DURATION);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", stacks, MAX_STACKS, dispTurns(left));
	}

	private static final String STACKS = "stacks";
	private static final String LEFT = "left";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(STACKS, stacks);
		bundle.put(LEFT, left);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getInt(STACKS);
		left = bundle.getFloat(LEFT);
	}
}
