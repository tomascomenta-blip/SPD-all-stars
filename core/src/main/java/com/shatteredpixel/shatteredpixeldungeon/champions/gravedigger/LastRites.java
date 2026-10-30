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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//Q: shovel ready (free action). next melee hit = bonus dmg + heal, +1 grave on kill,
//and if there's enough graves they all wake up as walkers
public class LastRites extends ChampionAbility {

	public static final LastRites INSTANCE = new LastRites();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.LOL_LAST_RITES; }
	@Override public int color() { return 0x88DDCC; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final float PRIMED_TURNS = 5f;

	@Override
	public float cooldown( Hero hero ) {
		float cd = 8;
		if (hero.subClass == HeroSubClass.EXHUMER){
			cd *= 0.75f * (1f - 0.15f*hero.pointsInTalent(Talent.EX_DIG_DEEP));
		}
		return cd;
	}

	public static float multiplier( Hero hero ){
		return 1f + 0.2f*hero.pointsInTalent(Talent.GRA_HEAVY_SHOVEL);
	}

	public static int minDmg( Hero hero ){
		return Math.round((1 + hero.lvl/2) * multiplier(hero));
	}

	public static int maxDmg( Hero hero ){
		return Math.round((3 + hero.lvl) * multiplier(hero));
	}

	//heal %
	public static int healPercent( Hero hero ){
		if (hero.subClass == HeroSubClass.EXHUMER){
			return 40 + 10*hero.pointsInTalent(Talent.EX_SOUL_SHOVEL);
		}
		return 25;
	}

	public static int walkersToRaise( Hero hero ){
		return ShepherdOfSouls.graves(hero) + hero.pointsInTalent(Talent.GRA_FLOCK);
	}

	public static boolean canAwaken( Hero hero ){
		return ShepherdOfSouls.graves(hero) >= ShepherdOfSouls.awakenAt(hero);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Buff.prolong(hero, Primed.class, PRIMED_TURNS);
		Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 0.6f, 0.8f);
		hero.sprite.showStatus(0x99DDCC, Messages.titleCase(name()));
		startCooldown(hero);

		if (canAwaken(hero)){
			//awakening, everyone up
			int graves = ShepherdOfSouls.graves(hero);
			int raised = MistWalker.raiseAround(hero, hero.pos, walkersToRaise(hero), false);
			if (raised > 0){
				ShepherdOfSouls.spendGraves(hero, graves);
				GLog.p(Messages.get(this, "awaken", raised));
				Invisibility.dispel(hero);
				GravediggerHooks.castVisual(hero, hero.pos);
				hero.spendAndNext(1f);
				return;
			}
			GLog.w(Messages.get(this, "no_space"));
		}

		//0 time. gripping a shovel is free
		hero.spendAndNext(0f);
	}

	public static int proc( Hero hero, Char enemy, int damage ){
		Primed p = hero.buff(Primed.class);
		if (p == null) return damage;
		p.detach();
		damage += Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero));
		ChampionHooks.heal(hero, Math.round(damage * healPercent(hero) / 100f));
		if (enemy.isAlive()){
			Reaped r = Buff.affect(enemy, Reaped.class, 1f);
			r.owner = ChampionHooks.indexOf(hero);
		}
		if (enemy.sprite != null && enemy.sprite.visible){
			enemy.sprite.emitter().burst(ShadowParticle.UP, 6);
		}
		return damage;
	}

	//reaped enemy died -> +1 grave
	public static void onEnemyDeath( Mob mob ){
		Reaped r = mob.buff(Reaped.class);
		if (r == null || !ShepherdOfSouls.counts(mob)) return;
		Hero owner = ChampionHooks.heroByIndex(r.owner);
		if (owner == null || !owner.isAlive() || owner.heroClass != HeroClass.GRAVEDIGGER) return;
		ShepherdOfSouls.gain(owner, 1, mob.pos);
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", minDmg(hero), maxDmg(hero), healPercent(hero),
				ShepherdOfSouls.awakenAt(hero), MistWalker.health(hero), Math.round(MistWalker.duration(hero)));
		d += "\n\n" + Messages.get(this, "graves", ShepherdOfSouls.graves(hero), ShepherdOfSouls.maxGraves(hero));
		return d;
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
			icon.hardlight(0.5f, 0.95f, 0.85f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / PRIMED_TURNS);
		}
	}

	//hit by Q this turn, dies now = +1 grave
	public static class Reaped extends FlavourBuff {

		public int owner = 0;

		@Override
		public int icon() {
			return BuffIndicator.NONE;
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
}
