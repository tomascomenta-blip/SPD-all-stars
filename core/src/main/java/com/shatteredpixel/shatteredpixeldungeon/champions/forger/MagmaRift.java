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

package com.shatteredpixel.shatteredpixeldungeon.champions.forger;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//Q: fire line, magma pillar at the end
public class MagmaRift extends ChampionAbility {

	public static final MagmaRift INSTANCE = new MagmaRift();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.FORGER_RIFT; }
	@Override public int color() { return 0xFF7733; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	@Override
	public float cooldown( Hero hero ) {
		return 9;
	}

	public static int length( Hero hero ){
		return 5 + hero.pointsInTalent(Talent.FOR_LONG_RIFT);
	}

	public static int minDmg( Hero hero ){
		return 2 + hero.lvl/3;
	}

	public static int maxDmg( Hero hero ){
		return 6 + Math.round(hero.lvl*0.9f);
	}

	public static int pillarTurns( Hero hero ){
		return 10 + 2*hero.pointsInTalent(Talent.FOR_FIRE_PILLARS) + 2*hero.pointsInTalent(Talent.FER_DENSE_MAGMA);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null || target == hero.pos) return;
		Ballistica aim = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		int len = length(hero);
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 1; i < aim.path.size() && i <= len; i++){
			int c = aim.path.get(i);
			if (Dungeon.level.solid[c]) break;
			cells.add(c);
		}
		if (cells.isEmpty()){
			GLog.w(Messages.get(this, "bad_cell"));
			return;
		}
		int end = cells.get(cells.size() - 1);

		ForgerHooks.gesture(hero, end);
		((MagicMissile) hero.sprite.parent.recycle(MagicMissile.class)).reset(
				MagicMissile.FIRE, hero.sprite, end, null);
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.8f);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.7f, 0.9f);

		int pillar = -1;
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(FlameParticle.FACTORY, 5);
			Char ch = Actor.findChar(c);
			if (ch != null && CoopRules.hits(hero, ch) && ch.isAlive()){
				ch.damage(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)), this);
			} else if (ch == null){
				pillar = c;
			}
		}
		//last FREE cell, so if someone's at the very end it goes one before
		if (pillar != -1) MagmaPillar.raise(pillar, pillarTurns(hero));

		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", length(hero), minDmg(hero), maxDmg(hero), pillarTurns(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
