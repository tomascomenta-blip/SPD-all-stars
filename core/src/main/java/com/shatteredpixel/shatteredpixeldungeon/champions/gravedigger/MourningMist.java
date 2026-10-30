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

package com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

//E: black mist ball, 3x3. dmg + mark, his allies prioritize marked stuff and hit it harder
public class MourningMist extends ChampionAbility {

	public static final MourningMist INSTANCE = new MourningMist();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.LOL_MOURNING_MIST; }
	@Override public int color() { return 0x55BBAA; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 6;

	@Override
	public float cooldown( Hero hero ) {
		return 14;
	}

	public static int minDmg( Hero hero ){
		return 1 + hero.lvl/3;
	}

	public static int maxDmg( Hero hero ){
		return 4 + Math.round(hero.lvl*0.7f);
	}

	public static int markTurns( Hero hero ){
		return 8 + 3*hero.pointsInTalent(Talent.GRA_CLINGING_MIST);
	}

	public static int slowTurns( Hero hero ){
		int p = hero.pointsInTalent(Talent.GRA_CLINGING_MIST);
		return p > 0 ? 1 + p : 0;
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]
				|| Dungeon.level.solid[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		final int cell = target;
		hero.busy();
		GravediggerHooks.castVisual(hero, cell);
		Sample.INSTANCE.play(Assets.Sounds.ZAP, 1f, 0.7f);
		Invisibility.dispel(hero);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.SHADOW, hero.sprite, cell, new Callback() {
			@Override
			public void call() {
				burst(hero, cell);
				startCooldown(hero);
				hero.spendAndNext(1f);
			}
		});
	}

	private void burst( Hero hero, int cell ){
		int owner = ChampionHooks.indexOf(hero);
		for (int n : PathFinder.NEIGHBOURS9){
			int c = cell + n;
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(MistParticle.FACTORY, 8);
			Char ch = Actor.findChar(c);
			if (ch == null || !CoopRules.hits(hero, ch) || !ch.isAlive()) continue;
			ch.damage(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)), this);
			if (ch.isAlive()){
				MistMark m = Buff.prolong(ch, MistMark.class, markTurns(hero));
				m.owner = owner;
				if (slowTurns(hero) > 0) Buff.prolong(ch, Slow.class, slowTurns(hero));
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.GAS, 0.8f, 0.8f);
	}

	//grim harvest: marked thing died -> marker's Q cd goes down
	public static void onEnemyDeath( Mob mob ){
		MistMark m = mob.buff(MistMark.class);
		if (m == null) return;
		Hero owner = ChampionHooks.heroByIndex(m.owner);
		if (owner == null || !owner.hasTalent(Talent.GRA_GRIM_HARVEST)) return;
		LastRites.CD cd = owner.buff(LastRites.CD.class);
		if (cd != null) cd.reduce(2*owner.pointsInTalent(Talent.GRA_GRIM_HARVEST));
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", RANGE, minDmg(hero), maxDmg(hero), markTurns(hero));
		if (slowTurns(hero) > 0) d += " " + Messages.get(this, "desc_slow", slowTurns(hero));
		return d;
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
