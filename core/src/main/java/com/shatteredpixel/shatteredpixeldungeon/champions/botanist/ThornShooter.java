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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

//Q: sprout turret on a visible cell, ~10 turns. gardener of war gets 2 (2nd is weaker)
public class ThornShooter extends ChampionAbility {

	public static final ThornShooter INSTANCE = new ThornShooter();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.BOT_THORN_SHOOTER; }
	@Override public int color() { return 0x66CC33; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 7;

	@Override
	public float cooldown( Hero hero ) {
		return 14;
	}

	public static float duration( Hero hero ){
		return Math.round((10 + 3*hero.pointsInTalent(Talent.BOT_PRUNING)) * BotanistHooks.plantTime(hero));
	}

	public static float secondFactor( Hero hero ){
		return 0.6f + 0.2f*hero.pointsInTalent(Talent.WG_TWIN_SPROUTS);
	}

	private static boolean free( int c ){
		return Dungeon.level.insideMap(c) && !Dungeon.level.solid[c] && Dungeon.level.passable[c]
				&& !Dungeon.level.avoid[c] && Actor.findChar(c) == null;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		if (!free(target)){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		plantAt(hero, target, 1f);
		if (BotanistHooks.gardener(hero)){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = target + n;
				if (free(c) && Dungeon.level.distance(hero.pos, c) <= RANGE + 1){
					plantAt(hero, c, secondFactor(hero));
					break;
				}
			}
		}
		Dungeon.observe();
		Sample.INSTANCE.play(Assets.Sounds.PLANT);
		ChampionHooks.operateVisual(hero, target);
		Invisibility.dispel(hero);
		BotanistHooks.afterCast(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	private static void plantAt( Hero hero, int cell, float factor ){
		Sprout s = Sprout.plant(hero, cell, factor);
		GameScene.add(s);
		Dungeon.level.occupyCell(s);
		if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(LeafParticle.GENERAL, 6);
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", RANGE, Sprout.minDmg(hero.lvl), Sprout.maxDmg(hero.lvl), Math.round(duration(hero)));
		if (BotanistHooks.gardener(hero)) d += " " + Messages.get(this, "desc_gardener");
		return d;
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
