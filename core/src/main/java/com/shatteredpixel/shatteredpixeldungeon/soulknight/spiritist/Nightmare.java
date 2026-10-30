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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//Q: nightmare mark. +dmg taken, a ghost shows up to fight it, more ghosts when it dies
public class Nightmare extends ChampionAbility {

	public static final Nightmare INSTANCE = new Nightmare();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.SP_NIGHTMARE; }
	@Override public int color() { return 0x9966CC; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 7;
	public static final float FACTOR = 1.2f;

	@Override
	public float cooldown( Hero hero ) {
		return 20;
	}

	public static int duration( Hero hero ){
		return 15 + 5*hero.pointsInTalent(Talent.SP_LINGERING_DREAD);
	}

	//on mark AND on death
	public static int ghosts( Hero hero ){
		return hero.subClass == HeroSubClass.SPECTRE_LORD ? 2 : 1;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		Char ch = Actor.findChar(target);
		if (ch == null || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, target) > RANGE){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}

		Mark m = Buff.affect(ch, Mark.class, duration(hero));
		m.owner = ChampionHooks.indexOf(hero);
		m.factor = FACTOR;
		if (ch.sprite != null) ch.sprite.emitter().burst(ShadowParticle.CURSE, 10);
		Sample.INSTANCE.play(Assets.Sounds.CURSED, 0.8f, 1.2f);

		SpiritistHooks.summonGhosts(hero, hero.pos, ghosts(hero), false);
		Dungeon.observe();

		ChampionHooks.operateVisual(hero, target);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, duration(hero), Math.round((FACTOR-1)*100), ghosts(hero),
				Ghost.duration(hero));
	}

	public static class Mark extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		public int owner = 0;
		public float factor = FACTOR;

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.6f, 0.3f, 0.9f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (20 - visualcooldown()) / 20f);
		}

		@Override
		public void fx( boolean on ) {
			if (target.sprite == null) return;
			if (on) target.sprite.add(CharSprite.State.MARKED);
			else target.sprite.remove(CharSprite.State.MARKED);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.round((factor-1)*100), dispTurns());
		}

		private static final String OWNER  = "owner";
		private static final String FACTOR_KEY = "factor";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(OWNER, owner);
			bundle.put(FACTOR_KEY, factor);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			owner = bundle.getInt(OWNER);
			factor = bundle.getFloat(FACTOR_KEY);
		}
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
