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

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

//R: jump, grab, SLAM. splash on impact
public class ShowStopper extends ChampionAbility {

	public static final ShowStopper INSTANCE = new ShowStopper();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.LOL_SHOW_STOPPER; }
	@Override public int color() { return 0xFFDD44; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 2;

	@Override
	public float cooldown( Hero hero ) {
		float cd = 50;
		if (hero.subClass == HeroSubClass.SHOWMAN) cd *= 0.7f;
		return cd;
	}

	public static int baseDamage( Hero hero ){
		return Math.round((6 + 2*hero.lvl) * (1f + 0.1f*hero.pointsInTalent(Talent.BRA_BIG_SHOW)));
	}

	public static float splashShare( Hero hero ){
		float s = 0.5f;
		if (hero.subClass == HeroSubClass.SHOWMAN) s += 0.15f*hero.pointsInTalent(Talent.SM_CROWD);
		return s;
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
		Invisibility.dispel(hero);
		final int dest = landing;
		hero.sprite.jump(hero.pos, dest == hero.pos ? enemy.pos : dest, 4f, 0.35f, new Callback() {
			@Override
			public void call() {
				if (dest != hero.pos) ChampionHooks.placeHero(hero, dest);
				else hero.sprite.place(hero.pos);
				slam(hero, enemy);
			}
		});
	}

	private void slam( Hero hero, Char enemy ){
		int center = enemy.pos;
		if (enemy.isAlive()){
			float pct = Char.hasProp(enemy, Char.Property.BOSS) ? 0.05f : 0.15f;
			int dmg = baseDamage(hero) + Math.round(enemy.HT * pct);
			enemy.damage(dmg, this);
			if (enemy.isAlive()) Buff.prolong(enemy, Paralysis.class, 1f);
			int splash = Math.round(baseDamage(hero) * splashShare(hero));
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(center + n);
				if (ch == null || ch == enemy || !CoopRules.hits(hero, ch) || !ch.isAlive()) continue;
				ch.damage(splash, this);
				if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 2f);
			}
		}
		if (Dungeon.level.heroFOV[center]){
			CellEmitter.center(center).burst(Speck.factory(Speck.ROCK), 10);
		}
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		PixelScene.shake(3, 0.5f);

		if (!enemy.isAlive() && hero.subClass == HeroSubClass.SHOWMAN && hero.hasTalent(Talent.SM_ENCORE)){
			startCooldown(hero, cooldown(hero) * (1f - 0.25f*hero.pointsInTalent(Talent.SM_ENCORE)));
		} else {
			startCooldown(hero);
		}
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, baseDamage(hero), Math.round(100*splashShare(hero)));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
