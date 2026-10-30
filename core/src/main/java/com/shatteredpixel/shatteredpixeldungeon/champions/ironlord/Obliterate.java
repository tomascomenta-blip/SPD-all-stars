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
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//Q: cone swing. closest target takes a lot more if nobody else got hit. isolation bonus
public class Obliterate extends ChampionAbility {

	public static final Obliterate INSTANCE = new Obliterate();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.IRON_OBLITERATE; }
	@Override public int color() { return 0x8855CC; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	@Override
	public float cooldown( Hero hero ) {
		return 10;
	}

	public static int range( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.IRL_BRUTE_FORCE);
	}

	//weapon dmg fraction, everyone in the cone
	public static float baseFactor( Hero hero ){
		float f = 0.8f + 0.15f*hero.pointsInTalent(Talent.IRL_HEAVY_MACE);
		if (hero.subClass == HeroSubClass.TYRANT) f += 0.1f*hero.pointsInTalent(Talent.TY_OVERLORD);
		return f;
	}

	//main target, alone
	public static float aloneFactor( Hero hero ){
		return baseFactor(hero) + 0.7f;
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		if (target == null || target == hero.pos) return;
		final Ballistica aim = new Ballistica(hero.pos, target, Ballistica.WONT_STOP);
		final int dist = Math.min(aim.dist, range(hero));
		final ConeAOE cone = new ConeAOE(aim, dist, 70, Ballistica.STOP_SOLID);

		final ArrayList<Char> targets = new ArrayList<>();
		for (int cell : cone.cells){
			Char ch = Actor.findChar(cell);
			if (ch != null && ch != hero && CoopRules.hits(hero, ch) && ch.isAlive()) targets.add(ch);
		}
		if (targets.isEmpty()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		//closest first, tie -> cell index
		Collections.sort(targets, new Comparator<Char>() {
			@Override
			public int compare( Char a, Char b ) {
				int da = Dungeon.level.distance(hero.pos, a.pos);
				int db = Dungeon.level.distance(hero.pos, b.pos);
				if (da != db) return da - db;
				return a.pos - b.pos;
			}
		});
		final Char main = targets.get(0);
		boolean alone = true;
		for (Char ch : Actor.chars()){
			if (ch != main && ch != hero && CoopRules.hits(hero, ch) && ch.isAlive()
					&& Dungeon.level.distance(main.pos, ch.pos) <= 2){
				alone = false;
				break;
			}
		}
		final boolean isolated = alone;

		hero.busy();
		Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.7f);
		hero.sprite.attack(main.pos, new Callback() {
			@Override
			public void call() {
				int hits = 0;
				for (Char ch : targets){
					if (!ch.isAlive()) continue;
					float f = (ch == main && isolated) ? aloneFactor(hero) : baseFactor(hero);
					if (hero.attack(ch, f, 0, Char.INFINITE_ACCURACY)) hits++;
					if (ch.sprite != null && ch.sprite.visible){
						CellEmitter.center(ch.pos).burst(ShadowParticle.UP, 6);
					}
				}
				if (hits > 0 && hero.hasTalent(Talent.IRL_RESONANCE)){
					RisingDarkness rd = RisingDarkness.of(hero);
					if (rd != null) rd.addHits(hero, 1);
				}
				PixelScene.shake(1.5f, 0.3f);
				Invisibility.dispel(hero);
				startCooldown(hero);
				hero.spendAndNext(hero.attackDelay());
			}
		});
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", range(hero), Math.round(baseFactor(hero)*100), Math.round(aloneFactor(hero)*100));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
