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

package com.shatteredpixel.shatteredpixeldungeon.champions.jailer;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//E: chain sweep, dmg + knockback around
public class Flay extends ChampionAbility {

	public static final Flay INSTANCE = new Flay();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.LOL_FLAY; }
	@Override public int color() { return 0x22CC77; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		return 12;
	}

	public static int push( Hero hero ){
		return 2 + hero.pointsInTalent(Talent.JAI_FLAY_FORCE);
	}

	public static int minDmg( Hero hero ){
		return 1 + hero.lvl/3 + Damnation.abilityBonus(hero);
	}

	public static int maxDmg( Hero hero ){
		return 4 + Math.round(hero.lvl*0.7f) + Damnation.abilityBonus(hero);
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		final ArrayList<Char> targets = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(hero.pos + n);
			if (ch != null && CoopRules.hits(hero, ch) && ch.isAlive()) targets.add(ch);
		}
		if (targets.isEmpty()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		hero.busy();
		Sample.INSTANCE.play(Assets.Sounds.CHAINS, 1f, 0.8f);
		hero.sprite.attack(targets.get(0).pos, new Callback() {
			@Override
			public void call() {
				int hits = 0;
				for (Char ch : targets){
					if (!ch.isAlive()) continue;
					ch.damage(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)), Flay.this);
					hits++;
					if (ch.isAlive()){
						Buff.prolong(ch, Cripple.class, 2f);
						int away = ch.pos + (ch.pos - hero.pos);
						Ballistica trajectory = new Ballistica(ch.pos, away, Ballistica.MAGIC_BOLT);
						WandOfBlastWave.throwChar(ch, trajectory, push(hero), false, true, Flay.this);
					}
				}
				if (hero.subClass == HeroSubClass.SOULREAPER && hero.hasTalent(Talent.SR_REAP)){
					ChampionHooks.heal(hero, hits * hero.pointsInTalent(Talent.SR_REAP));
				}
				Invisibility.dispel(hero);
				startCooldown(hero);
				hero.spendAndNext(1f);
			}
		});
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", minDmg(hero), maxDmg(hero), push(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
