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

package com.shatteredpixel.shatteredpixeldungeon.champions.forger;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//next hit from ANY party hero = stun + bonus dmg. Char.attack calls proc() before dmg, afterHit() after
public class Brittle extends FlavourBuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public static final float DURATION = 12f;

	public int bonus = 2;
	public int caster = 0;          //party idx of the forger
	public boolean triggered = false; //mid-hit flag

	public static int bonus( Hero forger ){
		return 3 + forger.lvl/2;
	}

	public static void apply( Hero forger, Char ch ){
		apply(forger, ch, DURATION);
	}

	public static void apply( Hero forger, Char ch, float turns ){
		if (ch == null || !ch.isAlive() || !CoopRules.hits(forger, ch)) return;
		Brittle b = Buff.prolong(ch, Brittle.class, turns);
		b.bonus = Math.max(b.bonus, bonus(forger));
		b.caster = ChampionHooks.indexOf(forger);
		b.triggered = false;
		if (ch.sprite != null) CellEmitter.get(ch.pos).burst(Speck.factory(Speck.ROCK), 4);
	}

	//pre-armor
	public static float proc( Char attacker, Char enemy, float dmg ){
		if (!(attacker instanceof Hero)) return dmg;
		Brittle b = enemy.buff(Brittle.class);
		if (b == null) return dmg;
		b.triggered = true;
		return dmg + b.bonus;
	}

	public static void afterHit( Char attacker, Char enemy ){
		if (!(attacker instanceof Hero)) return;
		Brittle b = enemy.buff(Brittle.class);
		if (b == null || !b.triggered) return;
		b.detach();
		if (enemy.isAlive()){
			Buff.prolong(enemy, Paralysis.class, 2f);
			if (enemy.sprite != null) enemy.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Brittle.class, "shattered"));
		}
		if (attacker instanceof Hero) ForgerHooks.onBrittleHit((Hero) attacker);
	}

	@Override
	public int icon() {
		return BuffIndicator.VULNERABLE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(1f, 0.55f, 0.25f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", bonus, dispTurns());
	}

	private static final String BONUS = "bonus";
	private static final String CASTER = "caster";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(BONUS, bonus);
		bundle.put(CASTER, caster);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		bonus = bundle.getInt(BONUS);
		caster = bundle.getInt(CASTER);
	}
}
