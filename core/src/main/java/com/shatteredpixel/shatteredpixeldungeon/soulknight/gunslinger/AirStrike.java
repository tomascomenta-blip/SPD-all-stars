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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.gunslinger;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//E: air strike. bombs marked around the spot, land next turn, enemies only
public class AirStrike extends ChampionAbility {

	public static final AirStrike INSTANCE = new AirStrike();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.GS_AIRSTRIKE; }
	@Override public int color() { return 0xFF7733; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 8;

	@Override
	public float cooldown( Hero hero ) {
		return 40;
	}

	public static int bombs( Hero hero ){
		return 5 + hero.pointsInTalent(Talent.GS_CARPET_BOMB);
	}

	public static int minDmg( Hero hero ){
		return 6 + hero.lvl/2;
	}

	public static int maxDmg( Hero hero ){
		return 12 + hero.lvl;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target] || Dungeon.level.solid[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		ArrayList<Integer> around = SKCombat.openArea(target, 2);
		around.remove((Integer) target);
		ArrayList<Integer> drops = new ArrayList<>();
		drops.add(target);
		for (int i = 1; i < bombs(hero) && !around.isEmpty(); i++){
			drops.add(around.remove(Random.Int(around.size())));
		}
		LinkedHashSet<Integer> blast = new LinkedHashSet<>();
		for (int c : drops) blast.addAll(SKCombat.openArea(c, 1));
		SKCombat.mark(blast, 1f);

		Strike s = Buff.append(hero, Strike.class, 1f);
		s.cells = new ArrayList<>(blast);
		s.drops = drops;
		s.min = minDmg(hero);
		s.max = maxDmg(hero);
		Sample.INSTANCE.play(Assets.Sounds.BEACON, 0.8f, 1.4f);

		ChampionHooks.operateVisual(hero, target);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, bombs(hero), minDmg(hero), maxDmg(hero));
	}

	//incoming. falls when this expires
	public static class Strike extends FlavourBuff {

		{
			type = buffType.NEUTRAL;
		}

		public ArrayList<Integer> cells = new ArrayList<>();
		public ArrayList<Integer> drops = new ArrayList<>();
		public int min, max;

		@Override
		public boolean act() {
			for (int c : drops){
				if (Dungeon.level.heroFOV[c]){
					CellEmitter.center(c).burst(BlastParticle.FACTORY, 20);
					CellEmitter.get(c).burst(SmokeParticle.FACTORY, 4);
				}
			}
			for (int c : cells){
				if (Dungeon.level.heroFOV[c] && Random.Visual.Int(3) == 0) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
			}
			Hero hero = target instanceof Hero ? (Hero) target : null;
			for (Char ch : SKCombat.foesIn(target, cells)){
				float dmg = Random.NormalIntRange(min, max);
				if (hero != null) dmg *= GunslingerHooks.shotFactor(hero);
				ch.damage(Math.max(1, Math.round(dmg) - ch.drRoll()/2), INSTANCE);
			}
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
			PixelScene.shake(2, 0.4f);
			detach();
			return true;
		}

		private static final String CELLS = "cells";
		private static final String DROPS = "drops";
		private static final String MIN   = "min";
		private static final String MAX   = "max";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			int[] c = new int[cells.size()];
			for (int i = 0; i < c.length; i++) c[i] = cells.get(i);
			int[] d = new int[drops.size()];
			for (int i = 0; i < d.length; i++) d[i] = drops.get(i);
			bundle.put(CELLS, c);
			bundle.put(DROPS, d);
			bundle.put(MIN, min);
			bundle.put(MAX, max);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			cells.clear();
			drops.clear();
			for (int c : bundle.getIntArray(CELLS)) cells.add(c);
			for (int d : bundle.getIntArray(DROPS)) drops.add(d);
			min = bundle.getInt(MIN);
			max = bundle.getInt(MAX);
		}
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
