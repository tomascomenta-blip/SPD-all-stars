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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

//W (free): next melee +50%, cripple, bleed
public class CripplingStrike extends ChampionAbility {

	public static final CripplingStrike INSTANCE = new CripplingStrike();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.LOL_CRIPPLING_STRIKE; }
	@Override public int color() { return 0xAA6633; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		return 8;
	}

	public static int extraStacks( Hero hero ){
		return 1 + hero.pointsInTalent(Talent.EXE_CRUEL_BLOW);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Buff.prolong(hero, Primed.class, 5f);
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.6f, 1.3f);
		hero.sprite.showStatus(CharSprite.NEGATIVE, Messages.titleCase(name()));
		startCooldown(hero);
		hero.spendAndNext(0f);
	}

	public static int proc( Hero hero, Char enemy, int damage ){
		Primed p = hero.buff(Primed.class);
		if (p == null) return damage;
		p.detach();
		damage = Math.round(damage * 1.5f);
		if (enemy.isAlive()){
			Buff.prolong(enemy, Cripple.class, 3f);
			Hemorrhage.apply(hero, enemy, extraStacks(hero));
		}
		return damage;
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", extraStacks(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	public static class Primed extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.WEAPON;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.3f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 5f);
		}
	}
}
