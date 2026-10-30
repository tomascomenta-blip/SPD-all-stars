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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//W: short fire cone, a bit of dmg + brittle (next hit from anyone stuns + hits harder)
public class BellowsBreath extends ChampionAbility {

	public static final BellowsBreath INSTANCE = new BellowsBreath();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.FORGER_BELLOWS; }
	@Override public int color() { return 0xFFAA22; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	@Override
	public float cooldown( Hero hero ) {
		return 14;
	}

	public static int range( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.FOR_WIDE_BELLOWS);
	}

	public static float degrees( Hero hero ){
		return 60 + 15*hero.pointsInTalent(Talent.FOR_WIDE_BELLOWS);
	}

	public static int minDmg( Hero hero ){
		return 1 + hero.lvl/4;
	}

	public static int maxDmg( Hero hero ){
		return 4 + Math.round(hero.lvl*0.7f);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null || target == hero.pos) return;
		Ballistica aim = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		ConeAOE cone = new ConeAOE(aim, range(hero), degrees(hero),
				Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);

		ForgerHooks.gesture(hero, target);
		for (Ballistica ray : cone.outerRays){
			((MagicMissile) hero.sprite.parent.recycle(MagicMissile.class)).reset(
					MagicMissile.FIRE_CONE, hero.sprite, ray.path.get(ray.dist), null);
		}
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 1.2f);

		ArrayList<Char> hit = new ArrayList<>();
		for (int cell : cone.cells){
			if (cell == hero.pos) continue;
			Char ch = Actor.findChar(cell);
			if (ch != null && CoopRules.hits(hero, ch) && ch.isAlive()) hit.add(ch);
		}
		for (Char ch : hit){
			ch.damage(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)), this);
			if (ch.isAlive()) Brittle.apply(hero, ch);
		}

		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", range(hero), Math.round(degrees(hero)), minDmg(hero), maxDmg(hero),
				Brittle.bonus(hero), Math.round(Brittle.DURATION));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
