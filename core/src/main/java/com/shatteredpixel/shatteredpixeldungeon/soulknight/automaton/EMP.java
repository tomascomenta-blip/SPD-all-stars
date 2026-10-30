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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//E: EMP around him. hurts + stuns nearby enemies, deletes enemy bullets/turrets in range. the panic button
public class EMP extends ChampionAbility {

	public static final EMP INSTANCE = new EMP();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.AUT_EMP; }
	@Override public int color() { return 0x3399FF; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		return 20 * AutomatonHooks.cooldownFactor(hero);
	}

	public static int radius( Hero hero ){
		return 2 + (hero.pointsInTalent(Talent.AUT_GROUNDING) >= 1 ? 1 : 0);
	}

	public static int stun( Hero hero ){
		return 2 + (hero.pointsInTalent(Talent.AUT_GROUNDING) >= 2 ? 1 : 0);
	}

	public static int damage( Hero hero ){
		return 3 + hero.lvl/2;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		discharge(hero);
		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		startCooldown(hero);
		AutomatonHooks.afterAbility(hero);
		hero.spendAndNext(1f);
	}

	//the actual pulse, failsafe calls it too. no time spent here
	public static void discharge( Hero hero ){
		int r = radius(hero);
		ArrayList<Integer> cells = SKCombat.openArea(hero.pos, r);
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(SparkParticle.FACTORY, 3);
		}
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
		PixelScene.shake(1, 0.3f);
		for (Char ch : SKCombat.foesIn(hero, cells)){
			ch.damage(damage(hero), INSTANCE);
			if (ch.isAlive()){
				boolean boss = Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
				Buff.prolong(ch, Paralysis.class, boss ? 1 : stun(hero));
			}
		}
		SKBulletField.destroyAround(hero.pos, r + 1);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", radius(hero), damage(hero), stun(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
