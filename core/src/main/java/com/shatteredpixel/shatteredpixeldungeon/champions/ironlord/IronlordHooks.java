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

package com.shatteredpixel.shatteredpixeldungeon.champions.ironlord;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.watabou.utils.Random;

//ironlord hooks. hero param only
public class IronlordHooks {

	public static void ensurePassives( Hero hero ){
		Buff.affect(hero, RisingDarkness.class);
	}

	public static int onAttackProc( Hero hero, Char enemy, int damage, boolean melee ){
		if (!melee) return damage;
		damage = RealmOfDeath.boost(hero, enemy, damage);
		if (hero.subClass == HeroSubClass.TYRANT && RisingDarkness.DarkAura.active(hero)){
			damage += hero.pointsInTalent(Talent.TY_IRON_FIST);
		}
		if (enemy.alignment == Char.Alignment.ENEMY){
			RisingDarkness rd = Buff.affect(hero, RisingDarkness.class);
			RisingDarkness.addDamage(hero, damage);
			rd.onHit(hero);
		}
		return damage;
	}

	public static void onHeroDamaged( Hero hero, int effectiveDamage, Object src ){
	}

	public static int drBonus( Hero hero ){
		int p = hero.pointsInTalent(Talent.IRL_DARK_HIDE);
		if (p > 0 && RisingDarkness.DarkAura.active(hero)){
			return Random.NormalIntRange(p, 2*p);
		}
		return 0;
	}

	public static void onMobDeath( Mob mob, Object cause ){
	}

	//coop: he went down. if he was in the duel room, everybody out
	public static void onHeroFall( Hero hero ){
		RealmOfDeath.Duel d = hero.buff(RealmOfDeath.Duel.class);
		if (d != null) d.abort();
	}

	public static void onFoodEaten( Hero hero ){
		if (!hero.hasTalent(Talent.IRL_MEAL)) return;
		Obliterate.INSTANCE.resetCooldown(hero);
		if (hero.pointsInTalent(Talent.IRL_MEAL) >= 2){
			RisingDarkness.DarkAura.trigger(hero, 4, false);
		}
		Item.updateQuickslot();
	}

	public static void onPotionUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.IRL_LIQUID_IRON)){
			ChampionHooks.reduceCooldowns(hero, factor * (1 + 2*hero.pointsInTalent(Talent.IRL_LIQUID_IRON)));
		}
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		if (item instanceof MeleeWeapon){
			return 1f + 0.75f*hero.pointsInTalent(Talent.IRL_INTUITION);
		}
		return 1f;
	}
}
