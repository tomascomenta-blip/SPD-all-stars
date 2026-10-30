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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//R: spirit stampede, cone in front. more pack alive = more dmg, stuns, heals the wolves
public class Stampede extends ChampionAbility {

	public static final Stampede INSTANCE = new Stampede();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.BM_STAMPEDE; }
	@Override public int color() { return 0xCC9955; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 5;

	@Override
	public float cooldown( Hero hero ) {
		return 70;
	}

	public static int beasts( Hero hero ){
		int n = SKAlly.of(hero, Wolf.class).size();
		if (FuzzyBear.of(hero) != null) n++;
		return Math.min(4, n);
	}

	public static int damage( Hero hero ){
		return (3 + hero.lvl/2) * (1 + beasts(hero));
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null || target == hero.pos) return;
		Ballistica aim = new Ballistica(hero.pos, target, Ballistica.WONT_STOP);
		ConeAOE cone = new ConeAOE(aim, RANGE, 70, Ballistica.STOP_SOLID);

		ArrayList<Char> foes = new ArrayList<>();
		for (int c : cone.cells){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 3);
			Char ch = Actor.findChar(c);
			if (ch != null && CoopRules.hits(hero, ch) && ch.isAlive()) foes.add(ch);
		}
		if (foes.isEmpty()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		int dmg = damage(hero);
		for (Char ch : foes){
			ch.damage(dmg, this);
			if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
		}
		for (Wolf w : SKAlly.of(hero, Wolf.class)){
			ChampionHooks.heal(w, w.HT);
		}
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.7f);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.8f, 1.2f);
		PixelScene.shake(3, 0.6f);

		hero.sprite.zap(target);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, 3 + hero.lvl/2, beasts(hero), damage(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
