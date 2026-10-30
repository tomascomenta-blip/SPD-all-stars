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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//Q: him + wolves get elemental for a few turns. harder hits, alternating fire (burn) / frost (chill).
//wolves heal 50% too
public class FrostfireWolves extends ChampionAbility {

	public static final FrostfireWolves INSTANCE = new FrostfireWolves();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.BM_FROSTFIRE; }
	@Override public int color() { return 0xFF8844; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		return 30;
	}

	public static int duration( Hero hero ){
		return 5 + hero.pointsInTalent(Talent.BM_ELEMENTAL_FANGS);
	}

	public static int bonus( Hero hero ){
		return 2 + hero.lvl/3 + hero.pointsInTalent(Talent.BM_ELEMENTAL_FANGS);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		int turns = duration(hero);
		int bonus = bonus(hero);
		Imbue me = Buff.affect(hero, Imbue.class, turns);
		me.bonus = bonus;
		for (Wolf w : SKAlly.of(hero, Wolf.class)){
			Imbue i = Buff.affect(w, Imbue.class, turns);
			i.bonus = bonus;
			ChampionHooks.heal(w, w.HT/2);
			if (w.sprite != null){
				w.sprite.emitter().burst(FlameParticle.FACTORY, 6);
				w.sprite.emitter().burst(SnowParticle.FACTORY, 6);
			}
		}
		//alpha roar = fear around
		if (hero.subClass == HeroSubClass.PACK_ALPHA && hero.hasTalent(Talent.PA_ALPHA_ROAR)){
			for (Char ch : Actor.chars()){
				if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && Dungeon.level.distance(hero.pos, ch.pos) <= 3
						&& !Char.hasProp(ch, Char.Property.BOSS)){
					Buff.affect(ch, Terror.class, hero.pointsInTalent(Talent.PA_ALPHA_ROAR)).object = hero.id();
				}
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 1.2f);
		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", duration(hero), bonus(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	//fuego, hielo, fuego, hielo...
	public static class Imbue extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		public int bonus = 2;
		private boolean fire = true;

		public boolean fire(){
			return fire;
		}

		public void flip(){
			fire = !fire;
		}

		@Override
		public int icon() {
			return BuffIndicator.FIRE;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.6f, 0.3f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (6 - visualcooldown()) / 6f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", bonus, dispTurns());
		}

		private static final String BONUS = "bonus";
		private static final String FIRE = "fire";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(BONUS, bonus);
			bundle.put(FIRE, fire);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			bonus = bundle.getInt(BONUS);
			fire = bundle.getBoolean(FIRE);
		}
	}
}
