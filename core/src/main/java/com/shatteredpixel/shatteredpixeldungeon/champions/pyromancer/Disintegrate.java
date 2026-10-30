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
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

//Q: fireball. kill = cd refund
public class Disintegrate extends ChampionAbility {

	public static final Disintegrate INSTANCE = new Disintegrate();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.LOL_DISINTEGRATE; }
	@Override public int color() { return 0xFF7722; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 6;

	@Override
	public float cooldown( Hero hero ) {
		return 5;
	}

	public static int minDmg( Hero hero ){
		return 2 + hero.lvl/2 + hero.pointsInTalent(Talent.PYR_SPARK);
	}

	public static int maxDmg( Hero hero ){
		return 6 + hero.lvl + hero.pointsInTalent(Talent.PYR_SPARK);
	}

	public static float multiplier( Hero hero, Char target ){
		float m = 1f + 0.1f*hero.pointsInTalent(Talent.PYR_COMBUSTION);
		m *= Incinerate.burningBonus(hero, target);
		return m;
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

		final boolean stun = Pyromania.onOffensiveCast(hero);
		hero.busy();
		hero.sprite.zap(enemy.pos);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
		Invisibility.dispel(hero);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.FIRE, hero.sprite, enemy.pos, new Callback() {
			@Override
			public void call() {
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				if (enemy.isAlive()){
					int dmg = Math.round(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)) * multiplier(hero, enemy));
					enemy.damage(dmg, Disintegrate.this);
					if (stun) Pyromania.stun(hero, enemy);
					if (enemy.sprite != null && enemy.sprite.visible){
						CellEmitter.center(enemy.pos).burst(FlameParticle.FACTORY, 8);
					}
					//pyroclasm, splash
					int splash = hero.subClass == HeroSubClass.INFERNALIST ? hero.pointsInTalent(Talent.IN_PYROCLASM) : 0;
					if (splash > 0){
						for (int n : PathFinder.NEIGHBOURS8){
							Char ch = Actor.findChar(enemy.pos + n);
							if (ch != null && CoopRules.hits(hero, ch) && ch.isAlive()){
								ch.damage(Math.round(dmg * splash / 3f), Disintegrate.this);
								if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch);
							}
						}
					}
				}
				if (!enemy.isAlive()){
					hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(Disintegrate.class, "refund"));
					resetCooldown(hero);
					int c = hero.pointsInTalent(Talent.PYR_COMBUSTION);
					if (c > 0) ChampionHooks.reduceCooldowns(hero, c);
				} else {
					startCooldown(hero);
				}
				hero.spendAndNext(1f);
			}
		});
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", minDmg(hero), maxDmg(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
