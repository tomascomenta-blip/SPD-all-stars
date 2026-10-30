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

package com.shatteredpixel.shatteredpixeldungeon.champions.guardian;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//guardian passive: melee lifesteal + permanent stacks from Siphoning Strike kills.
//also where the other abilities stash stuff that has to survive a save (last siphon target, fury hp).
//yes it's a junk drawer. it's a buff that's always there, so it's the junk drawer
public class SoulEater extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public int stacks = 0;
	public float drained = 0;      //leftover fraction
	public int siphonTarget = -1;  //last Q target id, this turn only
	public int furyBoost = 0;      //fury hp currently in HTBoost

	public static SoulEater of( Hero hero ){
		return hero.buff(SoulEater.class);
	}

	public static int stacks( Hero hero ){
		SoulEater s = of(hero);
		return s == null ? 0 : s.stacks;
	}

	public static float lifesteal( Hero hero ){
		return 0.08f + 0.03f*hero.pointsInTalent(Talent.GUA_LIFE_DRAIN);
	}

	public void drain( Hero hero, Char enemy, int damage ){
		int dealt = Math.min(damage, enemy.HP + enemy.shielding());
		if (dealt <= 0) return;
		drained += dealt * lifesteal(hero);
		if (drained >= 1f){
			int heal = (int) drained;
			drained -= heal;
			ChampionHooks.heal(hero, heal);
		}
	}

	public void gain( Hero hero, int amount ){
		if (amount <= 0) return;
		stacks += amount;
		if (hero.sprite != null){
			hero.sprite.showStatus(0xFFCC44, Messages.get(SoulEater.class, "stack", amount, stacks));
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.5f, 1.6f);
		if (hero.subClass == HeroSubClass.CURATOR){
			ChampionHooks.heal(hero, 2*amount);
		}
		BuffIndicator.refreshHero();
	}

	//take the fury hp back, but don't kill him doing it (that was a fun bug)
	public static void endFury( Hero hero ){
		SoulEater s = of(hero);
		if (s == null || s.furyBoost <= 0) return;
		hero.HTBoost -= s.furyBoost;
		s.furyBoost = 0;
		boolean alive = hero.HP > 0;
		hero.updateHT(false);
		if (alive && hero.HP < 1) hero.HP = 1;
	}

	@Override
	public boolean act() {
		if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.GUARDIAN){
			detach();
			return true;
		}
		Hero hero = (Hero) target;
		siphonTarget = -1;
		//safety net in case the fury ended w/o cleaning up. shouldn't happen. it happened
		if (furyBoost > 0 && hero.buff(FuryOfSands.Fury.class) == null){
			endFury(hero);
		}
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.SACRIFICE;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public String name() {
		return Messages.get(SoulEater.class, "name");
	}

	@Override
	public String desc() {
		Hero hero = target instanceof Hero ? (Hero) target : null;
		int pct = hero == null ? 8 : Math.round(lifesteal(hero) * 100);
		int bonus = hero == null ? stacks/4 : SiphoningStrike.stackBonus(hero);
		return Messages.get(SoulEater.class, "desc", pct, stacks, bonus);
	}

	private static final String STACKS = "stacks";
	private static final String DRAINED = "drained";
	private static final String SIPHON_TARGET = "siphon_target";
	private static final String FURY_BOOST = "fury_boost";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(STACKS, stacks);
		bundle.put(DRAINED, drained);
		bundle.put(SIPHON_TARGET, siphonTarget);
		bundle.put(FURY_BOOST, furyBoost);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getInt(STACKS);
		drained = bundle.getFloat(DRAINED);
		siphonTarget = bundle.contains(SIPHON_TARGET) ? bundle.getInt(SIPHON_TARGET) : -1;
		furyBoost = bundle.getInt(FURY_BOOST);
	}
}
