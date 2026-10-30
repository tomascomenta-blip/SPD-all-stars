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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

//Q (free): next melee hit gets bonus dmg from Soul Eater stacks. kill = permanent stacks. the farming button
public class SiphoningStrike extends ChampionAbility {

	public static final SiphoningStrike INSTANCE = new SiphoningStrike();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.LOL_SIPHONING_STRIKE; }
	@Override public int color() { return 0xFFCC44; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final float PRIMED_TURNS = 10f;

	@Override
	public float cooldown( Hero hero ) {
		float cd = 6;
		if (hero.subClass == HeroSubClass.CURATOR) cd -= hero.pointsInTalent(Talent.CU_QUICK_SIPHON);
		return cd;
	}

	//1 per 4 stacks, 1 per 3 for curator
	public static int stackBonus( Hero hero ){
		int s = SoulEater.stacks(hero);
		return hero.subClass == HeroSubClass.CURATOR ? s/3 : s/4;
	}

	public static int bonusDamage( Hero hero ){
		return 2 + hero.lvl/2 + 2*hero.pointsInTalent(Talent.GUA_HUNGRY_STAFF) + stackBonus(hero);
	}

	public static int stacksFor( Hero hero, Mob mob ){
		boolean boss = Char.hasProp(mob, Char.Property.BOSS);
		boolean big = Char.hasProp(mob, Char.Property.MINIBOSS) || mob.buff(ChampionEnemy.class) != null;
		//no stacks from junk (summons, splits, grey mobs). no farming rats on floor 1 sorry
		if (!boss && !big && (mob.EXP <= 0 || hero.lvl > mob.maxLvl)) return 0;

		int collector = hero.pointsInTalent(Talent.GUA_COLLECTOR);
		int amount;
		if (boss){
			amount = 5 + 2*collector;
		} else if (big){
			amount = 3 + collector;
		} else {
			amount = 1;
			if (mob.EXP >= 10) amount += collector;
		}
		if (hero.subClass == HeroSubClass.CURATOR && hero.hasTalent(Talent.CU_ARCHIVE)
				&& Random.Float() < 0.2f*hero.pointsInTalent(Talent.CU_ARCHIVE)){
			amount++;
		}
		return amount;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Buff.prolong(hero, Primed.class, PRIMED_TURNS);
		if (hero.sprite != null){
			hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.titleCase(name()));
			hero.sprite.emitter().burst(ShadowParticle.UP, 6);
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.5f, 0.8f);
		startCooldown(hero);
		hero.spendAndNext(0f);
	}

	public static int proc( Hero hero, Char enemy, int damage ){
		Primed p = hero.buff(Primed.class);
		if (p == null) return damage;
		p.detach();
		damage += bonusDamage(hero);
		SoulEater s = SoulEater.of(hero);
		if (s != null) s.siphonTarget = enemy.id();
		if (enemy.sprite != null) enemy.sprite.emitter().burst(ShadowParticle.CURSE, 8);
		Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC, 0.8f, 0.8f);
		return damage;
	}

	//was that our strike target?
	public static void onKill( Hero hero, Mob mob ){
		SoulEater s = SoulEater.of(hero);
		if (s == null || s.siphonTarget != mob.id()) return;
		s.siphonTarget = -1;
		s.gain(hero, stacksFor(hero, mob));
	}

	@Override
	public String desc( Hero hero ) {
		int collector = hero.pointsInTalent(Talent.GUA_COLLECTOR);
		return Messages.get(this, "desc", bonusDamage(hero), SoulEater.stacks(hero),
				hero.subClass == HeroSubClass.CURATOR ? 3 : 4, 3 + collector, 5 + 2*collector);
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	public static class Primed extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.WEAPON;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.8f, 0.25f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / PRIMED_TURNS);
		}

		@Override
		public String desc() {
			int dmg = target instanceof Hero ? bonusDamage((Hero) target) : 0;
			return Messages.get(this, "desc", dmg, dispTurns());
		}
	}
}
