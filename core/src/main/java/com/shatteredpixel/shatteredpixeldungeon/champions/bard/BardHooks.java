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

package com.shatteredpixel.shatteredpixeldungeon.champions.bard;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;

//bard hooks. same deal as the others, use the given hero, never Dungeon.local()
public class BardHooks {

	//minstrel bonus vs helpless enemies. +20%
	public static final float MINSTREL_BONUS = 0.2f;

	public static void ensurePassives( Hero hero ){
		Buff.affect(hero, Refrain.class);
	}

	//asleep/paralysed/slowed
	public static boolean helpless( Char enemy ){
		if (enemy.paralysed > 0 || enemy.buff(MagicalSleep.class) != null || enemy.buff(Slow.class) != null) return true;
		return enemy instanceof Mob && ((Mob) enemy).state == ((Mob) enemy).SLEEPING;
	}

	//Valor isn't here, Char.attack does it for every hero (Ballad.Valor). took me a while to find that again
	public static int onAttackProc( Hero hero, Char enemy, int damage, boolean melee ){
		if (hero.subClass == HeroSubClass.MINSTREL && helpless(enemy)){
			damage = Math.round(damage * (1f + MINSTREL_BONUS));
		}
		return damage;
	}

	public static void onHeroDamaged( Hero hero, int effectiveDamage, Object src ){
	}

	public static int drBonus( Hero hero ){
		return 0;
	}

	//Dungeon.hero here = the context hero, not the local one
	public static void onMobDeath( Mob mob, Object cause ){
	}

	public static void onHeroMove( Hero hero, boolean travelling ){
	}

	//snack -> refrain notes
	public static void onFoodEaten( Hero hero ){
		int notes = hero.pointsInTalent(Talent.BAR_MEAL);
		Refrain r = Refrain.of(hero);
		if (notes > 0 && r != null) r.gain(notes);
	}

	//liquid music, potions cut song cds
	public static void onPotionUsed( Hero hero, float factor ){
		int p = hero.pointsInTalent(Talent.BAR_LIQUID_MUSIC);
		if (p > 0) ChampionHooks.reduceCooldowns(hero, factor * (1 + 2*p));
	}

	//keen ear: rings id faster
	public static float itemIDSpeedFactor( Hero hero, Item item ){
		if (item instanceof Ring){
			return 1f + 0.75f*hero.pointsInTalent(Talent.BAR_INTUITION);
		}
		return 1f;
	}
}
