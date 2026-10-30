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

package com.shatteredpixel.shatteredpixeldungeon.champions.pyromancer;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

import java.util.HashSet;
import java.util.LinkedHashSet;

//pyro passive: every few casts the next damaging one stuns. + fire resist (immune for infernalist, obv)
public class Pyromania extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public int stacks = 0;

	public static int needed( Hero hero ){
		return hero.hasTalent(Talent.PYR_EARLY_SPARK) ? 3 : 4;
	}

	public static float stunTurns( Hero hero ){
		return 2f + (hero.pointsInTalent(Talent.PYR_EARLY_SPARK) >= 2 ? 1 : 0);
	}

	public boolean charged(){
		return target instanceof Hero && stacks >= needed((Hero) target);
	}

	public static Pyromania of( Hero hero ){
		return hero.buff(Pyromania.class);
	}

	public void gain( int amount ){
		if (!(target instanceof Hero)) return;
		boolean was = charged();
		stacks = Math.min(needed((Hero) target), stacks + amount);
		if (!was && charged() && target.sprite != null){
			target.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Pyromania.class, "charged"));
		}
		BuffIndicator.refreshHero();
	}

	//true = this cast stuns
	public static boolean onOffensiveCast( Hero hero ){
		Pyromania p = of(hero);
		if (p == null) return false;
		if (p.charged()){
			p.stacks = 0;
			BuffIndicator.refreshHero();
			return true;
		}
		p.gain(1);
		return false;
	}

	//support casts just add a stack
	public static void onSupportCast( Hero hero ){
		Pyromania p = of(hero);
		if (p != null && !p.charged()) p.gain(1);
	}

	public static void stun( Hero hero, Char ch ){
		if (ch.isAlive() && CoopRules.hits(hero, ch)){
			Buff.prolong(ch, Paralysis.class, stunTurns(hero));
		}
		//tamer: stun -> angry bear
		if (hero.subClass == HeroSubClass.TAMER){
			for (Char c : Actor.chars()){
				if (c instanceof EmberBear && ((EmberBear) c).ownedBy(hero)){
					Buff.prolong(c, Haste.class, 3f);
				}
			}
		}
	}

	@Override
	public boolean act() {
		if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.PYROMANCER){
			detach();
			return true;
		}
		spend(TICK);
		return true;
	}

	@Override
	public HashSet<Class> resistances() {
		HashSet<Class> r = new LinkedHashSet<>(super.resistances());
		r.add(Burning.class);
		r.add(Fire.class);
		return r;
	}

	@Override
	public HashSet<Class> immunities() {
		HashSet<Class> r = new LinkedHashSet<>(super.immunities());
		if (target instanceof Hero && ((Hero) target).subClass == HeroSubClass.INFERNALIST){
			r.add(Burning.class);
			r.add(Fire.class);
		}
		return r;
	}

	@Override
	public int icon() {
		return BuffIndicator.FIRE;
	}

	@Override
	public void tintIcon( Image icon ) {
		if (charged()) icon.hardlight(1f, 0.9f, 0.2f);
		else           icon.hardlight(0.8f, 0.4f, 0.1f);
	}

	@Override
	public float iconFadePercent() {
		if (!(target instanceof Hero)) return 0;
		return 1f - stacks / (float) needed((Hero) target);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public String name() {
		return Messages.get(Pyromania.class, "name");
	}

	@Override
	public String desc() {
		int need = target instanceof Hero ? needed((Hero) target) : 4;
		String d = Messages.get(Pyromania.class, "desc", need, stacks, need);
		if (charged()) d += "\n\n" + Messages.get(Pyromania.class, "desc_charged");
		return d;
	}

	private static final String STACKS = "stacks";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(STACKS, stacks);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getInt(STACKS);
	}
}
