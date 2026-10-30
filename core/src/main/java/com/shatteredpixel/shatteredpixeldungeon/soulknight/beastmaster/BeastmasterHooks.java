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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;

//beastmaster hooks. passive is the wolf pack (Pack)
public class BeastmasterHooks {

	public static void initHero( Hero hero ){
		(hero.belongings.weapon = new CrispyBone()).identify();
		hero.belongings.weapon.activate(hero);

		BeastmasterKit kit = new BeastmasterKit();
		kit.identify().collect();
		Dungeon.quickslot.setSlot(0, kit);

		new PotionOfHealing().identify();
		new ScrollOfLullaby().identify();
	}

	public static void ensurePassives( Hero hero ){
		Buff.affect(hero, Pack.class);
	}

	public static int onAttackProc( Hero hero, Char enemy, int damage, boolean melee ){
		if (!melee) return damage;
		FrostfireWolves.Imbue imbue = hero.buff(FrostfireWolves.Imbue.class);
		if (imbue != null && enemy.isAlive()){
			damage += imbue.bonus;
			if (imbue.fire()){
				Buff.affect(enemy, Burning.class).reignite(enemy, 2f);
				if (enemy.sprite != null && enemy.sprite.visible) CellEmitter.get(enemy.pos).burst(FlameParticle.FACTORY, 4);
			} else {
				Buff.prolong(enemy, Chill.class, 2f);
				if (enemy.sprite != null && enemy.sprite.visible) CellEmitter.get(enemy.pos).burst(SnowParticle.FACTORY, 4);
			}
			imbue.flip();
		}
		//bone throw: extra vs stuff the wolves are already chewing on
		if (hero.hasTalent(Talent.BM_BONE_THROW) && hero.belongings.weapon() instanceof CrispyBone){
			for (Wolf w : SKAlly.of(hero, Wolf.class)){
				if (Dungeon.level.adjacent(w.pos, enemy.pos)){
					damage += hero.pointsInTalent(Talent.BM_BONE_THROW);
					break;
				}
			}
		}
		return damage;
	}

	public static int drBonus( Hero hero ){
		return FuzzyBear.drBonus(hero);
	}

	public static void onFoodEaten( Hero hero ){
		int p = hero.pointsInTalent(Talent.BM_SHARED_MEAL);
		if (p <= 0) return;
		for (Wolf w : SKAlly.of(hero, Wolf.class)){
			ChampionHooks.heal(w, Math.round(w.HT * 0.3f * p));
		}
	}

	//nature's bond, small heal when a beast gets a kill
	public static void onMobDeath( Mob mob, Object cause ){
		if (!(cause instanceof SKAlly)) return;
		Hero owner = ((SKAlly) cause).owner();
		if (owner == null || !owner.isAlive()) return;
		int p = owner.pointsInTalent(Talent.BM_NATURES_BOND);
		if (p > 0) ChampionHooks.heal(owner, 2*p);
	}
}
