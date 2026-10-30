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

package com.shatteredpixel.shatteredpixeldungeon.champions.brawler;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//E: grab everyone around and bonk them together. enemies on opposite sides = stun, else just slow
public class Facebreaker extends ChampionAbility {

	public static final Facebreaker INSTANCE = new Facebreaker();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.LOL_FACEBREAKER; }
	@Override public int color() { return 0xDD8844; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final int RANGE = 2;

	@Override
	public float cooldown( Hero hero ) {
		float cd = 14;
		if (hero.subClass == HeroSubClass.SHOWMAN) cd -= 2*hero.pointsInTalent(Talent.SM_FLASHY);
		return cd;
	}

	public static int minDmg( Hero hero ){
		return 2 + hero.lvl/3;
	}

	public static int maxDmg( Hero hero ){
		return 5 + Math.round(hero.lvl*0.8f);
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		final ArrayList<Char> targets = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
			if (Dungeon.level.distance(hero.pos, ch.pos) > RANGE) continue;
			if (!Dungeon.level.adjacent(hero.pos, ch.pos)
					&& new Ballistica(hero.pos, ch.pos, Ballistica.PROJECTILE).collisionPos != ch.pos) continue;
			targets.add(ch);
		}
		if (targets.isEmpty()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		//opposite sides check. probably a nicer way to do this
		int w = Dungeon.level.width();
		boolean left = false, right = false, up = false, down = false;
		for (Char ch : targets){
			int dx = ch.pos % w - hero.pos % w;
			int dy = ch.pos / w - hero.pos / w;
			if (dx < 0) left = true;
			if (dx > 0) right = true;
			if (dy < 0) up = true;
			if (dy > 0) down = true;
		}
		final boolean bothSides = (left && right) || (up && down);

		hero.busy();
		Invisibility.dispel(hero);
		hero.sprite.attack(targets.get(0).pos, new Callback() {
			@Override
			public void call() {
				for (Char ch : targets){
					if (!ch.isAlive()) continue;
					if (!Dungeon.level.adjacent(hero.pos, ch.pos)){
						int dest = ChampionHooks.pullDestination(hero, ch);
						if (dest != -1) ChampionHooks.moveChar(ch, dest);
					}
					ch.damage(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)), Facebreaker.this);
					if (!ch.isAlive()) continue;
					if (bothSides){
						Buff.prolong(ch, Paralysis.class, 2f);
					} else {
						Buff.prolong(ch, Cripple.class, 2f);
						if (hero.subClass == HeroSubClass.SHOWMAN) Buff.prolong(ch, Paralysis.class, 1f);
					}
					if (hero.hasTalent(Talent.BRA_TAUNT)){
						Buff.prolong(ch, Vulnerable.class, 1 + hero.pointsInTalent(Talent.BRA_TAUNT));
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.9f);
				PixelScene.shake(1, 0.25f);
				startCooldown(hero);
				hero.spendAndNext(1f);
			}
		});
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, minDmg(hero), maxDmg(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
