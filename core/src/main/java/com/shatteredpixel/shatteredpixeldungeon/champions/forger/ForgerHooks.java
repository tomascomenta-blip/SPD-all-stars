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

package com.shatteredpixel.shatteredpixeldungeon.champions.forger;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

//forger hooks. copy of the others. use the hero param, not Dungeon.local()
public class ForgerHooks {

	public static void ensurePassives( Hero hero ){
		Buff.affect(hero, LivingForge.class);
	}

	public static int onAttackProc( Hero hero, Char enemy, int damage, boolean melee ){
		if (melee && hero.hasTalent(Talent.FOR_HEAVY_HAMMER)){
			damage = Math.round(damage * (1f + 0.1f*hero.pointsInTalent(Talent.FOR_HEAVY_HAMMER)));
		}
		return damage;
	}

	public static int drBonus( Hero hero ){
		int dr = Random.NormalIntRange(1, 1 + hero.lvl/5);
		int p = hero.pointsInTalent(Talent.FOR_THICK_PLATES);
		if (p > 0) dr += Random.NormalIntRange(p, 2*p);
		if (hero.subClass == HeroSubClass.LIVINGMOUNTAIN && hero.hasTalent(Talent.LM_GRANITE_HEART) && hero.shielding() > 0){
			int g = hero.pointsInTalent(Talent.LM_GRANITE_HEART);
			dr += Random.NormalIntRange(g, 2*g);
		}
		return dr;
	}

	//anyone breaks a brittle enemy -> living mountain shield for the forger
	public static void onBrittleHit( Hero attacker ){
		if (attacker.heroClass != HeroClass.FORGER || attacker.subClass != HeroSubClass.LIVINGMOUNTAIN) return;
		shield(attacker, 2 + attacker.lvl/3 + 2*attacker.pointsInTalent(Talent.LM_STONE_SKIN));
	}

	public static void shield( Char ch, int amount ){
		if (amount <= 0) return;
		Barrier b = Buff.affect(ch, Barrier.class);
		if (b.shielding() < amount){
			b.setShield(amount);
			if (ch.sprite != null) ch.sprite.showStatusWithIcon(CharSprite.POSITIVE,
					Integer.toString(amount), FloatingText.SHIELDING);
		}
	}

	public static void onFoodEaten( Hero hero ){
		if (hero.heroClass != HeroClass.FORGER || !hero.hasTalent(Talent.FOR_MEAL)) return;
		MagmaRift.INSTANCE.resetCooldown(hero);
		if (hero.pointsInTalent(Talent.FOR_MEAL) >= 2){
			shield(hero, 3 + hero.lvl/2);
		}
		Item.updateQuickslot();
	}

	public static void onPotionUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.FOR_LIQUID_FORGE)){
			ChampionHooks.reduceCooldowns(hero, factor * (1 + 2*hero.pointsInTalent(Talent.FOR_LIQUID_FORGE)));
		}
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		if (item instanceof MeleeWeapon || item instanceof Armor){
			return 1f + 0.75f*hero.pointsInTalent(Talent.FOR_INTUITION);
		}
		return 1f;
	}

	//visual only
	public static void gesture( final Hero hero, int cell ){
		if (hero.sprite != null) hero.sprite.zap(cell, new Callback() {
			@Override
			public void call() {
				if (hero.sprite != null) hero.sprite.idle();
			}
		});
	}
}
