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

package com.shatteredpixel.shatteredpixeldungeon.champions.executioner;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//R: jump + true dmg scaling w/ bleed stacks. kill = reset, go again
public class Guillotine extends ChampionAbility {

	public static final Guillotine INSTANCE = new Guillotine();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.LOL_GUILLOTINE; }
	@Override public int color() { return 0xFF2020; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 3;

	@Override
	public float cooldown( Hero hero ) {
		float cd = 40;
		if (hero.subClass == HeroSubClass.HEADSMAN) cd *= 0.75f;
		return cd;
	}

	public static int baseDamage( Hero hero ){
		return Math.round((6 + 2.5f*hero.lvl) * (1f + 0.1f*hero.pointsInTalent(Talent.EXE_SHARP_GUILLOTINE)));
	}

	public static int damageAgainst( Hero hero, Char enemy ){
		int stacks = hero.buff(Hemorrhage.ExecutionerMight.class) != null
				? Hemorrhage.MAX_STACKS : Hemorrhage.stacksOn(enemy);
		return Math.round(baseDamage(hero) * (1f + 0.2f*stacks));
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		final Char enemy = ChampionHooks.boltTarget(hero, target);
		if (enemy == null || !CoopRules.hits(hero, enemy)){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, enemy.pos) > RANGE){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}

		int landing = hero.pos;
		if (!Dungeon.level.adjacent(hero.pos, enemy.pos)){
			if (hero.rooted){
				GLog.w(Messages.get(ChampionAbility.class, "rooted"));
				return;
			}
			landing = ChampionHooks.landingNextTo(hero, enemy.pos);
			if (landing == -1){
				GLog.w(Messages.get(this, "no_space"));
				return;
			}
		}

		hero.busy();
		final int dest = landing;
		final Callback strike = new Callback() {
			@Override
			public void call() {
				hero.sprite.attack(enemy.pos, new Callback() {
					@Override
					public void call() {
						execute(hero, enemy);
					}
				});
			}
		};

		if (dest != hero.pos){
			hero.sprite.jump(hero.pos, dest, new Callback() {
				@Override
				public void call() {
					ChampionHooks.placeHero(hero, dest);
					strike.call();
				}
			});
		} else {
			strike.call();
		}
	}

	private void execute( Hero hero, Char enemy ){
		if (enemy.isAlive()){
			int dmg = damageAgainst(hero, enemy);
			enemy.damage(dmg, this);
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.8f);
			if (enemy.sprite != null && enemy.sprite.visible){
				CellEmitter.center(enemy.pos).burst(BloodParticle.BURST, 14);
			}
			PixelScene.shake(2, 0.3f);
		}
		Invisibility.dispel(hero);
		if (!enemy.isAlive()){
			hero.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "executed"));
			resetCooldown(hero);
			if (hero.hasTalent(Talent.HS_REPEAT)){
				Buff.prolong(hero, Haste.class, hero.pointsInTalent(Talent.HS_REPEAT));
			}
		} else {
			startCooldown(hero);
		}
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, baseDamage(hero), Math.round(baseDamage(hero) * 2f));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
