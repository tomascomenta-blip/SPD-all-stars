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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

//armor ability: rising darkness on demand, bigger + slows
public class IronEclipse extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static int turns( Hero hero ){
		return 8 + 2*hero.pointsInTalent(Talent.IE_LONG_NIGHT);
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		RisingDarkness.DarkAura.trigger(hero, turns(hero), true);

		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.5f);
		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.IRON_ECLIPSE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.IE_LONG_NIGHT, Talent.IE_SMOTHERING_DARK, Talent.IE_IRON_HUNGER, Talent.HEROIC_ENERGY};
	}
}
