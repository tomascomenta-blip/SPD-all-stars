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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
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
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

//W: shield from recent dmg dealt. W again inside the window = leftover shield becomes hp.
//cd starts when the window closes, not on cast
public class Indestructible extends ChampionAbility {

	public static final Indestructible INSTANCE = new Indestructible();

	public static final int WINDOW = 12;

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.IRON_INDESTRUCTIBLE; }
	@Override public int color() { return 0x7799CC; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		float c = 28;
		if (hero.subClass == HeroSubClass.REVENANT) c *= 0.75f;
		return c;
	}

	public static float shieldFactor( Hero hero ){
		float f = 1f + 0.15f*hero.pointsInTalent(Talent.IRL_GRAVE_WARD);
		if (hero.subClass == HeroSubClass.REVENANT) f += 0.25f + 0.2f*hero.pointsInTalent(Talent.RV_BULWARK);
		return f;
	}

	public static int shield( Hero hero ){
		float base = 3 + hero.lvl / 2f + RisingDarkness.recentDamage(hero) * 0.8f;
		return Math.min(Math.round(base * shieldFactor(hero)), Math.round(hero.HT * 0.6f));
	}

	public static float healFactor( Hero hero ){
		float f = 1f;
		if (hero.subClass == HeroSubClass.REVENANT) f += 0.2f*hero.pointsInTalent(Talent.RV_SOUL_FEAST);
		return f;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Armed armed = hero.buff(Armed.class);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.6f);
		if (armed != null){
			//2nd press
			Barrier b = hero.buff(Barrier.class);
			int left = b == null ? 0 : b.shielding();
			if (b != null) b.detach();
			int heal = Math.round(left * healFactor(hero));
			ChampionHooks.heal(hero, heal);
			armed.detach();
		} else {
			int s = shield(hero);
			Barrier b = Buff.affect(hero, Barrier.class);
			b.setShield(Math.max(b.shielding(), s));
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(s), FloatingText.SHIELDING);
			Buff.affect(hero, Armed.class, WINDOW);
			if (hero.pointsInTalent(Talent.IRL_RESONANCE) >= 2){
				RisingDarkness rd = RisingDarkness.of(hero);
				if (rd != null) rd.addHits(hero, 1);
			}
		}
		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", shield(hero), WINDOW, Math.round(healFactor(hero)*100));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	//recast window
	public static class Armed extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public void detach() {
			super.detach();
			if (target instanceof Hero) INSTANCE.startCooldown((Hero) target);
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.5f, 0.6f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / WINDOW);
		}

		@Override
		public String name() {
			return Messages.get(this, "name");
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
