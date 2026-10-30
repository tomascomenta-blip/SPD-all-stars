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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Yog;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;

//yog jellyfish. a bell of violet jelly with a yog eye inside, drifting a little over the ground
//and the water. it opens its tentacles (marked) and lets out a discharge around itself: it numbs
//for a moment and corrupts
public class YogJellyfish extends WildColossus implements Yog {

	{
		size = 3;
		spriteClass = JellySprite.class;

		HP = HT = 360;
		defenseSkill = 20;
		EXP = 30;
		baseSpeed = 0.8f;
		flying = true;

		minDmg = 26; maxDmg = 36; acc = 60;
		armorMin = 0; armorMax = 8;

		properties.add(Property.DEMONIC);
	}

	private static final int DISCHARGE = 0;

	@Override
	public int taintOnHit() {
		return 4;
	}

	@Override
	protected boolean wades() {
		return true;
	}

	//the eye in the bell
	@Override
	public int[][] weakSpots() {
		return new int[][]{{1, 0}};
	}

	@Override
	protected boolean special( Char enemy ) {
		if (reach(enemy.pos) > 2) return false;
		warn(DISCHARGE, ring(2), enemy.pos, 2f);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return new int[]{45, 65};
	}

	@Override
	protected int dust( int kind ) {
		return Speck.BLUE_LIGHT;
	}

	@Override
	protected String blowSound( int kind ) {
		return Assets.Sounds.LIGHTNING;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		Buff.prolong(ch, Paralysis.class, 1f);
		Continent.taint(ch, 5);
	}

	//0-1 idle, 2-5 drifting, 6 tentacles open, 7 lash, 8 discharge
	public static class JellySprite extends WildSprite {

		public JellySprite(){
			super();
			beast(IslesAssets.JELLY, 3, 64, 64, 8, 16);
		}

		@Override
		protected int blow( int kind ) {
			return 8;
		}
	}
}
