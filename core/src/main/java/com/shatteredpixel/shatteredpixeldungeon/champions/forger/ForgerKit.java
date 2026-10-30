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

package com.shatteredpixel.shatteredpixeldungeon.champions.forger;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

//anvil medallion. 4 abilities + the forge button (1/floor)
public class ForgerKit extends ChampionKit {

	{
		image = ItemSpriteSheet.KIT_FORGER;
	}

	private static final ChampionAbility[] ABILITIES = new ChampionAbility[]{
			MagmaRift.INSTANCE, BellowsBreath.INSTANCE, Onslaught.INSTANCE, GodsCall.INSTANCE
	};

	@Override
	public ChampionAbility[] abilities() {
		return ABILITIES;
	}

	@Override
	public String extraLabel( Hero hero ) {
		return Messages.titleCase(Messages.get(LivingForge.class, "name")) + "  (" + LivingForge.usesLeft(hero) + "/" + LivingForge.maxUses(hero) + ")";
	}

	@Override
	public String extraInfo( Hero hero ) {
		return Messages.get(LivingForge.class, "desc", LivingForge.maxUses(hero));
	}

	@Override
	public int extraIcon() {
		return HeroIcon.FORGER_FORGE;
	}

	@Override
	public boolean extraEnabled( Hero hero ) {
		return LivingForge.usesLeft(hero) > 0;
	}

	@Override
	public void extraAction( Hero hero ) {
		LivingForge.begin(hero);
	}
}
