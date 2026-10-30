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

package com.shatteredpixel.shatteredpixeldungeon.champions.ironlord;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Chains;
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//E: line grab, pulls everyone on it next to him + dmg
public class DeathsGrasp extends ChampionAbility {

	public static final DeathsGrasp INSTANCE = new DeathsGrasp();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.IRON_DEATHS_GRASP; }
	@Override public int color() { return 0x9966AA; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	@Override
	public float cooldown( Hero hero ) {
		return 15;
	}

	public static int range( Hero hero ){
		return 4 + hero.pointsInTalent(Talent.IRL_LONG_REACH);
	}

	public static int minDamage( Hero hero ){
		return Math.round((3 + hero.lvl / 2f) * factor(hero));
	}

	public static int maxDamage( Hero hero ){
		return Math.round((8 + hero.lvl * 1.2f) * factor(hero));
	}

	private static float factor( Hero hero ){
		float f = 1f;
		if (hero.subClass == HeroSubClass.TYRANT) f += 0.1f*hero.pointsInTalent(Talent.TY_OVERLORD);
		return f;
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		if (target == null || target == hero.pos) return;
		Ballistica line = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID);
		final ArrayList<Char> victims = new ArrayList<>();
		int last = Math.min(range(hero), line.path.size() - 1);
		for (int i = 1; i <= last; i++){
			int cell = line.path.get(i);
			Char ch = Actor.findChar(cell);
			if (ch != null && ch != hero && CoopRules.hits(hero, ch) && ch.isAlive()
					&& !Char.hasProp(ch, Char.Property.IMMOVABLE)) {
				victims.add(ch);
			}
		}
		if (victims.isEmpty()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		hero.busy();
		hero.sprite.zap(victims.get(victims.size()-1).pos);
		Sample.INSTANCE.play(Assets.Sounds.CHAINS, 1f, 0.8f);
		final Char far = victims.get(victims.size()-1);
		hero.sprite.parent.add(new Chains(
				hero.sprite.center(), far.sprite.center(), Effects.Type.CHAIN, new Callback() {
			@Override
			public void call() {
				int dealt = 0;
				for (Char ch : victims){
					if (!ch.isAlive()) continue;
					int dest = ChampionHooks.pullDestination(hero, ch);
					if (dest != -1) ChampionHooks.moveChar(ch, dest);
					int dmg = Random.NormalIntRange(minDamage(hero), maxDamage(hero));
					int before = ch.HP;
					ch.damage(dmg, hero);
					dealt += Math.max(0, before - Math.max(0, ch.HP));
					if (ch.sprite != null && ch.sprite.visible){
						CellEmitter.center(ch.pos).burst(ShadowParticle.UP, 5);
					}
				}
				RisingDarkness.addDamage(hero, dealt);
				if (hero.hasTalent(Talent.IRL_RESONANCE)){
					RisingDarkness rd = RisingDarkness.of(hero);
					if (rd != null) rd.addHits(hero, 1);
				}
				Invisibility.dispel(hero);
				startCooldown(hero);
				hero.spendAndNext(1f);
			}
		}));
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", range(hero), minDamage(hero), maxDamage(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
