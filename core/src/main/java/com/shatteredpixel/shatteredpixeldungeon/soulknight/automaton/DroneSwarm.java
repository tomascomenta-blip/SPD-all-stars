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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.automaton;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

//W: drones out of his back. recast replaces the old ones, swarm engineer gets +1
public class DroneSwarm extends ChampionAbility {

	public static final DroneSwarm INSTANCE = new DroneSwarm();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.AUT_DRONES; }
	@Override public int color() { return 0xFFDD44; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		return 45 * AutomatonHooks.cooldownFactor(hero);
	}

	public static int count( Hero hero ){
		return hero.subClass == HeroSubClass.SWARM_ENGINEER ? 3 : 2;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		//old ones leave
		for (Drone d : SKAlly.of(hero, Drone.class)){
			d.timeLeft = 0;
			d.die(null);
		}

		int made = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			if (made >= count(hero)) break;
			int c = hero.pos + n;
			if (Actor.findChar(c) != null || Dungeon.level.solid[c]) continue;
			Drone d = Drone.make(hero, c);
			GameScene.add(d);
			Dungeon.level.occupyCell(d);
			if (d.sprite != null) d.sprite.emitter().burst(Speck.factory(Speck.WOOL), 4);
			made++;
		}
		if (made == 0){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}
		Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1f, 1.5f);
		Dungeon.observe();
		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		startCooldown(hero);
		AutomatonHooks.afterAbility(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", count(hero), Drone.duration(hero), Drone.RANGE,
				Drone.minDmg(hero.lvl), Drone.maxDmg(hero.lvl));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
