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

package com.shatteredpixel.shatteredpixeldungeon.champions.bard;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//W: lullaby at a spot. enemies there sleep a few turns (dmg wakes them), bosses/sleep-immune just get slowed
public class Lullaby extends BardSong {

	public static final Lullaby INSTANCE = new Lullaby();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.BARD_LULLABY; }
	@Override public int color() { return 0x9977FF; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	@Override
	public float cooldown( Hero hero ) {
		float cd = 22;
		if (hero.subClass == HeroSubClass.MINSTREL) cd *= 0.7f;
		return cd;
	}

	public static int range( Hero hero ){
		return 6 + hero.pointsInTalent(Talent.BAR_LOUD_VOICE);
	}

	public static int sleepTurns( Hero hero ){
		return 3 + hero.lvl/4 + 2*hero.pointsInTalent(Talent.BAR_DEEP_SLEEP);
	}

	public static int slowTurns( Hero hero ){
		return Math.max(2, (sleepTurns(hero)+1)/2);
	}

	public static int rudeDamage( Hero hero ){
		if (hero.subClass != HeroSubClass.MINSTREL) return 0;
		return hero.pointsInTalent(Talent.MI_RUDE_AWAKENING) * (2 + hero.lvl/3);
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > range(hero) || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		final int cell = target;
		final boolean forte = Refrain.onSong(hero);
		hero.busy();
		gesture(hero, cell, true);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY, 1f, 1.1f);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.SPECK + Speck.NOTE, hero.sprite, cell, new Callback() {
			@Override
			public void call() {
				land(hero, cell, forte);
				finish(hero, 0);
			}
		});
	}

	private void land( Hero hero, int cell, boolean forte ){
		int radius = forte ? 2 : 1;
		float power = power(hero, forte, false);
		int sleep = Math.round(sleepTurns(hero) * power);
		int slow = Math.round(slowTurns(hero) * power);
		int rude = rudeDamage(hero);

		ArrayList<Char> targets = ChampionHooks.enemiesAround(cell, radius);
		//dissonance first, otherwise it would wake them right up. obviously
		dissonance(hero, targets, this);

		int slept = 0;
		for (Char ch : targets){
			if (!ch.isAlive()) continue;
			boolean big = Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
			if (!big && ch instanceof Mob && !ch.isImmune(Sleep.class) && !ch.isImmune(MagicalSleep.class)){
				Buff.affect(ch, MagicalSleep.class);
				if (ch.buff(MagicalSleep.class) != null){
					Drowse d = Buff.affect(ch, Drowse.class);
					d.left = Math.max(d.left, sleep);
					d.rude = Math.max(d.rude, rude);
					slept++;
				}
			} else {
				Buff.prolong(ch, Slow.class, slow);
			}
			if (ch.sprite != null && ch.sprite.visible){
				ch.sprite.centerEmitter().start(Speck.factory(Speck.NOTE), 0.3f, 5);
			}
		}
		if (slept == 1)     GLog.i(Messages.get(this, "slept_one"));
		else if (slept > 1) GLog.i(Messages.get(this, "slept", slept));
	}

	@Override
	public String prompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", range(hero), sleepTurns(hero), slowTurns(hero));
		if (rudeDamage(hero) > 0) d += " " + Messages.get(this, "desc_rude", rudeDamage(hero));
		if (dissonanceDamage(hero) > 0) d += " " + Messages.get(BardSong.class, "desc_dissonance", dissonanceDamage(hero));
		return d + forteDesc(hero, fortePercent(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	//hidden timer, wakes them when the song ends. woken early by dmg -> minstrel's rude awakening procs
	public static class Drowse extends Buff {

		{
			type = buffType.NEUTRAL;
		}

		public int left = 0;
		public int rude = 0;

		@Override
		public boolean act() {
			if (!target.isAlive()){
				detach();
				return true;
			}
			if (target.buff(MagicalSleep.class) == null){
				//rude!
				if (rude > 0 && target.alignment == Char.Alignment.ENEMY){
					target.damage(rude, this);
					if (target.sprite != null && target.sprite.visible){
						target.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Drowse.class, "rude"));
						Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.8f, 1.2f);
					}
				}
				detach();
				return true;
			}
			left--;
			if (left <= 0){
				Buff.detach(target, MagicalSleep.class);
				if (target instanceof Mob && ((Mob) target).state == ((Mob) target).SLEEPING){
					((Mob) target).state = ((Mob) target).WANDERING;
				}
				detach();
				return true;
			}
			spend(TICK);
			return true;
		}

		private static final String LEFT = "left";
		private static final String RUDE = "rude";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
			bundle.put(RUDE, rude);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
			rude = bundle.getInt(RUDE);
		}
	}
}
