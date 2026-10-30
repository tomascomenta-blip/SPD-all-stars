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

package com.shatteredpixel.shatteredpixeldungeon.champions.jailer;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.Chains;
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//Q: hook. first enemy hit gets dragged over
public class DeathSentence extends ChampionAbility {

	public static final DeathSentence INSTANCE = new DeathSentence();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.LOL_DEATH_SENTENCE; }
	@Override public int color() { return 0x55EE99; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	@Override
	public float cooldown( Hero hero ) {
		return 14;
	}

	public static int range( Hero hero ){
		return 5 + hero.pointsInTalent(Talent.JAI_LONG_CHAIN);
	}

	public static int minDmg( Hero hero ){
		return 2 + hero.lvl/2 + bonus(hero);
	}

	public static int maxDmg( Hero hero ){
		return 5 + hero.lvl + bonus(hero);
	}

	private static int bonus( Hero hero ){
		int b = Damnation.abilityBonus(hero);
		if (hero.subClass == HeroSubClass.SOULREAPER) b += Damnation.abilityBonus(hero) * hero.pointsInTalent(Talent.SR_SOUL_FLAME);
		return b;
	}

	public static float multiplier( Hero hero ){
		return 1f + 0.15f*hero.pointsInTalent(Talent.JAI_DEATHS_GRIP);
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		final Char enemy = ChampionHooks.boltTarget(hero, target);
		if (enemy == null || !CoopRules.hits(hero, enemy)){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, enemy.pos) > range(hero)){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		final int pulledPos = ChampionHooks.pullDestination(hero, enemy);

		hero.busy();
		hero.sprite.zap(enemy.pos);
		Sample.INSTANCE.play(Assets.Sounds.CHAINS);
		Invisibility.dispel(hero);
		hero.sprite.parent.add(new Chains(hero.sprite.center(), enemy.sprite.center(),
				Effects.Type.ETHEREAL_CHAIN, new Callback() {
			@Override
			public void call() {
				if (enemy.isAlive()){
					enemy.damage(Math.round(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)) * multiplier(hero)), DeathSentence.this);
				}
				if (enemy.isAlive()){
					if (pulledPos != -1) ChampionHooks.moveChar(enemy, pulledPos);
					Buff.prolong(enemy, Cripple.class, 2f);
					if (hero.hasTalent(Talent.JAI_WARDENS_GAZE)){
						Buff.prolong(enemy, Vulnerable.class, 1 + hero.pointsInTalent(Talent.JAI_WARDENS_GAZE));
					}
				}
				startCooldown(hero);
				hero.spendAndNext(1f);
			}
		}));
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", range(hero), minDmg(hero), maxDmg(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
