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

package com.shatteredpixel.shatteredpixeldungeon.champions.pyromancer;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

//R: BEAR. drops from the sky on fire. R again while alive = point it at stuff
public class SummonBear extends ChampionAbility {

	public static final SummonBear INSTANCE = new SummonBear();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.LOL_SUMMON_BEAR; }
	@Override public int color() { return 0xFF3300; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 4;

	@Override
	public float cooldown( Hero hero ) {
		float cd = 70;
		if (hero.subClass == HeroSubClass.TAMER) cd *= 1f - 0.15f*hero.pointsInTalent(Talent.TM_RESUMMON);
		return cd;
	}

	//directing works during cd
	@Override
	public boolean onCooldown( Hero hero ) {
		return EmberBear.of(hero) == null && super.onCooldown(hero);
	}

	@Override
	public String prompt() {
		return Messages.get(this, "prompt");
	}

	public static int impactMin( Hero hero ){
		return 3 + hero.lvl/2;
	}

	public static int impactMax( Hero hero ){
		return 8 + hero.lvl;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;

		EmberBear bear = EmberBear.of(hero);
		if (bear != null){
			bear.directTocell(target);
			return;
		}

		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		int cell = target;
		if (Actor.findChar(cell) != null || Dungeon.level.solid[cell] || !Dungeon.level.passable[cell]){
			//lands next to the target cell
			cell = -1;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = target + n;
				if (Actor.findChar(c) == null && Dungeon.level.passable[c] && !Dungeon.level.solid[c]
						&& Dungeon.level.distance(hero.pos, c) <= RANGE + 1){
					cell = c;
					break;
				}
			}
			if (cell == -1){
				GLog.w(Messages.get(this, "no_space"));
				return;
			}
		}

		boolean stun = Pyromania.onOffensiveCast(hero);

		//thud
		for (int n : PathFinder.NEIGHBOURS9){
			int c = cell + n;
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(FlameParticle.FACTORY, 6);
			Char ch = Actor.findChar(c);
			if (ch != null && CoopRules.hits(hero, ch) && ch.isAlive()){
				ch.damage(Hero.heroDamageIntRange(impactMin(hero), impactMax(hero)), this);
				if (ch.isAlive()){
					Buff.affect(ch, Burning.class).reignite(ch);
					if (stun) Pyromania.stun(hero, ch);
				}
			}
		}
		if (Dungeon.level.heroFOV[cell]) CellEmitter.center(cell).burst(BlastParticle.FACTORY, 20);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		PixelScene.shake(2, 0.4f);

		bear = EmberBear.summon(hero, cell);
		GameScene.add(bear);
		Dungeon.level.occupyCell(bear);
		if (bear.sprite != null) bear.sprite.emitter().burst(FlameParticle.FACTORY, 15);
		Dungeon.observe();

		hero.sprite.zap(cell);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", RANGE, impactMin(hero), impactMax(hero), 15 + 5*hero.lvl);
		if (hero.subClass == HeroSubClass.TAMER) d += " " + Messages.get(this, "desc_tamer");
		return d;
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
