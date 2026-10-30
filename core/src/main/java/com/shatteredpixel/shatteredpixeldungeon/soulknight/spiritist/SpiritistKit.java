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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//skull lantern
public class SpiritistKit extends ChampionKit {

	{
		image = ItemSpriteSheet.KIT_SPIRITIST;
	}

	private static final ChampionAbility[] ABILITIES = new ChampionAbility[]{
			Nightmare.INSTANCE, OmenStone.INSTANCE, RaiseDead.INSTANCE, DanseMacabre.INSTANCE
	};

	@Override
	public ChampionAbility[] abilities() {
		return ABILITIES;
	}

	//ghost scout, costs souls
	@Override
	public String extraLabel( Hero hero ) {
		Souls s = hero.buff(Souls.class);
		return Messages.get(Souls.class, "scout_button", s == null ? 0 : s.souls, Souls.SCOUT);
	}

	@Override
	public String extraInfo( Hero hero ) {
		return Messages.get(Souls.class, "scout_info", Souls.SCOUT, Souls.SCOUT_RANGE, Souls.EMPOWER);
	}

	@Override
	public boolean extraEnabled( Hero hero ) {
		Souls s = hero.buff(Souls.class);
		return s != null && s.souls >= Souls.SCOUT;
	}

	@Override
	public void extraAction( Hero hero ) {
		Souls.scout(hero);
	}
}
