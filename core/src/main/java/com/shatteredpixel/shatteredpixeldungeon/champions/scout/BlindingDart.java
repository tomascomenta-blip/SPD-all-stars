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

package com.shatteredpixel.shatteredpixeldungeon.champions.scout;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Q: dart, dmg + blind. skirmisher's bounces
public class BlindingDart extends ChampionAbility {

	public static final BlindingDart INSTANCE = new BlindingDart();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.LOL_BLINDING_DART; }
	@Override public int color() { return 0xE8E060; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 6;

	@Override
	public float cooldown( Hero hero ) {
		return 10;
	}

	public static int minDmg( Hero hero ){
		return 2 + hero.lvl/3 + hero.pointsInTalent(Talent.SCO_BLINDING_EYE);
	}

	public static int maxDmg( Hero hero ){
		return 5 + hero.lvl + hero.pointsInTalent(Talent.SCO_BLINDING_EYE);
	}

	public static int blindTurns( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.SCO_BLINDING_EYE);
	}

	public static int bounces( Hero hero ){
		if (hero.subClass != HeroSubClass.SKIRMISHER) return 0;
		return 1 + hero.pointsInTalent(Talent.SK_RICOCHET);
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

		hero.busy();
		hero.sprite.zap(enemy.pos);
		Sample.INSTANCE.play(Assets.Sounds.ATK_SPIRITBOW, 1f, 1.6f);
		Invisibility.dispel(hero);
		startCooldown(hero);
		final ArrayList<Char> hitSoFar = new ArrayList<>();
		fire(hero, hero.pos, enemy, bounces(hero), hitSoFar, new Callback() {
			@Override
			public void call() {
				hero.spendAndNext(1f);
			}
		});
	}

	private void fire( final Hero hero, int from, final Char enemy, final int bouncesLeft,
	                   final ArrayList<Char> hitSoFar, final Callback done ){
		hitSoFar.add(enemy);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.POISON,
				from == hero.pos ? hero.sprite : enemyOrHeroSprite(from, hero), enemy.pos, new Callback() {
			@Override
			public void call() {
				if (enemy.isAlive()){
					enemy.damage(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)), BlindingDart.this);
					if (enemy.isAlive()){
						Buff.prolong(enemy, Blindness.class, blindTurns(hero));
					}
				}
				Char next = bouncesLeft > 0 ? nextTarget(enemy.pos, hitSoFar) : null;
				if (next != null){
					fire(hero, enemy.pos, next, bouncesLeft - 1, hitSoFar, done);
				} else {
					done.call();
				}
			}
		});
	}

	private static CharSprite enemyOrHeroSprite( int pos, Hero hero ){
		Char ch = Actor.findChar(pos);
		return ch != null && ch.sprite != null ? ch.sprite : hero.sprite;
	}

	//closest visible within 3, not hit yet. ties -> shared Random (NOT visual random, coop)
	private static Char nextTarget( int from, ArrayList<Char> exclude ){
		ArrayList<Char> options = new ArrayList<>();
		int best = Integer.MAX_VALUE;
		for (Char ch : Actor.chars()){
			if (ch.alignment != Char.Alignment.ENEMY || !ch.isAlive() || exclude.contains(ch)) continue;
			if (!Dungeon.level.heroFOV[ch.pos]) continue;
			int d = Dungeon.level.distance(from, ch.pos);
			if (d > 3) continue;
			if (d < best){
				best = d;
				options.clear();
			}
			if (d == best) options.add(ch);
		}
		return options.isEmpty() ? null : options.get(Random.Int(options.size()));
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", minDmg(hero), maxDmg(hero), blindTurns(hero));
		if (bounces(hero) > 0) d += " " + Messages.get(this, "desc_bounce", bounces(hero));
		return d;
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
