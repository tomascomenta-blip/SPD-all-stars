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

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

//pack talisman
public class BeastmasterKit extends ChampionKit {

	{
		image = ItemSpriteSheet.KIT_BEASTMASTER;
	}

	private static final ChampionAbility[] ABILITIES = new ChampionAbility[]{
			FrostfireWolves.INSTANCE, VenomVines.INSTANCE, FuzzyBear.INSTANCE, Stampede.INSTANCE
	};

	@Override
	public ChampionAbility[] abilities() {
		return ABILITIES;
	}

	//tame button, <25% hp
	@Override
	public String extraLabel( Hero hero ) {
		return Messages.get(Taming.class, "button");
	}

	@Override
	public String extraInfo( Hero hero ) {
		return Messages.get(Taming.class, "info", Taming.RANGE);
	}

	@Override
	public boolean extraEnabled( Hero hero ) {
		return true;
	}

	@Override
	public void extraAction( final Hero hero ) {
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell == null) return;
				Char ch = Actor.findChar(cell);
				String problem = Taming.problem(hero, ch);
				if (problem != null){
					GLog.w(problem);
					return;
				}
				Taming.tame(hero, (Mob) ch);
			}

			@Override
			public String prompt() {
				return Messages.get(Taming.class, "prompt");
			}
		});
	}
}
