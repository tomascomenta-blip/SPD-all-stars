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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.watabou.utils.Callback;

//gravedigger hooks, via ChampionHooks. given hero only, never Dungeon.local()
public class GravediggerHooks {

	//passives (run start + after revive)
	public static void ensurePassives( Hero hero ){
		Buff.affect(hero, ShepherdOfSouls.class);
	}

	public static int onAttackProc( Hero hero, Char enemy, int damage, boolean melee ){
		if (melee){
			float bonus = ShepherdOfSouls.exhumerBonus(hero);
			if (bonus > 0) damage = Math.round(damage * (1f + bonus));
			damage = LastRites.proc(hero, enemy, damage);
		}
		damage += Eulogy.maidenBonus(hero, enemy);
		return damage;
	}

	public static void onHeroDamaged( Hero hero, int effectiveDamage, Object src ){
	}

	public static int drBonus( Hero hero ){
		return 0;
	}

	//Dungeon.hero = context hero here
	public static void onMobDeath( Mob mob, Object cause ){
		ShepherdOfSouls.onEnemyDeath(mob);
		LastRites.onEnemyDeath(mob);
		MourningMist.onEnemyDeath(mob);
	}

	public static void onHeroMove( Hero hero, boolean travelling ){
	}

	//supper: food digs graves. don't think about it too hard
	public static void onFoodEaten( Hero hero ){
		if (hero.hasTalent(Talent.GRA_MEAL)){
			ShepherdOfSouls.gain(hero, hero.pointsInTalent(Talent.GRA_MEAL), hero.pos);
		}
	}

	//bottled mist: drink OR throw a potion -> walkers
	public static void onPotionUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.GRA_BOTTLED_MIST) && factor > 0){
			int count = Math.round(hero.pointsInTalent(Talent.GRA_BOTTLED_MIST) * factor);
			MistWalker.raiseAround(hero, hero.pos, count, false);
		}
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		if (item instanceof Armor){
			return 1f + 0.75f*hero.pointsInTalent(Talent.GRA_INTUITION);
		}
		return 1f;
	}

	//cast anim only. callback just goes back to idle so the anim end never touches the hero's action (coop rule #1)
	public static void castVisual( final Hero hero, int cell ){
		if (hero.sprite == null) return;
		hero.sprite.zap(cell, new Callback() {
			@Override
			public void call() {
				hero.sprite.idle();
			}
		});
	}

	//mourning mist target for allies. keep current one if marked, else closest visible marked one
	public static Char markedTarget( Mob ally, Char current ){
		if (ally.fieldOfView == null || ally.fieldOfView.length != Dungeon.level.length()) return null;
		if (ally.buff(Amok.class) != null || ally.buff(Terror.class) != null
				|| ally.buff(Dread.class) != null || ally.buff(Charm.class) != null){
			return null;
		}
		if (current != null && current.isAlive() && current.alignment == Char.Alignment.ENEMY
				&& current.buff(MistMark.class) != null && Actor.chars().contains(current)
				&& ally.fieldOfView[current.pos] && current.invisible <= 0){
			return current;
		}
		Char best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || m.buff(MistMark.class) == null) continue;
			if (!ally.fieldOfView[m.pos] || m.invisible > 0 || m.isInvulnerable(ally.getClass())) continue;
			int d = Dungeon.level.distance(ally.pos, m.pos);
			if (d < bestDist){
				best = m;
				bestDist = d;
			}
		}
		return best;
	}
}
