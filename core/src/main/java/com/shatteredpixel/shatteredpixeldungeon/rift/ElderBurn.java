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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//purple fire. flat dmg per turn, water does nothing, fire immunity does nothing. es fuego morado, ya
public class ElderBurn extends Buff implements Buff.DOTbuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public static final int COLOR = 0xAA66FF;

	public int power = 0;
	public float left = 0;

	public static void apply( Char ch, int power, float turns ){
		if (ch == null || !ch.isAlive()) return;
		ElderBurn b = Buff.affect(ch, ElderBurn.class);
		if (b.target != ch) return;
		b.power = Math.max(b.power, power);
		b.left = Math.max(b.left, turns);
		ch.needsIncomingDOTUpdate = true;
	}

	@Override
	public boolean act() {
		if (!target.isAlive() || power <= 0){
			detach();
			return true;
		}
		target.damage(power, this);
		if (target.sprite != null && target.sprite.visible){
			target.sprite.emitter().burst(PurpleParticle.BURST, 4);
		}
		if (!target.isAlive()){
			if (target instanceof Hero){
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "ondeath"));
			}
			detach();
			return true;
		}
		left -= TICK;
		if (left <= 0){
			detach();
		} else {
			target.needsIncomingDOTUpdate = true;
			spend(TICK);
		}
		return true;
	}

	@Override
	public void detach() {
		if (target != null) target.needsIncomingDOTUpdate = true;
		super.detach();
	}

	@Override
	public int totalIncomingDMG() {
		return Math.round(power * Math.max(1f, left));
	}

	@Override
	public int icon() {
		return BuffIndicator.FIRE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(COLOR);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(Math.round(left));
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", power, dispTurns(left));
	}

	private static final String POWER = "power";
	private static final String LEFT = "left";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(POWER, power);
		bundle.put(LEFT, left);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		power = bundle.getInt(POWER);
		left = bundle.getFloat(LEFT);
	}
}
