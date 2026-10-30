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

package com.shatteredpixel.shatteredpixeldungeon.champions.executioner;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//Q: axe spin, heal per enemy hit. el trompo
public class Decimate extends ChampionAbility {

	public static final Decimate INSTANCE = new Decimate();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.LOL_DECIMATE; }
	@Override public int color() { return 0xDD3333; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		return 12;
	}

	public static int radius( Hero hero ){
		return hero.subClass == HeroSubClass.CONQUEROR ? 2 : 1;
	}

	//% missing hp per hit, caps at 3
	public static float healPerHit( Hero hero ){
		return 0.10f + 0.04f*hero.pointsInTalent(Talent.CQ_BLOODFEAST);
	}

	public static ArrayList<Char> targets( Hero hero ){
		ArrayList<Char> result = new ArrayList<>();
		int r = radius(hero);
		for (Char ch : Actor.chars()){
			if (ch == hero || !CoopRules.hits(hero, ch) || !ch.isAlive()) continue;
			if (Dungeon.level.distance(hero.pos, ch.pos) > r) continue;
			if (!Dungeon.level.adjacent(hero.pos, ch.pos)
					&& new Ballistica(hero.pos, ch.pos, Ballistica.PROJECTILE).collisionPos != ch.pos) continue;
			if (!Dungeon.level.heroFOV[ch.pos] && !Dungeon.level.adjacent(hero.pos, ch.pos)) continue;
			result.add(ch);
		}
		return result;
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		final ArrayList<Char> targets = targets(hero);
		if (targets.isEmpty()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		hero.busy();
		Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 0.7f);
		hero.sprite.attack(targets.get(0).pos, new Callback() {
			@Override
			public void call() {
				int hits = 0;
				for (Char ch : targets){
					if (!ch.isAlive()) continue;
					if (hero.attack(ch, 1f, 0, 1f)) {
						hits++;
						int extra = hero.pointsInTalent(Talent.CQ_SPIN);
						if (extra > 0 && ch.isAlive()) Hemorrhage.apply(hero, ch, extra);
					}
					if (ch.sprite != null && ch.sprite.visible){
						CellEmitter.center(ch.pos).burst(BloodParticle.BURST, 6);
					}
				}
				int counted = Math.min(hits, 3);
				if (counted > 0){
					int heal = Math.round((hero.HT - hero.HP) * healPerHit(hero) * counted);
					if (heal > 0){
						hero.HP = Math.min(hero.HT, hero.HP + heal);
						hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
					}
					if (hero.hasTalent(Talent.EXE_THICK_SKIN)){
						int shield = hits * (1 + hero.pointsInTalent(Talent.EXE_THICK_SKIN));
						Buff.affect(hero, Barrier.class).incShield(shield);
						hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
					}
				}
				Invisibility.dispel(hero);
				startCooldown(hero);
				hero.spendAndNext(hero.attackDelay());
			}
		});
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", radius(hero) == 2 ? Messages.get(this, "radius2") : Messages.get(this, "radius1"),
				Math.round(100*healPerHit(hero)));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
