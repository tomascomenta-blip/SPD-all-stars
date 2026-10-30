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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.automaton;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//Q: zaps the closest enemies every turn for a while, kills extend it. tesla coil = more targets
public class Overload extends ChampionAbility {

	public static final Overload INSTANCE = new Overload();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.AUT_OVERLOAD; }
	@Override public int color() { return 0x66CCFF; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final int RANGE = 3;
	public static final int MAX_EXTENSION = 5;

	@Override
	public float cooldown( Hero hero ) {
		return 30 * AutomatonHooks.cooldownFactor(hero);
	}

	public static int duration( Hero hero ){
		return 6 + hero.pointsInTalent(Talent.AUT_CAPACITOR);
	}

	public static int targets( Hero hero ){
		int n = 2;
		if (hero.subClass == HeroSubClass.TESLA_COIL) n += 2 + hero.pointsInTalent(Talent.TC_ARC);
		return n;
	}

	public static int damage( Hero hero ){
		return 2 + hero.lvl/3 + hero.pointsInTalent(Talent.AUT_HIGH_VOLTAGE);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Charge c = Buff.affect(hero, Charge.class);
		c.left = duration(hero);
		c.extended = 0;
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 1.2f);
		if (hero.sprite != null) hero.sprite.emitter().burst(SparkParticle.FACTORY, 12);
		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		startCooldown(hero);
		AutomatonHooks.afterAbility(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", duration(hero), targets(hero), RANGE, damage(hero), MAX_EXTENSION);
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	//1 zap per turn
	public static class Charge extends Buff {

		{
			type = buffType.POSITIVE;
			actPriority = HERO_PRIO - 1;
		}

		public int left;
		public int extended;

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || !target.isAlive()){
				detach();
				return true;
			}
			Hero hero = (Hero) target;
			discharge(hero);
			left--;
			if (left <= 0) detach();
			spend(TICK);
			return true;
		}

		private void discharge( Hero hero ){
			ArrayList<Char> foes = new ArrayList<>();
			for (Char ch : Actor.chars()){
				if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && ch.invisible <= 0
						&& Dungeon.level.distance(hero.pos, ch.pos) <= RANGE && Dungeon.level.heroFOV[ch.pos]){
					foes.add(ch);
				}
			}
			if (foes.isEmpty()) return;
			final int from = hero.pos;
			Collections.sort(foes, new Comparator<Char>() {
				@Override
				public int compare( Char a, Char b ) {
					int da = Dungeon.level.distance(from, a.pos), db = Dungeon.level.distance(from, b.pos);
					return da != db ? da - db : a.pos - b.pos;
				}
			});
			int n = Math.min(targets(hero), foes.size());
			ArrayList<Lightning.Arc> arcs = new ArrayList<>();
			int dmg = damage(hero);
			int stunChance = hero.subClass == HeroSubClass.TESLA_COIL ? hero.pointsInTalent(Talent.TC_SHOCK) : 0;
			for (int i = 0; i < n; i++){
				Char ch = foes.get(i);
				if (hero.sprite != null && ch.sprite != null) arcs.add(new Lightning.Arc(hero.sprite.center(), ch.sprite.center()));
				ch.damage(dmg, INSTANCE);
				if (ch.isAlive()){
					if (stunChance > 0 && Random.Int(10) < stunChance) Buff.prolong(ch, Paralysis.class, 1f);
				} else {
					if (extended < MAX_EXTENSION){
						extended++;
						left++;
					}
					if (hero.subClass == HeroSubClass.TESLA_COIL && hero.hasTalent(Talent.TC_FEEDBACK)){
						for (Buff b : hero.buffs()){
							if (b instanceof AbilityCooldown && !(b instanceof CD)){
								((AbilityCooldown) b).reduce(hero.pointsInTalent(Talent.TC_FEEDBACK));
							}
						}
					}
				}
			}
			if (hero.sprite != null && hero.sprite.parent != null && !arcs.isEmpty()){
				hero.sprite.parent.addToFront(new Lightning(arcs, null));
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.6f, 1.2f);
			}
		}

		@Override
		public int icon() {
			return BuffIndicator.RECHARGING;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.4f, 0.8f, 1f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", left);
		}

		private static final String LEFT = "left";
		private static final String EXTENDED = "extended";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
			bundle.put(EXTENDED, extended);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
			extended = bundle.getInt(EXTENDED);
		}
	}
}
