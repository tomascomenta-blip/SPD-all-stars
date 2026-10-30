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

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.watabou.utils.Random;

//guardian hooks. given hero only (coop)
public class GuardianHooks {

	public static void ensurePassives( Hero hero ){
		Buff.affect(hero, SoulEater.class);
	}

	//returns new dmg
	public static int onAttackProc( Hero hero, Char enemy, int damage, boolean melee ){
		SoulEater se = Buff.affect(hero, SoulEater.class);
		//reset first, only this hit can earn stacks
		se.siphonTarget = -1;
		if (!melee) return damage;

		damage = SiphoningStrike.proc(hero, enemy, damage);

		if (FuryOfSands.active(hero) && hero.subClass == HeroSubClass.COLOSSUS
				&& hero.hasTalent(Talent.CO_WITHERING_STEPS) && CoopRules.hits(hero, enemy)){
			Wither.wither(hero, enemy, 1 + hero.pointsInTalent(Talent.CO_WITHERING_STEPS), false);
		}

		if (CoopRules.hits(hero, enemy)) se.drain(hero, enemy, damage);
		return damage;
	}

	public static void onHeroDamaged( Hero hero, int effectiveDamage, Object src ){
	}

	public static int drBonus( Hero hero ){
		int dr = 0;
		if (hero.hasTalent(Talent.GUA_SAND_SKIN) && hero.HP < hero.HT / 2){
			int p = hero.pointsInTalent(Talent.GUA_SAND_SKIN);
			dr += Random.NormalIntRange(p, 2*p);
		}
		if (FuryOfSands.active(hero)){
			dr += Random.NormalIntRange(0, FuryOfSands.armor(hero));
			dr += FuryOfSands.unbroken(hero);
		}
		return dr;
	}

	//context hero = Dungeon.hero
	public static void onMobDeath( Mob mob, Object cause ){
		Hero hero = null;
		if (cause instanceof Hero){
			hero = (Hero) cause;
		} else if (cause instanceof Weapon || cause instanceof Weapon.Enchantment){
			//weapon enchant procs
			hero = Dungeon.hero;
		}
		if (hero == null || hero.heroClass != HeroClass.GUARDIAN) return;
		SiphoningStrike.onKill(hero, mob);
	}

	public static void onHeroMove( Hero hero, boolean travelling ){
	}

	public static void onFoodEaten( Hero hero ){
		if (hero.heroClass != HeroClass.GUARDIAN || !hero.hasTalent(Talent.GUA_MEAL)) return;
		SiphoningStrike.INSTANCE.resetCooldown(hero);
		if (hero.pointsInTalent(Talent.GUA_MEAL) >= 2){
			SoulEater se = SoulEater.of(hero);
			if (se != null) se.gain(hero, 1);
		}
		Item.updateQuickslot();
	}

	public static void onPotionUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.GUA_LIQUID_SAND)){
			ChampionHooks.reduceCooldowns(hero, factor * (1 + 2*hero.pointsInTalent(Talent.GUA_LIQUID_SAND)));
		}
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		if (item instanceof MeleeWeapon){
			return 1f + 0.75f*hero.pointsInTalent(Talent.GUA_INTUITION);
		}
		return 1f;
	}
}
