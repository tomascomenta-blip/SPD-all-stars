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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

//armor ability: invis for a few turns + some ghosts pop out to fight
public class SoulVeil extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static int duration( Hero hero ){
		return 4 + 2*hero.pointsInTalent(Talent.SV_LONG_VEIL);
	}

	public static int ghosts( Hero hero ){
		return 2 + (hero.pointsInTalent(Talent.SV_PHANTOMS)+1)/2;
	}

	public static int mend( Hero hero ){
		return Math.round(hero.HT * 0.05f * hero.pointsInTalent(Talent.SV_SOUL_MEND));
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		Buff.affect(hero, Invisibility.class, duration(hero));
		SpiritistHooks.summonGhosts(hero, hero.pos, ghosts(hero), false);
		ChampionHooks.heal(hero, mend(hero));
		Sample.INSTANCE.play(Assets.Sounds.MELD);
		Dungeon.observe();

		ChampionHooks.operateVisual(hero, hero.pos);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc");
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.SPIRITIST){
			Hero h = Dungeon.hero;
			d += " " + Messages.get(this, "desc_numbers", duration(h), ghosts(h));
		}
		return d + "\n\n" + Messages.get(this, "cost", (int) baseChargeUse);
	}

	@Override
	public int icon() {
		return HeroIcon.SP_VEIL;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SV_LONG_VEIL, Talent.SV_PHANTOMS, Talent.SV_SOUL_MEND, Talent.HEROIC_ENERGY};
	}
}
