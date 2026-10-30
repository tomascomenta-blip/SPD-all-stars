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

package com.shatteredpixel.shatteredpixeldungeon.champions.brawler;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
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
import com.watabou.utils.Bundle;

//Q (free): next 2 punches get +%maxhp dmg
public class KnuckleDown extends ChampionAbility {

	public static final KnuckleDown INSTANCE = new KnuckleDown();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.LOL_KNUCKLE_DOWN; }
	@Override public int color() { return 0xFFCC33; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		return 9;
	}

	public static int percent( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.BRA_HEAVY_HANDS);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Knuckles k = Buff.prolong(hero, Knuckles.class, 6f);
		k.hits = 2;
		if (hero.hasTalent(Talent.BRA_FOOTWORK)){
			Buff.prolong(hero, Haste.class, hero.pointsInTalent(Talent.BRA_FOOTWORK));
		}
		hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.titleCase(name()));
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.5f, 1.4f);
		startCooldown(hero);
		hero.spendAndNext(0f);
	}

	public static int proc( Hero hero, Char enemy, int damage ){
		Knuckles k = hero.buff(Knuckles.class);
		if (k == null) return damage;
		float pct = percent(hero) / 100f;
		if (Char.hasProp(enemy, Char.Property.BOSS)) pct /= 2f;
		damage += 1 + Math.round(enemy.HT * pct);
		k.hits--;
		if (k.hits <= 0) k.detach();
		return damage;
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", percent(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	public static class Knuckles extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		public int hits = 2;

		@Override
		public int icon() {
			return BuffIndicator.WEAPON;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.8f, 0.2f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(hits);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", hits, dispTurns());
		}

		private static final String HITS = "hits";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(HITS, hits);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			hits = bundle.getInt(HITS);
		}
	}
}
