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

package com.shatteredpixel.shatteredpixeldungeon.champions.botanist;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss;
import com.shatteredpixel.shatteredpixeldungeon.plants.Stormvine;
import com.watabou.utils.Random;

//botanist hooks (the game -> ChampionHooks -> here)
//never Dungeon.local() in here, use the hero you get
public class BotanistHooks {

	//passive doesn't need a buff, nothing to do
	public static void ensurePassives( Hero hero ){
	}

	//gardener of war: stronger + longer plants
	public static boolean gardener( Hero hero ){
		return hero.subClass == HeroSubClass.WARGARDENER;
	}

	public static float plantDmg( Hero hero ){
		return gardener(hero) ? 1.25f : 1f;
	}

	public static float plantHealth( Hero hero ){
		return gardener(hero) ? 1.25f : 1f;
	}

	public static float plantTime( Hero hero ){
		return gardener(hero) ? 1.3f : 1f;
	}

	//for effects that only store a number (meadow) we just grab the first botanist. if you have 2 botanists... pues mala suerte
	public static Hero firstBotanist(){
		if (Party.active()){
			for (Hero h : Party.living()){
				if (h.heroClass == HeroClass.BOTANIST) return h;
			}
			return null;
		}
		return Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.BOTANIST ? Dungeon.hero : null;
	}

	//ancient bark: any cast = bark
	public static void afterCast( Hero hero ){
		int p = hero.pointsInTalent(Talent.BOT_ANCIENT_BARK);
		if (p > 0) Barkskin.conditionallyAppend(hero, 1 + p + hero.lvl/6, 4);
	}

	public static int onAttackProc( Hero hero, Char enemy, int damage, boolean melee ){
		return damage;
	}

	public static void onHeroDamaged( Hero hero, int effectiveDamage, Object src ){
	}

	public static int drBonus( Hero hero ){
		return 0;
	}

	public static void onMobDeath( Mob mob, Object cause ){
	}

	public static void onHeroMove( Hero hero, boolean travelling ){
	}

	//green thumb, grass heals a bit
	public static void onTrample( Hero hero ){
		ChampionHooks.heal(hero, 1 + hero.lvl/6 + 2*hero.pointsInTalent(Talent.BOT_FRESH_AIR));
	}

	//campfield snack, food -> a veces una semilla
	public static void onFoodEaten( Hero hero ){
		int p = hero.pointsInTalent(Talent.BOT_MEAL);
		if (p > 0 && (p >= 2 || Random.Int(2) == 0)){
			Item seed = Generator.random(Generator.Category.SEED);
			if (seed != null){
				seed.identify();
				if (!seed.collect(hero.belongings.backpack)){
					Dungeon.level.drop(seed, hero.pos).sprite.drop();
				}
			}
		}
	}

	//liquid sap: potions cut cooldowns
	public static void onPotionUsed( Hero hero, float factor ){
		int p = hero.pointsInTalent(Talent.BOT_LIQUID_SAP);
		if (p > 0) ChampionHooks.reduceCooldowns(hero, factor * (1 + 2*p));
	}

	//faster id for potions/seeds
	public static float itemIDSpeedFactor( Hero hero, Item item ){
		if (item instanceof Potion || item instanceof Plant.Seed){
			return 1f + 0.75f*hero.pointsInTalent(Talent.BOT_INTUITION);
		}
		return 1f;
	}

	//enemy stepped on one of our plants -> harmful ones hit harder
	public static void reinforce( Plant plant, Char ch ){
		if (!ch.isAlive()) return;
		if (plant instanceof Firebloom){
			Buff.affect(ch, Burning.class).reignite(ch);
		} else if (plant instanceof Sorrowmoss){
			Buff.affect(ch, Poison.class).extend(4);
		} else if (plant instanceof Blindweed){
			Buff.prolong(ch, Blindness.class, 4f);
		} else if (plant instanceof Stormvine){
			Buff.prolong(ch, Vertigo.class, 4f);
		}
	}

	//pruning: withered plant may drop its seed
	public static float seedChance( Plant plant ){
		if (plant.botanist < 0) return 0f;
		Hero owner = ChampionHooks.heroByIndex(plant.botanist);
		if (owner == null) return 0f;
		return 0.25f * owner.pointsInTalent(Talent.BOT_SEED_BAG);
	}
}
